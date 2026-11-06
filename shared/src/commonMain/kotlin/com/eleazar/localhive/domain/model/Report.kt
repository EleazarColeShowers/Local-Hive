package com.eleazar.localhive.domain.model

/** What a report is about. The keys are stored, and checked in firestore.rules. */
enum class ReportSubject(val key: String) {
    POST("post"),
    COMMENT("comment"),
    CHAT("chat"),
    USER("user"),
    /** Feedback about the app itself, from Settings. */
    APP("app")
}

enum class ReportReason(val key: String, val label: String) {
    CHILD_SAFETY("child_safety", "Child sexual abuse or exploitation"),
    HARASSMENT("harassment", "Harassment or bullying"),
    THREATS("threats", "Threats or violence"),
    SEXUAL("sexual", "Nudity or sexual content"),
    HATE("hate", "Hate speech"),
    SCAM("scam", "Spam, scam or fraud"),
    FALSE_INFO("false_info", "False information"),
    IMPERSONATION("impersonation", "Pretending to be someone else"),
    OTHER("other", "Something else"),

    // Feedback from Settings.
    PROBLEM("problem", "Something isn't working"),
    IDEA("idea", "An idea or suggestion"),
    SAFETY("safety", "A safety concern");

    companion object {
        /** Offered when reporting a post, comment, chat or person. */
        val forContent = listOf(CHILD_SAFETY, HARASSMENT, THREATS, SEXUAL, HATE, SCAM, FALSE_INFO, IMPERSONATION, OTHER)
        /** Offered by "Send feedback" in Settings. */
        val forFeedback = listOf(PROBLEM, IDEA, SAFETY, CHILD_SAFETY, OTHER)
    }
}

/**
 * A report of something in the app, or feedback about it, for the developer to review.
 *
 * @param targetId the post, comment, chat or user reported; blank for feedback
 * @param postId the post a reported comment is on
 * @param targetUserId whoever wrote or is behind what's reported
 * @param excerpt a copy of the reported text, kept in case it's edited or deleted
 */
data class Report(
    val subject: ReportSubject,
    val reason: ReportReason,
    val details: String = "",
    val targetId: String = "",
    val postId: String? = null,
    val targetUserId: String? = null,
    val excerpt: String? = null
) {
    companion object {
        fun of(post: Post, reason: ReportReason, details: String) = Report(
            subject = ReportSubject.POST, reason = reason, details = details,
            targetId = post.id, targetUserId = post.authorId,
            excerpt = listOfNotNull(post.content.takeIf { it.isNotBlank() }, post.imageUrl).joinToString("\n")
        )

        fun of(comment: Comment, postId: String, reason: ReportReason, details: String) = Report(
            subject = ReportSubject.COMMENT, reason = reason, details = details,
            targetId = comment.id, postId = postId, targetUserId = comment.authorId, excerpt = comment.content
        )
    }
}
