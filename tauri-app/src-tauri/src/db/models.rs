use serde::{Deserialize, Serialize};
use sqlx::FromRow;

#[derive(Debug, Serialize, Deserialize, Clone)]
pub enum LiveStatusEnum {
    OFFLINE,
    LIVE,
    ENDED,
    ERROR,
}

impl From<String> for LiveStatusEnum {
    fn from(s: String) -> Self {
        match s.as_str() {
            "LIVE" => LiveStatusEnum::LIVE,
            "ENDED" => LiveStatusEnum::ENDED,
            "ERROR" => LiveStatusEnum::ERROR,
            _ => LiveStatusEnum::OFFLINE,
        }
    }
}

#[derive(Debug, Serialize, Deserialize, Clone, FromRow)]
pub struct Video {
    pub pkid: i32,
    pub name: String,
    pub video_path: String,
    pub extension: String,
    pub live_status: String,
    pub last_time_stamp_before_stop: i64,
    pub message: Option<String>,
    pub should_be_stop: bool,
    // Store dates as String or i64 timestamp to avoid chrono serialization issues if not needed natively by frontend
    pub start_date_live: Option<String>,
    pub channel_name: String,
    pub video_live_history_id: Option<i32>,
    pub video_setting_id: Option<i32>,
}

#[derive(Debug, Serialize, Deserialize, Clone, FromRow)]
pub struct VideoLiveHistory {
    pub pkid: i32,
    pub folder_of_video_to_stream: String,
    pub local_date_time_start_live: String,
    pub stream_url: String,
    pub stream_key: String,
    pub platform_stream_name: String,
    pub user_name: Option<String>,
}

#[derive(Debug, Serialize, Deserialize, Clone, FromRow)]
pub struct VideoSetting {
    pub id: i32,
    pub title: Option<String>,
    pub video_codec: Option<i32>,
    pub video_codec_name: Option<String>,
    pub pixel_format: Option<i32>,
    pub video_bitrate: Option<i32>,
    pub video_format: Option<String>,
    pub last_modified: Option<String>,
    pub gop_size: i32,
    pub is_video_and_audio_setting_active: bool,
    pub audio_setting_id: Option<i32>,
}

#[derive(Debug, Serialize, Deserialize, Clone, FromRow)]
pub struct VideoSettingsOption {
    pub id: i32,
    pub key: String,
    pub value: String,
    pub video_setting_id: i32,
}

#[derive(Debug, Serialize, Deserialize, Clone, FromRow)]
pub struct AudioSetting {
    pub id: i32,
    pub audio_codec: Option<i32>,
    pub audio_bitrate: Option<i32>,
}

#[derive(Debug, Serialize, Deserialize, Clone, FromRow)]
pub struct Setting {
    pub id: i32,
    pub stream_url: String,
    pub stream_key: String,
    pub platform_stream_name: Option<String>,
    pub description: Option<String>,
    pub video_folder: String,
    pub is_active: bool,
    pub channel_name: String,
}
