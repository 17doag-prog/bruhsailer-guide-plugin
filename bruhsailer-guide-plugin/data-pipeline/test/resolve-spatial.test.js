import { describe, it } from 'node:test';
import assert from 'node:assert';
import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const SPATIAL_PATH = path.resolve(__dirname, '../../src/main/resources/spatial.json');

describe('spatial.json output', () => {
  it('(a) new-1-1 should resolve to npcIds containing 815 and worldPoint.plane === 1', () => {
    const spatial = JSON.parse(fs.readFileSync(SPATIAL_PATH, 'utf8'));
    const step = spatial.steps['new-1-1'];
    assert.ok(step, 'Step new-1-1 should exist');
    assert.ok(!step.unresolved, 'Step new-1-1 should not be unresolved');
    assert.ok(step.npcIds.includes(815), `Expected npcIds to contain 815, got: ${JSON.stringify(step.npcIds)}`);
    assert.strictEqual(step.worldPoint.plane, 1, `Expected worldPoint.plane to be 1, got: ${step.worldPoint.plane}`);
  });

  it('(b) at least 60% of steps should have a non-null worldPoint', () => {
    const spatial = JSON.parse(fs.readFileSync(SPATIAL_PATH, 'utf8'));
    const steps = Object.values(spatial.steps);
    const total = steps.length;
    const resolved = steps.filter(s => s.worldPoint).length;
    const pct = resolved / total;
    assert.ok(pct >= 0.6, `Expected at least 60% resolution, got ${resolved}/${total} = ${Math.round(pct * 100)}%`);
  });

  it('(c) no step should have both objectIds and npcIds populated', () => {
    const spatial = JSON.parse(fs.readFileSync(SPATIAL_PATH, 'utf8'));
    const steps = Object.values(spatial.steps);
    for (const step of steps) {
      if (step.unresolved) continue;
      const hasNpc = step.npcIds && step.npcIds.length > 0;
      const hasObj = step.objectIds && step.objectIds.length > 0;
      assert.ok(!(hasNpc && hasObj), `Step ${step.label} has both npcIds and objectIds populated`);
    }
  });

  it('(d) at least one step should be resolved via Quest Helper', () => {
    const spatial = JSON.parse(fs.readFileSync(SPATIAL_PATH, 'utf8'));
    const steps = Object.values(spatial.steps);
    const qhSteps = steps.filter(s => s.note && s.note.includes('Quest Helper'));
    assert.ok(qhSteps.length >= 1, `Expected at least one Quest Helper resolution, got ${qhSteps.length}`);
  });
});
