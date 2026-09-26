using Orbis.Stream.Core.Domain;

namespace Orbis.Stream.Core.Pages;

/// <summary>Pager of the lists: a window of five pages around the current one, as the React tables had.</summary>
public sealed record PagerModel(int Current, int Total, Func<int, string> Href)
{
    public IEnumerable<int> Window
    {
        get
        {
            var first = Math.Clamp(Current - 2, 0, Math.Max(Total - 5, 0));
            return Enumerable.Range(first, Math.Min(5, Total));
        }
    }
}

public static class LiveStatusView
{
    public static string Key(LiveStatus? status) => status switch
    {
        LiveStatus.Live => "liveStatusLive",
        LiveStatus.Ended => "liveStatusEnded",
        LiveStatus.Error => "liveStatusError",
        LiveStatus.Stopped => "liveStatusStopped",
        _ => "liveStatusOffline"
    };

    public static string Css(LiveStatus? status) => status switch
    {
        LiveStatus.Live => "live",
        LiveStatus.Ended => "ended",
        LiveStatus.Error => "error",
        LiveStatus.Stopped => "stopped",
        _ => string.Empty
    };

    public static string Date(DateTime? value, string format) =>
        value?.ToString(format, System.Globalization.CultureInfo.InvariantCulture) ?? "N/A";
}
