using Microsoft.AspNetCore.Builder;
using Microsoft.AspNetCore.Http;
using Microsoft.AspNetCore.Routing;
using Orbis.Stream.Core.Contracts;
using Orbis.Stream.Core.Data;
using Orbis.Stream.Core.Http;
using Orbis.Stream.Core.I18n;
using Orbis.Stream.Core.Services;

namespace Orbis.Stream.Core.Hosting;

/// <summary>
/// Minimal API translation of the five Spring controllers, keeping the same routes, the same
/// status codes and the same JSON payloads.
/// </summary>
public static class OrbisEndpoints
{
    public static IEndpointRouteBuilder MapOrbisEndpoints(this IEndpointRouteBuilder app)
    {
        ArgumentNullException.ThrowIfNull(app);

        MapLive(app);
        MapVideo(app);
        MapSettings(app);
        MapVideoSettings(app);
        MapImage(app);
        MapTaskManager(app);
        return app;
    }

    private static void MapLive(IEndpointRouteBuilder app)
    {
        var group = app.MapGroup("/live");

        group.MapPost("/start-live", (
            StartLiveRequest? request,
            StreamingService service,
            RequestValidator validator) =>
        {
            validator.RequireStartLive(request);
            return AsResult(service.StartLive(request!));
        });

        group.MapPost("/start-video-live", (
            VideoRequest? request,
            StreamingService service,
            RequestValidator validator) =>
        {
            validator.RequireVideo(request);
            return AsResult(service.StartVideo(request!));
        });

        group.MapPut("/stop-live", (
            int videoLivePkid,
            StreamingService service,
            ResponseFactory responses,
            Localizer localizer) =>
        {
            service.StopVideoStreamingByPkid(videoLivePkid);
            localizer.PrintMessage("live.stopped");
            return AsResult(responses.Build("live.stopped", StatusCodes.Status200OK));
        });
    }

    private static void MapVideo(IEndpointRouteBuilder app)
    {
        var group = app.MapGroup("/video");

        group.MapGet("/getPage", (HttpContext context, VideoService service) => service.GetVideoList(
            PageRequest.Parse(context.Request.Query, 20, "pkid", true)));

        group.MapGet("/getAllVideo", (HttpContext context, VideoService service) =>
        {
            var query = context.Request.Query;
            var filters = Filters(query, "page", "size", "sort", "platform", "lang");
            var page = PageRequest.Parse(query, 4, "startDateLive", true);
            return Results.Ok(service.GetAllVideoList(filters, page));
        });

        group.MapGet("/getAllChannelWithVideoLive", (VideoService service) => Results.Ok(service.GetAllChannelOnline()));

        group.MapPut("/unlockVideo", (int videoKey, VideoService service) => AsResult(service.UnlockVideo(videoKey)));
    }

    private static void MapSettings(IEndpointRouteBuilder app)
    {
        var group = app.MapGroup("/settings");

        group.MapPost("/save", (SettingRequest? request, SettingService service, RequestValidator validator) =>
        {
            validator.RequireSetting(request);
            return AsResult(service.AddNewConfiguration(request!));
        });

        group.MapPut("/change", (int id, SettingRequest? request, SettingService service, RequestValidator validator) =>
        {
            validator.RequireSetting(request);
            return AsResult(service.ModifySetting(id, request!));
        });

        group.MapGet("/retrive", (HttpContext context, SettingService service) =>
            Results.Ok(service.RetrieveSettings(Filters(context.Request.Query, "lang"))));

        group.MapGet("/retrive-channels", (HttpContext context, SettingService service) =>
            Results.Ok(service.RetrieveChannel(Filters(context.Request.Query, "lang"))));

        group.MapGet("/retrive-directories-path", (HttpContext context, SettingService service) =>
            Results.Ok(service.RetrieveDirectoriesSettingsPath(Filters(context.Request.Query, "lang"))));

        group.MapDelete("/delete", (int id, SettingService service) => AsResult(service.DeleteAStreamingSetting(id)));
    }

    private static void MapVideoSettings(IEndpointRouteBuilder app)
    {
        var group = app.MapGroup("/video-setting");

        group.MapGet("/getVideoSettings", (HttpContext context, VideoSettingService service) =>
            Results.Ok(service.GetAllVideoSettings(Filters(context.Request.Query, "lang"))));

        group.MapPost("/save", (VideoSettingsRequest? request, VideoSettingService service) =>
            AsResult(service.SaveSettingsVideo(request!)));

        group.MapPost("/link", (int videoPkidToLink, VideoSettingsRequest? request, VideoSettingService service) =>
            AsResult(service.LinkAndSaveSettingsVideo(request!, videoPkidToLink)));

        group.MapPut("/edit", (int id, VideoSettingsRequest? request, VideoSettingService service) =>
            AsResult(service.EditSettingsVideo(request!, id)));

        group.MapDelete("/delete", (int id, VideoSettingService service) => AsResult(service.DeleteVideoSetting(id)));
    }

    private static void MapImage(IEndpointRouteBuilder app)
    {
        var group = app.MapGroup("/image");

        group.MapPost("/upload", async (
            HttpRequest request,
            ImageService service,
            RequestValidator validator,
            CancellationToken cancellationToken) =>
        {
            if (!request.HasFormContentType)
            {
                throw new BadHttpRequestException("Expected a multipart/form-data request with the 'image' field.");
            }

            var form = await request.ReadFormAsync(cancellationToken).ConfigureAwait(false);
            var image = form.Files["image"];
            validator.RequireImage(image);

            return AsResult(service.SaveImage(image!));
        });

        group.MapGet("/loadimage", (ImageService service) =>
        {
            var image = service.LoadImage();
            return Results.File(image.Path, image.ContentType);
        });
    }

    private static void MapTaskManager(IEndpointRouteBuilder app)
    {
        var group = app.MapGroup("/taskManager/statistics");

        group.MapGet("/allInfo", (SystemInfoService service) => Results.Ok(service.GetAllSystemInfo()));
        group.MapGet("/cpu", (SystemInfoService service) => Results.Ok(service.GetCpuPercent()));
        group.MapGet("/ram", (SystemInfoService service) => Results.Ok(service.GetRamPercent()));
        group.MapGet("/swap", (SystemInfoService service) => Results.Ok(service.GetSwapPercent()));
        group.MapGet("/cpu/temperature", (SystemInfoService service) => Results.Ok(service.GetCpuTemperature()));
    }

    /// <summary>Writes the <c>{ response, message }</c> envelope with the Spring status code.</summary>
    private static IResult AsResult(MessageResponse response) =>
        Results.Json(response.Body, statusCode: response.StatusCode, contentType: "application/json");

    /// <summary>
    /// Every request parameter is an equality filter; the paging parameters are not entity
    /// properties and <c>platform</c> is a frontend-only filter that the Java version rejected.
    /// </summary>
    private static IReadOnlyDictionary<string, string> Filters(IQueryCollection query, params string[] excluded)
    {
        var filters = new Dictionary<string, string>(StringComparer.Ordinal);
        foreach (var (key, values) in query)
        {
            if (excluded.Contains(key, StringComparer.Ordinal))
            {
                continue;
            }

            filters[key] = values.ToString();
        }

        return filters;
    }
}
