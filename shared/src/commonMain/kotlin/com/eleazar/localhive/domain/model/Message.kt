package com.eleazar.localhive.domain.model

data class Message(
    val id: String = "",
    val chatId: String = "",
    val senderId: String = "",
    val content: String = "",
    val timestamp: Long = 0L
)
