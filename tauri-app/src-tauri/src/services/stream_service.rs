use sqlx::SqlitePool;
use std::collections::HashMap;
use std::sync::Arc;
use tokio::sync::Mutex;
use tauri_plugin_shell::ShellExt;
use tauri::{AppHandle, Manager};
use tokio::process::Command;
use std::process::Stdio;
use std::time::Duration;
use chrono::Utc;
use crate::db::models::{Video, VideoLiveHistory, VideoSetting};

pub struct StreamState {
    pub active_streams: Arc<Mutex<HashMap<i32, (tokio::task::JoinHandle<()>, Arc<Mutex<Option<tokio::process::Child>>>)>>>,
}

#[derive(serde::Deserialize, Debug)]
pub struct StartLiveRecord {
    pub channel_name: String,
    pub platform_stream_name: String,
    pub video_path: String,
    pub stream_key: String,
    pub stream_url: String,
    pub video_settings_record: Option<VideoSettingRecordDto>,
}

#[derive(serde::Deserialize, Debug)]
pub struct VideoSettingRecordDto {
    pub id: Option<i32>,
    pub video_format: Option<String>,
    pub video_codec: Option<i32>,
    pub video_codec_name: Option<String>,
    pub pixel_format: Option<i32>,
    pub video_bitrate: Option<i32>,
    pub gop_size: i32,
    pub audio_setting: Option<AudioSettingDto>,
}

#[derive(serde::Deserialize, Debug)]
pub struct AudioSettingDto {
    pub audio_codec: Option<i32>,
    pub audio_bitrate: Option<i32>,
}


pub async fn start_live(
    app: &AppHandle,
    pool: &SqlitePool,
    record: StartLiveRecord
) -> Result<String, String> {

    let channel_name = record.channel_name;
    let platform_stream_name = record.platform_stream_name;
    let video_path = record.video_path;
    let stream_key = record.stream_key;
    let stream_url = record.stream_url;

    // 1. Check if alive already streaming
    let count: (i64,) = sqlx::query_as(
        "SELECT COUNT(*) FROM video v JOIN video_live_history h ON v.video_live_history_id = h.pkid
         WHERE v.live_status = 'LIVE' AND v.channel_name = ? AND h.platform_stream_name = ?"
    )
    .bind(&channel_name)
    .bind(&platform_stream_name)
    .fetch_one(pool)
    .await
    .map_err(|e| e.to_string())?;

    if count.0 > 0 {
        return Err(format!("channel.has.already.a.live.active: {}", channel_name));
    }

    let streaming_url = if stream_url.ends_with('/') {
        format!("{}{}", stream_url, stream_key)
    } else {
        format!("{}/{}", stream_url, stream_key)
    };

    // 2. Save Video Live History
    let time_start_live = Utc::now().naive_utc();

    let history_id = sqlx::query(
        "INSERT INTO video_live_history (folder_of_video_to_stream, local_date_time_start_live, stream_url, stream_key, platform_stream_name, user_name)
         VALUES (?, ?, ?, ?, ?, 'Mario')"
    )
    .bind(&video_path)
    .bind(time_start_live)
    .bind(&stream_url)
    .bind(&stream_key)
    .bind(&platform_stream_name)
    .execute(pool)
    .await
    .map_err(|e| e.to_string())?
    .last_insert_rowid() as i32;

    // 3. Save Video
    let file_name = std::path::Path::new(&video_path)
        .file_name()
        .and_then(|n| n.to_str())
        .unwrap_or("video.mp4")
        .to_string();

    let extension = std::path::Path::new(&video_path)
        .extension()
        .and_then(|e| e.to_str())
        .unwrap_or("mp4")
        .to_string();

    let video_id = sqlx::query(
        "INSERT INTO video (name, video_path, extension, live_status, last_time_stamp_before_stop, should_be_stop, start_date_live, channel_name, video_live_history_id)
         VALUES (?, ?, ?, 'LIVE', 0, 0, ?, ?, ?)"
    )
    .bind(&file_name)
    .bind(&video_path)
    .bind(&extension)
    .bind(time_start_live)
    .bind(&channel_name)
    .bind(history_id)
    .execute(pool)
    .await
    .map_err(|e| e.to_string())?
    .last_insert_rowid() as i32;


    // 4. Construct FFmpeg command based on settings
    let mut args = vec![
        "-re".to_string(),
        "-i".to_string(), video_path.clone(),
    ];

    if let Some(settings) = record.video_settings_record {
        if let Some(codec) = settings.video_codec_name {
            args.push("-c:v".to_string());
            args.push(codec);
        } else {
            args.push("-c:v".to_string());
            args.push("libx264".to_string());
        }

        if let Some(fmt) = settings.video_format {
            args.push("-f".to_string());
            args.push(fmt);
        } else {
            args.push("-f".to_string());
            args.push("flv".to_string());
        }

        if let Some(bitrate) = settings.video_bitrate {
            args.push("-b:v".to_string());
            args.push(format!("{}k", bitrate));
        }

        let gop = settings.gop_size * 30; // Assuming ~30fps for gop calculation
        args.push("-g".to_string());
        args.push(gop.to_string());

        if let Some(audio) = settings.audio_setting {
             args.push("-c:a".to_string());
             args.push("aac".to_string());
             if let Some(ab) = audio.audio_bitrate {
                 args.push("-b:a".to_string());
                 args.push(format!("{}k", ab));
             }
        }
    } else {
        // Defaults
        args.extend(vec![
            "-c:v".to_string(), "libx264".to_string(),
            "-preset".to_string(), "veryfast".to_string(),
            "-maxrate".to_string(), "3000k".to_string(),
            "-bufsize".to_string(), "6000k".to_string(),
            "-pix_fmt".to_string(), "yuv420p".to_string(),
            "-g".to_string(), "50".to_string(),
            "-c:a".to_string(), "aac".to_string(),
            "-b:a".to_string(), "160k".to_string(),
            "-ar".to_string(), "44100".to_string(),
            "-f".to_string(), "flv".to_string(),
        ]);
    }

    args.push(streaming_url);

    // 5. Spawn FFmpeg natively using tokio
    let child = Command::new("ffmpeg")
        .args(&args)
        .stdin(Stdio::null())
        .stdout(Stdio::piped())
        .stderr(Stdio::piped())
        .spawn()
        .map_err(|e| format!("Failed to start ffmpeg: {}", e))?;

    let child_arc = Arc::new(Mutex::new(Some(child)));
    let child_arc_clone = Arc::clone(&child_arc);
    let pool_clone = pool.clone();

    // 6. Spawn Monitor Thread (Replicating Java's 50-frame check)
    let monitor_handle = tokio::spawn(async move {
        let mut interval = tokio::time::interval(Duration::from_millis(1600)); // Roughly every 50 frames at 30fps

        loop {
            interval.tick().await;

            // Check if process exited
            let mut child_lock = child_arc_clone.lock().await;
            if let Some(child) = child_lock.as_mut() {
                if let Ok(Some(status)) = child.try_wait() {
                    // Process finished naturally or crashed
                    let _ = sqlx::query("UPDATE video SET live_status = 'ENDED', message = 'video.live.ended' WHERE pkid = ?")
                        .bind(video_id)
                        .execute(&pool_clone)
                        .await;
                    break;
                }
            } else {
                break; // child was dropped/killed
            }

            // Check DB for should_be_stop flag
            if let Ok(video) = sqlx::query_as::<_, Video>("SELECT * FROM video WHERE pkid = ?")
                .bind(video_id)
                .fetch_one(&pool_clone)
                .await
            {
                if video.should_be_stop {
                    if let Some(mut child) = child_lock.take() {
                        let _ = child.kill().await;
                    }
                    let _ = sqlx::query("UPDATE video SET live_status = 'OFFLINE', should_be_stop = 0, message = 'live.stopped' WHERE pkid = ?")
                        .bind(video_id)
                        .execute(&pool_clone)
                        .await;
                    break;
                }
            }
        }
    });

    let state = app.state::<tauri::State<'_, StreamState>>();
    if let Some(stream_state) = app.try_state::<StreamState>() {
        stream_state.active_streams.lock().await.insert(video_id, (monitor_handle, child_arc));
    }

    Ok("live.started".to_string())
}

pub async fn stop_live(
    app: &AppHandle,
    pool: &SqlitePool,
    video_id: i32
) -> Result<String, String> {

    if let Some(state) = app.try_state::<StreamState>() {
        let mut streams = state.active_streams.lock().await;

        if let Some((monitor, child_arc)) = streams.remove(&video_id) {
            if let Some(mut child) = child_arc.lock().await.take() {
                let _ = child.kill().await;
            }
            monitor.abort();
        }
    }

    sqlx::query("UPDATE video SET live_status = 'OFFLINE', should_be_stop = 0 WHERE pkid = ?")
        .bind(video_id)
        .execute(pool)
        .await
        .map_err(|e| e.to_string())?;

    Ok("live.stopped".to_string())
}
