const express = require('express');
const multer = require('multer');
const fs = require('fs');
const path = require('path');
const config = require('./config');
const db = require('./db');
const streaming = require('./streaming');
const systemInfo = require('./systemInfo');
const { translate, resolveLocale } = require('./i18n');
const logger = require('./logger');

const app = express();

app.use(express.json({ limit: '10mb', strict: false }));
app.use((req, res, next) => {
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'GET, POST, PUT, DELETE, OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type, Accept, lang');
  if (req.method === 'OPTIONS') {
    return res.sendStatus(204);
  }
  next();
});

function localeOf(req) {
  return resolveLocale(req.query.lang, req.get('accept-language'));
}

function message(code, req, status, params) {
  return {
    response: status >= 200 && status < 300 ? 'success' : 'error',
    message: translate(code, localeOf(req), params),
  };
}

function ok(res, code, status = 200, params) {
  res.status(status).json(message(code, res.req, status, params));
}

function fail(res, code, status = 500, params) {
  res.status(status).json(message(code, res.req, status, params));
}

function pageDto(content, pageNumber, pageSize, totalElements) {
  const totalPages = pageSize > 0 ? Math.ceil(totalElements / pageSize) : 0;
  return {
    content,
    page: {
      size: pageSize,
      number: pageNumber,
      totalElements,
      totalPages,
    },
  };
}

function parsePagination(req, defaults) {
  let page = parseInt(req.query.page, 10);
  if (Number.isNaN(page) || page < 0) page = defaults.page ?? 0;
  let size = parseInt(req.query.size, 10);
  if (Number.isNaN(size) || size < 1) size = defaults.size ?? 20;
  const sortBy = defaults.sortBy ?? null;
  const sortDirection = defaults.direction ?? 'DESC';
  return { page, size, sortBy, sortDirection };
}

function removePaginationKeys(filters) {
  const copy = { ...filters };
  delete copy.page;
  delete copy.size;
  return copy;
}

/* ------------------------------- SETTINGS ------------------------------- */

app.post('/settings/save', (req, res) => {
  const record = req.body || {};
  if (!record.streamUrl || !record.streamKey) {
    return fail(res, 'not.valid.input', 400);
  }
  if (db.findSettingByStreamUrlAndStreamKey(record.streamUrl, record.streamKey)) {
    return fail(res, 'setting.is.already.present', 409);
  }
  db.saveSetting(record);
  ok(res, 'setting.created', 201);
});

app.put('/settings/change', (req, res) => {
  const id = Number(req.query.id);
  if (!db.findSettingById(id)) {
    return fail(res, 'setting.not.found', 404);
  }
  db.updateSetting(id, req.body || {});
  ok(res, 'setting.update', 202);
});

app.get('/settings/retrive', (req, res) => {
  const filters = removePaginationKeys(req.query);
  res.json(db.findSettings(filters));
});

app.get('/settings/retrive-channels', (req, res) => {
  const filters = removePaginationKeys(req.query);
  const channels = db.retrieveChannels(filters);
  res.json(channels);
});

app.delete('/settings/delete', (req, res) => {
  const id = Number(req.body);
  const setting = db.findSettingById(id);
  if (!setting) {
    return fail(res, 'setting.not.found', 404);
  }
  db.deleteSetting(id);
  ok(res, 'setting.delete', 200);
});

app.get('/settings/retrive-directories-path', (req, res) => {
  const filters = removePaginationKeys(req.query);
  const settings = db.findSettings(filters);
  const result = settings.map((setting) => {
    const videoPath = setting.videoFolder;
    let isAFolder = false;
    try {
      isAFolder = fs.statSync(videoPath).isDirectory();
    } catch (err) {
      isAFolder = false;
    }
    return { path: videoPath, isAFolder };
  });
  res.json(result);
});

/* ----------------------------- VIDEO SETTINGS ----------------------------- */

app.get('/video-setting/getVideoSettings', (req, res) => {
  const filters = removePaginationKeys(req.query);
  res.json(db.findVideoSettings(filters));
});

app.post('/video-setting/save', (req, res) => {
  db.insertVideoSetting(req.body || {});
  ok(res, 'video.settings.saved', 201);
});

app.post('/video-setting/link', (req, res) => {
  const videoPkidToLink = Number(req.query.videoPkidToLink);
  const video = db.findVideoByPkid(videoPkidToLink);
  if (!video) {
    return fail(res, 'video.to.link.not.found', 404);
  }
  const settingId = db.insertVideoSetting(req.body || {});
  db.linkVideoToSetting(videoPkidToLink, settingId);
  ok(res, 'success.operations', 201);
});

app.put('/video-setting/edit', (req, res) => {
  const id = Number(req.query.id);
  if (!db.findVideoSettingById(id)) {
    return fail(res, 'video.settings.not.found', 404);
  }
  db.updateVideoSetting(id, req.body || {});
  ok(res, 'video.settings.modified', 201);
});

app.delete('/video-setting/delete', (req, res) => {
  const id = Number(req.body);
  if (!db.findVideoSettingById(id)) {
    return fail(res, 'video.settings.not.found', 404);
  }
  db.deleteVideoSetting(id);
  ok(res, 'delete.successful', 200);
});

/* --------------------------------- VIDEO --------------------------------- */

app.get('/video/getPage', (req, res) => {
  const { page, size } = parsePagination(req, { page: 0, size: 20, sortBy: 'pkid', direction: 'DESC' });
  const offset = page * size;
  const { total, rows } = db.findAllVideos({}, { sortBy: 'pkid', sortDirection: 'DESC', offset, limit: size });
  const dtoList = rows.map((record) => {
    const { videoLiveHistory, videoSetting, ...dto } = record;
    return dto;
  });
  res.json(pageDto(dtoList, page, size, total));
});

app.get('/video/getAllVideo', (req, res) => {
  const { page, size, sortBy, sortDirection } = parsePagination(req, {
    page: 0,
    size: 4,
    sortBy: 'startDateLive',
    direction: 'DESC',
  });
  const filters = removePaginationKeys(req.query);
  const offset = page * size;
  const { total, rows } = db.findAllVideos(filters, { sortBy, sortDirection, offset, limit: size });
  res.json(pageDto(rows, page, size, total));
});

app.get('/video/getAllChannelWithVideoLive', (req, res) => {
  const liveVideos = db.findVideosByLiveStatus('LIVE');
  const channelNames = [...new Set(liveVideos.map((video) => video.channelName))];
  const result = channelNames.map((channelName) => ({
    channelName,
    videoPathAndKeyList: liveVideos
      .filter((video) => video.channelName === channelName)
      .map((video) => ({ videoPath: video.videoPath, videoKey: video.pkid })),
  }));
  res.json(result);
});

app.put('/video/unlockVideo', (req, res) => {
  const videoKey = Number(req.query.videoKey);
  const video = db.findVideoByPkid(videoKey);
  if (!video) {
    return fail(res, 'video.not.found', 404);
  }
  setTimeout(async () => {
    try {
      db.setVideoShouldBeStop(videoKey, true);
      for (let i = 0; i < 3; i++) {
        await new Promise((resolve) => setTimeout(resolve, 1000));
        const current = db.findVideoByPkid(videoKey);
        if (current && current.liveStatus === 'OFFLINE') {
          return;
        }
      }
      const current = db.findVideoByPkid(videoKey);
      if (current) {
        db.updateVideoLifeStatus(videoKey, { liveStatus: 'OFFLINE' });
      }
    } catch (err) {
      logger.error(`unlockVideo failed: ${err.message}`);
    }
  }, 0);
  ok(res, 'unlock.in.progress', 202);
});

/* --------------------------------- LIVE --------------------------------- */

function checkAlreadyStreaming(channelName, platformStreamName) {
  const liveVideos = db.findVideosByLiveStatusAndChannelName('LIVE', channelName);
  return liveVideos.some(
    (video) =>
      video.videoLiveHistory &&
      video.videoLiveHistory.platformStreamName &&
      video.videoLiveHistory.platformStreamName.toLowerCase() ===
        String(platformStreamName || '').toLowerCase(),
  );
}

function buildStreamingUrl(streamUrl, streamKey) {
  return streamUrl.endsWith('/') ? streamUrl + streamKey : streamUrl + '/' + streamKey;
}

function resolveVideoSettingId(videoSettingsRecord) {
  if (!videoSettingsRecord) {
    return null;
  }
  if (videoSettingsRecord.id != null && db.findVideoSettingById(videoSettingsRecord.id)) {
    return videoSettingsRecord.id;
  }
  return db.insertVideoSetting(videoSettingsRecord);
}

function extractExtension(fileName) {
  const parts = String(fileName).split('.');
  if (parts.length < 2) {
    throw new Error('video.not.valid');
  }
  return parts[parts.length - 1];
}

function saveVideoPaths(videoPathFolder, videoLiveHistoryPkid, videoSettingsRecord, channelName, startDate) {
  const videoSettingId = resolveVideoSettingId(videoSettingsRecord);
  const stat = fs.statSync(videoPathFolder);

  if (stat.isDirectory()) {
    const files = fs.readdirSync(videoPathFolder);
    const videoFiles = files
      .filter((fileName) => {
        const fullPath = path.join(videoPathFolder, fileName);
        if (fs.statSync(fullPath).isDirectory()) {
          return false;
        }
        const lastDot = fileName.lastIndexOf('.');
        if (lastDot === -1) {
          return false;
        }
        return db.isVideoExtension(fileName.substring(lastDot + 1));
      })
      .map((fileName) => ({
        name: fileName,
        videoPath: path.join(videoPathFolder, fileName),
        extension: extractExtension(fileName),
      }));

    if (videoFiles.length === 0) {
      const error = new Error('folder.empty');
      error.params = [videoPathFolder];
      throw error;
    }

    for (const file of videoFiles) {
      db.insertVideo({
        name: file.name,
        videoPath: file.videoPath,
        extension: file.extension,
        liveStatus: 'OFFLINE',
        lastTimeStampBeforeStop: 0,
        shouldBeStop: false,
        startDateLive: startDate,
        channelName,
        videoLiveHistoryPkid,
        videoSettingId,
      });
    }
  } else {
    const fileName = path.basename(videoPathFolder);
    db.insertVideo({
      name: fileName,
      videoPath: videoPathFolder,
      extension: extractExtension(fileName),
      liveStatus: 'OFFLINE',
      lastTimeStampBeforeStop: 0,
      shouldBeStop: false,
      startDateLive: startDate,
      channelName,
      videoLiveHistoryPkid,
      videoSettingId,
    });
  }
}

app.post('/live/start-live', (req, res) => {
  const record = req.body || {};
  const { streamUrl, streamKey, videoPath, platformStreamName, channelName, videoSettingsRecord } = record;

  if (!streamUrl || !streamKey || !videoPath || !platformStreamName || !channelName) {
    return fail(res, 'input.not.valid', 400);
  }

  if (checkAlreadyStreaming(channelName, platformStreamName)) {
    return fail(res, 'channel.has.already.a.live.active', 409, [channelName]);
  }

  const startDate = db.localDateTimeNow();
  const historyPkid = db.saveVideoLiveHistory({
    folderOfVideoToStream: videoPath,
    startDate,
    streamUrl,
    streamKey,
    platformStreamName,
    userName: 'Mario',
  });

  try {
    saveVideoPaths(videoPath, historyPkid, videoSettingsRecord, channelName, startDate);
  } catch (err) {
    if (err.message === 'folder.empty') {
      return fail(res, 'folder.empty', 404, [videoPath]);
    }
    logger.error(`saveVideoPaths failed: ${err.message}`);
    return fail(res, err.message === 'video.not.valid' ? 'video.not.valid' : 'error.during.starting.video', 500, [videoPath]);
  }

  const videos = db.findVideosByHistoryPkid(historyPkid);
  if (!videos || videos.length === 0) {
    return fail(res, 'video.streaming.not.found', 404);
  }

  const streamingUrl = buildStreamingUrl(streamUrl, streamKey);
  const locale = localeOf(req);
  Promise.all(videos.map((video) => streaming.streamVideo({
    videoKey: video.pkid,
    inputPath: video.videoPath,
    videoSetting: video.videoSetting || {},
    videoLiveHistoryId: historyPkid,
    streamingUrl,
    locale,
  }))).catch((err) => logger.error(`start-live background error: ${err.message}`));

  ok(res, 'live.started', 202);
});

app.put('/live/stop-live', (req, res) => {
  const videoLivePkid = Number(req.query.videoLivePkid);
  const video = db.findVideoByPkid(videoLivePkid);
  if (!video) {
    return fail(res, 'video.not.found', 404);
  }
  db.setVideoShouldBeStop(videoLivePkid, true);
  ok(res, 'live.stopped', 200);
});

app.post('/live/start-video-live', (req, res) => {
  const record = req.body || {};
  const historyRecord = record.videoLiveHistory || {};

  if (!record.videoPath || !record.channelName) {
    return fail(res, 'input.not.valid', 400);
  }

  const channelName = record.channelName;
  const platformStreamName = historyRecord.platformStreamName;
  const streamUrl = historyRecord.streamUrl;
  const streamKey = historyRecord.streamKey;

  if (checkAlreadyStreaming(channelName, platformStreamName)) {
    return fail(res, 'channel.has.already.a.live.active', 409, [channelName]);
  }

  const startDate = db.localDateTimeNow();
  db.saveVideoLiveHistory({
    folderOfVideoToStream: record.videoPath,
    startDate,
    streamUrl,
    streamKey,
    platformStreamName,
    userName: 'Mario',
  });

  if (streamUrl && streamKey) {
    const videos = db.findVideosByHistoryPkid(historyRecord.pkid);
    if (!videos || videos.length === 0) {
      return fail(res, 'video.streaming.not.found', 404);
    }
    const streamingUrl = buildStreamingUrl(streamUrl, streamKey);
    const locale = localeOf(req);
    Promise.all(videos.map((video) => streaming.streamVideo({
      videoKey: video.pkid,
      inputPath: video.videoPath,
      videoSetting: video.videoSetting || {},
      videoLiveHistoryId: historyRecord.pkid,
      streamingUrl,
      locale,
    }))).catch((err) => logger.error(`start-video-live background error: ${err.message}`));
  }

  ok(res, 'live.started', 202);
});

/* ------------------------------ TASK MANAGER ------------------------------ */

app.get('/taskManager/statistics/allInfo', async (req, res) => {
  try {
    res.json(await systemInfo.getAllSystemInfo());
  } catch (err) {
    logger.error(`allInfo failed: ${err.message}`);
    res.status(500).json({ response: 'error', message: err.message });
  }
});

app.get('/taskManager/statistics/cpu', async (req, res) => {
  try {
    res.json(await systemInfo.getCpuPercent());
  } catch (err) {
    logger.error(`cpu failed: ${err.message}`);
    res.status(500).json({ response: 'error', message: err.message });
  }
});

app.get('/taskManager/statistics/ram', async (req, res) => {
  try {
    res.json(await systemInfo.getRamPercent());
  } catch (err) {
    logger.error(`ram failed: ${err.message}`);
    res.status(500).json({ response: 'error', message: err.message });
  }
});

app.get('/taskManager/statistics/swap', async (req, res) => {
  try {
    res.json(await systemInfo.getSwapPercent());
  } catch (err) {
    logger.error(`swap failed: ${err.message}`);
    res.status(500).json({ response: 'error', message: err.message });
  }
});

app.get('/taskManager/statistics/cpu/temperature', async (req, res) => {
  try {
    res.json(await systemInfo.getCpuTemp());
  } catch (err) {
    logger.error(`cpu temp failed: ${err.message}`);
    res.status(500).json({ response: 'error', message: err.message });
  }
});

/* --------------------------------- IMAGE --------------------------------- */

const upload = multer({ storage: multer.memoryStorage(), limits: { fileSize: 50 * 1024 * 1024 } });

app.post('/image/upload', upload.single('image'), (req, res) => {
  try {
    const file = req.file;
    if (!file || !file.originalname) {
      return fail(res, 'input.not.valid', 400);
    }
    fs.mkdirSync(config.imagesDir, { recursive: true });

    const parts = file.originalname.split('.');
    const extension = parts[parts.length - 1];

    const previous = loadImageFile();
    if (previous) {
      fs.rmSync(previous, { force: true });
    }

    const targetPath = path.join(config.imagesDir, `HOME.${extension}`);
    fs.writeFileSync(targetPath, file.buffer);
    ok(res, 'image.saved', 201);
  } catch (err) {
    logger.error(`image upload failed: ${err.message}`);
    fail(res, 'error.during.save.file', 500);
  }
});

function loadImageFile() {
  if (!fs.existsSync(config.imagesDir) || !fs.statSync(config.imagesDir).isDirectory()) {
    return null;
  }
  const files = fs.readdirSync(config.imagesDir);
  if (files.length === 0) {
    return null;
  }
  return path.join(config.imagesDir, files[0]);
}

app.get('/image/loadimage', (req, res) => {
  try {
    const filePath = loadImageFile();
    if (!filePath) {
      return fail(res, 'folder.not.found', 500);
    }
    const ext = path.extname(filePath).slice(1).toLowerCase();
    const contentTypes = {
      png: 'image/png',
      jpg: 'image/jpeg',
      jpeg: 'image/jpeg',
      gif: 'image/gif',
      webp: 'image/webp',
      svg: 'image/svg+xml',
      bmp: 'image/bmp',
      ico: 'image/x-icon',
    };
    res.setHeader('Content-Type', contentTypes[ext] || 'application/octet-stream');
    res.sendFile(filePath);
  } catch (err) {
    logger.error(`image load failed: ${err.message}`);
    fail(res, 'error.during.loading', 500);
  }
});

/* -------------------------------- SPA STATIC -------------------------------- */

app.use(express.static(config.publicDir, { index: false }));

app.get('*', (req, res, next) => {
  if (req.path.includes('.')) {
    return next();
  }
  res.sendFile(path.join(config.publicDir, 'index.html'));
});

function startServer() {
  return new Promise((resolve, reject) => {
    const server = app.listen(config.port, config.host, () => {
      logger.info(`Orbis Stream server listening on http://${config.host}:${config.port}`);
      resolve(server);
    });
    server.on('error', reject);
  });
}

if (require.main === module) {
  startServer().catch((err) => {
    logger.error(`server failed: ${err.message}`);
    process.exit(1);
  });
}

module.exports = {
  app,
  startServer,
};
