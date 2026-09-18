const { spawn, spawnSync } = require('child_process');
const config = require('./config');
const db = require('./db');
const { resolveVideoCodec, resolveAudioCodec, resolvePixelFormat } = require('./codecs');
const { translate } = require('./i18n');
const logger = require('./logger');

const runningStreams = new Map();

function ffprobePath() {
  const base = config.ffmpegPath;
  if (base === 'ffmpeg') {
    return 'ffprobe';
  }
  if (base.endsWith('ffmpeg')) {
    return base.slice(0, -'ffmpeg'.length) + 'ffprobe';
  }
  return 'ffprobe';
}

function probeFrameRate(inputPath) {
  try {
    const result = spawnSync(
      ffprobePath(),
      [
        '-v', 'error',
        '-select_streams', 'v:0',
        '-show_entries', 'stream=avg_frame_rate',
        '-of', 'csv=p=0',
        inputPath,
      ],
      { encoding: 'utf8', timeout: 10000 },
    );
    if (result.status !== 0) {
      return null;
    }
    const parts = (result.stdout || '').trim().split('/');
    const num = parseFloat(parts[0]);
    const den = parseFloat(parts[1]);
    if (den && num) {
      const fps = num / den;
      return fps > 0 && fps < 240 ? fps : null;
    }
  } catch (err) {
    logger.warn(`ffprobe failed for ${inputPath}: ${err.message}`);
  }
  return null;
}

function buildArgs(inputPath, videoSetting, fps, streamingUrl) {
  const args = ['-y', '-re', '-i', inputPath];

  const videoCodec = resolveVideoCodec(videoSetting.videoCodec, videoSetting.videoCodecName);
  if (videoCodec) {
    args.push('-c:v', videoCodec);
  }

  if (videoSetting.videoBitrate != null) {
    args.push('-b:v', String(videoSetting.videoBitrate));
  }

  const pixelFormat = resolvePixelFormat(videoSetting.pixelFormat);
  if (pixelFormat) {
    args.push('-pix_fmt', pixelFormat);
  }

  if (fps) {
    args.push('-r', String(fps));
  }

  const gop = fps && videoSetting.gopSize != null ? Math.round(fps * videoSetting.gopSize) : null;
  if (gop) {
    args.push('-g', String(gop));
  }

  const audioSetting = videoSetting.audioSettingRecord || {};
  const audioCodec = resolveAudioCodec(audioSetting.audioCodec);
  if (audioCodec) {
    args.push('-c:a', audioCodec);
  }

  if (audioSetting.audioBitrate != null) {
    args.push('-b:a', String(audioSetting.audioBitrate));
  }

  if (videoSetting.videoFormat) {
    args.push('-f', videoSetting.videoFormat);
  }

  for (const option of videoSetting.videoOptions || []) {
    if (option && option.key) {
      args.push(`-${option.key}`, option.value ?? '');
    }
  }

  args.push(streamingUrl);
  return args;
}

function streamVideo({ videoKey, inputPath, videoSetting, videoLiveHistoryId, streamingUrl, locale }) {
  return new Promise((resolve) => {
    let fps = null;
    try {
      fps = probeFrameRate(inputPath);
    } catch (err) {
      logger.warn(`ffprobe failed: ${err.message}`);
    }

    const args = buildArgs(inputPath, videoSetting, fps, streamingUrl);

    logger.info(`START video=${videoKey} path=${inputPath}`);
    logger.info(`ffmpeg command: ${config.ffmpegPath} ${args.join(' ')}`);

    db.updateVideoLifeStatus(videoKey, {
      message: translate('video.live.started', locale, [inputPath]),
      liveStatus: 'LIVE',
      startDateLive: db.localDateTimeNow(),
    });

    let proc;
    try {
      proc = spawn(config.ffmpegPath, args, { stdio: ['ignore', 'ignore', 'pipe'] });
    } catch (err) {
      db.updateVideoLifeStatus(videoKey, {
        message: translate('error.during.starting.video', locale, [inputPath]) + `\n${err.message}`,
        liveStatus: 'ERROR',
      });
      logger.error(`error starting video ${videoKey}: ${err.message}`);
      resolve();
      return;
    }

    const entry = { proc, stopRequested: false, locale, inputPath };
    runningStreams.set(videoKey, entry);

    let stderr = '';
    proc.stderr.on('data', (chunk) => {
      stderr += chunk.toString();
    });

    const poll = setInterval(() => {
      try {
        const video = db.findVideoByPkid(videoKey);
        if (video && video.shouldBeStop) {
          entry.stopRequested = true;
          try {
            proc.kill('SIGTERM');
          } catch (err) {
            logger.warn(`kill failed for video ${videoKey}: ${err.message}`);
          }
        }
      } catch (err) {
        logger.warn(`poll failed for video ${videoKey}: ${err.message}`);
      }
    }, 500);
    entry.poll = poll;

    proc.on('close', (code) => {
      clearInterval(poll);
      runningStreams.delete(videoKey);

      if (entry.stopRequested) {
        db.setVideoShouldBeStop(videoKey, false);
        db.updateVideoLifeStatus(videoKey, {
          message: translate('live.stopped', locale),
          liveStatus: 'STOPPED',
        });
        logger.info(`video ${videoKey} stopped`);
      } else if (code === 0) {
        db.updateVideoLifeStatus(videoKey, {
          message: translate('video.live.ended', locale),
          liveStatus: 'ENDED',
        });
        logger.info(`video ${videoKey} ended`);
      } else {
        const lastError = stderr.trim().split('\n').filter(Boolean).pop() || '';
        const message =
          translate('error.during.streaming.video', locale, [inputPath, videoLiveHistoryId]) +
          (lastError ? `\n${lastError}` : '');
        db.updateVideoLifeStatus(videoKey, { message, liveStatus: 'ERROR' });
        logger.error(`video ${videoKey} failed with code ${code}`);
        logger.error(stderr);
      }

      resolve();
    });

    proc.on('error', (err) => {
      clearInterval(poll);
      runningStreams.delete(videoKey);
      db.updateVideoLifeStatus(videoKey, {
        message: translate('error.during.starting.video', locale, [inputPath]) + `\n${err.message}`,
        liveStatus: 'ERROR',
      });
      logger.error(`spawn error for video ${videoKey}: ${err.message}`);
      resolve();
    });
  });
}

function streamVideos(videoList, streamingUrl, locale) {
  return Promise.all(
    videoList.map((video) =>
      streamVideo({
        videoKey: video.pkid,
        inputPath: video.videoPath,
        videoSetting: video.videoSetting || {},
        videoLiveHistoryId: video.videoLiveHistory?.pkid,
        streamingUrl,
        locale,
      }),
    ),
  );
}

function killAllStreams() {
  for (const [videoKey, entry] of runningStreams.entries()) {
    entry.stopRequested = true;
    try {
      entry.proc.kill('SIGKILL');
    } catch (err) {
      logger.warn(`killAll failed for ${videoKey}: ${err.message}`);
    }
  }
  runningStreams.clear();
}

module.exports = {
  streamVideo,
  streamVideos,
  killAllStreams,
  runningStreams,
};
