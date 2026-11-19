// Runs after BrowserPublicationFixtureTest and EpubNavigatorAttachmentTest. No browser downloads.
// node format/readium/src/test/browser/verify-reader.cjs [fixture-directory] [chrome-executable]
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const os = require('node:os');
const { spawn } = require('node:child_process');

const fixture = path.resolve(process.argv[2] || 'format/readium/build/browser-fixture');
const fontsRoot = path.resolve(process.argv[4] || 'feature/reader/build/browser-fonts');
const fontManifest = JSON.parse(fs.readFileSync(path.join(fontsRoot, 'fonts.json'), 'utf8'));
const chrome = process.argv[3] || process.env.CHROME_PATH || [
  'C:/Program Files/Google/Chrome/Application/chrome.exe',
  '/usr/bin/chromium', '/usr/bin/chromium-browser', '/usr/bin/google-chrome',
].find(fs.existsSync);
assert(chrome, 'Set CHROME_PATH to an installed Chromium executable');
assert(fs.existsSync(path.join(fixture, 'chapter.xhtml')), 'Run BrowserPublicationFixtureTest first');
const profile = fs.mkdtempSync(path.join(os.tmpdir(), 'deft-reader-chromium-'));
const pause = ms => new Promise(resolve => setTimeout(resolve, ms));
const child = spawn(chrome, [
  '--headless=new', '--no-first-run', '--no-default-browser-check', '--disable-background-networking',
  '--remote-debugging-port=0', `--user-data-dir=${profile}`, 'about:blank',
], { windowsHide: true, stdio: 'ignore' });

async function main() {
  const portFile = path.join(profile, 'DevToolsActivePort');
  for (let count = 0; count < 100 && !fs.existsSync(portFile); count++) await pause(100);
  const port = fs.readFileSync(portFile, 'utf8').split('\n')[0];
  const pages = await (await fetch(`http://127.0.0.1:${port}/json`)).json();
  const ws = new WebSocket(pages.find(page => page.type === 'page').webSocketDebuggerUrl);
  await new Promise(resolve => { ws.onopen = resolve; });
  let nextId = 0;
  const pending = new Map();
  const loaded = [];
  const blocked = [];
  const logs = [];
  const command = (method, params = {}) => new Promise((resolve, reject) => {
    const id = ++nextId;
    pending.set(id, { resolve, reject });
    ws.send(JSON.stringify({ id, method, params }));
  });
  ws.onmessage = async event => {
    const message = JSON.parse(event.data);
    if (message.id) {
      const request = pending.get(message.id);
      pending.delete(message.id);
      if (message.error) request.reject(message.error); else request.resolve(message.result);
      return;
    }
    if (message.method === 'Log.entryAdded') logs.push(message.params.entry.text);
    if (message.method !== 'Fetch.requestPaused') return;
    const { requestId, request } = message.params;
    const url = new URL(request.url);
    const relative = decodeURIComponent(url.pathname).replace(/^\//, '');
    let file;
    if (url.hostname === 'readium_package') file = path.resolve(fixture, relative);
    if (url.hostname === 'readium_package' && relative.startsWith('__deft_fonts/') && !fs.existsSync(file)) {
      file = path.resolve(fontsRoot, relative);
    }
    if (url.hostname === 'readium_assets' && relative.startsWith('readium/')) {
      file = path.resolve(fixture, 'assets', relative);
    }
    if (!file || !(file.startsWith(fixture + path.sep) || file.startsWith(fontsRoot + path.sep)) || !fs.existsSync(file)) {
      blocked.push(request.url);
      await command('Fetch.failRequest', { requestId, errorReason: 'BlockedByClient' });
      return;
    }
    loaded.push(request.url);
    const type = { '.xhtml': 'application/xhtml+xml', '.js': 'application/javascript', '.css': 'text/css',
      '.svg': 'image/svg+xml', '.otf': 'font/otf', '.ttf': 'font/ttf' }[path.extname(file)];
    await command('Fetch.fulfillRequest', { requestId, responseCode: 200,
      responseHeaders: [{ name: 'Content-Type', value: type || 'application/octet-stream' }],
      body: fs.readFileSync(file).toString('base64') });
  };
  const evaluate = async expression => {
    const result = await command('Runtime.evaluate', { expression, returnByValue: true, awaitPromise: true });
    assert(!result.exceptionDetails, JSON.stringify(result.exceptionDetails));
    return result.result.value;
  };
  try {
    await command('Log.enable');
    await command('Page.enable');
    await command('Fetch.enable', { patterns: [{ urlPattern: '*' }] });
    await command('Emulation.setDeviceMetricsOverride', { width: 412, height: 780, deviceScaleFactor: 1, mobile: true });
    await command('Page.addScriptToEvaluateOnNewDocument', { source: `
      window.tapCount=0;
      window.Android=new Proxy({}, {get: (_, key) => (...args) => {
        if (key==='getViewportWidth') return 412;
        if (key==='onTap') window.tapCount++;
        return false;
      }});` });
    await command('Page.navigate', { url: 'https://readium_package/chapter.xhtml' });
    for (let count = 0; count < 100; count++) {
      if (await evaluate(`document.readyState === 'complete'`)) break;
      await pause(50);
    }
    assert.equal(await evaluate(`typeof window.readium?.setProperty`), 'function', logs.join('\n'));
    assert.equal(await evaluate(`Boolean(window.bookScriptRan || window.bookHandlerRan)`), false);
    assert.equal(await evaluate(`document.querySelector('#local-image').naturalWidth`), 48, logs.join('\n'));
    assert.equal(await evaluate(`document.querySelector('#remote-image').hasAttribute('src')`), false);
    assert.equal(await evaluate(`document.querySelector('meta[name="viewport"]').content.includes('user-scalable=no')`), true);
    await evaluate(`readium.setCSSProperties({
      '--USER__advancedSettings':'readium-advanced-on', '--USER__fontOverride':'readium-font-on',
      '--USER__fontFamily':'Probe', '--USER__fontSize':'175%', '--USER__lineHeight':'1.8',
      '--USER__backgroundColor':'#201a17', '--USER__textColor':'#eadccc',
      '--USER__view':'readium-scroll-on'
    });`);
    await evaluate(`document.fonts.load('20px Probe')`);
    const result = await evaluate(`JSON.stringify({
      font:getComputedStyle(document.querySelector('#sample')).fontFamily,
      fontSize:parseFloat(getComputedStyle(document.documentElement).fontSize),
      background:getComputedStyle(document.documentElement).backgroundColor,
      color:getComputedStyle(document.querySelector('#sample')).color,
      scroll:document.scrollingElement.scrollHeight > innerHeight,
      fontLoaded:document.fonts.check('20px Probe')
    })`);
    const style = JSON.parse(result);
    assert(style.font.includes('Probe'), result);
    assert(style.fontSize >= 27, result);
    assert.equal(style.background, 'rgb(32, 26, 23)', result);
    assert.equal(style.color, 'rgb(234, 220, 204)', result);
    assert(style.scroll && style.fontLoaded, result);
    assert(loaded.some(url => url.endsWith('ReadiumCSS-after.css')));
    assert(loaded.some(url => url.endsWith('__deft_fonts/probe.font')));
    assert(!logs.some(log => log.includes('invalid source')), logs.join('\n'));
    assert(!loaded.some(url => new URL(url).hostname === 'example.test'));
    await evaluate(`(()=>{const style=document.createElementNS('http://www.w3.org/1999/xhtml','style');
      style.textContent=${JSON.stringify(fontManifest.css)};document.head.appendChild(style);})()`);
    let fontFacesChecked = 0;
    for (const font of fontManifest.families) {
      for (const variant of ['normal 400', 'italic 400', 'normal 700']) {
        const descriptor = `${variant} 20px "${font.family}"`;
        const faces = await evaluate(`document.fonts.load(${JSON.stringify(descriptor)}).then(faces=>faces.map(face=>({
          family:face.family,status:face.status
        })))`);
        assert(faces.length > 0 && faces.every(face => face.status === 'loaded'), `${font.id} ${variant}: ${JSON.stringify(faces)}`);
        fontFacesChecked++;
      }
    }
    assert.equal(fontFacesChecked, 33);
    assert(!loaded.some(url => url === 'https://readium_assets/readium/fonts/OpenDyslexic-Regular.otf'),
      'Deft OpenDyslexic must not collide with Readium\'s appended cross-origin face');
    console.log(JSON.stringify({ passed: true, resourcesLoaded: loaded.length, fontFacesChecked, blocked, style }, null, 2));
  } finally {
    await command('Browser.close').catch(() => {});
    ws.close();
  }
}

main().catch(error => { console.error(error); process.exitCode = 1; }).finally(() => {
  child.kill();
  // Keep no user data; only this uniquely created temporary profile is removed.
  assert(profile.startsWith(path.resolve(os.tmpdir()) + path.sep) && path.basename(profile).startsWith('deft-reader-chromium-'));
  setTimeout(() => fs.rmSync(profile, { recursive: true, force: true, maxRetries: 4, retryDelay: 100 }), 250);
});
