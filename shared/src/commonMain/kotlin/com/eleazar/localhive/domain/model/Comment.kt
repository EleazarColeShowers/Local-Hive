package com.eleazar.localhive.domain.model

data class Comment(
    val id: String = "",
    val postId: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val content: String = "",
    val createdAt: Long = 0L,
    /** Members tagged in [content], by user id, with the name shown after the `@`. */
    val mentions: Map<String, String> = emptyMap(),
    /**
     * The top-level comment this one replies to, or null for a comment on the post itself.
     * Replies are one level deep: answering a reply joins the same thread.
     */
    val parentId: String? = null
)
