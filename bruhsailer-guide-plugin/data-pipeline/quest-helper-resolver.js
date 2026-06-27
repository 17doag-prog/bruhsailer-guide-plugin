import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const REPO_DIR = path.resolve(__dirname, '.questhelper-cache', 'quest-helper');
const QUESTS_DIR = path.resolve(REPO_DIR, 'src/main/java/com/questhelper/helpers/quests');
const MINIQUESTS_DIR = path.resolve(REPO_DIR, 'src/main/java/com/questhelper/helpers/miniquests');
const MAPPING_PATH = path.resolve(__dirname, 'quest-mapping.json');

let questIndexMap = null;
let questMapping = null;

function loadQuestMapping() {
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

function listRepoQuestDirs() {
	const dirs = [];
	for (const baseDir of [QUESTS_DIR, MINIQUESTS_DIR]) {
		if (!fs.existsSync(baseDir)) continue;
		for (const entry of fs.readdirSync(baseDir, { withFileTypes: true })) {
			if (entry.isDirectory()) {
				const fullPath = path.join(baseDir, entry.name);
				const files = fs.readdirSync(fullPath).filter(f => f.endsWith('.java'));
				if (files.length > 0) {
					dirs.push({ dir: entry.name, files, baseDir });
				}
			}
		}
	}
	return dirs;
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

function loadQuestIndex() {
	if (questIndexMap) return questIndexMap;

	const dirs = listRepoQuestDirs();
	questIndexMap = new Map();
	for (const { dir, files, baseDir } of dirs) {
		questIndexMap.set(dir, { file: pickMainFile(dir, files), baseDir });
	}
	return questIndexMap;
}

function levenshtein(a, b) {
	const matrix = Array.from({ length: a.length + 1 }, (_, i) => [i]);
	for (let j = 1; j <= b.length; j++) {
		matrix[0][j] = j;
	}
	for (let i = 1; i <= a.length; i++) {
		for (let j = 1; j <= b.length; j++) {
			const cost = a[i - 1] === b[j - 1] ? 0 : 1;
			matrix[i][j] = Math.min(
				matrix[i - 1][j] + 1,
				matrix[i][j - 1] + 1,
				matrix[i - 1][j - 1] + cost
			);
		}
	}
	return matrix[a.length][b.length];
}

function fuzzyFindQuestDir(normalized, index) {
	if (normalized.length < 4) return null;

	let bestDir = null;
	let bestDist = Infinity;
	for (const dir of index.keys()) {
		const dist = levenshtein(normalized, dir);
		if (dist < bestDist && dist <= 2) {
			bestDist = dist;
			bestDir = dir;
		}
	}
	return bestDir;
}

function findQuestFile(questName) {
	const mapping = loadQuestMapping();
	const lookupName = questName.toLowerCase().trim();

	if (mapping[lookupName]) {
		questName = mapping[lookupName];
	}

	const normalized = normalizeQuestName(questName);
	if (!normalized) return null;

	const index = loadQuestIndex();

	if (index.has(normalized)) {
		const { file, baseDir } = index.get(normalized);
		return { dir: normalized, file, baseDir };
	}

	const withThe = 'the' + normalized;
	if (index.has(withThe)) {
		const { file, baseDir } = index.get(withThe);
		return { dir: withThe, file, baseDir };
	}

	if (normalized.endsWith('s')) {
		const withoutS = normalized.slice(0, -1);
		if (index.has(withoutS)) {
			const { file, baseDir } = index.get(withoutS);
			return { dir: withoutS, file, baseDir };
		}
	}

	if (normalized.endsWith('s')) {
		const withoutApostropheS = normalized.slice(0, -2);
		if (index.has(withoutApostropheS)) {
			const { file, baseDir } = index.get(withoutApostropheS);
			return { dir: withoutApostropheS, file, baseDir };
		}
	}

	const fuzzyDir = fuzzyFindQuestDir(normalized, index);
	if (fuzzyDir) {
		const { file, baseDir } = index.get(fuzzyDir);
		return { dir: fuzzyDir, file, baseDir };
	}

	return null;
}

function getQuestSourcePath(target) {
	const { dir, file, baseDir } = target;
	if (baseDir && fs.existsSync(path.join(baseDir, dir, file))) {
		return path.join(baseDir, dir, file);
	}
	for (const candidate of [QUESTS_DIR, MINIQUESTS_DIR]) {
		const fullPath = path.join(candidate, dir, file);
		if (fs.existsSync(fullPath)) {
			return fullPath;
		}
	}
	return null;
}

function getQuestSource(questName) {
	const target = findQuestFile(questName);
	if (!target) return null;
	const sourcePath = getQuestSourcePath(target);
	if (!sourcePath) return null;
	return fs.readFileSync(sourcePath, 'utf8');
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
	const content = getQuestSource(questName);
	if (!content) return null;

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

export {
	loadQuestIndex,
	normalizeQuestName,
	findQuestFile,
	getQuestSource,
	getQuestSourcePath,
	resolveQuestHelper
};
