package com.eleazar.localhive.data.repository

import com.eleazar.localhive.domain.model.Comment
import com.eleazar.localhive.domain.model.Message
import com.eleazar.localhive.domain.model.Post
import com.eleazar.localhive.domain.model.User
import dev.gitlive.firebase.firestore.DocumentSnapshot
import dev.gitlive.firebase.firestore.FirebaseFirestore

/*
 * Typed reads of one field, null when it's missing or holds something else. Documents are mapped
 * field by field rather than decoded whole, so one odd field (say, an old document from before a
 * field changed type) costs that field rather than the whole document.
 */

internal fun DocumentSnapshot.string(field: String): String? = read(field) { get<String?>(it) }

internal fun DocumentSnapshot.long(field: String): Long? = read(field) { get<Long?>(it) }

internal fun DocumentSnapshot.boolean(field: String): Boolean? = read(field) { get<Boolean?>(it) }

internal fun DocumentSnapshot.stringList(field: String): List<String>? = read(field) { get<List<String>?>(it) }

internal fun DocumentSnapshot.stringMap(field: String): Map<String, String>? = read(field) { get<Map<String, String>?>(it) }

internal fun DocumentSnapshot.intMap(field: String): Map<String, Int>? = read(field) { get<Map<String, Int>?>(it) }

private inline fun <T> DocumentSnapshot.read(field: String, reader: DocumentSnapshot.(String) -> T?): T? =
    if (!contains(field)) null else runCatching { reader(field) }.getOrNull()

internal fun FirebaseFirestore.users() = collection("users")

internal fun DocumentSnapshot.toUser(): User = User(
    id = id,
    email = string("email"),
    displayName = string("displayName"),
    username = string("username"),
    profileImageUrl = string("profileImageUrl"),
    bio = string("bio"),
    estateId = string("estateId"),
    address = string("address"),
    occupation = string("occupation"),
    phone = string("phone"),
    fcmToken = string("fcmToken"),
    lightAlerts = boolean("lightAlerts") ?: false
)

/** Only the fields that are set, for merge writes that mustn't blank anything out. */
internal fun User.toNonNullMap(): Map<String, Any> = buildMap {
    email?.let { put("email", it) }
    displayName?.let { put("displayName", it) }
    username?.let { put("username", it) }
    profileImageUrl?.let { put("profileImageUrl", it) }
    bio?.let { put("bio", it) }
    estateId?.let { put("estateId", it) }
    address?.let { put("address", it) }
    occupation?.let { put("occupation", it) }
    phone?.let { put("phone", it) }
}

internal fun DocumentSnapshot.toPost(): Post = Post(
    id = id,
    authorId = string("authorId").orEmpty(),
    authorName = string("authorName").orEmpty(),
    authorImageUrl = string("authorImageUrl"),
    estateId = string("estateId").orEmpty(),
    content = string("content").orEmpty(),
    imageUrl = string("imageUrl"),
    videoUrl = string("videoUrl"),
    videoThumbnailUrl = string("videoThumbnailUrl"),
    createdAt = long("createdAt") ?: 0L,
    likeCount = long("likeCount")?.toInt() ?: 0,
    commentCount = long("commentCount")?.toInt() ?: 0,
    likedBy = stringList("likedBy").orEmpty(),
    category = string("category").orEmpty(),
    mentions = stringMap("mentions").orEmpty(),
    location = string("location").orEmpty(),
    emergencyType = string("emergencyType").orEmpty(),
    incidentStatus = string("incidentStatus").orEmpty(),
    statusUpdatedByName = string("statusUpdatedByName").orEmpty(),
    statusUpdatedAt = long("statusUpdatedAt") ?: 0L,
    editedAt = long("editedAt") ?: 0L,
    responders = stringMap("responders").orEmpty()
)

internal fun DocumentSnapshot.toComment(): Comment = Comment(
    id = id,
    postId = string("postId").orEmpty(),
    authorId = string("authorId").orEmpty(),
    authorName = string("authorName").orEmpty(),
    content = string("content").orEmpty(),
    createdAt = long("createdAt") ?: 0L,
    mentions = stringMap("mentions").orEmpty()
)

internal fun DocumentSnapshot.toMessage(): Message = Message(
    id = id,
    chatId = string("chatId").orEmpty(),
    senderId = string("senderId").orEmpty(),
    content = string("content").orEmpty(),
    timestamp = long("timestamp") ?: 0L
)
