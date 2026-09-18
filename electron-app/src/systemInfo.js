const si = require('systeminformation');

const NOT_FOUND = -1;

async function getCpuPercent() {
  const load = await si.currentLoad();
  const value = load.currentLoad ?? load.avgLoad ?? 0;
  return Math.round(value);
}

async function getRamPercent() {
  const mem = await si.mem();
  const total = mem.total;
  const used = total - mem.available;
  if (!total) {
    return 0;
  }
  return Math.round((used * 100.0) / total);
}

async function getSwapPercent() {
  const mem = await si.mem();
  const total = mem.swaptotal;
  const used = mem.swapused;
  if (!total) {
    return 0;
  }
  return Math.round((used * 100.0) / total);
}

async function getCpuTemp() {
  const temp = await si.cpuTemperature();
  const value = temp.main ?? temp.cores?.[0] ?? NaN;
  if (value === null || value === undefined || Number.isNaN(value) || value < 0) {
    return NOT_FOUND;
  }
  return Math.round(value);
}

async function getAllSystemInfo() {
  return [
    { field: 'CPU', value: await getCpuPercent() },
    { field: 'RAM', value: await getRamPercent() },
    { field: 'SWAP', value: await getSwapPercent() },
    { field: 'TEMPERATURA CPU', value: await getCpuTemp() },
  ];
}

module.exports = {
  getAllSystemInfo,
  getCpuPercent,
  getRamPercent,
  getSwapPercent,
  getCpuTemp,
};
