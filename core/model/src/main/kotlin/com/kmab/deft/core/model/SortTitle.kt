package com.kmab.deft.core.model

import java.util.Locale

private val leadingArticle = Regex("^(the|an|a) ")

fun sortTitle(title: String): String = title.lowercase(Locale.ROOT).replaceFirst(leadingArticle, "")
