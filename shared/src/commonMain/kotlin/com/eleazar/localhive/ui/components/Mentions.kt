package com.eleazar.localhive.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eleazar.localhive.domain.model.User

/** Long enough for a first and last name, short enough that an old `@` stops counting. */
private const val MAX_QUERY_LENGTH = 30
private const val MAX_SUGGESTIONS = 5

/** The `@…` being typed at the cursor: the `@` sits at [start], [query] is what follows it. */
data class MentionQuery(val start: Int, val query: String)

/** The name a member is tagged by. */
val User.mentionName: String get() = (displayName ?: username).orEmpty().trim()

/**
 * The mention being typed at the cursor, if any: an `@` at the start of the text or after a
 * space, followed by up to [MAX_QUERY_LENGTH] characters on the same line.
 */
fun activeMentionQuery(value: TextFieldValue): MentionQuery? {
    if (!value.selection.collapsed) return null
    val cursor = value.selection.start
    val text = value.text
    var i = cursor - 1
    while (i >= 0 && cursor - i <= MAX_QUERY_LENGTH + 1) {
        val c = text[i]
        if (c == '\n' || (c == '@' && i > 0 && !text[i - 1].isWhitespace())) return null
        if (c == '@') {
            val query = text.substring(i + 1, cursor)
            // Two spaces in a row means they've moved on from the name.
            return if ("  " in query || query.startsWith(" ")) null else MentionQuery(i, query)
        }
        i--
    }
    return null
}

/** Members whose name has a word starting with [query], best matches first. */
fun mentionSuggestions(members: List<User>, query: String): List<User> {
    val q = query.trim()
    return members
        .filter { it.mentionName.isNotEmpty() }
        .filter { member ->
            // The whole name for "Ada Ob", any one word for "Ob".
            q.isEmpty() || member.mentionName.startsWith(q, ignoreCase = true) ||
                member.mentionName.split(' ').any { it.startsWith(q, ignoreCase = true) }
        }
        .sortedWith(compareBy({ !it.mentionName.startsWith(q, ignoreCase = true) }, { it.mentionName.lowercase() }))
        .take(MAX_SUGGESTIONS)
}

/** Replaces the `@query` being typed with `@Name `, leaving the cursor after it. */
fun insertMention(value: TextFieldValue, query: MentionQuery, member: User): TextFieldValue {
    val tag = "@${member.mentionName} "
    val end = query.start + 1 + query.query.length
    val text = value.text.replaceRange(query.start, end, tag)
    return TextFieldValue(text, TextRange(query.start + tag.length))
}

/** Of the members picked while writing, those whose `@Name` is still in the final [text]. */
fun mentionsIn(text: String, picked: Map<String, String>): Map<String, String> =
    picked.filterValues { name -> "@$name" in text }

/** The members matching what's being typed after an `@`; tap one to tag them. */
@Composable
fun MentionSuggestionList(suggestions: List<User>, onPick: (User) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        suggestions.forEachIndexed { index, member ->
            if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onPick(member) }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Avatar(imageUrl = member.profileImageUrl, name = member.mentionName, size = 32.dp)
                Spacer(Modifier.width(12.dp))
                Text(
                    member.mentionName,
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
