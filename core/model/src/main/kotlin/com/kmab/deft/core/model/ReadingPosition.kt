package com.kmab.deft.core.model

data class ReadingPosition(
    val locatorJson: String,
    val totalProgression: Double,
    val position: Int? = null,
)
