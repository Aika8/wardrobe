#!/usr/bin/env node
// Screenshot of the Expo web build in a headless browser, so a screen task can look at what it built.
//
//   npm run screenshot -- <id> [--route /path]
//
// Builds the web export into a temp folder (EXPO_PUBLIC_API_URL=http://localhost:<api port>, default 8091),
// serves it on a free local port with Node's `http` module, starts this repo's server on the API port
// (`make dev PORT=8091`) if nothing answers `GET /items` there and stops it at the end, opens the page at
// 390 × 844 (device scale 2), waits for the item list and saves docs/backlog/reports/screens/<id>.png.
// Chrome comes from $CHROME_PATH (or a local Chrome); nothing is downloaded.
//
// CI does not run this script: there is no server and no browser there. CI only tests parseScreenshotArgs.
// The only file it writes into the repository is the PNG it names.
import { spawn, spawnSync } from 'node:child_process';
import { existsSync, mkdirSync, mkdtempSync, openSync, readFileSync, rmSync, statSync } from 'node:fs';
import { createServer } from 'node:http';
import { tmpdir } from 'node:os';
import { dirname, extname, join, normalize, resolve, sep } from 'node:path';
import { fileURLToPath, pathToFileURL } from 'node:url';

export const DEFAULT_API_PORT = 8091;
export const SCREENS_DIR = 'docs/backlog/reports/screens';

const USAGE = 'usage: npm run screenshot -- <task id, e.g. WR-035> [--route /path]';

/**
 * Pure: reads the command-line arguments and says where the PNG goes.
 * @param {string[]} argv arguments after the script name
 * @param {string} repoRoot absolute path of the repository root
 * @returns {{ id: string, route: string, apiPort: number, out: string }}
 */
export function parseScreenshotArgs(argv, repoRoot) {
  let id = null;
  let route = '/';
  for (let i = 0; i < argv.length; i++) {
    const a = argv[i];
    if (a === '--route') {
      const r = argv[++i];
      if (r === undefined || !r.startsWith('/')) throw new Error(`--route needs a path starting with '/'\n${USAGE}`);
      route = r;
    } else if (a.startsWith('--')) {
      throw new Error(`unknown option '${a}'\n${USAGE}`);
    } else if (id === null) {
      id = a;
    } else {
      throw new Error(`one task id only, got '${id}' and '${a}'\n${USAGE}`);
    }
  }
  if (id === null) throw new Error(`a task id is required\n${USAGE}`);
  // The id becomes a file name: letters, digits and dashes only, so it can never leave the screens folder.
  if (!/^[A-Z][A-Z0-9]*-[0-9]+$/.test(id)) throw new Error(`'${id}' is not a task id like WR-035\n${USAGE}`);
  return { id, route, apiPort: DEFAULT_API_PORT, out: join(repoRoot, SCREENS_DIR, `${id}.png`) };
}

const LOCAL_CHROMES = [
  '/usr/bin/google-chrome',
  '/usr/bin/chromium',
  '/usr/bin/chromium-browser',
  '/Applications/Google Chrome.app/Contents/MacOS/Google Chrome',
  'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe',
];

function findChrome() {
  if (process.env.CHROME_PATH) return existsSync(process.env.CHROME_PATH) ? process.env.CHROME_PATH : null;
  return LOCAL_CHROMES.find((p) => existsSync(p)) ?? null;
}

async function answersItems(port) {
  try {
    const r = await fetch(`http://localhost:${port}/items`, { signal: AbortSignal.timeout(2000) });
    return r.ok;
  } catch {
    return false;
  }
}

const tail = (file) => readFileSync(file, 'utf8').split('\n').slice(-25).join('\n');

async function startServerIfNeeded(repoRoot, port, logFile) {
  if (await answersItems(port)) return null;
  console.log(`starting the server: make dev PORT=${port} (log: ${logFile})`);
  const log = openSync(logFile, 'a');
  const child = spawn('make', ['dev', `PORT=${port}`], { cwd: repoRoot, stdio: ['ignore', log, log], detached: true });
  const deadline = Date.now() + 240_000;
  while (Date.now() < deadline) {
    if (child.exitCode !== null) {
      throw new Error(`make dev PORT=${port} exited with ${child.exitCode}; end of its log:\n${tail(logFile)}`);
    }
    if (await answersItems(port)) return child;
    await new Promise((r) => setTimeout(r, 1000));
  }
  stopServer(repoRoot, port, child);
  throw new Error(`the server did not answer GET /items on port ${port} within 4 minutes`);
}

function stopServer(repoRoot, port, child) {
  if (!child) return;
  try {
    process.kill(-child.pid, 'SIGTERM');
  } catch {
    // already gone
  }
  spawnSync('make', ['stop', `PORT=${port}`], { cwd: repoRoot, stdio: 'ignore' });
}

const TYPES = {
  '.html': 'text/html; charset=utf-8',
  '.js': 'text/javascript; charset=utf-8',
  '.css': 'text/css; charset=utf-8',
  '.json': 'application/json',
  '.png': 'image/png',
  '.ico': 'image/x-icon',
  '.svg': 'image/svg+xml',
  '.ttf': 'font/ttf',
};

function serveStatic(dir) {
  const root = resolve(dir);
  const server = createServer((req, res) => {
    const path = decodeURIComponent(new URL(req.url ?? '/', 'http://x').pathname);
    let file = normalize(join(root, path));
    if (file !== root && !file.startsWith(root + sep)) {
      res.writeHead(403).end();
      return;
    }
    if (!existsSync(file) || statSync(file).isDirectory()) {
      const html = `${file}.html`;
      file = existsSync(html) ? html : join(root, 'index.html'); // single-page app: unknown routes get index.html
    }
    res.writeHead(200, { 'content-type': TYPES[extname(file)] ?? 'application/octet-stream' });
    res.end(readFileSync(file));
  });
  return new Promise((ok) => server.listen(0, '127.0.0.1', () => ok(server)));
}

async function main() {
  const clientDir = resolve(dirname(fileURLToPath(import.meta.url)), '..');
  const repoRoot = resolve(clientDir, '..');
  const args = parseScreenshotArgs(process.argv.slice(2), repoRoot);

  const chrome = findChrome();
  if (!chrome) {
    throw new Error(
      `Chrome not found: set CHROME_PATH to a Chrome or Chromium binary` +
        (process.env.CHROME_PATH ? ` (CHROME_PATH=${process.env.CHROME_PATH} does not exist)` : ''),
    );
  }
  const { chromium } = await import('playwright-core');

  const work = mkdtempSync(join(tmpdir(), 'wardrobe-screenshot-'));
  const build = join(work, 'web');
  let server = null;
  let api = null;
  let browser = null;
  try {
    console.log('building the web export…');
    const exported = spawnSync('npx', ['expo', 'export', '--platform', 'web', '--output-dir', build], {
      cwd: clientDir,
      stdio: 'inherit',
      env: { ...process.env, EXPO_PUBLIC_API_URL: `http://localhost:${args.apiPort}`, CI: '1' },
    });
    if (exported.status !== 0) throw new Error(`expo export failed (exit ${exported.status})`);

    api = await startServerIfNeeded(repoRoot, args.apiPort, join(work, 'server.log'));
    server = await serveStatic(build);
    const url = `http://127.0.0.1:${server.address().port}${args.route}`;

    browser = await chromium.launch({ executablePath: chrome, headless: true, args: ['--no-sandbox'] });
    const page = await browser.newPage({ viewport: { width: 390, height: 844 }, deviceScaleFactor: 2 });
    await page.goto(url);
    // The list is there when a status label shows; the load error is a failure, not a screenshot.
    const outcome = await page.waitForFunction(
      () => {
        const text = document.body.innerText;
        if (text.includes('Не удалось загрузить')) return 'error';
        return ['чистое', 'в стирке', 'в ремонте', 'в поездке'].some((s) => text.includes(s)) ? 'list' : false;
      },
      null,
      { timeout: 30_000 },
    );
    if ((await outcome.jsonValue()) === 'error') {
      throw new Error(`the page shows the load error: ${await page.innerText('body')}`);
    }
    mkdirSync(dirname(args.out), { recursive: true });
    await page.screenshot({ path: args.out });
    console.log(`saved ${args.out}`);
  } finally {
    await browser?.close();
    server?.close();
    stopServer(repoRoot, args.apiPort, api);
    rmSync(work, { recursive: true, force: true });
  }
}

if (process.argv[1] && import.meta.url === pathToFileURL(resolve(process.argv[1])).href) {
  main().catch((e) => {
    console.error(`screenshot: ${e.message}`);
    process.exit(1);
  });
}
