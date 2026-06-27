import { describe, it } from 'node:test';
import assert from 'node:assert';
import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const STATS_PATH = path.resolve(__dirname, '../../src/main/resources/stats.json');

describe('stats.json output', () => {
  it('(a) new-1-1 should have gpTotal containing "25"', () => {
    const stats = JSON.parse(fs.readFileSync(STATS_PATH, 'utf8'));
    const step = stats.steps['new-1-1'];
    assert.ok(step, 'Step new-1-1 should exist');
    assert.ok(step.gpTotal.includes('25'), `Expected gpTotal to include "25", got: ${step.gpTotal}`);
  });

  it('(b) new-1-6 should have gpTotal containing "2501"', () => {
    const stats = JSON.parse(fs.readFileSync(STATS_PATH, 'utf8'));
    const step = stats.steps['new-1-6'];
    assert.ok(step, 'Step new-1-6 should exist');
    assert.ok(step.gpTotal.includes('2501'), `Expected gpTotal to include "2501", got: ${step.gpTotal}`);
  });

  it('(c) steps object should have at least 220 keys', () => {
    const stats = JSON.parse(fs.readFileSync(STATS_PATH, 'utf8'));
    const keyCount = Object.keys(stats.steps).length;
    assert.ok(keyCount >= 220, `Expected at least 220 steps, got: ${keyCount}`);
  });
});
