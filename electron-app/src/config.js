const path = require('path');

const APP_DIR = path.join(__dirname, '..');

let dataDir = process.env.ORBIS_DATA_DIR || path.join(APP_DIR, 'data');
let imagesDir = process.env.ORBIS_IMAGES_DIR || path.join(APP_DIR, 'images');

const config = {
  port: parseInt(process.env.PORT || '1200', 10),
  host: process.env.HOST || 'localhost',
  appDir: APP_DIR,
  publicDir: path.join(APP_DIR, 'public'),
  ffmpegPath: process.env.FFMPEG_PATH || 'ffmpeg',
  get dataDir() {
    return dataDir;
  },
  get imagesDir() {
    return imagesDir;
  },
  get dbFile() {
    return path.join(dataDir, 'stream.db');
  },
  setDataPaths(userDataPath) {
    dataDir = path.join(userDataPath, 'data');
    imagesDir = path.join(userDataPath, 'images');
  },
};

module.exports = config;
