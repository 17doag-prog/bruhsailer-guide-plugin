import { getQuestSource } from './quest-helper-resolver.js';

const STEP_TYPES = ['NpcStep', 'ObjectStep', 'DetailedQuestStep', 'ItemStep', 'ConditionalStep'];
const STEP_CTOR_REGEX = new RegExp(`new\\s+(?:${STEP_TYPES.join('|')})\\s*\\(`, 'g');
const SET_TEXT_REGEX = /(\w+)\.setText\s*\(\s*"((?:\\"|[^"])*)"\s*\)/g;
const STRING_LITERAL_REGEX = /"((?:\\"|[^"])*)":?/g;

function findMethodBody(content, signature) {
	const start = content.indexOf(signature);
	if (start === -1) return null;
	const braceStart = content.indexOf('{', start);
	if (braceStart === -1) return null;

	let depth = 1;
	let i = braceStart + 1;
	while (i < content.length && depth > 0) {
		if (content[i] === '{') depth++;
		else if (content[i] === '}') depth--;
		i++;
	}
	if (depth !== 0) return null;
	return content.slice(braceStart + 1, i - 1);
}

function extractBalancedParen(content, startIdx) {
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

function extractInstructionFromCtorArgs(args) {
	const strings = [];
	let m;
	while ((m = STRING_LITERAL_REGEX.exec(args)) !== null) {
		strings.push(m[1]);
	}
	const meaningful = strings
		.map(s => s.trim())
		.filter(s => s.length > 0);
	if (meaningful.length === 0) return null;
	return meaningful.reduce((a, b) => (a.length >= b.length ? a : b), meaningful[0]);
}

function looksLikeInstruction(text) {
	const trimmed = text.trim();
	if (trimmed.length < 15) return false;
	if (!/[.!?]$/.test(trimmed) && !/^\s*(?:Talk|Speak|Go|Get|Pick|Use|Climb|Enter|Leave|Kill|Open|Close|Take|Buy|Sell|Head|Run|Walk|Bank|Teleport|Equip|Wear|Attack|Defeat|Finish|Complete|Return|Give|Show|Search|Inspect|Read|Dig|Fill|Fish|Cook|Light|Burn|Chop|Mine|Smith|Craft|Fletch|Herb|Plant|Water|Harvest|Offer|Pray|Cast|Suicide|Drink|Eat|Drop|Destroy|Deposit|Withdraw|Decant|Enchant|Charge|Alch|Dismantle|Mount|Board|Travel|Pay|Donate|Claim|Collect|Grab|Bring)\b/i.test(trimmed)) {
		return false;
	}
	return true;
}

function* scanRegion(region) {
	let i = 0;
	while (i < region.length) {
		STEP_CTOR_REGEX.lastIndex = i;
		SET_TEXT_REGEX.lastIndex = i;

		const ctorMatch = STEP_CTOR_REGEX.exec(region);
		const setTextMatch = SET_TEXT_REGEX.exec(region);

		let next = null;
		let advanceTo = i + 1;

		if (ctorMatch && setTextMatch) {
			if (ctorMatch.index <= setTextMatch.index) {
				next = { type: 'ctor', match: ctorMatch };
				advanceTo = ctorMatch.index + ctorMatch[0].length;
			} else {
				next = { type: 'setText', match: setTextMatch };
				advanceTo = setTextMatch.index + setTextMatch[0].length;
			}
		} else if (ctorMatch) {
			next = { type: 'ctor', match: ctorMatch };
			advanceTo = ctorMatch.index + ctorMatch[0].length;
		} else if (setTextMatch) {
			next = { type: 'setText', match: setTextMatch };
			advanceTo = setTextMatch.index + setTextMatch[0].length;
		} else {
			break;
		}

		if (next.type === 'setText') {
			yield { text: next.match[2], source: 'setText' };
		} else {
			const args = extractBalancedParen(region, next.match.index + next.match[0].length);
			if (args) {
				const instruction = extractInstructionFromCtorArgs(args);
				if (instruction) yield { text: instruction, source: 'ctor' };
			}
		}

		i = advanceTo;
	}
}

export async function resolveQuestActions(questName) {
	const content = getQuestSource(questName);
	if (!content) return null;

	let region = findMethodBody(content, 'public void setupSteps()');
	if (!region) {
		region = findMethodBody(content, 'public Map<Integer, QuestStep> loadSteps()');
	}
	if (!region) {
		return null;
	}

	const seen = new Set();
	const actions = [];

	for (const event of scanRegion(region)) {
		const text = event.text.trim();
		if (!text || seen.has(text) || !looksLikeInstruction(text)) {
			continue;
		}
		seen.add(text);
		actions.push(text);
	}

	return actions;
}
