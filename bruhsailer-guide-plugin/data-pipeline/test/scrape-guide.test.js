import { describe, it } from 'node:test';
import assert from 'node:assert';
import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';
import { execSync } from 'child_process';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const GUIDE_PATH = path.resolve(__dirname, '../../src/main/resources/guide.json');

describe('guide.json output', () => {
  it('(a) should have exactly 3 chapters', () => {
    const guide = JSON.parse(fs.readFileSync(GUIDE_PATH, 'utf8'));
    assert.strictEqual(guide.chapters.length, 3);
  });

  it('(b) Chapter 1 first step should start with "Choose a swag look" and have gpStack "25 gp"', () => {
    const guide = JSON.parse(fs.readFileSync(GUIDE_PATH, 'utf8'));
    const chapter1 = guide.chapters[0];
    assert.ok(chapter1.sections.length > 0, 'Chapter 1 should have at least one section');
    const firstStep = chapter1.sections[0].steps[0];
    assert.ok(firstStep.text.startsWith('Choose a swag look'), 'First step text should start with "Choose a swag look"');
    assert.strictEqual(firstStep.gpStack, '25 gp');
  });

  it('(c) Chapter 1 Section 1 should have a non-empty steps array', () => {
    const guide = JSON.parse(fs.readFileSync(GUIDE_PATH, 'utf8'));
    const chapter1 = guide.chapters[0];
    const section11 = chapter1.sections.find(s => s.index === 1);
    assert.ok(section11, 'Chapter 1 Section 1 should exist');
    assert.ok(section11.steps.length > 0, 'Chapter 1 Section 1 should have non-empty steps array');
  });

  it('(d) should be byte-identical to the committed snapshot', () => {
    assert.ok(fs.existsSync(GUIDE_PATH), 'Committed guide.json should exist');
    const committed = fs.readFileSync(GUIDE_PATH, 'utf8');

    // Run the pipeline in-process to compare
    execSync('node scrape-guide.js', {
      cwd: path.resolve(__dirname, '..')
    });

    const regenerated = fs.readFileSync(GUIDE_PATH, 'utf8');
    assert.strictEqual(regenerated, committed, 'Regenerated guide.json should be byte-identical to committed version');
  });
});
