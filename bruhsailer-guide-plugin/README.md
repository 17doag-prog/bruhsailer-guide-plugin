# BRUHsailer Guide

A RuneLite plugin that provides a sidebar progression panel and in-world hints for the **BRUHsailer** ironman route.

## What it does

- **Sidebar guide** — A tree-view panel (BRUHsailer Guide → Chapters → Sections → Steps) with step details, GP stack, items needed, estimated time, and skill changes.
- **Progress tracking** — Mark steps complete with a checkbox; completion state persists across sessions.
- **In-world hints** — Three optional overlays highlight your current step target:
  - **World map marker** — Arrow pointing to the objective world point.
  - **Minimap marker** — Cyan dot or edge arrow toward the target.
  - **Scene highlights** — Cyan outlines on target NPCs, objects, and tiles.

## Install from Plugin Hub

1. Open RuneLite.
2. Go to **Plugin Hub** (wrench icon → Plugin Hub).
3. Search for **"BRUHsailer Guide"**.
4. Click **Install**.

*(Plugin Hub submission is pending; once merged the steps above will work.)*

## Build from source

```bash
./gradlew build
```

The plugin JAR is produced at `build/libs/bruhsailer-guide-plugin-*-all.jar`.

## Regenerate data

The plugin bundles pre-generated JSON data. To refresh it from upstream sources:

```bash
# 1. Reshape the raw guide into guide.json
node data-pipeline/scrape-guide.js

# 2. Resolve spatial hints (NPC/object locations, world points)
node data-pipeline/resolve-spatial.js

# 3. Scrape step-level stats (GP, skill levels)
node data-pipeline/scrape-stats.js
```

These scripts write into `src/main/resources/`.

## License

BSD 2-Clause — see [LICENSE](LICENSE).

