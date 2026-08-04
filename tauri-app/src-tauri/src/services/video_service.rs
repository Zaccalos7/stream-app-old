use sqlx::SqlitePool;
use crate::db::models::Video;
use serde::{Deserialize, Serialize};

#[derive(Serialize)]
pub struct Page<T> {
    pub content: Vec<T>,
    pub total_elements: i64,
}

pub async fn get_video_list(pool: &SqlitePool, page: i64, size: i64) -> Result<Page<Video>, String> {
    let offset = (page - 1) * size;

    let videos = sqlx::query_as::<_, Video>(
        "SELECT * FROM video ORDER BY pkid DESC LIMIT ? OFFSET ?"
    )
    .bind(size)
    .bind(offset)
    .fetch_all(pool)
    .await.map_err(|e| e.to_string())?;

    let count: (i64,) = sqlx::query_as(
        "SELECT COUNT(*) FROM video"
    )
    .fetch_one(pool)
    .await.map_err(|e| e.to_string())?;

    Ok(Page {
        content: videos,
        total_elements: count.0,
    })
}
