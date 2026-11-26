/* Trusted reader helpers. Never executes publication scripts or rewrites passage nodes. */
(() => {
  "use strict";
  const normalize = text => String(text || "").replace(/\s+/g, " ").trim();
  function selector(element) {
    const parts = [];
    while (element && element.nodeType === 1) {
      const siblings = Array.from(element.parentElement?.children || [element]);
      parts.unshift(`*:nth-child(${siblings.indexOf(element) + 1})`);
      element = element.parentElement;
    }
    return parts.join(" > ");
  }
  function point(node, offset) {
    if (node.nodeType !== 3) {
      return node.nodeType === 1 ? {cssSelector: selector(node), childOffset: offset,
        textNodeIndex: 0, charOffset: 0} : null;
    }
    if (!node) return null;
    return { cssSelector: selector(node.parentElement),
      textNodeIndex: Array.from(node.parentElement.childNodes).filter(item => item.nodeType === 3).indexOf(node),
      charOffset: offset };
  }
  function selection(doc, lightweight = false) {
    const selected = doc.defaultView.getSelection();
    if (!selected?.rangeCount || selected.isCollapsed || !selected.toString().trim()) return null;
    const range = selected.getRangeAt(0);
    const ancestor = range.commonAncestorContainer;
    const result = { text: selected.toString(),
      cssSelector: selector(ancestor.nodeType === 1 ? ancestor : ancestor.parentElement),
      domRange: {start: point(range.startContainer, range.startOffset), end: point(range.endContainer, range.endOffset),
        text: range.toString()} };
    if (!lightweight) {
      const prefix = doc.createRange(); prefix.selectNodeContents(doc.body); prefix.setEnd(range.startContainer, range.startOffset);
      const suffix = doc.createRange(); suffix.selectNodeContents(doc.body); suffix.setStart(range.endContainer, range.endOffset);
      result.before = prefix.toString().slice(-128); result.after = suffix.toString().slice(0, 128);
    }
    return result;
  }
  function range(doc, anchor) {
    try {
      const resolve = value => {
        const element = doc.querySelector(value.cssSelector);
        if (Number.isInteger(value.childOffset)) return [element, value.childOffset];
        const node = Array.from(element.childNodes).filter(item => item.nodeType === 3)[value.textNodeIndex];
        return [node, value.charOffset || 0];
      };
      const result = doc.createRange();
      result.setStart(...resolve(anchor.domRange.start)); result.setEnd(...resolve(anchor.domRange.end));
      const valid = typeof anchor.domRange.text === 'string'
        ? result.toString() === anchor.domRange.text : normalize(result.toString()) === normalize(anchor.text);
      return valid ? result : null;
    } catch (_) { return null; }
  }
  // Readium 3.2 exposes its decoration groups, but its quote matcher ignores DOM ranges.
  // Keep its rendering and activation machinery, correcting only verified stored ranges.
  function correctReadium(group, saved) {
    if (!Array.isArray(group?.items) || typeof group.requestLayout !== 'function') return;
    const revision = (group.deftRangeRevision || 0) + 1;
    group.deftRangeRevision = revision;
    const reconcile = remaining => {
      if (group.deftRangeRevision !== revision) return;
      group.items.forEach(item => {
        const data = saved.find(value => value.id === item.decoration.id);
        const exact = data && range(document, data);
        if (exact) item.range = exact;
      });
      group.requestLayout();
      // Readium 3.2 queues add/update in RAF. Reconcile after that frame and its layout;
      // a newer snapshot cancels every outstanding correction from this snapshot.
      if (remaining > 0) requestAnimationFrame(() => reconcile(remaining - 1));
    };
    reconcile(2);
  }
  function goReadium(locator) {
    if (new URL(locator.href, 'https://readium_package/').pathname !== location.pathname) return false;
    const exact = range(document, {domRange: locator.locations?.domRange, text: locator.text?.highlight});
    if (!exact || typeof window.readium?.scrollToId !== 'function') return false;
    const rect = exact.getBoundingClientRect();
    const marker = document.createElementNS('http://www.w3.org/1999/xhtml', 'span');
    marker.id = 'deft-range-jump';
    while (document.getElementById(marker.id)) marker.id += '-';
    marker.style.cssText = `position:absolute;pointer-events:none;width:1px;height:1px;left:${rect.left + scrollX}px;top:${rect.top + scrollY}px`;
    document.documentElement.appendChild(marker);
    try { return window.readium.scrollToId(marker.id); } finally { marker.remove(); }
  }
  window.deftAnnotations = { selection, sample: doc => selection(doc, true), range, correctReadium, goReadium };
})();
