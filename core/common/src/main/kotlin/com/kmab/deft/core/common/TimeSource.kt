package com.kmab.deft.core.common

import java.time.Instant

fun interface TimeSource {
    fun now(): Instant
}

object SystemTimeSource : TimeSource {
    override fun now(): Instant = Instant.now()
}
