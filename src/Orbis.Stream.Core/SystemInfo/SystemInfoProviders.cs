using System.Runtime.InteropServices;

namespace Orbis.Stream.Core.SystemInfo;

/// <summary>
/// Port of the OSHI calls behind <c>TaskManagerInfoComponent</c>. The contract (percentages as
/// integers, <c>-1</c> when a value is not available) is preserved for the React build.
/// </summary>
public interface ISystemInfoProvider
{
    int GetCpuPercent();

    int GetRamPercent();

    int GetSwapPercent();

    int GetCpuTemperature();
}

public static class SystemInfoProviderFactory
{
    public const int NotAvailable = -1;

    public static ISystemInfoProvider CreateDefault() =>
        OperatingSystem.IsWindows() ? new WindowsSystemInfoProvider() : new PortableSystemInfoProvider();
}

/// <summary>Windows 11 implementation: kernel32 counters plus the ACPI thermal zone through WMI.</summary>
public sealed class WindowsSystemInfoProvider : ISystemInfoProvider
{
    private const int CpuSampleDelayMilliseconds = 500;

    public int GetCpuPercent()
    {
        if (!TryReadSystemTimes(out var idle, out var kernel, out var user))
        {
            return 0;
        }

        Thread.Sleep(CpuSampleDelayMilliseconds);

        if (!TryReadSystemTimes(out var idleAfter, out var kernelAfter, out var userAfter))
        {
            return 0;
        }

        var totalDelta = (kernelAfter - kernel) + (userAfter - user);
        if (totalDelta <= 0)
        {
            return 0;
        }

        var idleDelta = idleAfter - idle;
        var load = (totalDelta - idleDelta) * 100.0 / totalDelta;
        return (int)load;
    }

    public int GetRamPercent()
    {
        if (!TryGetMemoryStatus(out var memory))
        {
            return 0;
        }

        var total = memory.TotalPhys;
        var available = memory.AvailablePhys;
        if (total <= 0)
        {
            return 0;
        }

        var used = total - available;
        return (int)((used * 100.0) / total);
    }

    public int GetSwapPercent()
    {
        if (!TryGetMemoryStatus(out var memory))
        {
            return 0;
        }

        var total = memory.TotalPageFile;
        if (total <= 0)
        {
            return 0;
        }

        var used = total - memory.AvailablePageFile;
        if (used < 0)
        {
            used = 0;
        }

        return (int)((used * 100.0) / total);
    }

    public int GetCpuTemperature()
    {
        if (!OperatingSystem.IsWindows())
        {
            return SystemInfoProviderFactory.NotAvailable;
        }

        try
        {
            using var searcher = new System.Management.ManagementObjectSearcher(
                "SELECT CurrentTemperature FROM MSAcpi_ThermalZoneTemperature");
            using var collection = searcher.Get();
            foreach (System.Management.ManagementBaseObject item in collection)
            {
                using (item)
                {
                    if (item["CurrentTemperature"] is not ulong rawTemperature)
                    {
                        continue;
                    }

                    // The ACPI thermal zone reports tenths of a kelvin.
                    var celsius = (rawTemperature / 10.0) - 273.15;
                    return (int)Math.Round(celsius);
                }
            }
        }
        catch (Exception)
        {
            // Temperature is optional: OSHI returned NaN (-1 here) when the sensor is missing.
        }

        return SystemInfoProviderFactory.NotAvailable;
    }

    private static bool TryReadSystemTimes(out ulong idle, out ulong kernel, out ulong user)
    {
        idle = 0;
        kernel = 0;
        user = 0;

        if (!GetSystemTimes(out idle, out kernel, out user))
        {
            return false;
        }

        return true;
    }

    [DllImport("kernel32.dll", SetLastError = true)]
    private static extern bool GetSystemTimes(out ulong idleTime, out ulong kernelTime, out ulong userTime);

    [StructLayout(LayoutKind.Sequential, CharSet = CharSet.Auto)]
    private struct MemoryStatusEx
    {
        public uint Length;
        public uint MemoryLoad;
        public ulong TotalPhys;
        public ulong AvailablePhys;
        public ulong TotalPageFile;
        public ulong AvailablePageFile;
        public ulong TotalVirtual;
        public ulong AvailableVirtual;
        public ulong AvailableExtendedVirtual;
    }

    [DllImport("kernel32.dll", SetLastError = true)]
    [return: MarshalAs(UnmanagedType.Bool)]
    private static extern bool GlobalMemoryStatusEx(ref MemoryStatusEx buffer);

    private static bool TryGetMemoryStatus(out MemoryStatusEx status)
    {
        status = default;
        status.Length = (uint)Marshal.SizeOf<MemoryStatusEx>();
        return GlobalMemoryStatusEx(ref status);
    }
}

/// <summary>
/// Linux/macOS implementation so the server and the test-suite can run on any developer machine;
/// the Windows build always uses <see cref="WindowsSystemInfoProvider"/>.
/// </summary>
public sealed class PortableSystemInfoProvider : ISystemInfoProvider
{
    private const int CpuSampleDelayMilliseconds = 500;

    public int GetCpuPercent()
    {
        if (!TryReadLinuxCpuTimes(out var idle, out var total))
        {
            return 0;
        }

        Thread.Sleep(CpuSampleDelayMilliseconds);

        if (!TryReadLinuxCpuTimes(out var idleAfter, out var totalAfter))
        {
            return 0;
        }

        var totalDelta = totalAfter - total;
        if (totalDelta <= 0)
        {
            return 0;
        }

        var idleDelta = idleAfter - idle;
        return (int)(((totalDelta - idleDelta) * 100.0) / totalDelta);
    }

    public int GetRamPercent()
    {
        if (!TryReadLinuxMemory(out var totalKilobytes, out var availableKilobytes))
        {
            return 0;
        }

        if (totalKilobytes <= 0)
        {
            return 0;
        }

        var used = totalKilobytes - availableKilobytes;
        return (int)((used * 100.0) / totalKilobytes);
    }

    public int GetSwapPercent()
    {
        if (!TryReadLinuxMemory(out var totalKilobytes, out var availableKilobytes, swap: true))
        {
            return 0;
        }

        if (totalKilobytes <= 0)
        {
            return 0;
        }

        var used = totalKilobytes - availableKilobytes;
        if (used < 0)
        {
            used = 0;
        }

        return (int)((used * 100.0) / totalKilobytes);
    }

    public int GetCpuTemperature()
    {
        // Only the thermal zones of the top level are read: the entries below them are symlinks to
        // the device tree, and a recursive enumeration walks the whole sysfs graph.
        var candidates = new List<string>();
        if (Directory.Exists("/sys/class/thermal"))
        {
            foreach (var zone in Directory.EnumerateDirectories("/sys/class/thermal", "thermal_zone*"))
            {
                candidates.Add(Path.Combine(zone, "temp"));
            }
        }

        foreach (var candidate in candidates)
        {
            try
            {
                var raw = File.ReadAllText(candidate).Trim();
                if (long.TryParse(raw, out var milliCelsius) && milliCelsius > 0)
                {
                    return (int)(milliCelsius / 1000);
                }
            }
            catch (Exception exception) when (exception is IOException or UnauthorizedAccessException)
            {
                // Ignore unreadable sensors.
            }
        }

        return SystemInfoProviderFactory.NotAvailable;
    }

    private static bool TryReadLinuxCpuTimes(out ulong idle, out ulong total)
    {
        idle = 0;
        total = 0;
        try
        {
            var line = File.ReadLines("/proc/stat").FirstOrDefault() ?? string.Empty;
            var parts = line.Split(' ', StringSplitOptions.RemoveEmptyEntries);
            if (parts.Length < 5 || parts[0] != "cpu")
            {
                return false;
            }

            for (var index = 1; index < parts.Length; index++)
            {
                if (!ulong.TryParse(parts[index], out var value))
                {
                    continue;
                }

                total += value;
                if (index is 4 or 5)
                {
                    idle += value;
                }
            }

            return total > 0;
        }
        catch (IOException)
        {
            return false;
        }
    }

    private static bool TryReadLinuxMemory(out long totalKilobytes, out long availableKilobytes, bool swap = false)
    {
        totalKilobytes = 0;
        availableKilobytes = 0;
        var totalKey = swap ? "SwapTotal" : "MemTotal";
        var availableKey = swap ? "SwapFree" : "MemAvailable";

        try
        {
            foreach (var line in File.ReadLines("/proc/meminfo"))
            {
                if (line.StartsWith(totalKey, StringComparison.Ordinal))
                {
                    totalKilobytes = ParseKilobytes(line);
                }
                else if (line.StartsWith(availableKey, StringComparison.Ordinal))
                {
                    availableKilobytes = ParseKilobytes(line);
                }
            }

            return totalKilobytes > 0;
        }
        catch (IOException)
        {
            return false;
        }
    }

    private static long ParseKilobytes(string line)
    {
        var parts = line.Split(' ', StringSplitOptions.RemoveEmptyEntries);
        return parts.Length >= 2 && long.TryParse(parts[1], out var value) ? value : 0;
    }
}
