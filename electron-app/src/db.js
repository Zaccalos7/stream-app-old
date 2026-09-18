const { DatabaseSync } = require('node:sqlite');
const fs = require('fs');
const path = require('path');
const config = require('./config');
const logger = require('./logger');

const LIVE_STATUS = { LIVE: 0, OFFLINE: 1, ENDED: 2, ERROR: 3, STOPPED: 4 };
const LIVE_STATUS_BY_ID = Object.fromEntries(
  Object.entries(LIVE_STATUS).map(([name, id]) => [id, name]),
);

const VIDEO_EXTENSIONS = new Set(['mp4', 'flv', 'mov', 'webm', 'vp9', 'mkv']);

fs.mkdirSync(config.dataDir, { recursive: true });

const db = new DatabaseSync(config.dbFile);
db.exec('PRAGMA journal_mode = WAL;');
db.exec('PRAGMA foreign_keys = OFF;');

db.exec(`
CREATE TABLE IF NOT EXISTS audio_setting (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  audio_bitrate INTEGER,
  audio_codec INTEGER
);

CREATE TABLE IF NOT EXISTS setting (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  channel_name TEXT NOT NULL DEFAULT 'Zingy',
  description TEXT,
  is_active BOOLEAN DEFAULT 0,
  platform_stream_name VARCHAR(255),
  stream_key VARCHAR(512) NOT NULL,
  stream_url VARCHAR(512) NOT NULL,
  video_folder VARCHAR(255) NOT NULL DEFAULT '/',
  UNIQUE (stream_url, stream_key)
);

CREATE TABLE IF NOT EXISTS video (
  pkid INTEGER PRIMARY KEY AUTOINCREMENT,
  channel_name VARCHAR(512) NOT NULL DEFAULT 'Zingy',
  extension VARCHAR(255) NOT NULL,
  last_time_stamp_before_stop BIGINT DEFAULT 0,
  live_status TINYINT DEFAULT 1 CHECK (live_status BETWEEN 0 AND 4),
  message TEXT,
  name VARCHAR(255) NOT NULL,
  should_be_stop BOOLEAN DEFAULT 0,
  start_date_live TIMESTAMP,
  video_path VARCHAR(255) NOT NULL,
  video_live_history_pkid BIGINT,
  video_setting_id INTEGER
);

CREATE TABLE IF NOT EXISTS video_settings_options (
  video_setting_id INTEGER NOT NULL,
  option_key VARCHAR(255),
  option_value VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS video_live_history (
  pkid INTEGER PRIMARY KEY AUTOINCREMENT,
  folder_of_video_to_stream TEXT NOT NULL,
  local_date_time_start_live TIMESTAMP NOT NULL,
  platform_stream_name VARCHAR(255) NOT NULL DEFAULT 'N/A',
  stream_key VARCHAR(512) NOT NULL DEFAULT 'N/A',
  stream_url VARCHAR(512) NOT NULL DEFAULT 'N/A',
  user_name VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS video_setting (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  gop_size INTEGER NOT NULL DEFAULT 2,
  is_video_and_audio_setting_active BOOLEAN NOT NULL DEFAULT 0,
  last_modified TIMESTAMP,
  pixel_format INTEGER,
  title VARCHAR(255) DEFAULT 'Title',
  video_bitrate INTEGER,
  video_codec INTEGER,
  video_codec_name VARCHAR(255),
  video_format VARCHAR(255),
  audio_setting_id INTEGER UNIQUE
);
`);

function localDateTimeNow() {
  const now = new Date();
  const pad = (n, length = 2) => String(n).padStart(length, '0');
  return `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}T` +
    `${pad(now.getHours())}:${pad(now.getMinutes())}:${pad(now.getSeconds())}.${pad(now.getMilliseconds(), 3)}`;
}

function liveStatusToId(value) {
  if (value === null || value === undefined || value === '') {
    return LIVE_STATUS.OFFLINE;
  }
  if (typeof value === 'number') {
    return value;
  }
  const name = String(value).toUpperCase();
  return LIVE_STATUS[name] ?? LIVE_STATUS.OFFLINE;
}

function liveStatusIdToName(id) {
  return LIVE_STATUS_BY_ID[id] ?? 'OFFLINE';
}

function toBool(value) {
  if (value === null || value === undefined) {
    return null;
  }
  return value === 1 || value === true || value === 'true' || value === '1';
}

function toInt(value) {
  if (value === null || value === undefined) {
    return null;
  }
  const n = Number(value);
  return Number.isNaN(n) ? null : n;
}

function nowMs() {
  return Date.now();
}

function castFilterValue(kind, value) {
  if (kind === 'int') {
    const n = parseInt(value, 10);
    return Number.isNaN(n) ? value : n;
  }
  if (kind === 'boolean') {
    return value === 'true' || value === '1' ? 1 : 0;
  }
  if (kind === 'enum') {
    return liveStatusToId(value);
  }
  return value;
}

function buildWhere(filters, columnKinds) {
  const where = [];
  const params = [];
  for (const [key, rawValue] of Object.entries(filters)) {
    if (rawValue === undefined || rawValue === null || rawValue === '') {
      continue;
    }
    const kind = columnKinds[key];
    if (!kind) {
      continue;
    }
    where.push(`"${snakeCase(key)}" = ?`);
    params.push(castFilterValue(kind, rawValue));
  }
  return { clause: where.length ? `WHERE ${where.join(' AND ')}` : '', params };
}

function snakeCase(camel) {
  return camel.replace(/([A-Z])/g, (m) => `_${m.toLowerCase()}`);
}

const VIDEO_FILTER_KINDS = {
  pkid: 'int',
  name: 'string',
  videoPath: 'string',
  extension: 'string',
  liveStatus: 'enum',
  lastTimeStampBeforeStop: 'int',
  message: 'string',
  shouldBeStop: 'boolean',
  startDateLive: 'string',
  channelName: 'string',
};

const SETTING_FILTER_KINDS = {
  id: 'int',
  streamUrl: 'string',
  streamKey: 'string',
  platformStreamName: 'string',
  description: 'string',
  videoFolder: 'string',
  isActive: 'boolean',
  channelName: 'string',
};

const VIDEO_SETTING_FILTER_KINDS = {
  id: 'int',
  title: 'string',
  videoCodec: 'int',
  videoCodecName: 'string',
  pixelFormat: 'int',
  videoBitrate: 'int',
  videoFormat: 'string',
  lastModified: 'string',
  gopSize: 'int',
  isVideoAndAudioSettingActive: 'boolean',
};

function isVideoExtension(extension) {
  return extension != null && VIDEO_EXTENSIONS.has(extension.toLowerCase());
}

/* ------------------------------ SETTING ------------------------------ */

const settingSelect = `SELECT id, stream_url, stream_key, platform_stream_name, description,
  video_folder, is_active, channel_name FROM setting`;

function settingRowToDto(row) {
  if (!row) return null;
  return {
    id: toInt(row.id),
    streamUrl: row.stream_url,
    streamKey: row.stream_key,
    platformStreamName: row.platform_stream_name,
    description: row.description,
    videoFolder: row.video_folder,
    gopSize: null,
    lastModified: null,
    isActive: toBool(row.is_active),
    channelName: row.channel_name,
  };
}

function findSettingByStreamUrlAndStreamKey(streamUrl, streamKey) {
  const row = db
    .prepare(`${settingSelect} WHERE stream_url = ? AND stream_key = ?`)
    .get(streamUrl, streamKey);
  return row || null;
}

function findSettingById(id) {
  const row = db.prepare(`${settingSelect} WHERE id = ?`).get(id);
  return row || null;
}

function saveSetting(record) {
  const result = db
    .prepare(
      `INSERT INTO setting (stream_url, stream_key, platform_stream_name, description,
         video_folder, is_active, channel_name)
       VALUES (?, ?, ?, ?, ?, ?, ?)`,
    )
    .run(
      record.streamUrl,
      record.streamKey,
      record.platformStreamName ?? null,
      record.description ?? null,
      record.videoFolder ?? '/',
      record.isActive ? 1 : 0,
      record.channelName ?? 'Zingy',
    );
  return Number(result.lastInsertRowid);
}

function updateSetting(id, record) {
  db.prepare(
    `UPDATE setting SET stream_url = ?, stream_key = ?, platform_stream_name = ?,
       description = ?, video_folder = ?, is_active = ?, channel_name = ?
     WHERE id = ?`,
  ).run(
    record.streamUrl,
    record.streamKey,
    record.platformStreamName ?? null,
    record.description ?? null,
    record.videoFolder ?? '/',
    record.isActive ? 1 : 0,
    record.channelName ?? 'Zingy',
    id,
  );
}

function deleteSetting(id) {
  db.prepare('DELETE FROM setting WHERE id = ?').run(id);
}

function findSettings(filters) {
  const { clause, params } = buildWhere(filters, SETTING_FILTER_KINDS);
  const rows = db.prepare(`${settingSelect} ${clause}`).all(...params);
  return rows.map(settingRowToDto);
}

function retrieveChannels(filters) {
  return findSettings(filters).map((s) => s.channelName);
}

/* ---------------------------- VIDEO SETTING ---------------------------- */

const videoSettingSelect = `SELECT vs.id, vs.title, vs.video_codec, vs.video_codec_name,
  vs.pixel_format, vs.video_bitrate, vs.video_format, vs.last_modified, vs.gop_size,
  vs.is_video_and_audio_setting_active, vs.audio_setting_id,
  a.audio_codec AS audio_codec, a.audio_bitrate AS audio_bitrate
FROM video_setting vs
LEFT JOIN audio_setting a ON a.id = vs.audio_setting_id`;

function videoSettingRowToRecord(row) {
  if (!row) return null;
  const options = db
    .prepare('SELECT option_key, option_value FROM video_settings_options WHERE video_setting_id = ?')
    .all(row.id)
    .map((o) => ({ key: o.option_key, value: o.option_value }));
  return {
    id: toInt(row.id),
    title: row.title,
    videoCodec: toInt(row.video_codec),
    videoCodecName: row.video_codec_name,
    pixelFormat: toInt(row.pixel_format),
    videoBitrate: toInt(row.video_bitrate),
    lastModified: row.last_modified,
    isVideoAndAudioSettingActive: toBool(row.is_video_and_audio_setting_active),
    gopSize: toInt(row.gop_size),
    videoOptions: options,
    videoFormat: row.video_format,
    audioSettingRecord:
      row.audio_setting_id != null
        ? { audioCodec: toInt(row.audio_codec), audioBitrate: toInt(row.audio_bitrate) }
        : null,
  };
}

function insertAudioSetting(record) {
  const result = db
    .prepare('INSERT INTO audio_setting (audio_codec, audio_bitrate) VALUES (?, ?)')
    .run(
      record?.audioCodec != null ? Number(record.audioCodec) : null,
      record?.audioBitrate != null ? Number(record.audioBitrate) : null,
    );
  return Number(result.lastInsertRowid);
}

function updateAudioSetting(audioSettingId, record) {
  db.prepare('UPDATE audio_setting SET audio_codec = ?, audio_bitrate = ? WHERE id = ?').run(
    record?.audioCodec != null ? Number(record.audioCodec) : null,
    record?.audioBitrate != null ? Number(record.audioBitrate) : null,
    audioSettingId,
  );
}

function insertVideoSettingOptions(settingId, options) {
  const stmt = db.prepare(
    'INSERT INTO video_settings_options (video_setting_id, option_key, option_value) VALUES (?, ?, ?)',
  );
  for (const option of options || []) {
    stmt.run(settingId, option.key ?? null, option.value ?? null);
  }
}

function insertVideoSetting(record) {
  const audioSettingId = record.audioSettingRecord
    ? insertAudioSetting(record.audioSettingRecord)
    : null;
  const result = db
    .prepare(
      `INSERT INTO video_setting (title, video_codec, video_codec_name, pixel_format,
         video_bitrate, video_format, last_modified, gop_size, is_video_and_audio_setting_active,
         audio_setting_id)
       VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`,
    )
    .run(
      record.title ?? 'Title',
      record.videoCodec != null ? Number(record.videoCodec) : null,
      record.videoCodecName ?? null,
      record.pixelFormat != null ? Number(record.pixelFormat) : null,
      record.videoBitrate != null ? Number(record.videoBitrate) : null,
      record.videoFormat ?? null,
      record.lastModified ?? localDateTimeNow(),
      record.gopSize != null ? Number(record.gopSize) : 2,
      record.isVideoAndAudioSettingActive ? 1 : 0,
      audioSettingId,
    );
  const settingId = Number(result.lastInsertRowid);
  insertVideoSettingOptions(settingId, record.videoOptions);
  return settingId;
}

function updateVideoSetting(id, record) {
  const existing = db.prepare('SELECT audio_setting_id FROM video_setting WHERE id = ?').get(id);
  let audioSettingId = existing?.audio_setting_id ?? null;
  if (record.audioSettingRecord) {
    if (audioSettingId != null) {
      updateAudioSetting(audioSettingId, record.audioSettingRecord);
    } else {
      audioSettingId = insertAudioSetting(record.audioSettingRecord);
    }
  }
  db.prepare(
    `UPDATE video_setting SET title = ?, video_codec = ?, video_codec_name = ?,
       pixel_format = ?, video_bitrate = ?, video_format = ?, last_modified = ?,
       gop_size = ?, is_video_and_audio_setting_active = ?, audio_setting_id = ?
     WHERE id = ?`,
  ).run(
    record.title ?? 'Title',
    record.videoCodec != null ? Number(record.videoCodec) : null,
    record.videoCodecName ?? null,
    record.pixelFormat != null ? Number(record.pixelFormat) : null,
    record.videoBitrate != null ? Number(record.videoBitrate) : null,
    record.videoFormat ?? null,
    record.lastModified ?? localDateTimeNow(),
    record.gopSize != null ? Number(record.gopSize) : 2,
    record.isVideoAndAudioSettingActive ? 1 : 0,
    audioSettingId,
    id,
  );
  db.prepare('DELETE FROM video_settings_options WHERE video_setting_id = ?').run(id);
  insertVideoSettingOptions(id, record.videoOptions);
}

function findVideoSettingById(id) {
  const row = db.prepare(`${videoSettingSelect} WHERE vs.id = ?`).get(id);
  return videoSettingRowToRecord(row);
}

function findVideoSettings(filters) {
  const { clause, params } = buildWhere(filters, VIDEO_SETTING_FILTER_KINDS);
  const rows = db.prepare(`${videoSettingSelect} ${clause}`).all(...params);
  return rows.map(videoSettingRowToRecord);
}

function deleteVideoSetting(id) {
  const existing = db.prepare('SELECT audio_setting_id FROM video_setting WHERE id = ?').get(id);
  db.prepare('UPDATE video SET video_setting_id = NULL WHERE video_setting_id = ?').run(id);
  db.prepare('DELETE FROM video_settings_options WHERE video_setting_id = ?').run(id);
  db.prepare('DELETE FROM video_setting WHERE id = ?').run(id);
  if (existing?.audio_setting_id != null) {
    db.prepare('DELETE FROM audio_setting WHERE id = ?').run(existing.audio_setting_id);
  }
}

/* ---------------------------- VIDEO HISTORY ---------------------------- */

const historySelect = `SELECT pkid, folder_of_video_to_stream, local_date_time_start_live,
  stream_url, stream_key, platform_stream_name, user_name FROM video_live_history`;

function historyRowToRecord(row) {
  if (!row) return null;
  return {
    pkid: toInt(row.pkid),
    folderOfVideoToStream: row.folder_of_video_to_stream,
    localDateTimeStartLive: row.local_date_time_start_live,
    streamUrl: row.stream_url,
    streamKey: row.stream_key,
    platformStreamName: row.platform_stream_name,
    userName: row.user_name,
  };
}

function saveVideoLiveHistory({ folderOfVideoToStream, startDate, streamUrl, streamKey, platformStreamName, userName }) {
  const result = db
    .prepare(
      `INSERT INTO video_live_history (folder_of_video_to_stream, local_date_time_start_live,
         stream_url, stream_key, platform_stream_name, user_name)
       VALUES (?, ?, ?, ?, ?, ?)`,
    )
    .run(
      folderOfVideoToStream,
      startDate ?? localDateTimeNow(),
      streamUrl ?? 'N/A',
      streamKey ?? 'N/A',
      platformStreamName ?? 'N/A',
      userName ?? 'Mario',
    );
  return Number(result.lastInsertRowid);
}

function findHistoryByFolderAndStartDate(folder, startDate) {
  const row = db
    .prepare(`${historySelect} WHERE folder_of_video_to_stream = ? AND local_date_time_start_live = ?`)
    .get(folder, startDate);
  return historyRowToRecord(row);
}

function findHistoryById(id) {
  const row = db.prepare(`${historySelect} WHERE pkid = ?`).get(id);
  return historyRowToRecord(row);
}

/* ------------------------------ VIDEO ------------------------------ */

const videoSelect = `SELECT v.pkid, v.name, v.video_path, v.extension, v.live_status,
  v.last_time_stamp_before_stop, v.message, v.should_be_stop, v.start_date_live,
  v.channel_name, v.video_live_history_pkid, v.video_setting_id
FROM video v`;

function videoRowToDto(row) {
  if (!row) return null;
  return {
    pkid: toInt(row.pkid),
    name: row.name,
    videoPath: row.video_path,
    extension: row.extension,
    liveStatus: liveStatusIdToName(row.live_status),
    lastTimeStampBeforeStop: row.last_time_stamp_before_stop != null ? Number(row.last_time_stamp_before_stop) : null,
    message: row.message,
    shouldBeStop: toBool(row.should_be_stop),
    startDateLive: row.start_date_live,
    channelName: row.channel_name,
  };
}

function videoRowToRecord(row) {
  if (!row) return null;
  const dto = videoRowToDto(row);
  const history = row.video_live_history_pkid != null
    ? findHistoryById(row.video_live_history_pkid)
    : null;
  const videoSetting = row.video_setting_id != null
    ? findVideoSettingById(row.video_setting_id)
    : null;
  return {
    ...dto,
    videoLiveHistory: history,
    videoSetting,
  };
}

function findVideoByPkid(pkid) {
  const row = db.prepare(`${videoSelect} WHERE v.pkid = ?`).get(pkid);
  return row ? videoRowToRecord(row) : null;
}

function findVideoByPathAndHistory(videoPath, historyPkid) {
  const row = db
    .prepare(`${videoSelect} WHERE v.video_path = ? AND v.video_live_history_pkid = ?`)
    .get(videoPath, historyPkid);
  return row ? videoRowToRecord(row) : null;
}

function findVideosByHistoryPkid(historyPkid) {
  const rows = db
    .prepare(`${videoSelect} WHERE v.video_live_history_pkid = ?`)
    .all(historyPkid);
  return rows.map(videoRowToRecord);
}

function findVideosByLiveStatusAndChannelName(liveStatus, channelName) {
  const rows = db
    .prepare(`${videoSelect} WHERE v.live_status = ? AND v.channel_name = ?`)
    .all(liveStatusToId(liveStatus), channelName);
  return rows.map(videoRowToRecord);
}

function findVideosByLiveStatus(liveStatus) {
  const rows = db.prepare(`${videoSelect} WHERE v.live_status = ?`).all(liveStatusToId(liveStatus));
  return rows.map(videoRowToRecord);
}

function insertVideo({
  name,
  videoPath,
  extension,
  liveStatus = 'OFFLINE',
  lastTimeStampBeforeStop = 0,
  message = null,
  shouldBeStop = false,
  startDateLive,
  channelName,
  videoLiveHistoryPkid,
  videoSettingId,
}) {
  const result = db
    .prepare(
      `INSERT INTO video (name, video_path, extension, live_status, last_time_stamp_before_stop,
         message, should_be_stop, start_date_live, channel_name, video_live_history_pkid,
         video_setting_id)
       VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`,
    )
    .run(
      name,
      videoPath,
      extension,
      liveStatusToId(liveStatus),
      lastTimeStampBeforeStop ?? 0,
      message,
      shouldBeStop ? 1 : 0,
      startDateLive ?? null,
      channelName ?? 'Zingy',
      videoLiveHistoryPkid ?? null,
      videoSettingId ?? null,
    );
  return Number(result.lastInsertRowid);
}

function updateVideoLifeStatus(pkid, { message, liveStatus, startDateLive }) {
  db.prepare(
    `UPDATE video SET message = ?, live_status = ?, start_date_live = COALESCE(?, start_date_live)
     WHERE pkid = ?`,
  ).run(message ?? null, liveStatusToId(liveStatus), startDateLive ?? null, pkid);
}

function setVideoShouldBeStop(pkid, value) {
  db.prepare('UPDATE video SET should_be_stop = ? WHERE pkid = ?').run(value ? 1 : 0, pkid);
}

function linkVideoToSetting(videoPkid, videoSettingId) {
  db.prepare('UPDATE video SET video_setting_id = ? WHERE pkid = ?').run(videoSettingId, videoPkid);
}

function findAllVideos(filters, { sortBy, sortDirection, offset, limit }) {
  const { clause, params } = buildWhere(filters, VIDEO_FILTER_KINDS);
  const orderClause = sortBy
    ? `ORDER BY "${snakeCase(sortBy)}" ${sortDirection === 'ASC' ? 'ASC' : 'DESC'}`
    : '';
  const totalRow = db
    .prepare(`SELECT COUNT(*) AS total FROM video ${clause}`)
    .get(...params);
  const rows = db
    .prepare(`${videoSelect} ${clause} ${orderClause} LIMIT ? OFFSET ?`)
    .all(...params, limit, offset);
  return {
    total: Number(totalRow.total),
    rows: rows.map(videoRowToRecord),
  };
}

function allVideosCount() {
  const row = db.prepare('SELECT COUNT(*) AS total FROM video').get();
  return Number(row.total);
}

function runInTransaction(fn) {
  db.exec('BEGIN');
  try {
    const result = fn();
    db.exec('COMMIT');
    return result;
  } catch (err) {
    db.exec('ROLLBACK');
    throw err;
  }
}

module.exports = {
  db,
  LIVE_STATUS,
  localDateTimeNow,
  isVideoExtension,
  runInTransaction,
  nowMs,
  /* settings */
  findSettingByStreamUrlAndStreamKey,
  findSettingById,
  saveSetting,
  updateSetting,
  deleteSetting,
  findSettings,
  retrieveChannels,
  /* video settings */
  findVideoSettingById,
  findVideoSettings,
  insertVideoSetting,
  updateVideoSetting,
  deleteVideoSetting,
  /* history */
  saveVideoLiveHistory,
  findHistoryByFolderAndStartDate,
  findHistoryById,
  /* video */
  findVideoByPkid,
  findVideoByPathAndHistory,
  findVideosByHistoryPkid,
  findVideosByLiveStatusAndChannelName,
  findVideosByLiveStatus,
  insertVideo,
  updateVideoLifeStatus,
  setVideoShouldBeStop,
  linkVideoToSetting,
  findAllVideos,
  allVideosCount,
};
