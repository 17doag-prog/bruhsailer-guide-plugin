import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const SOURCE_URL = 'https://raw.githubusercontent.com/pobblesbe/bruhsailer-transition-tool/main/data/new-guide.json';
const OUTPUT_PATH = path.resolve(__dirname, '../src/main/resources/guide.json');

async function main() {
  const response = await fetch(SOURCE_URL);
  if (!response.ok) {
    throw new Error(`Failed to fetch: ${response.status} ${response.statusText}`);
  }

  const source = await response.json();

  // Reshape to our target schema
  const output = {
    sourceUrl: source.sourceUrl,
    updatedOn: source.updatedOn,
    chapters: source.chapters.map((chapter, chapterIndex) => {
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
        sections.get(sectionKey).steps.push({
          id: step.id,
          number: step.number,
          text: step.text,
          gpStack: step.metadata.gp_stack,
          itemsNeeded: step.metadata.items_needed,
          totalTime: step.metadata.total_time
        });
      }

      return {
        index: chapterIndex + 1,
        title: chapter.title,
        sections: Array.from(sections.values())
      };
    })
  };

  // Stable JSON stringify for idempotency
  const json = JSON.stringify(output, null, 2);
  fs.writeFileSync(OUTPUT_PATH, json, 'utf8');
  console.log(`Wrote ${OUTPUT_PATH} (${json.length} bytes)`);
}

main().catch(err => {
  console.error(err);
  process.exit(1);
});
