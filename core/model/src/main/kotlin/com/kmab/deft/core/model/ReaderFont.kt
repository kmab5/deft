package com.kmab.deft.core.model

data class ReaderFontFace(
    val assetPath: String? = null,
    val privateFilePath: String? = null,
    val italic: Boolean = false,
    val weight: Int = NORMAL_FONT_WEIGHT,
)

data class ReaderFont(
    val id: String,
    val name: String,
    val faces: List<ReaderFontFace>,
    val imported: Boolean = false,
)

private const val NORMAL_FONT_WEIGHT = 400
