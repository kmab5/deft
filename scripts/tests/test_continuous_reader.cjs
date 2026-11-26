/* Browser contract test using installed Chrome and Node's native CDP/WebSocket support. */
const assert = require('node:assert/strict');
const fs = require('node:fs');
const cp = require('node:child_process');
const path = require('node:path');
const os = require('node:os');
const chrome = process.env.CHROME || (process.platform === 'win32'
  ? 'C:/Program Files/Google/Chrome/Application/chrome.exe' : '/usr/bin/google-chrome');
const profile = fs.mkdtempSync(path.join(os.tmpdir(), 'deft-continuous-test-'));
const source = fs.readFileSync('format/readium/src/main/kotlin/com/kmab/deft/format/readium/ContinuousReaderDocument.kt', 'utf8');
const script = fs.readFileSync('format/readium/src/main/assets/deft-reader/continuous.js', 'utf8');
const chapters = Array.from({length: 30}, (_, index) => ({href: `OPS/chapters/${index}.xhtml`, title: `Chapter ${index}`}));
const html = source.match(/return """([\s\S]*?)"""\.trimIndent/)[1].replace('$manifest', JSON.stringify(chapters));
const child = cp.spawn(chrome, ['--headless=new', '--no-first-run', '--no-default-browser-check',
  '--remote-debugging-port=0', '--user-data-dir=' + profile, 'about:blank'], {windowsHide: true, stdio: 'ignore'});
const pause = ms => new Promise(resolve => setTimeout(resolve, ms));
let ws;
async function main() {
  let port;
  for (let tries = 0; tries < 100; tries++) {
    try { port = fs.readFileSync(path.join(profile, 'DevToolsActivePort'), 'utf8').split('\n')[0]; break; }
    catch (_) { await pause(50); }
  }
  assert.ok(port, 'Installed Chrome starts');
  const pages = await (await fetch(`http://127.0.0.1:${port}/json`)).json();
  ws = new WebSocket(pages.find(page => page.type === 'page').webSocketDebuggerUrl);
  await new Promise(resolve => { ws.onopen = resolve; });
  let id = 0;
  const pending = new Map();
  const errors = [];
  const requested = [];
  const send = (method, params = {}) => new Promise((resolve, reject) => {
    const next = ++id;
    pending.set(next, {resolve, reject});
    ws.send(JSON.stringify({id: next, method, params}));
  });
  ws.onmessage = async event => {
    const message = JSON.parse(event.data);
    if (message.id) {
      const request = pending.get(message.id);
      pending.delete(message.id);
      message.error ? request.reject(message.error) : request.resolve(message.result);
      return;
    }
    if (message.method === 'Runtime.exceptionThrown') errors.push(message.params.exceptionDetails);
    if (message.method !== 'Fetch.requestPaused') return;
    const {requestId, request} = message.params;
    requested.push(request.url);
    let body = '', type = 'text/plain', code = 200;
    if (request.url.endsWith('/__deft_continuous/index.html')) { body = html; type = 'text/html'; }
    else if (request.url.endsWith('/deft-reader/continuous.js')) { body = script; type = 'application/javascript'; }
    else if (request.url.endsWith('/deft-reader/annotations.js')) {
      body = fs.readFileSync('format/readium/src/main/assets/deft-reader/annotations.js', 'utf8'); type = 'application/javascript';
    }
    else if (/\/chapters\/\d+\.xhtml/.test(request.url)) {
      const index = Number(request.url.match(/\/chapters\/(\d+)\.xhtml/)[1]);
      const paragraphs = Array.from({length: 60}, (_, n) => `<p id="p${n}">Chapter ${index}, paragraph ${n}. A <em>quiet</em> passage through the islands, with enough words to wrap across lines on this screen.</p>`).join('');
      body = `<html xmlns="http://www.w3.org/1999/xhtml"><head><title>Chapter ${index}</title><link rel="stylesheet" href="../styles/book.css"/><style>p{color:${index % 2 ? 'blue' : 'red'}}</style></head><body><img id="picture" src="../images/picture.svg"/><svg xmlns="http://www.w3.org/2000/svg" id="svg-cover" width="40" height="40"><image href="../images/picture.svg" width="40" height="40"/></svg><a id="next" href="${index + 1}.xhtml#p20">Next chapter</a>${paragraphs}<script>window.bookScriptExecuted=true</script></body></html>`;
      if (index === 26) body = '<html xmlns="http://www.w3.org/1999/xhtml"><head><title>Broken chapter without a body</title></head></html>';
      type = 'application/xhtml+xml';
    } else if (request.url.endsWith('/styles/book.css')) { body = 'p{margin:0 0 20px}body{padding:16px;margin:0}'; type = 'text/css'; }
    else if (request.url.endsWith('/images/picture.svg')) { body = '<svg xmlns="http://www.w3.org/2000/svg" width="40" height="40"><rect width="40" height="40" fill="green"/></svg>'; type = 'image/svg+xml'; }
    else { code = 404; }
    try {
      await send('Fetch.fulfillRequest', {requestId, responseCode: code,
        responseHeaders: [{name: 'Content-Type', value: type}], body: Buffer.from(body).toString('base64')});
    } catch (error) {
      // A superseded jump can detach an iframe while its request is intercepted.
      if (!String(error.message).includes('Invalid InterceptionId')) errors.push(error);
    }
  };
  const evaluate = async expression => {
    const result = await send('Runtime.evaluate', {expression, returnByValue: true, awaitPromise: true});
    if (result.exceptionDetails) throw new Error(JSON.stringify(result.exceptionDetails));
    return result.result.value;
  };
  const until = async (expression, description) => {
    for (let attempt = 0; attempt < 120; attempt++) {
      if (await evaluate(expression)) return;
      await pause(50);
    }
    throw new Error('Timed out: ' + description + '; state=' + await evaluate('JSON.stringify({location:window.deftContinuous?.current(),y:scrollY,height:document.body.scrollHeight,frames:document.querySelectorAll("iframe").length})'));
  };
  await send('Page.enable');
  await send('Runtime.enable');
  await send('Emulation.setDeviceMetricsOverride', {width: 393, height: 700, deviceScaleFactor: 1, mobile: false});
  await send('Page.addScriptToEvaluateOnNewDocument', {source: `if(location.pathname==='/__deft_continuous/index.html'){window.calls=[];window.DeftContinuous=Object.fromEntries(['location','tap','image','link','error','annotation'].map(name=>[name,(...args)=>calls.push([name,...args])]))}`});
  await send('Fetch.enable', {patterns: [{urlPattern: '*'}]});
  await send('Page.navigate', {url: 'https://readium_package/__deft_continuous/index.html'});
  await until('!!window.deftContinuous && !!document.querySelector("iframe")?.contentDocument?.getElementById("deft-continuous-style")', 'first XHTML frame ready');
  await evaluate('deftContinuous.configure("body{font:18px/1.5 serif!important}p{line-height:1.5!important}");deftContinuous.go("OPS/chapters/0.xhtml",0,null)');
  await until('getComputedStyle(document.querySelector("iframe").contentDocument.body).fontSize==="18px"', 'trusted CSS applies to XHTML');
  assert.equal(await evaluate('document.querySelector("iframe").contentWindow.bookScriptExecuted === true'), false, 'sandbox blocks publication scripts');
  assert.ok(requested.some(url => url === 'https://readium_package/OPS/images/picture.svg'), 'relative image URL retained');
  assert.ok(requested.some(url => url === 'https://readium_package/OPS/styles/book.css'), 'relative publisher CSS URL retained');
  await until('document.querySelector("iframe").contentDocument.images[0].naturalWidth === 40', 'local image decoded');
  await pause(400);
  await evaluate('document.querySelector("iframe").contentDocument.getElementById("picture").click();document.querySelector("iframe").contentDocument.getElementById("next").click()');
  assert.ok(await evaluate('calls.some(c=>c[0]==="image"&&c[1]==="https://readium_package/OPS/images/picture.svg")'), 'Image tap reports original absolute URL');
  await evaluate('calls.length=0;document.querySelector("iframe").contentDocument.getElementById("svg-cover").dispatchEvent(new MouseEvent("click",{bubbles:true}))');
  assert.ok(await evaluate('calls.some(c=>c[0]==="image"&&c[1]==="https://readium_package/OPS/images/picture.svg")'), 'SVG cover container resolves its local image');
  await evaluate('document.querySelector("iframe").contentDocument.getElementById("next").click()');
  assert.ok(await evaluate('calls.some(c=>c[0]==="link"&&c[1]==="https://readium_package/OPS/chapters/1.xhtml#p20")'), 'Link tap reports target without navigating iframe');
  await evaluate('deftContinuous.go("OPS/chapters/1.xhtml",0,null)');
  await until('deftContinuous.current()?.href === "OPS/chapters/1.xhtml" && [...document.querySelectorAll("iframe")].some(f=>f.contentDocument?.getElementById("deft-continuous-style") && f.src.endsWith("/1.xhtml"))', 'next frame loaded');
  await until('calls.some(c=>c[0]==="location"&&c[1]==="OPS/chapters/1.xhtml")', 'chapter jump reports a measured location');
  await evaluate('window.scrollTo(0,document.querySelectorAll("section")[1].offsetTop-250)');
  await until('[...document.querySelectorAll("iframe")].filter(f=>{const r=f.getBoundingClientRect();return r.top<innerHeight && r.bottom>0}).length>=2', 'both adjacent chapters settle in viewport');
  const spanning = await evaluate('[...document.querySelectorAll("iframe")].filter(f=>{const r=f.getBoundingClientRect();return r.top<innerHeight && r.bottom>0}).length');
  assert.ok(spanning >= 2, 'Adjacent chapters share the same viewport');
  const divider = await evaluate('(()=>{const s=document.querySelectorAll("section")[1];const c=getComputedStyle(s,"::before");return {content:c.content,height:parseFloat(c.height),opacity:parseFloat(c.opacity)}})()');
  assert.equal(divider.content, '""', 'Adjacent chapters have a quiet visual divider');
  assert.ok(divider.height > 0 && divider.height <= 1 && divider.opacity < 0.5, 'Chapter divider stays subtle');
  await send('Input.dispatchMouseEvent', {type: 'mouseWheel', x: 190, y: 350, deltaX: 0, deltaY: 500});
  await until('deftContinuous.current()?.href === "OPS/chapters/1.xhtml"', 'real wheel scroll crosses chapter boundary');
  const colors = await evaluate('[...document.querySelectorAll("iframe")].slice(0,2).map(f=>getComputedStyle(f.contentDocument.querySelector("p")).color)');
  assert.ok(colors.includes('rgb(255, 0, 0)') && colors.includes('rgb(0, 0, 255)'), 'Publisher styles stay isolated per chapter');
  await evaluate('deftContinuous.go("OPS/chapters/10.xhtml",0.6,null,"Chapter 10, paragraph 45.")');
  await until('deftContinuous.current()?.href === "OPS/chapters/10.xhtml" && [...document.querySelectorAll("iframe")].some(f=>f.src.endsWith("/10.xhtml") && f.contentDocument?.getElementById("deft-continuous-style"))', 'quote target loaded');
  await until('(()=>{const f=[...document.querySelectorAll("iframe")].find(f=>f.src.endsWith("/10.xhtml"));return f && Math.abs(f.getBoundingClientRect().top+f.contentDocument.getElementById("p45").getBoundingClientRect().top)<50})()', 'quoted paragraph settles at viewport top');
  const quoteOffset = await evaluate('(()=>{const f=[...document.querySelectorAll("iframe")].find(f=>f.src.endsWith("/10.xhtml"));return f.getBoundingClientRect().top+f.contentDocument.getElementById("p45").getBoundingClientRect().top})()');
  assert.ok(Math.abs(quoteOffset) < 50, 'Quoted search jumps to its actual paragraph');
  const anchorBefore = await evaluate(`(()=>{const f=[...document.querySelectorAll('iframe')].find(f=>f.src.endsWith('/10.xhtml'));
    const offset=scrollY-f.parentElement.offsetTop;const p=f.contentDocument.caretRangeFromPoint(Math.min(innerWidth/3,120),offset+2);
    return {offset,rangeId:p?.startContainer.parentElement?.closest('p')?.id,rangeOffset:p?.startOffset,scrollY};})()`);
  await evaluate('deftContinuous.configure("body{font:25px/1.8 serif!important}p{line-height:1.8!important}")');
  await pause(350);
  const resizedOffset = await evaluate('(()=>{const f=[...document.querySelectorAll("iframe")].find(f=>f.src.endsWith("/10.xhtml"));return f.getBoundingClientRect().top+f.contentDocument.getElementById("p45").getBoundingClientRect().top})()');
  assert.ok(Math.abs(resizedOffset - quoteOffset) < 60, 'Font reflow preserves current passage: '+JSON.stringify({quoteOffset,resizedOffset,anchorBefore}));
  const selection = await evaluate('(()=>{const f=[...document.querySelectorAll("iframe")].find(f=>f.src.endsWith("/10.xhtml"));const r=f.contentDocument.createRange();r.selectNodeContents(f.contentDocument.getElementById("p45"));f.contentWindow.getSelection().addRange(r);return deftContinuous.selection()})()');
  assert.equal(selection.href, 'OPS/chapters/10.xhtml');
  assert.ok(selection.text.includes('paragraph 45'));
  assert.ok(selection.domRange?.start?.cssSelector, 'Selection preserves a stable DOM range');
  assert.ok(selection.before.length > 0, 'Selection preserves leading quote context');
  const sample = await evaluate('(()=>{const capture=deftAnnotations.selection;deftAnnotations.selection=()=>{throw new Error("Full anchor capture must not run in feedback sampling")};try{return deftContinuous.selection(true)}finally{deftAnnotations.selection=capture}})()');
  assert.equal(sample.text, selection.text, 'Feedback reads the selected text without chapter context traversal');
  assert.equal(sample.domRange, undefined, 'Feedback samples do not serialize persistent anchors');
  const crossParagraph = await evaluate(`(()=>{
    const frame=[...document.querySelectorAll('iframe')].find(f=>f.src.endsWith('/10.xhtml'));
    const doc=frame.contentDocument;
    const range=doc.createRange();range.setStart(doc.getElementById('p45').firstChild,0);
    const last=doc.getElementById('p46').lastChild;range.setEnd(last,last.length);
    const selected=frame.contentWindow.getSelection();selected.removeAllRanges();selected.addRange(range);
    const capture=deftContinuous.selection();selected.removeAllRanges();
    return {capture,raw:range.toString(),restored:deftAnnotations.range(doc,capture)?.toString()};
  })()`);
  assert.equal(crossParagraph.capture.domRange.text, crossParagraph.raw, 'Cross-paragraph anchor retains exact DOM text separately from display text');
  assert.equal(crossParagraph.restored, crossParagraph.raw, 'Cross-paragraph display newlines do not invalidate exact anchors');
  await evaluate(`(()=>{const frame=[...document.querySelectorAll('iframe')].find(f=>f.src.endsWith('/10.xhtml'));const range=frame.contentDocument.createRange();range.selectNodeContents(frame.contentDocument.getElementById('p45'));frame.contentWindow.getSelection().addRange(range)})()`);
  await evaluate(`window.savedAnnotation=${JSON.stringify({id:'71',color:'#9867c5',...selection})};deftContinuous.annotations([savedAnnotation])`);
  assert.ok(await evaluate('document.querySelectorAll(".deft-annotation").length > 0'), 'Explicit save renders persistent ranges');
  await evaluate('calls.length=0;(()=>{const f=[...document.querySelectorAll("iframe")].find(f=>f.src.endsWith("/10.xhtml"));const p=f.contentDocument.getElementById("p45");p.dispatchEvent(new PointerEvent("pointerup",{bubbles:true}));p.click();window.dispatchEvent(new Event("resize"));})()');
  await pause(150);
  assert.ok(await evaluate('deftContinuous.selection()?.text.includes("paragraph 45")'), 'Handle release and remeasurement preserve selected text');
  assert.equal(await evaluate('calls.some(c=>c[0]==="tap")'), false, 'Selection release never toggles reader controls');
  await evaluate('deftContinuous.clearSelection()');
  assert.equal(await evaluate('deftContinuous.selection()'), null);
  await evaluate('deftContinuous.annotations([{...savedAnnotation,underline:true}])');
  const underlines = await evaluate(`Array.from(document.querySelectorAll(".deft-annotation"), box => {
    const style = getComputedStyle(box);
    const rect = box.getBoundingClientRect();
    return {shadow: style.boxShadow, background: style.backgroundColor, width: rect.width, height: rect.height};
  })`);
  assert.ok(underlines.length > 0, 'An underlined selection produces visible line boxes');
  for (const line of underlines) {
    assert.ok(line.width > 0 && line.height > 2, 'Underline has positive width and room above the stroke');
    assert.match(line.shadow, /0px -2px 0px 0px inset/, 'A two-pixel stroke sits against the lower edge');
    assert.equal(line.background, 'rgba(0, 0, 0, 0)', 'Underline leaves glyph background clear');
  }
  await evaluate('deftContinuous.annotations([{...savedAnnotation,color:null}])');
  await evaluate('deftContinuous.go(savedAnnotation.href,0,null,savedAnnotation.text,savedAnnotation.before,savedAnnotation.after,savedAnnotation.domRange)');
  assert.equal(await evaluate('[...document.querySelectorAll("iframe")].some(f=>f.contentWindow.CSS?.highlights?.has("deft-search"))'), false, 'Opening a colorless note never applies a search highlight');
  await evaluate('deftContinuous.go(savedAnnotation.href,0,null,savedAnnotation.text,savedAnnotation.before,savedAnnotation.after,savedAnnotation.domRange,true)');
  assert.equal(await evaluate('[...document.querySelectorAll("iframe")].some(f=>f.contentWindow.CSS?.highlights?.has("deft-search"))'), true, 'Explicit search keeps its temporary hit highlight');
  assert.equal(await evaluate('document.querySelectorAll(".deft-annotation").length'), 0, 'Colorless note has no fill');
  await evaluate('deftContinuous.annotations([savedAnnotation]);deftContinuous.go("OPS/chapters/20.xhtml",0,null)');
  await pause(300);
  await evaluate('deftContinuous.go("OPS/chapters/10.xhtml",0,null,savedAnnotation.text,savedAnnotation.before,savedAnnotation.after,savedAnnotation.domRange)');
  await until('document.querySelectorAll(".deft-annotation").length > 0', 'Saved decoration reapplies after chapter reload');
  const anchoredOffset = await evaluate('(()=>{const box=document.querySelector(".deft-annotation");const frame=[...document.querySelectorAll("iframe")].find(f=>f.src.endsWith("/10.xhtml"));return Math.abs(box.getBoundingClientRect().top-frame.getBoundingClientRect().top-frame.contentDocument.getElementById("p45").getBoundingClientRect().top)})()');
  assert.ok(anchoredOffset < 20, 'Cross-node repeated text restores the exact paragraph');
  await pause(400);
  await evaluate('calls.length=0;(()=>{const f=[...document.querySelectorAll("iframe")].find(f=>f.src.endsWith("/10.xhtml"));const p=f.contentDocument.getElementById("p45");const r=f.contentDocument.createRange();r.selectNodeContents(p);const b=r.getClientRects()[0];p.dispatchEvent(new MouseEvent("click",{bubbles:true,clientX:b.left+2,clientY:b.top+2}))})()');
  assert.ok(await evaluate('calls.some(c=>c[0]==="annotation"&&c[1]==="71")'), 'Tapping saved range opens annotation without toggling chrome');
  assert.equal(await evaluate('calls.some(c=>c[0]==="tap")'), false);
  for (const color of ['#9867c5','#588767','#b88436','#969298']) {
    await evaluate(`deftContinuous.annotations([{...savedAnnotation,color:${JSON.stringify(color)}}])`);
    assert.ok(await evaluate('document.querySelectorAll(".deft-annotation").length > 0'), 'Every highlight color renders');
  }
  await evaluate('deftContinuous.annotations([])');
  assert.equal(await evaluate('document.querySelectorAll(".deft-annotation").length'), 0, 'Deleting removes stale fill');
  await evaluate('deftContinuous.go("OPS/chapters/10.xhtml",0,null,"A quiet passage","Chapter 10, paragraph 45.","through the islands")');
  await pause(250);
  const repeatedOffset = await evaluate('(()=>{const f=[...document.querySelectorAll("iframe")].find(f=>f.src.endsWith("/10.xhtml"));return f.getBoundingClientRect().top+f.contentDocument.getElementById("p45").getBoundingClientRect().top})()');
  assert.ok(Math.abs(repeatedOffset) < 100, 'Repeated search phrase resolves using before/after context, not first occurrence');
  await evaluate('calls.length=0;deftContinuous.go("OPS/chapters/18.xhtml",0.3,null);deftContinuous.go("OPS/chapters/23.xhtml",0.4,null)');
  await until('calls.some(c=>c[0]==="location"&&c[1]==="OPS/chapters/23.xhtml")', 'latest queued jump wins');
  assert.equal(await evaluate('calls.some(c=>c[0]==="location"&&c[1]==="OPS/chapters/18.xhtml")'), false, 'Superseded jump never reports a false location');
  await evaluate('deftContinuous.go("OPS/chapters/26.xhtml",0,null)');
  await until('calls.some(c=>c[0]==="error"&&c[1]==="OPS/chapters/26.xhtml")', 'unreadable chapter reports an error');
  await evaluate('deftContinuous.go("OPS/chapters/29.xhtml",1,null)');
  await until('deftContinuous.current()?.href === "OPS/chapters/29.xhtml" && calls.some(c=>c[0]==="location" && c[1]==="OPS/chapters/29.xhtml")', 'last chapter end reports actual clamped position');
  await pause(250);
  assert.equal(await evaluate('deftContinuous.current().progression'), 1, 'True publication end reports 100 percent');
  assert.ok(await evaluate('document.querySelectorAll("iframe").length') <= 4, 'Chapter windows remain bounded after distant jumps');
  await evaluate('deftContinuous.go("unknown.xhtml",0,null)');
  assert.ok(await evaluate('calls.some(c=>c[0]==="error"&&c[1]==="unknown.xhtml")'), 'Unknown jumps report a recoverable error');
  assert.equal(errors.length, 0, JSON.stringify(errors));
  console.log('PASS continuous XHTML: shared viewport, wheel boundary, CSS/image URLs, script sandbox, quote jump, reflow anchor, selection, end clamp, bounded frames.');
}
main().catch(error => { console.error(error); process.exitCode = 1; }).finally(async () => {
  ws?.close();
  const stopped = new Promise(resolve => child.once('exit', resolve));
  child.kill();
  await Promise.race([stopped, pause(2000)]);
  const resolved = fs.realpathSync(profile);
  assert.equal(path.dirname(resolved), fs.realpathSync(os.tmpdir()));
  assert.ok(path.basename(resolved).startsWith('deft-continuous-test-'));
  fs.rmSync(resolved, {recursive: true, force: true, maxRetries: 10, retryDelay: 200});
});
