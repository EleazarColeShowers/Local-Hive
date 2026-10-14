package com.eleazar.localhive.domain.model

data class Comment(
    val id: String = "",
    val postId: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val content: String = "",
    val createdAt: Long = 0L,
    /** Members tagged in [content], by user id, with the name shown after the `@`. */
    val mentions: Map<String, String> = emptyMap()
)
