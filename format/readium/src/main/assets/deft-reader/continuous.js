/* Trusted host only. Publication frames never receive script permission. */
(() => {
  "use strict";
  const root = document.getElementById("deft-content");
  const chapters = JSON.parse(document.getElementById("deft-chapters").textContent);
  const publication = "https://readium_package/";
  const clamp = value => Math.max(0, Math.min(1, Number(value) || 0));
  const bridge = (method, ...args) => {
    try { window.DeftContinuous?.[method]?.(...args); } catch (_) { /* Host may have detached. */ }
  };
  const baseline = "html,body{height:auto!important;min-height:0!important;max-height:none!important;" +
    "overflow:visible!important;column-count:auto!important;column-width:auto!important;" +
    "-webkit-column-count:auto!important;-webkit-column-width:auto!important;}" +
    "html{overflow-x:hidden!important}img,svg{max-width:100%!important;}" +
    "::highlight(deft-search){background:rgba(195,115,255,.35);color:inherit}";
  const entries = chapters.map(chapter => {
    const section = document.createElement("section");
    section.setAttribute("aria-label", chapter.title || "chapter");
    const height = Math.max(window.innerHeight, 1) * 3;
    section.style.height = height + "px";
    root.appendChild(section);
    return { ...chapter, url: new URL(chapter.href, publication), section, height, frame: null,
      ready: false, observer: null, failed: false };
  });
  let suppliedCss = "";
  let pending = null;
  let lastAnchor = null;
  let scheduled = false;
  let measurementScheduled = false;
  let disposed = false;
  let selectionChangedAt = 0;
  let lastLocation = "";
  let restoring = false;

  function entryAt(y) {
    if (!entries.length) return null;
    let low = 0, high = entries.length - 1;
    while (low < high) {
      const middle = Math.ceil((low + high) / 2);
      if (entries[middle].section.offsetTop <= y) low = middle; else high = middle - 1;
    }
    return entries[low];
  }

  function current() {
    if (pending) return { href: pending.entry.href, progression: pending.progression };
    const last = entries[entries.length - 1];
    const end = Math.max(0, document.documentElement.scrollHeight - window.innerHeight);
    if (last?.ready && window.scrollY >= end - 1) return { href: last.href, progression: 1 };
    const entry = entryAt(window.scrollY);
    return entry ? { href: entry.href, progression: clamp((window.scrollY - entry.section.offsetTop) / entry.height) } : null;
  }

  function selection() {
    for (const entry of entries) {
      if (!entry.ready) continue;
      const selected = entry.frame.contentWindow.getSelection();
      if (selected && !selected.isCollapsed && selected.toString().trim()) {
        const rect = selected.getRangeAt(0).getBoundingClientRect();
        return { href: entry.href, progression: clamp(rect.top / entry.height), text: selected.toString() };
      }
    }
    return null;
  }

  function captureAnchor() {
    const entry = entryAt(window.scrollY);
    if (!entry) return null;
    const offset = Math.max(0, window.scrollY - entry.section.offsetTop);
    let range = null, gap = 0;
    if (entry.ready) {
      const doc = entry.frame.contentDocument;
      const point = doc.caretRangeFromPoint?.(Math.min(window.innerWidth / 3, 120), offset + 2);
      if (point && point.startContainer.nodeType === Node.TEXT_NODE) {
        range = point.cloneRange();
        gap = range.getBoundingClientRect().top - offset;
      }
    }
    return { entry, progression: clamp(offset / entry.height), range, gap };
  }

  function restoreAnchor(anchor) {
    if (!anchor || pending) return;
    let offset = anchor.progression * anchor.entry.height;
    if (anchor.range && anchor.entry.ready && anchor.range.startContainer.isConnected) {
      offset = anchor.range.getBoundingClientRect().top - anchor.gap;
    }
    const target = anchor.entry.section.offsetTop + Math.max(0, offset);
    if (Math.abs(window.scrollY - target) > 1) {
      restoring = true;
      window.scrollTo(0, target);
      requestAnimationFrame(() => { restoring = false; });
    }
  }

  function measure(entry) {
    if (!entry.ready) return;
    const doc = entry.frame.contentDocument;
    // Remove the old viewport floor so a smaller font can also shrink a chapter.
    entry.frame.style.height = "1px";
    const height = Math.max(1, Math.ceil(Math.max(
      doc.documentElement.scrollHeight,
      doc.body?.scrollHeight || 0,
      doc.body?.getBoundingClientRect().bottom || 0
    )));
    entry.height = height;
    entry.frame.style.height = height + "px";
    entry.section.style.height = height + "px";
  }

  function measureSoon() {
    if (measurementScheduled || disposed) return;
    measurementScheduled = true;
    requestAnimationFrame(() => {
      measurementScheduled = false;
      const anchor = lastAnchor || captureAnchor();
      entries.filter(entry => entry.ready).forEach(measure);
      restoreAnchor(anchor);
      if (pending?.entry.ready) applyPending();
      schedule();
    });
  }

  function applyCss(entry) {
    if (!entry.ready) return;
    const doc = entry.frame.contentDocument;
    let style = doc.getElementById("deft-continuous-style");
    if (!style) {
      style = doc.createElementNS("http://www.w3.org/1999/xhtml", "style");
      style.id = "deft-continuous-style";
      (doc.head || doc.documentElement).appendChild(style);
    }
    style.textContent = suppliedCss + "\n" + baseline;
    root.style.color = getComputedStyle(doc.documentElement).color;
  }

  function configure(css) {
    lastAnchor = captureAnchor();
    suppliedCss = String(css || "");
    entries.forEach(applyCss);
    measureSoon();
  }

  function installInput(entry) {
    const doc = entry.frame.contentDocument;
    let down = null;
    doc.addEventListener("pointerdown", event => {
      down = { x: event.clientX, y: event.clientY, at: performance.now() };
    }, true);
    doc.addEventListener("selectionchange", () => {
      selectionChangedAt = performance.now();
    });
    doc.addEventListener("click", event => {
      const target = event.target.nodeType === Node.ELEMENT_NODE ? event.target : event.target.parentElement;
      if (!target) return;
      const link = target.closest("a[href]");
      if (link) event.preventDefault();
      if (selection() || performance.now() - selectionChangedAt < 350) return;
      if (down && (performance.now() - down.at > 450 ||
          Math.hypot(event.clientX - down.x, event.clientY - down.y) > 12)) return;
      const image = target.closest("img,image") || target.closest("svg")?.querySelector("image");
      if (image) {
        event.preventDefault();
        const source = image.currentSrc || image.getAttribute("src") || image.getAttribute("href") || image.getAttribute("xlink:href");
        if (source) emitResolved("image", source, doc.baseURI, entry.href);
      } else if (link) {
        emitResolved("link", link.getAttribute("href"), doc.baseURI, entry.href);
      } else {
        bridge("tap");
      }
    }, true);
  }

  function emitResolved(method, value, base, chapter) {
    try { bridge(method, new URL(value, base).href); }
    catch (_) { bridge("error", chapter); }
  }

  function load(entry) {
    if (entry.frame || entry.failed) return;
    const frame = document.createElement("iframe");
    frame.setAttribute("sandbox", "allow-same-origin");
    frame.setAttribute("scrolling", "no");
    frame.setAttribute("title", entry.title || "chapter");
    frame.setAttribute("referrerpolicy", "no-referrer");
    frame.style.height = entry.height + "px";
    entry.frame = frame;
    frame.addEventListener("load", () => {
      if (entry.frame !== frame) return;
      try {
        if (!frame.contentDocument?.documentElement || !frame.contentDocument.body) throw new Error("empty chapter");
        entry.ready = true;
        applyCss(entry);
        installInput(entry);
        entry.observer = new ResizeObserver(measureSoon);
        entry.observer.observe(frame.contentDocument.body);
        frame.contentDocument.querySelectorAll("img").forEach(image => {
          image.addEventListener("load", measureSoon);
          image.addEventListener("error", measureSoon);
        });
        frame.contentDocument.fonts?.ready.then(measureSoon);
        measureSoon();
      } catch (_) { fail(entry); }
    });
    frame.addEventListener("error", () => fail(entry));
    frame.src = entry.url.href;
    entry.section.appendChild(frame);
  }

  function fail(entry) {
    entry.failed = true;
    entry.ready = false;
    entry.observer?.disconnect();
    entry.observer = null;
    entry.frame?.remove();
    entry.frame = null;
    if (pending?.entry === entry) pending = null;
    entry.section.textContent = "This chapter could not be displayed. Open Contents to continue.";
    entry.section.style.height = "auto";
    entry.height = Math.max(entry.section.offsetHeight, 48);
    entry.section.style.height = entry.height + "px";
    bridge("error", entry.href);
    schedule();
  }

  function unload(entry) {
    if (!entry.frame) return;
    const selected = entry.ready && entry.frame.contentWindow.getSelection();
    if (selected && !selected.isCollapsed) return;
    entry.observer?.disconnect();
    entry.observer = null;
    entry.ready = false;
    entry.frame.remove();
    entry.frame = null;
  }

  function quoteRange(doc, quote, progression, before, after) {
    const wanted = String(quote || "").replace(/\s+/g, " ").trim();
    if (!wanted) return null;
    const walker = doc.createTreeWalker(doc.body, NodeFilter.SHOW_TEXT, {
      acceptNode(node) {
        return node.parentElement?.closest("script,style,noscript") ? NodeFilter.FILTER_REJECT : NodeFilter.FILTER_ACCEPT;
      }
    });
    let node, text = "", points = [], space = false;
    while ((node = walker.nextNode())) {
      for (let offset = 0; offset < node.textContent.length; offset++) {
        const character = node.textContent[offset];
        if (/\s/.test(character)) {
          if (!space && text) { text += " "; points.push([node, offset]); }
          space = true;
        } else { text += character; points.push([node, offset]); space = false; }
      }
    }
    const leading = String(before || "").replace(/\s+/g, " ").trim();
    const trailing = String(after || "").replace(/\s+/g, " ").trim();
    const candidates = [];
    for (let index = text.indexOf(wanted); index >= 0; index = text.indexOf(wanted, index + 1)) {
      const matchesBefore = !leading || text.slice(0, index).trimEnd().endsWith(leading);
      const matchesAfter = !trailing || text.slice(index + wanted.length).trimStart().startsWith(trailing);
      candidates.push({index, context: matchesBefore && matchesAfter,
        distance: Math.abs(index / Math.max(text.length, 1) - clamp(progression))});
    }
    candidates.sort((left, right) => Number(right.context) - Number(left.context) || left.distance - right.distance);
    const start = candidates[0]?.index;
    if (start === undefined) return null;
    const range = doc.createRange();
    range.setStart(...points[start]);
    const end = points[start + wanted.length - 1];
    range.setEnd(end[0], end[1] + 1);
    return range;
  }

  function applyPending() {
    if (!pending?.entry.ready) return;
    const jump = pending;
    const doc = jump.entry.frame.contentDocument;
    let offset = jump.progression * jump.entry.height;
    const range = jump.quote ? quoteRange(doc, jump.quote, jump.progression, jump.before, jump.after) : null;
    if (range) {
      offset = range.getBoundingClientRect().top;
      if (jump.entry.frame.contentWindow.CSS?.highlights && window.Highlight) {
        const highlight = new Highlight(range);
        jump.entry.frame.contentWindow.CSS.highlights.set("deft-search", highlight);
      }
    } else if (jump.fragment) {
      let fragment = jump.fragment.replace(/^#/, "");
      try { fragment = decodeURIComponent(fragment); } catch (_) { /* Keep literal fragment. */ }
      const element = doc.getElementById(fragment) || doc.getElementsByName(fragment)[0];
      if (element) offset = element.getBoundingClientRect().top;
    }
    window.scrollTo(0, jump.entry.section.offsetTop + Math.max(0, offset));
    pending = null;
    lastAnchor = captureAnchor();
    lastLocation = "";
    schedule();
  }

  function go(href, progression, fragment, quote, before, after) {
    let target;
    try { target = new URL(href, publication); } catch (_) { bridge("error", String(href)); return; }
    const entry = entries.find(item => item.url.origin === target.origin && item.url.pathname === target.pathname && item.url.search === target.search);
    if (!entry) { bridge("error", String(href)); return; }
    if (entry.failed) { bridge("error", entry.href); return; }
    clearSelection();
    entries.filter(item => item.ready).forEach(item => item.frame.contentWindow.CSS?.highlights?.delete("deft-search"));
    pending = { entry, progression: clamp(progression), fragment: fragment || target.hash, quote, before, after };
    load(entry);
    if (entry.ready) applyPending(); else window.scrollTo(0, entry.section.offsetTop);
    schedule();
  }

  function clearSelection() {
    entries.filter(entry => entry.ready).forEach(entry => {
      entry.frame.contentWindow.getSelection()?.removeAllRanges();
    });
  }

  function update() {
    scheduled = false;
    if (disposed) return;
    const y = window.scrollY;
    const margin = window.innerHeight * 2;
    for (const entry of entries) {
      const top = entry.section.offsetTop;
      if (entry === pending?.entry || (top + entry.height >= y - margin && top <= y + window.innerHeight + margin)) load(entry);
      else unload(entry);
    }
    if (!pending && !restoring) {
      const location = current();
      if (location) {
        const key = location.href + ":" + location.progression.toFixed(5);
        if (key !== lastLocation) {
          lastLocation = key;
          bridge("location", location.href, location.progression);
        }
      }
      lastAnchor = captureAnchor();
    }
  }

  function schedule() {
    if (!scheduled && !disposed) { scheduled = true; requestAnimationFrame(update); }
  }

  window.deftContinuous = { configure, go, current, selection, clearSelection };
  window.addEventListener("scroll", schedule, { passive: true });
  window.addEventListener("resize", () => { measureSoon(); schedule(); });
  window.addEventListener("pagehide", () => {
    disposed = true;
    entries.forEach(entry => entry.observer?.disconnect());
  });
  schedule();
})();
