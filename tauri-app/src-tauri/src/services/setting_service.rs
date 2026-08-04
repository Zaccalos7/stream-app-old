use sqlx::SqlitePool;
use crate::db::models::Setting;
use serde::{Deserialize, Serialize};

#[derive(Serialize, Deserialize)]
pub struct SettingRecord {
    pub id: Option<i32>,
    pub stream_url: String,
    pub stream_key: String,
    pub platform_stream_name: Option<String>,
    pub description: Option<String>,
    pub video_folder: String,
    pub is_active: Option<bool>,
    pub channel_name: String,
}

pub async fn add_new_configuration(pool: &SqlitePool, record: SettingRecord) -> Result<String, String> {
    sqlx::query(
        "INSERT INTO setting (stream_url, stream_key, platform_stream_name, description, video_folder, is_active, channel_name) VALUES (?, ?, ?, ?, ?, ?, ?)"
    )
    .bind(record.stream_url)
    .bind(record.stream_key)
    .bind(record.platform_stream_name)
    .bind(record.description)
    .bind(record.video_folder)
    .bind(record.is_active.unwrap_or(false))
    .bind(record.channel_name)
    .execute(pool)
    .await
    .map_err(|e| e.to_string())?;

    Ok("setting.created".to_string())
}

pub async fn retrieve_settings(pool: &SqlitePool) -> Result<Vec<Setting>, String> {
    sqlx::query_as::<_, Setting>("SELECT * FROM setting")
        .fetch_all(pool)
        .await
        .map_err(|e| e.to_string())
}
