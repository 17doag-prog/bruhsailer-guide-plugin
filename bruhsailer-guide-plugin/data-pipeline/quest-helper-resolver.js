import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const INDEX_PATH = path.resolve(__dirname, 'quest-helper-index.json');
const MAPPING_PATH = path.resolve(__dirname, 'quest-mapping.json');
const CACHE_DIR = path.resolve(__dirname, '.questhelper-cache');

const USER_AGENT = 'bruhsailer-guide-plugin/0.1 (https://github.com/bobthabuilda; runelite plugin)';
const RAW_BASE = 'https://raw.githubusercontent.com/Zoinkwiz/quest-helper/master/src/main/java/com/questhelper/helpers/quests';

let questIndexMap = null;
let questMapping = null;

function loadQuestIndex() {
  if (questIndexMap) return questIndexMap;

  const entries = JSON.parse(fs.readFileSync(INDEX_PATH, 'utf8'));
  const map = new Map();
  const grouped = new Map();

  for (const { dir, file } of entries) {
    if (!grouped.has(dir)) grouped.set(dir, []);
    grouped.get(dir).push(file);
  }

  for (const [dir, files] of grouped) {
    const mainFile = pickMainFile(dir, files);
    map.set(dir, mainFile);
  }

  questIndexMap = map;
  return questIndexMap;
}

function pickMainFile(dir, files) {
  let best = files[0];
  let bestScore = 0;

  for (const file of files) {
    const base = file.replace(/\.java$/i, '').toLowerCase();
    const dirL = dir.toLowerCase();
    let score = 0;

    if (base === dirL) {
      score = 100;
    } else if (base === dirL.replace(/^the/, '')) {
      score = 90;
    } else if (base.startsWith(dirL) || dirL.startsWith(base)) {
      score = 80;
    } else if (base.includes(dirL) || dirL.includes(base)) {
      score = 60;
    } else if (/start$/i.test(base)) {
      score = 50;
    } else if (/main$/i.test(base)) {
      score = 40;
    }

    if (score > bestScore) {
      bestScore = score;
      best = file;
    }
  }

  return best;
}

function loadMapping() {
  if (questMapping) return questMapping;
  if (!fs.existsSync(MAPPING_PATH)) {
    questMapping = {};
    return questMapping;
  }
  questMapping = JSON.parse(fs.readFileSync(MAPPING_PATH, 'utf8'));
  return questMapping;
}

function normalizeQuestName(name) {
  if (!name) return '';
  let normalized = name.toLowerCase();
  normalized = normalized.replace(/'/g, '');
  normalized = normalized.replace(/[^a-z0-9]/g, '');
  normalized = normalized.replace(/quest$/, '');
  return normalized;
}

function findQuestFile(questName) {
  const mapping = loadMapping();
  const lookupName = questName.toLowerCase().trim();

  if (mapping[lookupName]) {
    questName = mapping[lookupName];
  }

  const normalized = normalizeQuestName(questName);
  if (!normalized) return null;

  const index = loadQuestIndex();

  if (index.has(normalized)) {
    return { dir: normalized, file: index.get(normalized) };
  }

  const withThe = 'the' + normalized;
  if (index.has(withThe)) {
    return { dir: withThe, file: index.get(withThe) };
  }

  if (normalized.endsWith('s')) {
    const withoutS = normalized.slice(0, -1);
    if (index.has(withoutS)) {
      return { dir: withoutS, file: index.get(withoutS) };
    }
  }

  // Try stripping trailing apostrophe-s that might have been removed
  if (normalized.endsWith('s')) {
    const withoutApostropheS = normalized.slice(0, -2);
    if (index.has(withoutApostropheS)) {
      return { dir: withoutApostropheS, file: index.get(withoutApostropheS) };
    }
  }

  return null;
}

function extractBalancedArgs(content, startIdx) {
  let depth = 1;
  let i = startIdx;
  while (i < content.length && depth > 0) {
    if (content[i] === '(') depth++;
    else if (content[i] === ')') depth--;
    i++;
  }
  if (depth !== 0) return null;
  return content.slice(startIdx, i - 1);
}

function parseWorldPoint(args) {
  const wpMatch = args.match(/new\s+WorldPoint\s*\(\s*(-?\d+)\s*,\s*(-?\d+)(?:\s*,\s*(-?\d+))?\s*\)/);
  if (!wpMatch) return null;
  return {
    x: parseInt(wpMatch[1], 10),
    y: parseInt(wpMatch[2], 10),
    plane: wpMatch[3] ? parseInt(wpMatch[3], 10) : 0
  };
}

async function resolveQuestHelper(questName) {
  const target = findQuestFile(questName);
  if (!target) return null;

  const { dir, file } = target;
  const cacheFile = path.join(CACHE_DIR, `${dir}_${file}`);

  let content;
  if (fs.existsSync(cacheFile)) {
    content = fs.readFileSync(cacheFile, 'utf8');
  } else {
    await sleep(300);

    const url = `${RAW_BASE}/${dir}/${file}`;
    try {
      const response = await fetch(url, {
        headers: { 'User-Agent': USER_AGENT }
      });
      if (!response.ok) {
        return null;
      }
      content = await response.text();

      if (!fs.existsSync(CACHE_DIR)) {
        fs.mkdirSync(CACHE_DIR, { recursive: true });
      }
      fs.writeFileSync(cacheFile, content, 'utf8');
    } catch (err) {
      return null;
    }
  }

  const stepRegex = /new\s+(NpcStep|ObjectStep|DetailedQuestStep|ItemStep)\s*\(/s;
  const match = content.match(stepRegex);
  if (!match) return null;

  const stepType = match[1];
  const args = extractBalancedArgs(content, match.index + match[0].length);
  if (!args) return null;

  const worldPoint = parseWorldPoint(args);
  if (!worldPoint) return null;

  return {
    worldPoint,
    npcIds: [],
    objectIds: [],
    stepType
  };
}

function sleep(ms) {
  return new Promise(r => setTimeout(r, ms));
}

export {
  loadQuestIndex,
  normalizeQuestName,
  findQuestFile,
  resolveQuestHelper
};
