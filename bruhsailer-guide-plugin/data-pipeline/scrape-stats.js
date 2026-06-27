import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const SHEET_URL = 'https://docs.google.com/spreadsheets/d/1XZ-3Kja7_QS4Rxj4mJXeATXHBP03lrdUSt46gO3pWHk/export?format=csv&gid=1506136399';
const OUT_PATH = path.resolve(__dirname, '../src/main/resources/stats.json');

const SKILLS = [
  'ATK', 'STR', 'DEF', 'HP', 'RNG', 'PRY', 'MAG', 'RC', 'CON', 'AGI',
  'HRB', 'THI', 'CRF', 'FLE', 'SLY', 'HUN', 'MNG', 'SMI', 'FSH', 'COK',
  'FM', 'WC', 'FRM', 'SAI'
];

function parseCsvLine(line) {
  const fields = [];
  let current = '';
  let inQuotes = false;

  for (let i = 0; i < line.length; i++) {
    const ch = line[i];
    if (ch === '"') {
      if (inQuotes && i + 1 < line.length && line[i + 1] === '"') {
        current += '"';
        i++;
      } else {
        inQuotes = !inQuotes;
      }
    } else if (ch === ',' && !inQuotes) {
      fields.push(current);
      current = '';
    } else {
      current += ch;
    }
  }
  fields.push(current);
  return fields;
}

function normalizeNumber(raw) {
  if (!raw || raw.trim() === '') return '';
  // Handle European-style thousands separators like "12.230.750" or "67,5"
  const cleaned = raw.trim().replace(/\./g, '').replace(/,/g, '');
  return cleaned;
}

function toNum(raw) {
  const n = normalizeNumber(raw);
  if (n === '') return 0;
  return Number(n);
}

async function main() {
  const res = await fetch(SHEET_URL);
  if (!res.ok) {
    throw new Error(`Failed to fetch sheet: ${res.status} ${res.statusText}`);
  }
  const csvText = await res.text();
  const lines = csvText.split(/\r?\n/).filter(l => l.trim() !== '');

  // Skip header rows (row 0 = column group headers, row 1 = skill names)
  const dataLines = lines.slice(2);

  let currentChapter = null;
  const steps = {};
  let currentStepNum = null;
  let currentBlock = [];

  function flushBlock() {
    if (currentStepNum === null || currentBlock.length === 0) return;
    const stepId = `new-${currentChapter}-${currentStepNum}`;

    let gpChangeSum = 0;
    for (const row of currentBlock) {
      const gpChangeRaw = row[3] || '';
      if (gpChangeRaw.trim() !== '') {
        gpChangeSum += toNum(gpChangeRaw);
      }
    }

    let gpTotal = '';
    for (let i = currentBlock.length - 1; i >= 0; i--) {
      const val = (currentBlock[i][4] || '').trim();
      if (val !== '') {
        gpTotal = normalizeNumber(val);
        break;
      }
    }

    const lastRow = currentBlock[currentBlock.length - 1];
    const levels = {};
    for (let s = 0; s < SKILLS.length; s++) {
      const skill = SKILLS[s];
      const val = (lastRow[5 + s] || '').trim();
      if (val !== '') {
        levels[skill] = toNum(val);
      }
    }

    steps[stepId] = {
      gpChange: String(gpChangeSum),
      gpTotal: gpTotal,
      levels
    };
  }

  for (const line of dataLines) {
    const fields = parseCsvLine(line);
    const stepVal = (fields[1] || '').trim();
    const activityVal = (fields[2] || '').trim();

    // Section header: empty Step cell, Activity starts with "X.Y:"
    if (stepVal === '' && /^\d+\.\d+:/.test(activityVal)) {
      flushBlock();
      currentBlock = [];
      currentStepNum = null;
      const chapterMatch = activityVal.match(/^(\d+)\.\d+:/);
      if (chapterMatch) {
        currentChapter = chapterMatch[1];
      }
      continue;
    }

    // Numbered step row
    if (stepVal !== '' && /^\d+$/.test(stepVal)) {
      flushBlock();
      currentBlock = [fields];
      currentStepNum = stepVal;
      continue;
    }

    // Sub-step row (empty Step cell, not a section header)
    if (stepVal === '' && currentStepNum !== null) {
      currentBlock.push(fields);
    }
  }

  flushBlock();

  const output = {
    sourceUrl: SHEET_URL,
    exportedAt: new Date().toISOString(),
    skills: SKILLS,
    steps
  };

  fs.mkdirSync(path.dirname(OUT_PATH), { recursive: true });
  fs.writeFileSync(OUT_PATH, JSON.stringify(output, null, 2));

  console.log(`Wrote ${Object.keys(steps).length} steps to ${OUT_PATH}`);
}

main().catch(err => {
  console.error(err);
  process.exit(1);
});

export { parseCsvLine, normalizeNumber, toNum, main };
