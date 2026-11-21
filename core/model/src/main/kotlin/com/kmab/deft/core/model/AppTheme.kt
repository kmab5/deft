package com.kmab.deft.core.model

enum class AppTheme {
    System,
    Light,
    Dark,
    ;

    fun isDark(systemDark: Boolean): Boolean =
        when (this) {
            System -> systemDark
            Light -> false
            Dark -> true
        }
}
