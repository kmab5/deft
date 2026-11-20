# Reader fonts

The bundled assets contain eleven families with regular, italic and bold faces.
The reader uses WOFF2; Android font-name previews use separate, tiny SFNT subsets
containing only the family name. Both sets count against the 3,000,000-byte budget.
The checked-in result is under 1.8 MB. No face was dropped.

`tools/build_fonts.py` recreates the assets with fontTools 4.68.0 and Brotli. Run it
from the repository root. It downloads only font sources into this module's ignored
build directory. Google Fonts and the official OpenDyslexic repository are pinned
to commits; `font-sources.json` records source URLs and SHA-256 hashes. Each family's
SIL Open Font License is packaged under `src/main/assets/licenses`.

Subsets cover Latin (including extended), Greek and Cyrillic with common punctuation
and currencies. Other scripts fall back to the platform's appropriate font. Variable
fonts are instantiated at their default optical size and 400/700 weight. Hinting is
removed. Modified internal family names use `Deft Reader 01` through `Deft Reader 11`; UI names identify the
original family. This respects the licenses' reserved-name requirements.

User imports accept bounded, structurally validated TTF/OTF files and validate them
with Android's font parser before publishing a private UUID-named copy. The original
document is never modified. Imports are limited to 20 MiB each and 32 families.
Deleting the active imported font selects Literata before deleting its private copy.
Missing or corrupt stored imports are excluded on reopen and the selection falls
back to Literata. These user-owned assets are excluded from the bundled font budget.
