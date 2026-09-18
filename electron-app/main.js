const { app, BrowserWindow, dialog } = require('electron');
const path = require('path');
const fs = require('fs');
const config = require('./src/config');
const logger = require('./src/logger');
const streaming = require('./src/streaming');
const { startServer } = require('./src/server');

const isDev = process.argv.includes('--dev');

if (app.isPackaged) {
  config.setDataPaths(app.getPath('userData'));
  seedDefaultImages();
}

let mainWindow = null;
let server = null;

const gotTheLock = app.requestSingleInstanceLock();
if (!gotTheLock) {
  app.quit();
} else {
  app.on('second-instance', () => {
    if (mainWindow) {
      if (mainWindow.isMinimized()) {
        mainWindow.restore();
      }
      mainWindow.focus();
    }
  });

  app.whenReady().then(async () => {
    try {
      server = await startServer();
    } catch (err) {
      logger.error(`server failed to start: ${err.message}`);
      dialog.showErrorBox(
        'Orbis Stream - Errore',
        `Impossibile avviare il server sulla porta ${config.port}.\n${err.message}`,
      );
      app.quit();
      return;
    }

    createWindow();

    app.on('activate', () => {
      if (BrowserWindow.getAllWindows().length === 0) {
        createWindow();
      }
    });
  });
}

function createWindow() {
  mainWindow = new BrowserWindow({
    width: 1920,
    height: 1080,
    show: false,
    title: 'Orbis Stream',
    webPreferences: {
      preload: path.join(__dirname, 'preload.js'),
      contextIsolation: true,
      nodeIntegration: false,
      spellcheck: false,
    },
  });

  mainWindow.once('ready-to-show', () => {
    mainWindow.show();
    if (isDev) {
      mainWindow.webContents.openDevTools({ mode: 'detach' });
    }
  });

  mainWindow.loadURL(`http://${config.host}:${config.port}/orbis/handler`);

  mainWindow.webContents.on('did-fail-load', (event, errorCode, errorDescription) => {
    logger.error(`page failed to load: ${errorCode} ${errorDescription}`);
  });

  mainWindow.on('closed', () => {
    mainWindow = null;
  });
}

function seedDefaultImages() {
  try {
    fs.mkdirSync(config.imagesDir, { recursive: true });
    if (fs.readdirSync(config.imagesDir).length > 0) {
      return;
    }
    const defaultImage = path.join(process.resourcesPath, 'images', 'HOME.png');
    if (fs.existsSync(defaultImage)) {
      fs.copyFileSync(defaultImage, path.join(config.imagesDir, 'HOME.png'));
    }
  } catch (err) {
    logger.warn(`seed default images failed: ${err.message}`);
  }
}

function shutdown() {
  try {
    streaming.killAllStreams();
  } catch (err) {
    logger.warn(`kill streams failed: ${err.message}`);
  }
  if (server) {
    try {
      server.close();
    } catch (err) {
      logger.warn(`server close failed: ${err.message}`);
    }
  }
}

app.on('window-all-closed', () => {
  shutdown();
  app.quit();
});

app.on('before-quit', () => {
  shutdown();
});
