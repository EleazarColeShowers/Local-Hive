package com.eleazar.localhive.ui.feed

import com.eleazar.localhive.domain.model.Post

/**
 * The feed as shown: the live first page, then older pages fetched once, newest first. A post in
 * both keeps its live copy, and posts by anyone in [blockedIds] are left out.
 */
internal fun mergeFeed(live: List<Post>, older: List<Post>, blockedIds: Set<String>): List<Post> {
    val merged = LinkedHashMap<String, Post>()
    live.forEach { merged[it.id] = it }
    older.forEach { merged.getOrPut(it.id) { it } }
    return merged.values.filter { it.authorId !in blockedIds }.sortedByDescending { it.createdAt }
}
