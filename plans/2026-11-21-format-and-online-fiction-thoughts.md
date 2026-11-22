# Format extensions and online fiction — discussion only

Requested in the 0.3.1 device feedback. These are recommendations, not approved
implementation scope. No downloadable code, online fetching or identity changes
are included in 0.3.2.

## Optional format support

Yes, Deft can have an extensible format architecture. The spec already separates
metadata extraction, conversion to EPUB and fixed-layout page rendering. New
engines can implement those contracts while sharing library, tracking and reader
controls. That is useful even if every engine ships in the same APK. An
independently developed engine would also need a public, versioned contract,
capability discovery and stable format IDs instead of assuming every format is
known by the core app. Compatibility checks and isolated failures matter as much
as loading the module. It does not make every format cheap: faithful rendering,
positions, search, DRM detection,
licensing and native builds still need work for each engine.

Downloadable engines are a separate distribution feature. I recommend keeping
M8/M9 engines (renumbered on 2026-11-22) bundled for v1 and measuring their actual size before adding an
extension system. Google Play's on-demand delivery is built around Play delivery,
so it is not a common solution for Deft's GitHub APK and F-Droid channels.
[Android's delivery overview](https://developer.android.com/guide/playcore/feature-delivery).

For a later extension system, I would prefer explicitly installed companion APKs
with a versioned service protocol and narrowly granted book access. That offers
a process/package boundary, at the cost of more complex rendering and IPC.
Downloading arbitrary DEX/native libraries into Deft is simpler to prototype but
loads code with Deft's own privileges and complicates compatibility, provenance
and recovery. Android discourages this approach; F-Droid requires informed opt-in
for executable downloads that bypass its checks, not a blanket ban on all addons.
[Android guidance](https://developer.android.com/privacy-and-security/security-tips),
[F-Droid policy](https://f-droid.org/en/docs/Inclusion_Policy/).

My recommendation: extensible engine contracts now, bundled supported engines
for v1, optional installation later if measured size or community demand warrants
it. Separate “format engines” from “content sources”: one interprets local files,
the other retrieves content and has different permissions and failure modes.

## Online fiction without plugins

This is feasible and fits Deft's reading/tracking experience if downloaded content
stays readable offline. I would start with a small **Add from link** flow rather
than a browser or a full catalog: paste/share a story URL, show title/author/source,
choose available chapters, explicitly download, then read using Deft's existing
EPUB rendering. Provide an original-page link and a manual **Check for chapters**
action. Start with official downloads or a supported/permitted integration for
one source; add more only after that path is reliable.

The biggest change is identity. Today a changed file is a new Book. Rebuilding an
EPUB whenever a serial gains a chapter would lose continuity under that rule.
An online story needs a stable `(source, story ID)` identity, stable chapter IDs,
chapter revisions and reading positions anchored to chapter content. New chapters
must not erase sessions, ratings or notes. “Caught up” should be distinguishable
from a completed story; revised/deleted chapters need an explicit reconciliation
policy. Cached/generated content should be app-owned, never edits to users' SAF
book files.

For Royal Road specifically, I would first establish the supported access route
and permission rather than promise a scraper. Site changes, login-only chapters,
rate limits and anti-bot responses can make scraping a continuing maintenance
burden. The current terms page returned HTTP 403 during this review; I could not
verify an official public API or current permission for this integration. AO3 is
a better first candidate: its operator documents official EPUB downloads, which
can already be read by Deft once saved into a scanned folder. Its live FAQ was
unavailable to the research tool, so login/restricted-work and update behavior
still need a concrete check before automating integration.
[OTW download information](https://www.transformativeworks.org/ao3-celebrates-9-million-registered-users/).
This uncertainty is about implementation planning, not a claim that integration
is impossible.

My preference is a post-v1 optional online-source feature built into Deft initially,
with a reusable source interface. It would require an approved revision of the
current “network only in matching” rule and the local-file-only Book model. A
website shown in a WebView is a quicker experiment, but it gives weaker control
over typography, offline access, stable locations and accurate reading signals.
