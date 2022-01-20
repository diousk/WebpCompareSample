package com.example.webp

data class Gift(
    val type: Type,
    val url: String,
    val duration: Long = 0,
    val isSelf: Boolean = false,
)

sealed class Type {
    object Big : Type()
    data class Small(val position: Int) : Type()
}