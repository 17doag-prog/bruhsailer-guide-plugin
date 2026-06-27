import fs from 'fs';
import path from 'path';
import { execSync, spawn } from 'child_process';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const REPO_URL = 'https://github.com/Zoinkwiz/quest-helper.git';
const REPO_DIR = path.resolve(__dirname, '.questhelper-cache', 'quest-helper');
const CHECKOUT_PATHS = [
  'src/main/java/com/questhelper/helpers/quests',
  'src/main/java/com/questhelper/helpers/miniquests'
];

function exec(args, opts = {}) {
  return new Promise((resolve, reject) => {
    const child = spawn('git', args, {
      cwd: opts.cwd || REPO_DIR,
      stdio: ['ignore', 'inherit', 'inherit']
    });
    child.on('error', reject);
    child.on('close', code => {
      if (code !== 0) {
        reject(new Error(`git ${args.join(' ')} exited with ${code}`));
      } else {
        resolve();
      }
    });
  });
}

async function cloneRepo() {
  if (fs.existsSync(REPO_DIR)) {
    fs.rmSync(REPO_DIR, { recursive: true, force: true });
  }
  fs.mkdirSync(REPO_DIR, { recursive: true });

  console.log(`Cloning ${REPO_URL} into ${REPO_DIR} ...`);
  await exec(['clone', '--depth', '1', '--sparse', REPO_URL, path.basename(REPO_DIR)], { cwd: path.dirname(REPO_DIR) });
  await exec(['sparse-checkout', 'init', '--cone']);
  await exec(['sparse-checkout', 'set', ...CHECKOUT_PATHS]);
  await exec(['checkout']);
}

async function updateRepo() {
  console.log(`Updating existing clone in ${REPO_DIR} ...`);
  await exec(['fetch', '--depth', '1', 'origin']);
  await exec(['reset', '--hard', 'origin/HEAD']);
}

async function main() {
  try {
    if (fs.existsSync(path.join(REPO_DIR, '.git'))) {
      await updateRepo();
    } else {
      await cloneRepo();
    }
    console.log('Quest Helper clone ready.');
  } catch (err) {
    console.error('Failed to prepare Quest Helper clone:', err.message);
    process.exit(1);
  }
}

main();
