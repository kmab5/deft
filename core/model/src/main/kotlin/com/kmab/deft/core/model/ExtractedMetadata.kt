package com.kmab.deft.core.model

data class ExtractedMetadata(
    val title: String?,
    val authors: List<String>,
    val seriesName: String?,
    val seriesIndex: Double?,
    val language: String?,
    val publisher: String?,
    val publishedDate: String?,
    val description: String?,
    val isbn10: String?,
    val isbn13: String?,
    val pageCount: Int?,
    val coverImage: ByteArray?,
    val drmProtected: Boolean,
) {
    companion object {
        val EMPTY =
            ExtractedMetadata(
                title = null,
                authors = emptyList(),
                seriesName = null,
                seriesIndex = null,
                language = null,
                publisher = null,
                publishedDate = null,
                description = null,
                isbn10 = null,
                isbn13 = null,
                pageCount = null,
                coverImage = null,
                drmProtected = false,
            )
    }
}
