namespace Orbis.Stream.Core.Domain;

/// <summary>Port of <c>com.orbis.stream.enums.SystemInfoEnum</c>; the names are part of the API contract.</summary>
public enum SystemInfoField
{
    Cpu,
    Ram,
    Swap,
    CpuTemperature
}

public static class SystemInfoFieldExtensions
{
    public static string ToInfoName(this SystemInfoField field) => field switch
    {
        SystemInfoField.Cpu => "CPU",
        SystemInfoField.Ram => "RAM",
        SystemInfoField.Swap => "SWAP",
        SystemInfoField.CpuTemperature => "TEMPERATURA CPU",
        _ => field.ToString().ToUpperInvariant()
    };
}

/// <summary>Port of <c>com.orbis.stream.enums.VideoExtensionEnum</c>.</summary>
public static class VideoExtensions
{
    private static readonly string[] Supported = ["mp4", "flv", "mov", "webm", "vp9", "mkv"];

    public static IReadOnlyList<string> All => Supported;

    public static bool IsVideoExtensionPresent(string? extension)
    {
        if (string.IsNullOrEmpty(extension))
        {
            return false;
        }

        return Supported.Any(supported => supported.Equals(extension, StringComparison.OrdinalIgnoreCase));
    }
}
