import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';
import { resolveQuestHelper, findQuestFile, loadQuestIndex } from './quest-helper-resolver.js';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const GUIDE_PATH = path.resolve(__dirname, '../src/main/resources/guide.json');
const SEEDS_PATH = path.resolve(__dirname, 'seeds.json');
const OUTPUT_PATH = path.resolve(__dirname, '../src/main/resources/spatial.json');
const CACHE_DIR = path.resolve(__dirname, '.wiki-cache');

const USER_AGENT = 'bruhsailer-guide-plugin/0.1 (https://github.com/bobthabuilda; runelite plugin)';
const WIKI_API = 'https://oldschool.runescape.wiki/api.php';

const sleep = (ms) => new Promise(r => setTimeout(r, ms));

const PATTERNS = [
  /Talk (?:to|with) ([A-Z][a-zA-Z' ]+?)(?:[,.]|\s+to\s)/,
  /Speak (?:to|with) ([A-Z][a-zA-Z' ]+?)(?:[,.]|\s+to\s)/,
  /speak with ([A-Z][a-zA-Z' ]+?)(?:[,.]|\s+to\s)/,
  /pay ([A-Z][a-zA-Z' ]+?)(?:[,.]|\s+to\s)/,
  /visit ([A-Z][a-zA-Z' ]+?)(?:[,.]|\s+to\s)/,
  /ask (?:the )?([A-Z][a-zA-Z' ]+?)(?:[,.]|\s+to\s)/,
  /give (?:the pieces of perfect jewellery to|the [^,.]+ to )?([A-Z][a-zA-Z' ]+?)(?:[,.]|\s+to\s)/,
  /[Hh]ave ([A-Z][a-zA-Z' ]+?)(?:[,.]|\s+to\s)/,
  /(?:buy|collect|claim|obtain|receive)\b[^.]{0,80}?\bfrom (?:the )?([A-Z][a-zA-Z' ]+?)(?:[,.]|\s+to\s)/,
];

const STOP_WORDS = new Set(['in','on','at','and','to','for','with','from','near','by','the','a','an','of','up','about','as']);

function cleanName(name) {
  let trimmed = name.trim();
  const sepMatch = trimmed.match(/^(.+?)(?:\s+(?:in|on|at|and|to|for|with|from|near|by|the|a|an|of|up|about|as)\b)/i);
  if (sepMatch) {
    trimmed = sepMatch[1].trim();
  }
  trimmed = trimmed.split('(')[0].trim();
  while (trimmed.length > 0 && /[^a-zA-Z' ]$/.test(trimmed)) {
    trimmed = trimmed.replace(/[^a-zA-Z' ]+$/, '').trim();
  }
  const parts = trimmed.split(/\s+/).filter(p => p.length > 0);
  return parts.join(' ');
}

function extractEntity(text) {
  for (const pattern of PATTERNS) {
    const match = text.match(pattern);
    if (match) {
      const cleaned = cleanName(match[1]);
      if (cleaned.length > 0) {
        return cleaned;
      }
    }
  }
  return null;
}

function sanitizeName(name) {
  return name.replace(/[^a-zA-Z0-9]/g, '_');
}

async function fetchWiki(pageName) {
  const cacheFile = path.join(CACHE_DIR, `${sanitizeName(pageName)}.json`);

  if (fs.existsSync(cacheFile)) {
    return JSON.parse(fs.readFileSync(cacheFile, 'utf8'));
  }

  await sleep(1000);

  const url = `${WIKI_API}?action=parse&page=${encodeURIComponent(pageName)}&prop=wikitext&format=json&redirects=1`;
  const response = await fetch(url, {
    headers: { 'User-Agent': USER_AGENT }
  });

  if (!response.ok) {
    return null;
  }

  const data = await response.json();

  if (!fs.existsSync(CACHE_DIR)) {
    fs.mkdirSync(CACHE_DIR, { recursive: true });
  }
  fs.writeFileSync(cacheFile, JSON.stringify(data, null, 2), 'utf8');

  return data;
}

function parseWikitext(wikitext) {
  if (!wikitext) return null;

  const infoboxMatch = wikitext.match(/\{\{Infobox\s+(NPC|Object)/);
  if (!infoboxMatch) return null;

  const infoboxType = infoboxMatch[1];

  const idMatch = wikitext.match(/\|\s*id\s*=\s*([0-9,\s]+)/);
  let npcIds = [];
  let objectIds = [];
  if (idMatch) {
    const ids = idMatch[1].split(',').map(s => parseInt(s.trim(), 10)).filter(n => !isNaN(n));
    if (infoboxType === 'NPC') {
      npcIds = ids;
    } else {
      objectIds = ids;
    }
  }

  const mapMatch = wikitext.match(/\{\{Map\|([^}]+)\}\}/);
  let worldPoint = null;
  if (mapMatch) {
    const mapContent = mapMatch[1];
    const planeMatch = mapContent.match(/plane=(\d+)/);
    const plane = planeMatch ? parseInt(planeMatch[1], 10) : 0;

    const namedXMatch = mapContent.match(/x=(\d+)/);
    const namedYMatch = mapContent.match(/y=(\d+)/);
    const pairMatch = mapContent.match(/(\d+),(\d+)/);

    if (namedXMatch && namedYMatch) {
      worldPoint = {
        x: parseInt(namedXMatch[1], 10),
        y: parseInt(namedYMatch[1], 10),
        plane
      };
    } else if (pairMatch) {
      worldPoint = {
        x: parseInt(pairMatch[1], 10),
        y: parseInt(pairMatch[2], 10),
        plane
      };
    }
  }

  return { npcIds, objectIds, worldPoint };
}

async function resolveEntity(entityName) {
  const data = await fetchWiki(entityName);
  if (!data || !data.parse || !data.parse.wikitext) return null;

  const wikitext = data.parse.wikitext['*'];

  const infoboxTypeMatch = wikitext.match(/\{\{Infobox\s+(NPC|Object)/);
  if (!infoboxTypeMatch && wikitext.match(/\{\{Infobox\s+Quest/)) {
    const questNpc = await extractQuestStartNpc(wikitext);
    if (questNpc && questNpc !== entityName) {
      return resolveEntity(questNpc);
    }
    return null;
  }

  return parseWikitext(wikitext);
}

async function extractQuestStartNpc(wikitext) {
  const startMatch = wikitext.match(/\|\s*start\s*=\s*([^|}\n]+)/);
  if (!startMatch) return null;

  const startText = startMatch[1].trim();

  const linkMatch = startText.match(/\[\[([^\]|]+)/);
  if (linkMatch) {
    return cleanName(linkMatch[1].trim());
  }

  const talkMatch = startText.match(/(?:Talk|Speak)\s+(?:to|with)\s+(?:the\s+)?([A-Z][a-zA-Z' ]+)/);
  if (talkMatch) {
    return cleanName(talkMatch[1].trim());
  }

  return null;
}

async function questFallback(text) {
  const questMatch = text.match(/(?:start|complete|continue|do)\s+(?:the\s+)?([A-Z][a-zA-Z' ]+?)(?:[,.]|\s+to\s|\s+and\s|\s+while\s|\s+for\s|\s+up\s|\s+after\s|\s+before\s)/i);
  if (!questMatch) return null;

  const questName = questMatch[1].trim();
  if (questName.toLowerCase() === 'wintertodt' || questName.toLowerCase() === 'tempoross') {
    return null;
  }

  const data = await fetchWiki(questName);
  if (!data || !data.parse || !data.parse.wikitext) return null;

  const wikitext = data.parse.wikitext['*'];

  if (!wikitext.match(/\{\{Infobox\s+Quest/)) return null;

  return extractQuestStartNpc(wikitext);
}

function extractQuestNames(text, stepId, seeds) {
  const candidates = new Set();

  // Seed candidate
  const seed = seeds[stepId];
  if (seed && findQuestFile(seed)) {
    candidates.add(seed);
  }

  // Regex extraction from text: verbs that precede quest names
  const regex = /(?:start|complete|continue|do|progress|finish|begin)\s+(?:the\s+)?([A-Z][a-zA-Z' ]+?)(?:[,.]|\s+to\s|\s+and\s|\s+while\s|\s+for\s|\s+up\s|\s+after\s|\s+before\s)/gi;
  let match;
  while ((match = regex.exec(text)) !== null) {
    const name = match[1].trim();
    if (name.length > 2) candidates.add(name);
  }

  // Direct substring checks using mapping keys
  const mappingPath = path.resolve(__dirname, 'quest-mapping.json');
  if (fs.existsSync(mappingPath)) {
    const mapping = JSON.parse(fs.readFileSync(mappingPath, 'utf8'));
    const lowerText = text.toLowerCase();
    for (const [key] of Object.entries(mapping)) {
      if (lowerText.includes(key.toLowerCase())) {
        candidates.add(key);
      }
    }
  }

  // Direct substring checks for quest dir names in normalized text
  const index = loadQuestIndex();
  const normalizedText = text.toLowerCase().replace(/[^a-z0-9]/g, '');
  for (const dir of index.keys()) {
    if (dir.length < 8) continue;
    const variants = [dir];
    if (dir.startsWith('the')) {
      variants.push(dir.slice(3));
    }
    for (const variant of variants) {
      if (normalizedText.includes(variant)) {
        candidates.add(dir);
        break;
      }
    }
  }

  return Array.from(candidates);
}

async function main() {
  if (!fs.existsSync(CACHE_DIR)) {
    fs.mkdirSync(CACHE_DIR, { recursive: true });
  }

  const guide = JSON.parse(fs.readFileSync(GUIDE_PATH, 'utf8'));
  const seeds = JSON.parse(fs.readFileSync(SEEDS_PATH, 'utf8'));

  const steps = [];
  for (const chapter of guide.chapters) {
    for (const section of chapter.sections) {
      for (const step of section.steps) {
        steps.push(step);
      }
    }
  }

  console.log(`Processing ${steps.length} steps...`);

  const output = {
    source: 'osrs-wiki+quest-helper',
    resolvedAt: new Date().toISOString(),
    steps: {}
  };

  let resolvedCount = 0;

  for (let i = 0; i < steps.length; i++) {
    const step = steps[i];
    const stepId = step.id;
    let resolved = false;

    // 1. Try seed via wiki
    const seed = seeds[stepId];
    if (seed) {
      try {
        const result = await resolveEntity(seed);
        if (result && result.worldPoint) {
          output.steps[stepId] = {
            label: seed,
            npcIds: result.npcIds,
            objectIds: result.objectIds,
            worldPoint: result.worldPoint,
            note: 'Resolved via seed'
          };
          resolvedCount++;
          resolved = true;
          console.log(`[${i + 1}/${steps.length}] ${stepId}: ${seed} -> (${result.worldPoint.x}, ${result.worldPoint.y}, ${result.worldPoint.plane})`);
        }
      } catch (err) {
        // fall through
      }
    }

    // 2. Try Quest Helper candidates
    if (!resolved) {
      const questCandidates = extractQuestNames(step.text, stepId, seeds);
      for (const candidate of questCandidates) {
        try {
          const qhResult = await resolveQuestHelper(candidate);
          if (qhResult && qhResult.worldPoint) {
            output.steps[stepId] = {
              label: candidate,
              npcIds: qhResult.npcIds,
              objectIds: qhResult.objectIds,
              worldPoint: qhResult.worldPoint,
              note: 'Resolved via Quest Helper'
            };
            resolvedCount++;
            resolved = true;
            console.log(`[${i + 1}/${steps.length}] ${stepId}: ${candidate} -> QH (${qhResult.worldPoint.x}, ${qhResult.worldPoint.y}, ${qhResult.worldPoint.plane})`);
            break;
          }
        } catch (err) {
          // try next candidate
        }
      }
    }

    // 3. Fall back to existing wiki path
    if (!resolved) {
      let entityName = null;
      let source = null;

      entityName = extractEntity(step.text);
      source = 'regex';

      if (!entityName) {
        const questNpc = await questFallback(step.text);
        if (questNpc) {
          entityName = questNpc;
          source = 'quest';
        }
      }

      if (!entityName) {
        output.steps[stepId] = {
          unresolved: true,
          note: 'No entity found in step text'
        };
        console.log(`[${i + 1}/${steps.length}] ${stepId}: unresolved (no entity)`);
        continue;
      }

      try {
        const result = await resolveEntity(entityName);

        if (result && result.worldPoint) {
          output.steps[stepId] = {
            label: entityName,
            npcIds: result.npcIds,
            objectIds: result.objectIds,
            worldPoint: result.worldPoint,
            note: `Resolved via ${source}`
          };
          resolvedCount++;
          console.log(`[${i + 1}/${steps.length}] ${stepId}: ${entityName} -> (${result.worldPoint.x}, ${result.worldPoint.y}, ${result.worldPoint.plane})`);
        } else {
          output.steps[stepId] = {
            unresolved: true,
            note: `Wiki lookup failed for "${entityName}"`
          };
          console.log(`[${i + 1}/${steps.length}] ${stepId}: ${entityName} -> unresolved (wiki)`);
        }
      } catch (err) {
        output.steps[stepId] = {
          unresolved: true,
          note: `Error: ${err.message}`
        };
        console.log(`[${i + 1}/${steps.length}] ${stepId}: ${entityName} -> error: ${err.message}`);
      }
    }
  }

  const json = JSON.stringify(output, null, 2);
  fs.writeFileSync(OUTPUT_PATH, json, 'utf8');

  console.log(`\nDone! Resolved ${resolvedCount}/${steps.length} steps (${Math.round(resolvedCount / steps.length * 100)}%)`);
  console.log(`Wrote ${OUTPUT_PATH} (${json.length} bytes)`);
}

main().catch(err => {
  console.error(err);
  process.exit(1);
});
