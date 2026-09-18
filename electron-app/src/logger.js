const fs = require('fs');
const path = require('path');
const config = require('./config');

const logFile = path.join(config.dataDir, 'logs', 'stream.log');

let stream = null;
try {
  fs.mkdirSync(path.dirname(logFile), { recursive: true });
  stream = fs.createWriteStream(logFile, { flags: 'a' });
} catch (err) {
  stream = null;
}

function format(level, args) {
  const timestamp = new Date().toISOString();
  const message = args
    .map((a) => (typeof a === 'string' ? a : JSON.stringify(a)))
    .join(' ');
  return `${timestamp} [${level}] ${message}`;
}

function write(level, args) {
  const line = format(level, args);
  if (process.env.ORBIS_SILENT !== '1') {
    const consoleFn = level === 'ERROR' ? console.error : level === 'WARN' ? console.warn : console.log;
    consoleFn(line);
  }
  if (stream) {
    stream.write(line + '\n');
  }
}

module.exports = {
  info: (...args) => write('INFO', args),
  warn: (...args) => write('WARN', args),
  error: (...args) => write('ERROR', args),
  debug: (...args) => {
    if (process.env.ORBIS_DEBUG === '1') {
      write('DEBUG', args);
    }
  },
};
