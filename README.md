# Orbis Stream

Desktop app to stream video files to streaming platforms (Twitch, YouTube, ...).
Electron rewrite of the original Java Spring Boot + JCEF project: the frontend
is the same compiled React build, while backend and shell are now
Node.js (Express) + Electron + external `ffmpeg`.

## Requirements

- Node.js >= 22.13 (uses `node:sqlite`)
- `ffmpeg` and `ffprobe` available on `PATH` (tested with ffmpeg 8.1)

## Install

```bash
cd electron-app
npm install
```

## Run

```bash
npm start        # launch Electron app (embedded server on http://localhost:1200)
npm run dev      # Electron app + detached DevTools
npm run start:server   # server only (for browser-based development)
```

The UI loads `http://localhost:1200/orbis/handler`. Data lives in
`electron-app/data/stream.db` (SQLite), images in `electron-app/images/`,
logs in `electron-app/data/logs/stream.log`.

## API overview

- `POST /live/start-live`, `POST /live/start-video-live`, `PUT /live/stop-live?videoLivePkid=`
- `GET /video/getAllVideo`, `GET /video/getPage`, `GET /video/getAllChannelWithVideoLive`, `PUT /video/unlockVideo?videoKey=`
- `GET|POST /settings/retrive`, `/settings/save`, `PUT /settings/change?id=`, `DELETE /settings/delete`, `GET /settings/retrive-channels`
- `GET|POST|PUT|DELETE /video-setting/*`
- `GET /taskManager/statistics/{allInfo,cpu,ram,swap,cpu/temperature}`
- `POST /image/upload`, `GET /image/loadimage`

Responses follow the Spring conventions the frontend expects:
`{ "response": "success" | "error", "message": "<localized>" }` and
Spring `Page` shape `{ content, page: { size, number, totalElements, totalPages } }`.

## Configuration (env vars)

- `PORT` (default `1200`), `HOST` (default `localhost`)
- `FFMPEG_PATH` (default `ffmpeg`)
- `ORBIS_DATA_DIR`, `ORBIS_IMAGES_DIR` (defaults: `data/`, `images/` next to the app)
- In packaged mode data/images are stored under Electron `userData`.

## Package

```bash
npm run pack   # unpacked build in dist/
npm run dist   # installers (dmg / nsis / AppImage)
```
