use sysinfo::System;

#[derive(serde::Serialize)]
pub struct SystemInfoDto {
    pub field: String,
    pub value: i32,
}

pub fn get_all_system_info(sys: &mut System) -> Vec<SystemInfoDto> {
    sys.refresh_all();

    vec![
        SystemInfoDto { field: "CPU".to_string(), value: get_cpu_percent(sys) },
        SystemInfoDto { field: "RAM".to_string(), value: get_ram_percent(sys) },
        SystemInfoDto { field: "SWAP".to_string(), value: get_swap_percent(sys) },
        SystemInfoDto { field: "CPU_TEMPERATURE".to_string(), value: get_cpu_temp() },
    ]
}

pub fn get_cpu_percent(sys: &mut System) -> i32 {
    sys.refresh_cpu_usage();
    std::thread::sleep(std::time::Duration::from_millis(500));
    sys.refresh_cpu_usage();
    sys.global_cpu_usage() as i32
}

pub fn get_ram_percent(sys: &mut System) -> i32 {
    sys.refresh_memory();
    let total = sys.total_memory();
    let used = sys.used_memory();

    if total == 0 { 0 } else { ((used as f64 * 100.0) / total as f64) as i32 }
}

pub fn get_swap_percent(sys: &mut System) -> i32 {
    sys.refresh_memory();
    let total = sys.total_swap();
    let used = sys.used_swap();

    if total == 0 { 0 } else { ((used as f64 * 100.0) / total as f64) as i32 }
}

pub fn get_cpu_temp() -> i32 {
    use sysinfo::Components;
    let components = Components::new_with_refreshed_list();
    for component in &components {
        if component.label().to_lowercase().contains("cpu") {
            if let Some(t) = component.temperature() {
                return t as i32;
            }
        }
    }
    0
}
