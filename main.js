const { app, BrowserWindow } = require('electron');
const { spawn } = require('child_process');
const http = require('http');
const path = require('path');

let mainWindow;
let javaProcess;

const port = 1200;
const url = `http://localhost:${port}/orbis/handler`;

function createWindow() {
  mainWindow = new BrowserWindow({
    width: 1920,
    height: 1080,
    webPreferences: {
      nodeIntegration: false, // Security best practice
      contextIsolation: true  // Security best practice
    }
  });

  // Rimuovi il menu predefinito (opzionale)
  // mainWindow.setMenu(null);

  mainWindow.loadURL(url);

  mainWindow.on('closed', () => {
    mainWindow = null;
  });
}

function startJavaBackend() {
  // Avvia il jar compilato
  // Nota: Questo presuppone che il file jar si trovi nella cartella target/
  // e si chiami stream-0.0.1-SNAPSHOT.jar (come definito nel pom.xml)
  const jarPath = path.join(__dirname, 'target', 'stream-0.0.1-SNAPSHOT.jar');

  console.log(`Avvio del backend Java da: ${jarPath}`);

  javaProcess = spawn('java', ['-jar', jarPath]);

  javaProcess.stdout.on('data', (data) => {
    console.log(`[Java STDOUT]: ${data}`);
  });

  javaProcess.stderr.on('data', (data) => {
    console.error(`[Java STDERR]: ${data}`);
  });

  javaProcess.on('close', (code) => {
    console.log(`Il processo Java si è chiuso con codice ${code}`);
  });
}

function waitForServer(callback) {
  const checkInterval = 1000; // 1 secondo
  const timeout = 60000; // 60 secondi
  let elapsed = 0;

  const intervalId = setInterval(() => {
    console.log('Controllo se il server è online...');
    http.get(url, (res) => {
      // Se risponde con uno status code (anche 404, ecc.), il server è su
      clearInterval(intervalId);
      console.log('Server online, avvio della finestra...');
      callback();
    }).on('error', (err) => {
      // Il server non è ancora pronto
      elapsed += checkInterval;
      if (elapsed >= timeout) {
        clearInterval(intervalId);
        console.error('Timeout: Il server Java non è partito in tempo.');
        app.quit();
      }
    });
  }, checkInterval);
}

app.on('ready', () => {
  startJavaBackend();
  waitForServer(() => {
    createWindow();
  });
});

app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') {
    app.quit();
  }
});

app.on('activate', () => {
  if (mainWindow === null) {
    createWindow();
  }
});

app.on('will-quit', () => {
  if (javaProcess) {
    console.log('Chiusura dell\'applicazione, terminazione del processo Java...');
    // Invia segnale di kill
    javaProcess.kill('SIGTERM');
  }
});
