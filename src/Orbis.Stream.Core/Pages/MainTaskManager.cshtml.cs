using Microsoft.AspNetCore.Mvc;
using Orbis.Stream.Core.Contracts;
using Orbis.Stream.Core.Services;
using Orbis.Stream.Core.SystemInfo;

namespace Orbis.Stream.Core.Pages;

public sealed class MainTaskManagerModel(SystemInfoService systemInfo) : OrbisPageModel
{
    /// <summary>CPU, RAM, SWAP, CPU temperature, in this order.</summary>
    public IReadOnlyList<SystemInfoResponse> Stats { get; private set; } = [];

    /// <summary>What the machine is: read once, the meters above are the ones that keep moving.</summary>
    public IReadOnlyList<SystemFact> Facts { get; private set; } = [];

    /// <summary>
    /// The value of a counter by its field name. The order of the ported Java list is not something
    /// a page should rely on to pair four numbers with four cards.
    /// </summary>
    public int? Stat(string field) => Stats
        .FirstOrDefault(stat => string.Equals(stat.Field, field, StringComparison.OrdinalIgnoreCase))?
        .Value;

    /// <summary>
    /// Only the meters: what the machine is needs WMI, whose first answer takes from a moment to
    /// about a second, and that wait would be the whole page. The facts come on their own, into a
    /// panel that shows a loading only when the answer is slow enough to be noticed.
    /// </summary>
    public void OnGet() => Stats = systemInfo.GetAllSystemInfo();

    public PartialViewResult OnGetFacts()
    {
        Facts = systemInfo.GetSystemFacts();
        return Partial("_SystemInfo", this);
    }
}
