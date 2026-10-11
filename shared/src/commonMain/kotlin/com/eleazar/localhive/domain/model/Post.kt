package com.eleazar.localhive.domain.model

data class Post(
    val id: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val authorImageUrl: String? = null,
    val estateId: String = "",
    val content: String = "",
    val imageUrl: String? = null,
    val createdAt: Long = 0L,
    val likeCount: Int = 0,
    val commentCount: Int = 0,
    val likedBy: List<String> = emptyList()
)
