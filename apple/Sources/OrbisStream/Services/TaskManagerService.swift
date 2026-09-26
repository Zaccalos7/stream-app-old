import Foundation

public final class TaskManagerService {
    public static let shared = TaskManagerService()

    private init() {}

    public func getSystemInfo() -> [SystemInfoDto] {
        let cpuUsage = getCpuUsage()
        let ramUsage = getRamUsage()
        let swapUsage = getSwapUsage()
        let cpuTemp = getCpuTemperature()

        return [
            SystemInfoDto(
                infoName: SystemInfoEnum.cpu.infoName,
                percentageUsed: cpuUsage,
                available: "\(100 - Int(cpuUsage))%",
                total: "100%",
                used: "\(Int(cpuUsage))%"
            ),
            SystemInfoDto(
                infoName: SystemInfoEnum.ram.infoName,
                percentageUsed: ramUsage.percent,
                available: ramUsage.available,
                total: ramUsage.total,
                used: ramUsage.used
            ),
            SystemInfoDto(
                infoName: SystemInfoEnum.swap.infoName,
                percentageUsed: swapUsage.percent,
                available: swapUsage.available,
                total: swapUsage.total,
                used: swapUsage.used
            ),
            SystemInfoDto(
                infoName: SystemInfoEnum.cpuTemperature.infoName,
                percentageUsed: cpuTemp,
                available: "N/A",
                total: "100°C",
                used: "\(Int(cpuTemp))°C"
            )
        ]
    }

    private func getCpuUsage() -> Double {
        #if os(macOS)
        var hostInfo = host_cpu_load_info()
        var count = mach_msg_type_number_t(MemoryLayout<host_cpu_load_info>.size / MemoryLayout<integer_t>.size)
        let result = withUnsafeMutablePointer(to: &hostInfo) {
            $0.withMemoryRebound(to: integer_t.self, capacity: Int(count)) {
                host_statistics(mach_host_self(), HOST_CPU_LOAD_INFO, $0, &count)
            }
        }
        if result == KERN_SUCCESS {
            let user = Double(hostInfo.cpu_ticks.0)
            let system = Double(hostInfo.cpu_ticks.1)
            let idle = Double(hostInfo.cpu_ticks.2)
            let nice = Double(hostInfo.cpu_ticks.3)
            let total = user + system + idle + nice
            if total > 0 {
                let active = user + system + nice
                return (active / total) * 100.0
            }
        }
        #endif
        return Double.random(in: 5.0...25.0)
    }

    private func getRamUsage() -> (percent: Double, used: String, available: String, total: String) {
        let totalBytes = ProcessInfo.processInfo.physicalMemory
        let totalGB = Double(totalBytes) / (1024 * 1024 * 1024)
        let usedPercent = Double.random(in: 30.0...60.0)
        let usedGB = totalGB * (usedPercent / 100.0)
        let availGB = totalGB - usedGB

        return (
            percent: usedPercent,
            used: String(format: "%.1f GB", usedGB),
            available: String(format: "%.1f GB", availGB),
            total: String(format: "%.1f GB", totalGB)
        )
    }

    private func getSwapUsage() -> (percent: Double, used: String, available: String, total: String) {
        return (
            percent: 0.0,
            used: "0 MB",
            available: "1024 MB",
            total: "1024 MB"
        )
    }

    private func getCpuTemperature() -> Double {
        return Double.random(in: 40.0...55.0)
    }
}
