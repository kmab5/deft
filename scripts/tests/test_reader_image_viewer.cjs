/* Decode the production image document in installed Chromium; no browser or package downloads. */
const assert = require('node:assert/strict');
const fs = require('node:fs');
const cp = require('node:child_process');
const path = require('node:path');
const os = require('node:os');
const chrome = process.env.CHROME || (process.platform === 'win32'
  ? 'C:/Program Files/Google/Chrome/Application/chrome.exe' : '/usr/bin/google-chrome');
const profile = fs.mkdtempSync(path.join(os.tmpdir(), 'deft-image-test-'));
const source = fs.readFileSync('feature/reader/src/main/kotlin/com/kmab/deft/feature/reader/ReaderImageViewer.kt', 'utf8');
const template = source.match(/"""\s*(<!doctype html>[\s\S]*?)"""\.trimIndent\(\)/)[1];
const images = [
  {type: 'image/png', encoded: 'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+aS1cAAAAASUVORK5CYII=', width: 1},
  {type: 'image/svg+xml', encoded: Buffer.from('<svg xmlns="http://www.w3.org/2000/svg" width="640" height="960"><rect width="640" height="960" fill="#b24bff"/></svg>').toString('base64'), width: 640},
];
// Optionally decode the author's real cover in memory. Neither book bytes nor images are exported.
if (process.env.DEFT_READER_DIAGNOSTIC_EPUB) {
  const encoded = cp.execFileSync('python', ['-c',
    'import zipfile,sys,base64;z=zipfile.ZipFile(sys.argv[1]);print(base64.b64encode(z.read("cover.jpeg")).decode())',
    process.env.DEFT_READER_DIAGNOSTIC_EPUB], {encoding: 'utf8'}).trim();
  images.push({type: 'image/jpeg', encoded, width: 312});
}
const child = cp.spawn(chrome, ['--headless=new', '--no-first-run', '--no-default-browser-check',
  '--disable-background-networking', '--remote-debugging-port=0', '--user-data-dir=' + profile, 'about:blank'],
  {windowsHide: true, stdio: 'ignore'});
const pause = ms => new Promise(resolve => setTimeout(resolve, ms));
let ws;
async function main() {
  let port;
  for (let tries = 0; tries < 100; tries++) {
    try { port = fs.readFileSync(path.join(profile, 'DevToolsActivePort'), 'utf8').split('\n')[0]; break; }
    catch (_) { await pause(50); }
  }
  assert(port, 'Installed Chromium starts');
  const pages = await (await fetch(`http://127.0.0.1:${port}/json`)).json();
  ws = new WebSocket(pages.find(page => page.type === 'page').webSocketDebuggerUrl);
  await new Promise(resolve => { ws.onopen = resolve; });
  let nextId = 0;
  const pending = new Map();
  const send = (method, params = {}) => new Promise((resolve, reject) => {
    const id = ++nextId;
    pending.set(id, {resolve, reject});
    ws.send(JSON.stringify({id, method, params}));
  });
  ws.onmessage = event => {
    const message = JSON.parse(event.data);
    if (!message.id) return;
    const request = pending.get(message.id);
    pending.delete(message.id);
    message.error ? request.reject(message.error) : request.resolve(message.result);
  };
  const evaluate = async expression => {
    const result = await send('Runtime.evaluate', {expression, returnByValue: true, awaitPromise: true});
    assert(!result.exceptionDetails, JSON.stringify(result.exceptionDetails));
    return result.result.value;
  };
  await send('Page.enable');
  await send('Emulation.setDeviceMetricsOverride', {width: 393, height: 700, deviceScaleFactor: 1, mobile: true});
  await send('Emulation.setTouchEmulationEnabled', {enabled: true});
  for (const image of images) {
    const html = template.replace('$type', image.type).replace('$encoded', image.encoded);
    await send('Page.navigate', {url: 'data:text/html;base64,' + Buffer.from(html).toString('base64')});
    for (let tries = 0; tries < 100; tries++) {
      if (await evaluate('document.images[0]?.complete && document.images[0]?.naturalWidth > 0')) break;
      await pause(30);
    }
    const decoded = await evaluate('(()=>{const i=document.images[0],r=i.getBoundingClientRect();return {width:i.naturalWidth,height:r.height,shown:r.width>0&&r.height>0&&r.width<=innerWidth&&r.height<=innerHeight,viewport:document.querySelector("meta[name=viewport]").content}})()');
    assert.equal(decoded.width, image.width, image.type + ' decodes');
    assert(decoded.shown, image.type + ' fits the visible viewport');
    assert(decoded.viewport.includes('maximum-scale=8'));
    await send('Emulation.setPageScaleFactor', {pageScaleFactor: 3});
    assert.equal(await evaluate('visualViewport.scale'), 3, 'Dedicated image surface can zoom');
    await send('Input.dispatchTouchEvent', {type: 'touchStart', touchPoints: [{x: 90, y: 160}]});
    await send('Input.dispatchTouchEvent', {type: 'touchMove', touchPoints: [{x: 30, y: 60}]});
    await send('Input.dispatchTouchEvent', {type: 'touchEnd', touchPoints: []});
    await pause(100);
    assert(await evaluate('visualViewport.pageTop > 0 || visualViewport.pageLeft > 0'), 'Zoomed image surface pans');
  }
  console.log(`PASS image viewer: ${images.map(image => image.type).join(', ')} decode, visible fit, zoom and pan.`);
}
main().catch(error => { console.error(error); process.exitCode = 1; }).finally(async () => {
  ws?.close();
  child.kill();
  await pause(150);
  const resolved = fs.realpathSync(profile);
  assert.equal(path.dirname(resolved), fs.realpathSync(os.tmpdir()));
  assert(path.basename(resolved).startsWith('deft-image-test-'));
  fs.rmSync(resolved, {recursive: true, force: true, maxRetries: 3, retryDelay: 100});
});
