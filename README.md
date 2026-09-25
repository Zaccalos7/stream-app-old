# Orbis Stream

Desktop application that streams local video files to Twitch, YouTube and any other
RTMP-compatible platform.

The application is a native rewrite of the original Java Spring Boot + JCEF project:

- **Shell**: .NET 10 WPF (`OrbisStream.exe`) hosting the WebView2 control.
- **Backend**: ASP.NET Core (Kestrel) in the same process, listening on `http://localhost:1200`.
- **Frontend**: the original compiled React build, served as static files with SPA fallback.
- **Streaming**: an external `ffmpeg` process fed by a generated FFmpeg concat playlist.
- **Persistence**: SQLite (`stream.db`), created and migrated on startup.

## Requirements

- Windows 11 (the shell is WPF/WebView2; the backend and tests also build on Linux/macOS).
- [.NET 10 SDK](https://dotnet.microsoft.com/download/dotnet/10.0).
- The WebView2 Runtime (preinstalled on current Windows 10/11).
- `ffmpeg` and `ffprobe` on `PATH` (or configured through environment variables).

## Build, test, run

```bash
dotnet build OrbisStream.slnx -c Release
dotnet test OrbisStream.slnx -c Release
dotnet run --project src/Orbis.Stream.App/Orbis.Stream.App.csproj
```

The shell opens on `http://localhost:1200/orbis/mainMenu`, the entry point of the React
frontend, whose router basename is `/orbis`.

## Publish

```bash
dotnet publish src/Orbis.Stream.App/Orbis.Stream.App.csproj -c Release -r win-x64 \
  --self-contained false -p:PublishSingleFile=true -o artifacts/publish
```

`wwwroot` is copied next to the executable, so the published folder is self-sufficient.
Ship `ffmpeg.exe` and `ffprobe.exe` in the same folder, or point the environment variables
at them.

## Runtime data

Directories are resolved in this order: `--data-dir`, `LAZY_STREAM_DATA_DIR`, the current
working directory when writable, then `~/.orbis-stream`.

| Path                      | Content                                        |
| ------------------------- | ---------------------------------------------- |
| `<data>/stream.db`        | SQLite database                                |
| `<data>/logs/twitch.log`  | application log                                |
| `<data>/webview2`         | WebView2 user data folder                      |
| `<cwd>/images`            | images uploaded through `/image/upload`        |

## Configuration

| Variable                       | Command line          | Default                    |
| ------------------------------ | --------------------- | -------------------------- |
| `LAZY_STREAM_DATA_DIR`         | `--data-dir=`         | working directory / `~/.orbis-stream` |
| `LAZY_STREAM_PORT`             | `--port=`             | `1200`                     |
| `LAZY_STREAM_EMBEDDED_BROWSER` | `--browser` / `--no-browser` | `true`            |
| `LAZY_STREAM_WEB_ROOT`         | `--web-root=`         | `wwwroot` beside the executable |
| `FFMPEG_PATH`                  | –                     | `ffmpeg`                   |
| `FFPROBE_PATH`                 | –                     | `ffprobe` beside `ffmpeg`  |

## API

| Area          | Endpoints                                                                                    |
| ------------- | -------------------------------------------------------------------------------------------- |
| Live          | `POST /live/start-live`, `POST /live/start-video-live`, `PUT /live/stop-live?videoLivePkid=`    |
| Video         | `GET /video/getAllVideo`, `GET /video/getPage`, `GET /video/getAllChannelWithVideoLive`, `PUT /video/unlockVideo?videoKey=` |
| Settings      | `GET|POST /settings/retrive`, `POST /settings/save`, `PUT /settings/change?id=`, `DELETE /settings/delete`, `GET /settings/retrive-channels` |
| Video setting | `GET|POST|PUT|DELETE /video-setting/*`                                                        |
| System        | `GET /taskManager/statistics/{allInfo,cpu,ram,swap,cpu/temperature}`                          |
| Image         | `POST /image/upload`, `GET /image/loadimage`                                                   |
| Docs          | `GET /documentazione`, `GET /swagger-ui.html`                                                  |

Responses keep the conventions the frontend expects:

```json
{ "response": "success", "message": "Operation completed" }
```

```json
{
  "content": [ { "id": 1, "videoPath": "/home/user/clip.mp4" } ],
  "page": { "size": 25, "number": 0, "totalElements": 1, "totalPages": 1 }
}
```

`GET /` redirects to `/orbis/mainMenu`, and any non-API path that is not a real file falls
back to `index.html` so that client-side routing works on a hard refresh.

## Streaming pipeline

1. `POST /live/start-video-live` reads the videos attached to a channel, creates a `.txt`
   playlist and stores the FFmpeg configuration (`FfmpegVideoPlaylistStreamer`).
2. The session probes the media with `ffprobe` to obtain duration, frame rate, resolution
   and the audio/video codecs.
3. A background loop feeds `ffmpeg -re -f concat -safe 0 -i <playlist> -c copy ...` and
   writes to the RTMP URL built from the channel and platform settings.
4. `PUT /live/stop-live` (or the frontend) flips the live status, which terminates the process
   tree and closes the playlist; the row ends in the `ENDED` state.

## Repository layout

```
src/Orbis.Stream.Core     backend: API, data, services, FFmpeg, i18n, system info
src/Orbis.Stream.App      WPF/WebView2 shell and the compiled React build in wwwroot
src/Orbis.Stream.Tests    xUnit suite: API end-to-end, database, paging, i18n, FFmpeg
```

## Tests

`dotnet test` runs the full suite, including an end-to-end test that boots the real host,
generates a short clip with FFmpeg, streams it and asserts the `ENDED` state. The FFmpeg
tests are skipped when `ffmpeg`/`ffprobe` are not available.

## License

MIT, see [LICENSE](LICENSE).
