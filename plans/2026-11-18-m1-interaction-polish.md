# M1 interaction polish — 0.1.1

**Goal:** Apply the second device feedback round to navigation, compact controls, transient feedback and motion.

**Approach:** Keep the established palette, typography and data model. Restore a flat Books toolbar: filter icon and current filter at the left, current sort and direction at the right, then the display icon. Remove separate sort-caption and persistent scan-result rows. Use compact checkbox/radio rows and adjacent layout/size controls in content-sized popups, with live application and explicit close controls.

User-started and automatic scan progress stays visible only while running. Newly completed scans produce an accessible bottom notice for roughly six seconds (extended by Android accessibility preferences), with a details action. Historic completion data must not reappear as a fresh notice on launch or navigation. Errors use animated bottom notices that can stack and dismiss; interaction pauses/restarts their expiry. The details action opens a dedicated popup with named added, skipped and failed files and reasons. Detailed diagnostics remain in bounded app-private storage, not oversized WorkManager output.

Motion covers popup entry/exit, notices and stack movement, selection, layout/filter/sort changes, folder/exclusion changes and scan transitions. Respect system animation scaling and preserve stable book keys. Avoid repeatedly animating an entire growing library during scan updates.

Settings continues directly to Library while that is the only implemented section. Restore the category chooser when a second real section is available. Patch handovers increment C: this one is version `0.1.1` (internal `0.1.1-1`, code `10101`), filename `deft-0.1.1.apk`; never roll a later merge back to `0.1.0`.

- [x] Restore compact toolbar and redesign all three control popups, with meaningful icons and accessible selected states.
- [x] Add detailed scan records and dedicated details popup without losing continuation information or exceeding WorkManager limits.
- [x] Add timed, stackable animated notices for scan outcomes and errors; suppress historical/repeated outcomes.
- [x] Apply restrained motion consistently and test reduced motion and font scaling.
- [x] Update versioning instructions/spec/visual reference and device checklist.
- [x] Run combined tests and one whole-change audit, fix findings, then `check assembleRelease` and signature verification.
- [x] Hand over `deft-0.1.1.apk`, signed with the existing release key and minified/shrunk. Push without waiting for CI; no merge or tag before device acceptance.

Verified source and release identity, test counts and device steps are recorded in [the 0.1.1 handover checklist](m1-0.1.1-test-checklist.md). Device acceptance remains pending.
