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

    public void OnGet()
    {
        Stats = systemInfo.GetAllSystemInfo();
        Facts = systemInfo.GetSystemFacts();
    }

    /// <summary>The refresh of the meters every five seconds, without the facts it would repeat.</summary>
    public PartialViewResult OnGetStats()
    {
        Stats = systemInfo.GetAllSystemInfo();
        return Partial("_Stats", this);
    }
}
