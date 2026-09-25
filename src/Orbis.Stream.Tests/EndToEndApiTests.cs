using System.Net;
using Microsoft.AspNetCore.Builder;
using System.Net.Http.Json;
using System.Text.Json;
using Orbis.Stream.Core.Configuration;
using Orbis.Stream.Core.Hosting;

namespace Orbis.Stream.Tests;

/// <summary>Boots the production host on a free port with a temporary data directory.</summary>
public sealed class TestHostRunner : IAsyncDisposable
{
    private WebApplication? _host;

    private TestHostRunner(WebApplication host, string dataDirectory, int port, string webRootPath)
    {
        _host = host;
        DataDirectory = dataDirectory;
        Port = port;
        WebRootPath = webRootPath;
        Client = CreateClient();
        NoRedirectClient = CreateClient(allowAutoRedirect: false);
    }

    public string DataDirectory { get; }

    public int Port { get; }

    public string WebRootPath { get; }

    public HttpClient Client { get; }

    public HttpClient NoRedirectClient { get; }

    public static TestHostRunner Start(string? ffmpegPath = null, string? ffprobePath = null)
    {
        var dataDirectory = Path.Combine(Path.GetTempPath(), "orbis-e2e", Guid.NewGuid().ToString("N"));
        Directory.CreateDirectory(dataDirectory);
        var port = FreePort();
        var webRootPath = Path.Combine(AppContext.BaseDirectory, "WebRoot");
        Directory.CreateDirectory(webRootPath);
        File.WriteAllText(
            Path.Combine(webRootPath, "index.html"),
            "<!doctype html><html><head><base href=\"/orbis/\"></head><body>orbis</body></html>");

        var environment = new Dictionary<string, string?>(StringComparer.Ordinal)
        {
            [OrbisRuntimeOptions.FfmpegEnvironmentVariable] = ffmpegPath ?? "ffmpeg-not-installed",
            [OrbisRuntimeOptions.FfprobeEnvironmentVariable] = ffprobePath ?? "ffprobe-not-installed"
        };

        var options = OrbisRuntimeOptions.Resolve(
            [$"--port={port}", $"--data-dir={dataDirectory}", $"--web-root={webRootPath}"],
            environment);

        options.EnsureDirectories();
        var host = OrbisHost.Create(options);
        host.StartAsync().GetAwaiter().GetResult();
        return new TestHostRunner(host, dataDirectory, port, webRootPath);
    }

    public async ValueTask DisposeAsync()
    {
        Client.Dispose();
        NoRedirectClient.Dispose();
        if (_host is not null)
        {
            using var shutdown = new CancellationTokenSource(TimeSpan.FromSeconds(10));
            await _host.StopAsync(shutdown.Token);
            await _host.DisposeAsync();
        }

        Microsoft.Data.Sqlite.SqliteConnection.ClearAllPools();
        try
        {
            Directory.Delete(DataDirectory, recursive: true);
        }
        catch (IOException)
        {
            // Best effort cleanup of the temporary directory.
        }
    }

    private HttpClient CreateClient(bool allowAutoRedirect = true) => new(new HttpClientHandler
    {
        AllowAutoRedirect = allowAutoRedirect
    })
    {
        BaseAddress = new Uri($"http://localhost:{Port}"),
        Timeout = TimeSpan.FromSeconds(20)
    };

    private static int FreePort()
    {
        using var listener = new System.Net.Sockets.TcpListener(IPAddress.Loopback, 0);
        listener.Start();
        var port = ((System.Net.IPEndPoint)listener.LocalEndpoint).Port;
        listener.Stop();
        return port;
    }
}

/// <summary>
/// Boots the real in-process host (same <see cref="OrbisHost"/> used by the WPF shell) on a free
/// port with a temporary data directory and exercises the HTTP contract the React build relies on.
/// </summary>
public sealed class ApplicationFixture : IAsyncLifetime
{
    private TestHostRunner _runner = null!;

    public string DataDirectory => _runner.DataDirectory;

    public int Port => _runner.Port;

    public HttpClient Client => _runner.Client;

    public HttpClient NoRedirectClient => _runner.NoRedirectClient;

    public string WebRootPath => _runner.WebRootPath;

    public Task InitializeAsync()
    {
        _runner = TestHostRunner.Start();
        return Task.CompletedTask;
    }

    public Task DisposeAsync() => _runner.DisposeAsync().AsTask();
}

public sealed class EndToEndApiTests : IClassFixture<ApplicationFixture>
{
    private const int TwitchVideoSettingId = 1;

    private readonly ApplicationFixture _fixture;

    public EndToEndApiTests(ApplicationFixture fixture) => _fixture = fixture;

    [Fact]
    public async Task Root_RedirectsToTheClientRouter()
    {
        using var response = await _fixture.NoRedirectClient.GetAsync("/", HttpCompletionOption.ResponseHeadersRead);

        Assert.Equal(HttpStatusCode.Redirect, response.StatusCode);
        Assert.Equal("/orbis/mainMenu", response.Headers.Location?.OriginalString);
    }

    [Fact]
    public async Task StaticFiles_AreServedWithTheSpringCacheHeaders()
    {
        using var index = await _fixture.Client.GetAsync("/index.html");
        Assert.Equal(HttpStatusCode.OK, index.StatusCode);
        Assert.NotNull(index.Headers.CacheControl);
        Assert.True(index.Headers.CacheControl!.NoCache);

        var asset = Path.Combine(_fixture.WebRootPath, "assets", "app.js");
        Directory.CreateDirectory(Path.GetDirectoryName(asset)!);
        await File.WriteAllTextAsync(asset, "console.log(1)");

        using var response = await _fixture.Client.GetAsync("/assets/app.js");
        Assert.Equal(HttpStatusCode.OK, response.StatusCode);
        Assert.NotNull(response.Headers.CacheControl);
        Assert.True(response.Headers.CacheControl!.Public);
        Assert.Equal(TimeSpan.FromDays(120), response.Headers.CacheControl.MaxAge);
        var extensions = response.Headers.GetValues("Cache-Control").ToList();
        Assert.Contains(extensions, value => value.Contains("immutable", StringComparison.OrdinalIgnoreCase));
    }

    [Fact]
    public async Task SpaFallback_ServesTheIndexForClientSideRoutes()
    {
        using var response = await _fixture.Client.GetAsync("/orbis/liveSettings");

        Assert.Equal(HttpStatusCode.OK, response.StatusCode);
        Assert.Contains("orbis", await response.Content.ReadAsStringAsync(), StringComparison.Ordinal);
    }

    [Fact]
    public async Task Swagger_IsExposedUnderTheDocumentedPaths()
    {
        using var document = await _fixture.Client.GetAsync("/documentazione");
        Assert.Equal(HttpStatusCode.OK, document.StatusCode);
        Assert.Equal("application/json", document.Content.Headers.ContentType?.MediaType);

        var json = await document.Content.ReadFromJsonAsync<JsonDocument>();
        Assert.NotNull(json);
        Assert.StartsWith("3.0", json!.RootElement.GetProperty("openapi").GetString()!, StringComparison.Ordinal);
        Assert.True(json.RootElement.TryGetProperty("paths", out var paths));
        Assert.True(paths.TryGetProperty("/live/start-live", out _));

        using var redirect = await _fixture.NoRedirectClient.GetAsync(
            "/swagger-ui.html",
            HttpCompletionOption.ResponseHeadersRead);
        Assert.Equal(HttpStatusCode.Redirect, redirect.StatusCode);
        Assert.Equal("/swagger-ui/index.html", redirect.Headers.Location?.OriginalString);
    }

    [Fact]
    public async Task GetPage_ReturnsTheSpringPageEnvelope()
    {
        using var response = await _fixture.Client.GetAsync("/video/getPage?size=5&page=0&sort=pkid,desc");

        Assert.Equal(HttpStatusCode.OK, response.StatusCode);

        var page = await response.Content.ReadFromJsonAsync<JsonElement>();
        Assert.Equal(JsonValueKind.Array, page.GetProperty("content").ValueKind);
        var metadata = page.GetProperty("page");
        var total = metadata.GetProperty("totalElements").GetInt64();
        Assert.Equal(5, metadata.GetProperty("size").GetInt32());
        Assert.Equal(0, metadata.GetProperty("number").GetInt32());
        Assert.True(total >= 0);
        Assert.Equal(total == 0, metadata.GetProperty("empty").GetBoolean());
        Assert.True(metadata.GetProperty("first").GetBoolean());
        Assert.Equal(page.GetProperty("content").GetArrayLength(), metadata.GetProperty("numberOfElements").GetInt32());
        Assert.Equal(
            total == 0 ? 0 : (int)Math.Ceiling(total / 5d),
            metadata.GetProperty("totalPages").GetInt32());
        Assert.Equal("DESC", metadata.GetProperty("sort").GetProperty("sort")[0].GetProperty("direction").GetString());
    }

    [Fact]
    public async Task GetAllVideo_RejectsAnUnknownFilterProperty()
    {
        using var response = await _fixture.Client.GetAsync("/video/getAllVideo?thisIsNotAColumn=1");

        Assert.Equal(HttpStatusCode.InternalServerError, response.StatusCode);
    }

    [Fact]
    public async Task GetAllVideo_IgnoresThePagingSortAndLanguageParameters()
    {
        using var response = await _fixture.Client.GetAsync(
            "/video/getAllVideo?size=2&page=0&sort=startDateLive,desc&lang=it&platform=Twitch");

        Assert.Equal(HttpStatusCode.OK, response.StatusCode);
    }

    [Fact]
    public async Task Settings_AreCreatedListedAndDeleted()
    {
        var payload = new
        {
            streamUrl = "rtmp://ingest.example/live",
            streamKey = "stream-key",
            platformStreamName = "channel-e2e",
            description = "created by the test suite",
            isActive = true,
            channelName = "channel-e2e",
            videoFolder = _fixture.DataDirectory
        };

        using var invalid = await _fixture.Client.PostAsJsonAsync("/settings/save", new { streamUrl = "rtmp://x" });
        Assert.Equal(HttpStatusCode.BadRequest, invalid.StatusCode);
        // The Java ExceptionsHandler answered the Bean Validation failures with a plain
        // field -> localized message map.
        var validation = await invalid.Content.ReadFromJsonAsync<JsonElement>();
        Assert.True(validation.GetProperty("streamKey").GetString()!.Length > 0);
        Assert.True(validation.GetProperty("channelName").GetString()!.Length > 0);

        using var created = await _fixture.Client.PostAsJsonAsync("/settings/save", payload);
        Assert.Equal(HttpStatusCode.Created, created.StatusCode);
        var createdBody = await created.Content.ReadFromJsonAsync<JsonElement>();
        Assert.Equal("success", createdBody.GetProperty("response").GetString());

        using var duplicated = await _fixture.Client.PostAsJsonAsync("/settings/save", payload);
        Assert.Equal(HttpStatusCode.Conflict, duplicated.StatusCode);

        using var list = await _fixture.Client.GetAsync("/settings/retrive?channelName=channel-e2e");
        var settings = await list.Content.ReadFromJsonAsync<JsonElement>();
        var saved = Assert.Single(settings.EnumerateArray());
        Assert.Equal("rtmp://ingest.example/live", saved.GetProperty("streamUrl").GetString());
        var id = saved.GetProperty("id").GetInt32();

        using var changed = await _fixture.Client.PutAsJsonAsync(
            $"/settings/change?id={id}",
            payload with { description = "updated" });
        // modifySetting answered HttpStatus.ACCEPTED in the Java controller.
        Assert.Equal(HttpStatusCode.Accepted, changed.StatusCode);

        using var reloaded = await _fixture.Client.GetAsync("/settings/retrive?channelName=channel-e2e");
        var updated = Assert.Single((await reloaded.Content.ReadFromJsonAsync<JsonElement>()).EnumerateArray());
        Assert.Equal("updated", updated.GetProperty("description").GetString());

        using var deleted = await _fixture.Client.DeleteAsync($"/settings/delete?id={id}");
        Assert.Equal(HttpStatusCode.OK, deleted.StatusCode);

        using var afterDelete = await _fixture.Client.GetAsync("/settings/retrive?channelName=channel-e2e");
        var remaining = await afterDelete.Content.ReadFromJsonAsync<JsonElement>();
        Assert.Empty(remaining.EnumerateArray());
    }

    [Fact]
    public async Task StartLive_ValidatesTheRequestAndRecordsTheFailureOfTheStreamer()
    {
        using var invalid = await _fixture.Client.PostAsJsonAsync("/live/start-live", new { streamUrl = "rtmp://x" });
        Assert.Equal(HttpStatusCode.BadRequest, invalid.StatusCode);

        var emptyFolder = Path.Combine(_fixture.DataDirectory, "empty-folder");
        Directory.CreateDirectory(emptyFolder);
        using var noVideos = await _fixture.Client.PostAsJsonAsync("/live/start-live", new
        {
            streamUrl = "rtmp://ingest.example/live",
            streamKey = "stream-key",
            videoPath = emptyFolder,
            platformStreamName = "channel-e2e-empty",
            channelName = "channel-e2e-empty"
        });
        Assert.Equal(HttpStatusCode.NotFound, noVideos.StatusCode);

        var folder = Path.Combine(_fixture.DataDirectory, "clips");
        Directory.CreateDirectory(folder);
        await File.WriteAllBytesAsync(Path.Combine(folder, "clip.mp4"), [0x00, 0x01, 0x02]);

        using var started = await _fixture.Client.PostAsJsonAsync("/live/start-live", new
        {
            streamUrl = "rtmp://ingest.example/live",
            streamKey = "stream-key",
            videoPath = folder,
            platformStreamName = "channel-e2e-live",
            channelName = "channel-e2e-live",
            videoSettingsRecord = new
            {
                id = TwitchVideoSettingId,
                title = "Twitch",
                isDefaultConfiguration = true,
                defaultPlatformConfiguration = "Twitch",
                videoCodec = 27,
                videoCodecName = "libx264",
                pixelFormat = 0,
                videoBitrate = 5_000_000,
                videoFormat = "flv",
                gopSize = 2,
                isVideoAndAudioSettingActive = true,
                audioSettingRecord = new { audioCodec = 86018, audioBitrate = 128_000 }
            }
        });
        Assert.Equal(HttpStatusCode.Accepted, started.StatusCode);

        // ffmpeg is not installed in the test environment: the row must exist and the failure must
        // be reported through the video status/message instead of an HTTP error.
        VideoSnapshot? video = null;
        for (var attempt = 0; attempt < 40 && video is null; attempt++)
        {
            using var list = await _fixture.Client.GetAsync("/video/getPage?size=50&sort=pkid,desc");
            var page = await list.Content.ReadFromJsonAsync<JsonElement>();
            video = page.GetProperty("content").EnumerateArray()
                .Select(item => new VideoSnapshot(
                    item.GetProperty("pkid").GetInt32(),
                    item.GetProperty("name").GetString() ?? string.Empty,
                    item.GetProperty("liveStatus").GetString() ?? string.Empty,
                    item.GetProperty("message").GetString()))
                .FirstOrDefault(item => item.Name == "clip.mp4");

            if (video is null)
            {
                await Task.Delay(250);
            }
        }

        Assert.NotNull(video);
        var deadline = DateTime.UtcNow.AddSeconds(20);
        while (video!.Status != "ERROR" && video.Status != "ENDED" && DateTime.UtcNow < deadline)
        {
            await Task.Delay(250);
            using var list = await _fixture.Client.GetAsync("/video/getPage?size=50&sort=pkid,desc");
            var page = await list.Content.ReadFromJsonAsync<JsonElement>();
            var current = page.GetProperty("content").EnumerateArray()
                .Select(item => new VideoSnapshot(
                    item.GetProperty("pkid").GetInt32(),
                    item.GetProperty("name").GetString() ?? string.Empty,
                    item.GetProperty("liveStatus").GetString() ?? string.Empty,
                    item.GetProperty("message").GetString()))
                .First(item => item.Pkid == video.Pkid);
            video = current;
        }

        Assert.Equal("ERROR", video!.Status);
        Assert.Contains("ffprobe-not-installed", video.Message!, StringComparison.Ordinal);
        Assert.Equal("mp4", await ExtensionOf(video.Pkid));

        using var stopped = await _fixture.Client.PutAsync($"/live/stop-live?videoLivePkid={video.Pkid}", null);
        Assert.Equal(HttpStatusCode.OK, stopped.StatusCode);

        // unlockVideo answered HttpStatus.ACCEPTED: the unlock runs in the task executor.
        using var lockResponse = await _fixture.Client.PutAsync($"/video/unlockVideo?videoKey={video.Pkid}", null);
        Assert.Equal(HttpStatusCode.Accepted, lockResponse.StatusCode);
    }

    private async Task<string?> ExtensionOf(int videoKey)
    {
        using var list = await _fixture.Client.GetAsync($"/video/getAllVideo?size=200&sort=pkid,desc");
        var page = await list.Content.ReadFromJsonAsync<JsonElement>();
        return page.GetProperty("content").EnumerateArray()
            .First(item => item.GetProperty("pkid").GetInt32() == videoKey)
            .GetProperty("extension")
            .GetString();
    }

    [Fact]
    public async Task VideoSettings_ExposeTheDefaultPlatformConfigurations()
    {
        using var response = await _fixture.Client.GetAsync("/video-setting/getVideoSettings");
        Assert.Equal(HttpStatusCode.OK, response.StatusCode);

        var settings = await response.Content.ReadFromJsonAsync<JsonElement>();
        var platforms = settings.EnumerateArray()
            .Select(item => item.GetProperty("defaultPlatformConfiguration").GetString())
            .ToHashSet(StringComparer.Ordinal);

        Assert.Contains("Twitch", platforms);
        Assert.Contains("Youtube", platforms);
    }

    [Fact]
    public async Task SystemInformation_IsServedAsPlainNumbers()
    {
        foreach (var endpoint in new[] { "/cpu", "/ram", "/swap" })
        {
            using var response = await _fixture.Client.GetAsync($"/taskManager/statistics{endpoint}");
            Assert.Equal(HttpStatusCode.OK, response.StatusCode);

            var value = await response.Content.ReadFromJsonAsync<JsonElement>();
            Assert.Equal(JsonValueKind.Number, value.ValueKind);
            Assert.InRange(value.GetDouble(), 0d, 100d);
        }

        using var all = await _fixture.Client.GetAsync("/taskManager/statistics/allInfo");
        Assert.Equal(HttpStatusCode.OK, all.StatusCode);
        var fields = (await all.Content.ReadFromJsonAsync<JsonElement>()).EnumerateArray()
            .Select(item => item.GetProperty("field").GetString())
            .ToHashSet(StringComparer.Ordinal);
        Assert.Contains("CPU", fields);
        Assert.Contains("RAM", fields);
    }

    [Fact]
    public async Task LanguageQueryParameterIsNotAFilter()
    {
        using var response = await _fixture.Client.GetAsync("/settings/retrive?lang=it");

        Assert.Equal(HttpStatusCode.OK, response.StatusCode);
    }

    private sealed record VideoSnapshot(int Pkid, string Name, string Status, string? Message);
}
