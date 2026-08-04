use sqlx::SqlitePool;
use crate::db::models::VideoSetting;
use serde::{Deserialize, Serialize};

#[derive(Serialize, Deserialize)]
pub struct VideoSettingsRecord {
    pub id: Option<i32>,
    pub title: Option<String>,
    pub video_codec: Option<i32>,
    pub video_codec_name: Option<String>,
    pub pixel_format: Option<i32>,
    pub video_bitrate: Option<i32>,
    pub video_format: Option<String>,
    pub gop_size: Option<i32>,
    pub is_video_and_audio_setting_active: Option<bool>,
}

pub async fn save_settings_video(pool: &SqlitePool, record: VideoSettingsRecord) -> Result<String, String> {
    sqlx::query(
        "INSERT INTO video_setting (title, video_codec, video_codec_name, pixel_format, video_bitrate, video_format, gop_size, is_video_and_audio_setting_active) VALUES (?, ?, ?, ?, ?, ?, ?, ?)"
    )
    .bind(record.title)
    .bind(record.video_codec)
    .bind(record.video_codec_name)
    .bind(record.pixel_format)
    .bind(record.video_bitrate)
    .bind(record.video_format)
    .bind(record.gop_size.unwrap_or(2))
    .bind(record.is_video_and_audio_setting_active.unwrap_or(false))
    .execute(pool)
    .await
    .map_err(|e| e.to_string())?;

    Ok("video.settings.saved".to_string())
}

pub async fn get_all_video_settings(pool: &SqlitePool) -> Result<Vec<VideoSetting>, String> {
    sqlx::query_as::<_, VideoSetting>("SELECT * FROM video_setting")
        .fetch_all(pool)
        .await
        .map_err(|e| e.to_string())
}

pub async fn link_and_save_settings_video(pool: &SqlitePool, record: VideoSettingsRecord, video_pkid_to_link: i32) -> Result<String, String> {
    let result = sqlx::query(
        "INSERT INTO video_setting (title, video_codec, video_codec_name, pixel_format, video_bitrate, video_format, gop_size, is_video_and_audio_setting_active) VALUES (?, ?, ?, ?, ?, ?, ?, ?)"
    )
    .bind(record.title)
    .bind(record.video_codec)
    .bind(record.video_codec_name)
    .bind(record.pixel_format)
    .bind(record.video_bitrate)
    .bind(record.video_format)
    .bind(record.gop_size.unwrap_or(2))
    .bind(record.is_video_and_audio_setting_active.unwrap_or(false))
    .execute(pool)
    .await
    .map_err(|e| e.to_string())?;

    sqlx::query("UPDATE video SET video_setting_id = ? WHERE pkid = ?")
        .bind(result.last_insert_rowid() as i32)
        .bind(video_pkid_to_link)
        .execute(pool)
        .await
        .map_err(|e| e.to_string())?;

    Ok("success.operations".to_string())
}

pub async fn edit_settings_video(pool: &SqlitePool, id: i32, record: VideoSettingsRecord) -> Result<String, String> {
    sqlx::query(
        "UPDATE video_setting SET title = ?, video_codec = ?, video_codec_name = ?, pixel_format = ?, video_bitrate = ?, video_format = ?, gop_size = ?, is_video_and_audio_setting_active = ? WHERE id = ?"
    )
    .bind(record.title)
    .bind(record.video_codec)
    .bind(record.video_codec_name)
    .bind(record.pixel_format)
    .bind(record.video_bitrate)
    .bind(record.video_format)
    .bind(record.gop_size.unwrap_or(2))
    .bind(record.is_video_and_audio_setting_active.unwrap_or(false))
    .bind(id)
    .execute(pool)
    .await
    .map_err(|e| e.to_string())?;

    Ok("video.settings.modified".to_string())
}

pub async fn delete_video_setting(pool: &SqlitePool, id: i32) -> Result<String, String> {
    sqlx::query("DELETE FROM video_setting WHERE id = ?")
        .bind(id)
        .execute(pool)
        .await
        .map_err(|e| e.to_string())?;

    Ok("delete.successful".to_string())
}
