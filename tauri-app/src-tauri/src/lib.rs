pub mod db;
pub mod services;

use std::sync::Arc;
use std::collections::HashMap;
use tokio::sync::Mutex;
use sqlx::SqlitePool;
use tauri::{State, AppHandle, Manager};
use sysinfo::System;
use services::{video_service, stream_service, setting_service, image_service, task_manager_info_service, video_setting_service};

pub struct AppState {
    pub db: Arc<Mutex<SqlitePool>>,
    pub sys: Arc<Mutex<System>>,
}

// ------------------------------------
// Video Commands
// ------------------------------------

#[tauri::command]
async fn get_video_list(state: State<'_, AppState>, page: i64, size: i64) -> Result<String, String> {
    let pool = state.db.lock().await;
    match video_service::get_video_list(&pool, page, size).await {
        Ok(data) => Ok(serde_json::to_string(&data).unwrap()),
        Err(e) => Err(e.to_string()),
    }
}

// ------------------------------------
// Stream Commands
// ------------------------------------

#[tauri::command]
async fn start_live(app: AppHandle, state: State<'_, AppState>, record: stream_service::StartLiveRecord) -> Result<String, String> {
    let pool = state.db.lock().await;
    stream_service::start_live(&app, &pool, record).await
}

#[tauri::command]
async fn stop_live(app: AppHandle, state: State<'_, AppState>, video_id: i32) -> Result<String, String> {
    let pool = state.db.lock().await;
    stream_service::stop_live(&app, &pool, video_id).await
}

// ------------------------------------
// Setting Commands
// ------------------------------------

#[tauri::command]
async fn save_settings(state: State<'_, AppState>, record: setting_service::SettingRecord) -> Result<String, String> {
    let pool = state.db.lock().await;
    setting_service::add_new_configuration(&pool, record).await
}

#[tauri::command]
async fn retrieve_settings(state: State<'_, AppState>) -> Result<String, String> {
    let pool = state.db.lock().await;
    match setting_service::retrieve_settings(&pool).await {
        Ok(data) => Ok(serde_json::to_string(&data).unwrap()),
        Err(e) => Err(e),
    }
}

// ------------------------------------
// Image Commands
// ------------------------------------

#[tauri::command]
async fn upload_image(image_bytes: Vec<u8>, filename: String) -> Result<String, String> {
    image_service::save_image(image_bytes, &filename)
}

#[tauri::command]
async fn get_image() -> Result<String, String> {
    image_service::load_image()
}

// ------------------------------------
// Task Manager Commands
// ------------------------------------

#[tauri::command]
async fn get_all_system_info(state: State<'_, AppState>) -> Result<String, String> {
    let mut sys = state.sys.lock().await;
    let data = task_manager_info_service::get_all_system_info(&mut sys);
    Ok(serde_json::to_string(&data).unwrap())
}

#[tauri::command]
async fn get_cpu_usage(state: State<'_, AppState>) -> Result<i32, String> {
    let mut sys = state.sys.lock().await;
    Ok(task_manager_info_service::get_cpu_percent(&mut sys))
}

#[tauri::command]
async fn get_ram_usage(state: State<'_, AppState>) -> Result<i32, String> {
    let mut sys = state.sys.lock().await;
    Ok(task_manager_info_service::get_ram_percent(&mut sys))
}

#[tauri::command]
async fn get_swap_percent(state: State<'_, AppState>) -> Result<i32, String> {
    let mut sys = state.sys.lock().await;
    Ok(task_manager_info_service::get_swap_percent(&mut sys))
}

#[tauri::command]
async fn get_cpu_temp() -> Result<i32, String> {
    Ok(task_manager_info_service::get_cpu_temp())
}

// ------------------------------------
// Video Setting Commands
// ------------------------------------

#[tauri::command]
async fn get_all_video_settings(state: State<'_, AppState>) -> Result<String, String> {
    let pool = state.db.lock().await;
    match video_setting_service::get_all_video_settings(&pool).await {
        Ok(data) => Ok(serde_json::to_string(&data).unwrap()),
        Err(e) => Err(e),
    }
}

#[tauri::command]
async fn save_video_settings(state: State<'_, AppState>, record: video_setting_service::VideoSettingsRecord) -> Result<String, String> {
    let pool = state.db.lock().await;
    video_setting_service::save_settings_video(&pool, record).await
}

#[tauri::command]
async fn link_and_save_video_settings(state: State<'_, AppState>, record: video_setting_service::VideoSettingsRecord, video_pkid_to_link: i32) -> Result<String, String> {
    let pool = state.db.lock().await;
    video_setting_service::link_and_save_settings_video(&pool, record, video_pkid_to_link).await
}

#[tauri::command]
async fn edit_video_settings(state: State<'_, AppState>, id: i32, record: video_setting_service::VideoSettingsRecord) -> Result<String, String> {
    let pool = state.db.lock().await;
    video_setting_service::edit_settings_video(&pool, id, record).await
}

#[tauri::command]
async fn delete_video_setting(state: State<'_, AppState>, id: i32) -> Result<String, String> {
    let pool = state.db.lock().await;
    video_setting_service::delete_video_setting(&pool, id).await
}


#[cfg_attr(mobile, tauri::mobile_entry_point)]
pub fn run() {
    let rt = tokio::runtime::Runtime::new().unwrap();
    let pool = rt.block_on(async {
        db::init_pool().await.unwrap_or_else(|_| panic!("Failed to init db"))
    });

    let sys = System::new_all();

    tauri::Builder::default()
        .manage(AppState {
            db: Arc::new(Mutex::new(pool.clone())),
            sys: Arc::new(Mutex::new(sys)),
        })
        .manage(stream_service::StreamState {
            active_streams: Arc::new(Mutex::new(HashMap::new()))
        })
        .plugin(tauri_plugin_opener::init())
        .plugin(tauri_plugin_shell::init())
        .plugin(tauri_plugin_sql::Builder::new().build())
        .invoke_handler(tauri::generate_handler![
            get_video_list,
            start_live,
            stop_live,
            save_settings,
            retrieve_settings,
            upload_image,
            get_image,
            get_all_system_info,
            get_cpu_usage,
            get_ram_usage,
            get_swap_percent,
            get_cpu_temp,
            get_all_video_settings,
            save_video_settings,
            link_and_save_video_settings,
            edit_video_settings,
            delete_video_setting
        ])
        .run(tauri::generate_context!())
        .expect("error while running tauri application");
}
