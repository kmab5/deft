package com.kmab.deft.format.readium

import org.readium.r2.shared.publication.Locator

data class EpubSearchResult(
    val locators: List<Locator>,
    val incomplete: Boolean,
)
