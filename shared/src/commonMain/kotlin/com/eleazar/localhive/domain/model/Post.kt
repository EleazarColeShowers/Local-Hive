package com.eleazar.localhive.domain.model

data class Post(
    val id: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val authorImageUrl: String? = null,
    val estateId: String = "",
    val content: String = "",
    val imageUrl: String? = null,
    /** A post carries a photo or a video, never both; the text is the video's caption. */
    val videoUrl: String? = null,
    /** Still frame shown in the feed until the video is opened. */
    val videoThumbnailUrl: String? = null,
    val createdAt: Long = 0L,
    val likeCount: Int = 0,
    val commentCount: Int = 0,
    val likedBy: List<String> = emptyList(),
    /** A [PostCategory] key, or blank for an untagged post. */
    val category: String = "",
    /** Members tagged in [content], by user id, with the name shown after the `@`. */
    val mentions: Map<String, String> = emptyMap(),
    /** Where in the estate a security report or emergency is, e.g. "Block C gate". */
    val location: String = "",
    /** An [EmergencyType] key, for an emergency. */
    val emergencyType: String = "",
    /** An [IncidentStatus] key, for a security report or emergency; blank for any other post. */
    val incidentStatus: String = "",
    /** Who last changed [incidentStatus], and when. */
    val statusUpdatedByName: String = "",
    val statusUpdatedAt: Long = 0L,
    /** When the author last changed the text, or 0 if they never have. */
    val editedAt: Long = 0L,
    /** Neighbours on their way to help with an emergency: user id to name. */
    val responders: Map<String, String> = emptyMap()
)
