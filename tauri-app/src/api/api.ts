import { invoke } from "@tauri-apps/api/core";

// Video
export const getVideoList = async (page: number, size: number) => {
    return await invoke('get_video_list', { page, size });
};

// Stream
export const startLive = async (record: any) => {
    return await invoke('start_live', { record });
};
export const stopLive = async (videoId: number) => {
    return await invoke('stop_live', { videoId });
};

// Settings
export const saveSettings = async (record: any) => {
    return await invoke('save_settings', { record });
};
export const retrieveSettings = async () => {
    return await invoke('retrieve_settings');
};

// Image
export const uploadImage = async (imageBytes: Uint8Array, filename: string) => {
    return await invoke('upload_image', { imageBytes, filename });
};
export const getImage = async () => {
    return await invoke('get_image');
};

// Task Manager
export const getAllSystemInfo = async () => {
    return await invoke('get_all_system_info');
};
export const getCpuUsage = async () => {
    return await invoke('get_cpu_usage');
};
export const getRamUsage = async () => {
    return await invoke('get_ram_usage');
};
export const getSwapPercent = async () => {
    return await invoke('get_swap_percent');
};
export const getCpuTemp = async () => {
    return await invoke('get_cpu_temp');
};

// Video Settings
export const getAllVideoSettings = async () => {
    return await invoke('get_all_video_settings');
};
export const saveVideoSettings = async (record: any) => {
    return await invoke('save_video_settings', { record });
};
export const linkAndSaveVideoSettings = async (record: any, videoPkidToLink: number) => {
    return await invoke('link_and_save_video_settings', { record, videoPkidToLink });
};
export const editVideoSettings = async (id: number, record: any) => {
    return await invoke('edit_video_settings', { id, record });
};
export const deleteVideoSetting = async (id: number) => {
    return await invoke('delete_video_setting', { id });
};
