use std::fs;
use std::path::{Path, PathBuf};
use base64::{engine::general_purpose::STANDARD as BASE64, Engine};

const UPLOAD_DIR: &str = "images";
const IMAGE_NAME: &str = "HOME";

pub fn save_image(image_bytes: Vec<u8>, filename: &str) -> Result<String, String> {
    let upload_path = Path::new(UPLOAD_DIR);

    if !upload_path.exists() {
        fs::create_dir_all(upload_path).map_err(|e| e.to_string())?;
    }

    // Get extension
    let extension = Path::new(filename)
        .extension()
        .and_then(|ext| ext.to_str())
        .unwrap_or("png");

    let file_path = upload_path.join(format!("{}.{}", IMAGE_NAME, extension));

    // Delete previous if exists
    if let Ok(mut entries) = fs::read_dir(upload_path) {
        if let Some(Ok(entry)) = entries.next() {
            let _ = fs::remove_file(entry.path());
        }
    }

    fs::write(&file_path, image_bytes).map_err(|e| e.to_string())?;

    Ok("image.saved".to_string())
}

pub fn load_image() -> Result<String, String> {
    let folder = Path::new(UPLOAD_DIR);

    if !folder.exists() || !folder.is_dir() {
        return Err("folder.not.found".to_string());
    }

    if let Ok(mut entries) = fs::read_dir(folder) {
        if let Some(Ok(entry)) = entries.next() {
            let bytes = fs::read(entry.path()).map_err(|e| e.to_string())?;
            let b64 = BASE64.encode(&bytes);

            let ext = entry.path().extension().and_then(|e| e.to_str()).unwrap_or("png").to_string();
            let mime = match ext.as_str() {
                "jpg" | "jpeg" => "image/jpeg",
                "png" => "image/png",
                "gif" => "image/gif",
                "svg" => "image/svg+xml",
                "webp" => "image/webp",
                _ => "application/octet-stream",
            };

            return Ok(format!("data:{};base64,{}", mime, b64));
        }
    }

    Err("resource.not.found".to_string())
}
