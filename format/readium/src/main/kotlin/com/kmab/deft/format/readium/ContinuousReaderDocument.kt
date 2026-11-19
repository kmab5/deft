package com.kmab.deft.format.readium

/** Trusted container; each publication resource keeps its own URL, styles and sandbox. */
object ContinuousReaderDocument {
    fun html(chapters: List<Pair<String, String>>): String {
        val manifest =
            chapters.joinToString(",", prefix = "[", postfix = "]") { (href, title) ->
                "{\"href\":${jsonString(href)},\"title\":${jsonString(title)}}"
            }
        return """
            <!doctype html>
            <html><head><meta charset="utf-8">
            <meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1, user-scalable=no">
            <meta http-equiv="Content-Security-Policy" content="default-src 'none'; script-src https://*/deft-reader/; style-src 'unsafe-inline'; frame-src 'self'; base-uri 'none'; form-action 'none'">
            <style>
            html,body{margin:0;padding:0;overflow-anchor:none;overscroll-behavior-x:none;background:transparent}
            body{overflow-x:hidden}main,section{display:block;position:relative;width:100%;margin:0;padding:0}
            section+section::before{content:"";position:absolute;z-index:1;top:0;left:24px;right:24px;
            height:1px;background:currentColor;opacity:.2;pointer-events:none}
            iframe{display:block;width:100%;border:0;margin:0;padding:0;overflow:hidden}
            </style>
            <script type="application/json" id="deft-chapters">$manifest</script>
            <script defer src="https://readium_assets/deft-reader/continuous.js"></script>
            </head><body><main id="deft-content"></main></body></html>
            """.trimIndent()
    }

    private fun jsonString(value: String): String =
        buildString {
            append('"')
            value.forEach { character ->
                when (character) {
                    '"' -> append("\\\"")
                    '\\' -> append("\\\\")
                    '<' -> append("\\u003c")
                    '>' -> append("\\u003e")
                    '&' -> append("\\u0026")
                    else ->
                        if (character < ' ' || character == '\u2028' || character == '\u2029') {
                            append("\\u")
                            append(character.code.toString(HEX_RADIX).padStart(UNICODE_DIGITS, '0'))
                        } else {
                            append(character)
                        }
                }
            }
            append('"')
        }
}

private const val HEX_RADIX = 16
private const val UNICODE_DIGITS = 4
