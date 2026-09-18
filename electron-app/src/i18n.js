const fs = require('fs');
const path = require('path');

const LOCALES = ['it', 'en', 'de', 'fr', 'es', 'ru', 'zh'];
const DEFAULT_LOCALE = 'en';

const messagesDir = path.join(__dirname, '..', 'messages');

function parseProperties(content) {
  const result = {};
  for (const rawLine of content.split(/\r?\n/)) {
    let line = rawLine;
    if (!line || line.trimStart().startsWith('#') || line.trimStart().startsWith('!')) {
      continue;
    }
    if (line.includes('\\')) {
      line = line.replace(/\\n/g, '\n').replace(/\\:/g, ':').replace(/\\=/g, '=');
    }
    const separatorIndex = line.indexOf('=');
    if (separatorIndex === -1) {
      continue;
    }
    const key = line.slice(0, separatorIndex).trim();
    let value = line.slice(separatorIndex + 1);
    if (value.startsWith(' ')) {
      value = value.slice(1);
    }
    result[key] = value;
  }
  return result;
}

function loadMessages() {
  const messages = {};
  for (const locale of LOCALES) {
    const file = path.join(messagesDir, `messages_${locale}.properties`);
    if (fs.existsSync(file)) {
      messages[locale] = parseProperties(fs.readFileSync(file, 'utf-8'));
    }
  }
  return messages;
}

const messages = loadMessages();

function normalizeLocale(locale) {
  if (!locale) {
    return DEFAULT_LOCALE;
  }
  const lang = locale.toLowerCase().split('-')[0];
  return LOCALES.includes(lang) ? lang : DEFAULT_LOCALE;
}

function resolveLocale(langParam, acceptLanguage) {
  if (langParam && LOCALES.includes(langParam.toLowerCase())) {
    return langParam.toLowerCase();
  }
  if (acceptLanguage) {
    const preferred = acceptLanguage
      .split(',')
      .map((part) => part.trim().split(';')[0])
      .map((part) => part.toLowerCase().split('-')[0])
      .find((part) => LOCALES.includes(part));
    if (preferred) {
      return preferred;
    }
  }
  return DEFAULT_LOCALE;
}

function formatMessage(template, params) {
  if (!params || params.length === 0) {
    return template;
  }
  return template.replace(/\{(\d+)\}/g, (match, index) => {
    const value = params[Number(index)];
    return value !== undefined ? String(value) : match;
  });
}

function translate(code, locale, params) {
  const lang = normalizeLocale(locale);
  const table = messages[lang] || messages[DEFAULT_LOCALE];
  const template = table[code] ?? messages[DEFAULT_LOCALE][code] ?? code;
  return formatMessage(template, params);
}

module.exports = {
  LOCALES,
  DEFAULT_LOCALE,
  messages,
  resolveLocale,
  translate,
};
