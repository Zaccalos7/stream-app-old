pub mod models;
use sqlx::{sqlite::SqlitePoolOptions, SqlitePool};
use std::env;

pub async fn init_pool() -> Result<SqlitePool, sqlx::Error> {
    let database_url = env::var("DATABASE_URL").unwrap_or_else(|_| "sqlite:./stream.db".to_string());

    // In a real scenario, you'd want to run migrations here.
    let pool = SqlitePoolOptions::new()
        .max_connections(5)
        .connect(&database_url)
        .await?;

    Ok(pool)
}
