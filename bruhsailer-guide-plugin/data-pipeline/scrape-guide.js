import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';
import { resolveQuestActions } from './quest-helper-actions.js';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const SOURCE_URL = 'https://raw.githubusercontent.com/pobblesbe/bruhsailer-transition-tool/main/data/new-guide.json';
const OUTPUT_PATH = path.resolve(__dirname, '../src/main/resources/guide.json');

let knownQuestNames = null;

function splitCamelCase(name) {
	return name.replace(/([a-z0-9])([A-Z])/g, '$1 $2').trim();
}

function normalizeName(name) {
	return name.toLowerCase().replace(/'/g, '').replace(/[^a-z0-9]/g, '');
}

function loadKnownQuestNames() {
	if (knownQuestNames) return knownQuestNames;
	const names = new Map();

	const repoDir = path.resolve(__dirname, '.questhelper-cache', 'quest-helper');
	const dirs = [];
	for (const sub of ['src/main/java/com/questhelper/helpers/quests', 'src/main/java/com/questhelper/helpers/miniquests']) {
		const base = path.resolve(repoDir, sub);
		if (!fs.existsSync(base)) continue;
		for (const entry of fs.readdirSync(base, { withFileTypes: true })) {
			if (entry.isDirectory()) {
				dirs.push(entry.name);
			}
		}
	}

	for (const dir of dirs) {
		const mainClassPath = path.resolve(repoDir, 'src/main/java/com/questhelper/helpers/quests', dir, `${dir.charAt(0).toUpperCase()}${dir.slice(1)}.java`);
		let sourceName = dir;
		if (fs.existsSync(mainClassPath)) {
			const files = fs.readdirSync(path.dirname(mainClassPath));
			const mainFile = files.find(f => f.toLowerCase() === `${dir}.java`) || files[0];
			sourceName = mainFile.replace(/\.java$/i, '');
		}
		const display = splitCamelCase(sourceName.charAt(0).toUpperCase() + sourceName.slice(1));
		const normalized = normalizeName(display);
		if (!normalized) continue;
		names.set(normalized, display);
		if (display.includes("'")) {
			names.set(normalizeName(display.replace(/'/g, '')), display.replace(/'/g, ''));
		}
	}

	const mappingPath = path.resolve(__dirname, 'quest-mapping.json');
	if (fs.existsSync(mappingPath)) {
		const mapping = JSON.parse(fs.readFileSync(mappingPath, 'utf8'));
		for (const [key, value] of Object.entries(mapping)) {
			const dirDisplay = splitCamelCase(value.charAt(0).toUpperCase() + value.slice(1));
			names.set(normalizeName(key), key);
			names.set(normalizeName(dirDisplay), dirDisplay);
		}
	}

	knownQuestNames = names;
	return names;
}

function detectQuestName(text) {
	const names = loadKnownQuestNames();
	const sorted = Array.from(names.entries())
		.map(([normalized, display]) => ({ normalized, display }))
		.sort((a, b) => b.display.length - a.display.length);

	for (const { display } of sorted) {
		const escaped = display.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
		const re = new RegExp(`\\b${escaped}\\b`, 'i');
		if (re.test(text)) {
			return display;
		}
	}
	return null;
}

function splitSentences(text) {
	return text
		.replace(/\n/g, ' ')
		.replace(/\s+-\s+/g, '. ')
		.split(/(?<=[.!?])\s+/)
		.map(s => s.trim())
		.filter(s => s.length > 0);
}

function cleanSentence(sentence) {
	return sentence
		.replace(/^\s*[–—-]\s*/, '')
		.replace(/^\s*\([^)]*\)\s*/g, '')
		.trim();
}

async function buildActions(step, actionCache) {
	const actions = [];
	const sentences = splitSentences(step.text);

	for (const rawSentence of sentences) {
		const sentence = cleanSentence(rawSentence);
		if (!sentence) continue;

		const questName = detectQuestName(sentence);
		if (questName) {
			let questActions = actionCache.get(questName);
			if (questActions === undefined) {
				questActions = await resolveQuestActions(questName) || [];
				actionCache.set(questName, questActions);
			}
			if (questActions.length > 0) {
				for (const text of questActions) {
					actions.push({ text, source: 'quest-helper', quest: questName });
				}
				continue;
			}
			actions.push({ text: sentence, source: 'text-split' });
			continue;
		}

		if (sentence.length >= 8) {
			actions.push({ text: sentence, source: 'text-split' });
		}
	}

	if (actions.length === 0) {
		actions.push({ text: step.text.trim(), source: 'text-split' });
	}

	return actions.map((action, idx) => ({
		id: `${step.id}-${idx}`,
		...action
	}));
}

async function main() {
	const response = await fetch(SOURCE_URL);
	if (!response.ok) {
		throw new Error(`Failed to fetch: ${response.status} ${response.statusText}`);
	}

	const source = await response.json();
	const actionCache = new Map();

	const output = {
		sourceUrl: source.sourceUrl,
		updatedOn: source.updatedOn,
		chapters: []
	};

	for (const [chapterIndex, chapter] of source.chapters.entries()) {
		const sections = new Map();

		for (const step of chapter.steps) {
			const sectionKey = `${step.section.index}:${step.section.title}`;
			if (!sections.has(sectionKey)) {
				sections.set(sectionKey, {
					index: step.section.index,
					title: step.section.title,
					steps: []
				});
			}

			const actions = await buildActions(step, actionCache);

			sections.get(sectionKey).steps.push({
				id: step.id,
				number: step.number,
				text: step.text,
				gpStack: step.metadata.gp_stack,
				itemsNeeded: step.metadata.items_needed,
				totalTime: step.metadata.total_time,
				actions
			});
		}

		output.chapters.push({
			index: chapterIndex + 1,
			title: chapter.title,
			sections: Array.from(sections.values())
		});
	}

	const json = JSON.stringify(output, null, 2);
	fs.writeFileSync(OUTPUT_PATH, json, 'utf8');
	console.log(`Wrote ${OUTPUT_PATH} (${json.length} bytes)`);
}

main().catch(err => {
	console.error(err);
	process.exit(1);
});
