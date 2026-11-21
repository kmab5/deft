package com.kmab.deft.feature.book

data class BookOrganizationLinks(
    val openAuthor: (Long) -> Unit,
    val openCategory: (Long) -> Unit,
    val openBook: (Long) -> Unit,
)
