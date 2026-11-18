package com.kmab.deft.core.model

@JvmInline
value class ContentHash(
    val hex: String,
) {
    init {
        require(hex.length == SHA256_HEX_LENGTH && hex.all { it in '0'..'9' || it in 'a'..'f' }) {
            "Content hash must contain exactly 64 lowercase hexadecimal characters"
        }
    }

    private companion object {
        const val SHA256_HEX_LENGTH = 64
    }
}
