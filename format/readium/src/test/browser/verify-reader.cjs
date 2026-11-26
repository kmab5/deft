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
      window.annotationCalls=[];
      window.Android=new Proxy({}, {get: (_, key) => (...args) => {
        if (key==='getViewportWidth') return 412;
        if (key==='onTap') window.tapCount++;
        if (key==='onDecorationActivated') { window.annotationCalls.push(JSON.parse(args[0])); return true; }
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
    await evaluate(fs.readFileSync('format/readium/src/main/assets/deft-reader/annotations.js', 'utf8'));
    const anchors = await evaluate(`(()=>{
      const parent=document.createElementNS('http://www.w3.org/1999/xhtml','div');
      parent.id='annotation-probe';
      parent.innerHTML='<p id="repeat-one">Same <em>quiet</em> passage.</p><p id="repeat-two">Same <em>quiet</em> passage.</p>';
      document.body.appendChild(parent);
      const range=document.createRange();range.selectNodeContents(document.getElementById('repeat-two'));
      const selected=getSelection();selected.removeAllRanges();selected.addRange(range);
      const captured=deftAnnotations.selection(document);window.savedReaderCapture=captured;selected.removeAllRanges();
      readium.registerDecorationTemplates({probe:{layout:'boxes',width:'wrap',stylesheet:''}});
      const group=readium.getDecorations('deft-highlights');
      group.add({id:'71',style:'probe',element:'<div style="background:purple;opacity:.24"></div>',
        locator:{href:'chapter.xhtml',type:'application/xhtml+xml',locations:{cssSelector:'#annotation-probe'},text:{highlight:captured.text}}});
      const before=group.items[0].range.startContainer.parentElement.closest('p').id;
      deftAnnotations.correctReadium(group,[{id:'71',...captured}]);
      const item=group.items[0];
      const after=item.range.startContainer.nodeType===1?item.range.startContainer.id:item.range.startContainer.parentElement.closest('p').id;
      item.range.startContainer.parentElement.scrollIntoView();
      return {before,after,clickable:item.clickableElements.length,text:item.range.toString(),selected:captured.text};
    })()`);
    assert.equal(anchors.before, 'repeat-one', 'Readium quote matcher demonstrates the repeated-text ambiguity');
    assert.equal(anchors.after, 'repeat-two', 'Production adapter corrects Readium to the exact stored cross-node range');
    assert.equal(anchors.text, anchors.selected);
    assert.ok(anchors.clickable > 0, 'Readium activation hit areas survive correction');
    await pause(100);
    const hit = await evaluate(`(()=>{const r=readium.getDecorations('deft-highlights').items[0].clickableElements[0].getBoundingClientRect();return {x:r.left+2,y:r.top+2}})()`);
    await command('Input.dispatchMouseEvent', {type:'mousePressed',button:'left',clickCount:1,...hit});
    await command('Input.dispatchMouseEvent', {type:'mouseReleased',button:'left',clickCount:1,...hit});
    assert.ok(await evaluate(`annotationCalls.some(event=>event.id==='71'&&event.group==='deft-highlights')`), 'Actual bundled Readium dispatches annotation activation after range correction');
    for (const mutation of ['add','update']) {
      const deferred = await evaluate(`new Promise(resolve=>{
        const group=readium.getDecorations('deft-highlights');
        if (${JSON.stringify(mutation)}==='add') group.clear();
        requestAnimationFrame(()=>group[${JSON.stringify(mutation)}]({id:'71',style:'probe',element:'<div style="background:purple;opacity:.24"></div>',
          locator:{href:'chapter.xhtml',type:'application/xhtml+xml',locations:{cssSelector:'#annotation-probe'},text:{highlight:savedReaderCapture.text}}}));
        deftAnnotations.correctReadium(group,[{id:'71',...savedReaderCapture}]);
        requestAnimationFrame(()=>requestAnimationFrame(()=>requestAnimationFrame(()=>resolve(
          (group.items[0]?.range.startContainer.nodeType===1?group.items[0].range.startContainer:group.items[0]?.range.startContainer.parentElement)?.closest('p')?.id))));
      })`);
      assert.equal(deferred,'repeat-two',`Readium RAF-delayed ${mutation} preserves exact duplicate anchor`);
    }
    const boundaries = await evaluate(`(()=>{
      const parent=document.createElementNS('http://www.w3.org/1999/xhtml','div');
      parent.innerHTML='<p>A<br/>B<span>C</span>D<img/>E</p><p>A<br/>B<span>C</span>D<img/>E</p>';
      document.body.append(parent);const p=parent.lastElementChild;const selected=getSelection();
      return [[1,3],[5,7]].map(([start,end])=>{const range=document.createRange();range.setStart(p,start);range.setEnd(p,end);
        selected.removeAllRanges();selected.addRange(range);const capture=deftAnnotations.selection(document);selected.removeAllRanges();
        const restored=deftAnnotations.range(document,capture);return {raw:range.toString(),restored:restored?.toString(),
          start:restored?.startOffset,end:restored?.endOffset,expectedStart:start,expectedEnd:end};});
    })()`);
    for (const boundary of boundaries) {
      assert.equal(boundary.restored,boundary.raw,'Textless BR/image boundaries preserve exact range');
      assert.equal(boundary.start,boundary.expectedStart);
      assert.equal(boundary.end,boundary.expectedEnd);
    }
    const crossParagraph = await evaluate(`(()=>{
      const parent=document.getElementById('annotation-probe');
      parent.innerHTML='<div id="cross-one"><p>Across</p><p><em>quiet</em> fields.</p></div><div id="cross-two"><p>Across</p><p><em>quiet</em> fields.</p></div>';
      const target=document.getElementById('cross-two');
      target.style.marginTop='1500px';
      const tail=document.createElementNS('http://www.w3.org/1999/xhtml','div');
      tail.style.height='1500px';document.body.appendChild(tail);
      const selected=getSelection();const range=document.createRange();range.selectNodeContents(target);
      selected.removeAllRanges();selected.addRange(range);const capture=deftAnnotations.selection(document);selected.removeAllRanges();
      const group=readium.getDecorations('deft-highlights');group.clear();
      group.add({id:'72',style:'probe',element:'<div style="background:green;opacity:.24"></div>',
        locator:{href:'chapter.xhtml',type:'application/xhtml+xml',locations:{cssSelector:'#annotation-probe'},text:{highlight:capture.domRange.text}}});
      deftAnnotations.correctReadium(group,[{id:'72',...capture}]);
      window.crossLocator={href:'chapter.xhtml',locations:{domRange:capture.domRange},text:{highlight:capture.text}};
      const navigated=deftAnnotations.goReadium(crossLocator);
      const item=group.items[0];
      return {raw:range.toString(),storedRaw:capture.domRange.text,display:capture.text,
        restored:item?.range.toString(),container:(item?.range.startContainer.nodeType===1?item.range.startContainer:item?.range.startContainer.parentElement)?.closest('div')?.id,
        top:range.getBoundingClientRect().top,marker:!!document.getElementById('deft-range-jump'),navigated};
    })()`);
    assert.equal(crossParagraph.storedRaw, crossParagraph.raw, 'Cross-paragraph raw DOM quote is captured independently');
    assert.notEqual(crossParagraph.display, crossParagraph.raw, 'Fixture exercises browser-added block newlines');
    assert.equal(crossParagraph.restored, crossParagraph.raw);
    assert.equal(crossParagraph.container, 'cross-two', 'Actual Readium restores second identical cross-paragraph passage');
    assert.ok(Math.abs(crossParagraph.top)<50, 'Exact locator jump reaches the second cross-paragraph selection: '+JSON.stringify(crossParagraph));
    assert.equal(crossParagraph.marker,false,'Navigation leaves no marker in the publication document');
    await evaluate(`readium.setCSSProperties({'--USER__view':'readium-paged-on'})`);
    await pause(100);
    const pagedJump = await evaluate(`(()=>{const ok=deftAnnotations.goReadium(crossLocator);
      const rect=deftAnnotations.range(document,{domRange:crossLocator.locations.domRange,text:crossLocator.text.highlight}).getBoundingClientRect();
      return {ok,left:rect.left,width:innerWidth,scrollWidth:document.scrollingElement.scrollWidth};})()`);
    assert.ok(pagedJump.ok && pagedJump.left>=-2 && pagedJump.left<pagedJump.width,
      'Actual Readium paged layout navigates to the exact anchored column: '+JSON.stringify(pagedJump));
    assert(!loaded.some(url => url === 'https://readium_assets/readium/fonts/OpenDyslexic-Regular.otf'),
      'Deft OpenDyslexic must not collide with Readium\'s appended cross-origin face');
    console.log(JSON.stringify({ passed: true, resourcesLoaded: loaded.length, fontFacesChecked, blocked, style }, null, 2));
  } finally {
    await command('Browser.close').catch(() => {});
    ws.close();
  }
}

main().catch(error => { console.error(error); process.exitCode = 1; }).finally(async () => {
  const stopped = child.exitCode !== null ? Promise.resolve() : new Promise(resolve => child.once('exit', resolve));
  child.kill();
  await Promise.race([stopped, pause(2000)]);
  // Keep no user data; only this uniquely created temporary profile is removed.
  const resolved = fs.realpathSync(profile);
  assert.equal(path.dirname(resolved), fs.realpathSync(os.tmpdir()));
  assert(path.basename(resolved).startsWith('deft-reader-chromium-'));
  await fs.promises.rm(resolved, {recursive: true, force: true, maxRetries: 10, retryDelay: 200});
});
