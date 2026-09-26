using Microsoft.AspNetCore.Mvc;
using Orbis.Stream.Core.Contracts;
using Orbis.Stream.Core.Services;

namespace Orbis.Stream.Core.Pages;

public sealed class MainTaskManagerModel(SystemInfoService systemInfo) : OrbisPageModel
{
    /// <summary>CPU, RAM, SWAP, CPU temperature, in this order.</summary>
    public IReadOnlyList<SystemInfoResponse> Stats { get; private set; } = [];

    public void OnGet() => Stats = systemInfo.GetAllSystemInfo();

    public PartialViewResult OnGetStats()
    {
        OnGet();
        return Partial("_Stats", this);
    }
}
