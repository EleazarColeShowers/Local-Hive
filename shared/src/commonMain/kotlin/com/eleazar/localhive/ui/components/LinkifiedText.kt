package com.eleazar.localhive.ui.components

import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.TextUnit

/** `https://…`, `http://…` or a bare `www.…` address, up to the next space. */
private val UrlPattern = Regex("""(?i)\b(?:https?://|www\.)[^\s<>"]+""")

/** Punctuation that usually ends the sentence around a link rather than the link itself. */
private const val TrailingPunctuation = ".,!?;:'\""

/**
 * [Text] whose web addresses open in the browser when tapped. Links are underlined, and drawn in
 * [linkColor] when one is given, so they still read as links on a coloured chat bubble.
 *
 * Each `@Name` in [mentions] (user id to name) is drawn bold in [linkColor] and, given
 * [onMentionClick], opens that member.
 *
 * [selectable] text can be long-pressed to select and copy; leave it off where a tap on the text
 * should reach a clickable parent, such as a post card in the feed.
 */
@Composable
fun LinkifiedText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    linkColor: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    lineHeight: TextUnit = TextUnit.Unspecified,
    fontWeight: FontWeight? = null,
    mentions: Map<String, String> = emptyMap(),
    onMentionClick: ((userId: String) -> Unit)? = null,
    selectable: Boolean = false
) {
    val resolvedLink = linkColor.takeOrElse { color.takeOrElse { LocalContentColor.current } }
    val currentOnMention by rememberUpdatedState(onMentionClick)
    val annotated = remember(text, resolvedLink, mentions, onMentionClick != null) {
        linkify(text, resolvedLink, mentions) { userId -> currentOnMention?.invoke(userId) }
    }
    if (selectable) {
        SelectionContainer(modifier) {
            Text(text = annotated, color = color, fontSize = fontSize, lineHeight = lineHeight, fontWeight = fontWeight)
        }
    } else {
        Text(
            text = annotated,
            modifier = modifier,
            color = color,
            fontSize = fontSize,
            lineHeight = lineHeight,
            fontWeight = fontWeight
        )
    }
}

private inline fun Color.takeOrElse(fallback: () -> Color): Color = if (this != Color.Unspecified) this else fallback()

/** A stretch of the text that becomes a link: a web address, or a tagged member. */
private class Span(val range: IntRange, val annotation: LinkAnnotation)

internal fun linkify(
    text: String,
    linkColor: Color,
    mentions: Map<String, String> = emptyMap(),
    onMentionClick: ((userId: String) -> Unit)? = null
): AnnotatedString {
    val spans = mutableListOf<Span>()
    fun free(range: IntRange) = spans.none { it.range.first <= range.last && range.first <= it.range.last }

    // Longest names first, so "@Ada Obi" isn't claimed by a member called "Ada".
    val mentionStyle = TextLinkStyles(SpanStyle(color = linkColor, fontWeight = FontWeight.SemiBold))
    for ((userId, name) in mentions.entries.sortedByDescending { it.value.length }) {
        if (name.isBlank()) continue
        val tag = "@$name"
        var from = 0
        while (true) {
            val at = text.indexOf(tag, from)
            if (at < 0) break
            val range = at until at + tag.length
            if (free(range)) {
                val annotation = LinkAnnotation.Clickable(userId, mentionStyle) { onMentionClick?.invoke(userId) }
                spans += Span(range, annotation)
            }
            from = at + tag.length
        }
    }

    val urlStyle = TextLinkStyles(SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline))
    for (match in UrlPattern.findAll(text)) {
        val raw = trimLink(match.value)
        val range = match.range.first until match.range.first + raw.length
        val url = if (raw.startsWith("www.", ignoreCase = true)) "https://$raw" else raw
        if (free(range)) spans += Span(range, LinkAnnotation.Url(url, urlStyle))
    }

    return buildAnnotatedString {
        var cursor = 0
        for (span in spans.sortedBy { it.range.first }) {
            append(text.substring(cursor, span.range.first))
            pushLink(span.annotation)
            append(text.substring(span.range))
            pop()
            cursor = span.range.last + 1
        }
        append(text.substring(cursor))
    }
}

/** Drops sentence punctuation, and a closing bracket the link never opened, from the end. */
internal fun trimLink(link: String): String {
    var end = link.length
    while (end > 0) {
        val last = link[end - 1]
        val unbalanced = (last == ')' && link.count { it == '(' } < link.substring(0, end).count { it == ')' }) ||
            (last == ']' && '[' !in link)
        if (last in TrailingPunctuation || unbalanced) end-- else break
    }
    return link.substring(0, end)
}
