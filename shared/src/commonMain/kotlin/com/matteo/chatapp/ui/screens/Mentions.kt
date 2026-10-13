package com.matteo.chatapp.ui.screens

internal data class MentionQuery(val start: Int, val end: Int, val query: String)

internal fun mentionQuery(text: String, cursor: Int): MentionQuery? {
    val end = cursor.coerceIn(0, text.length)
    val start = text.lastIndexOf('@', end - 1)
    if (start < 0 || (start > 0 && (text[start - 1].isLetterOrDigit() || text[start - 1] in "_@"))) return null
    val query = text.substring(start + 1, end)
    if (query.length > 80 || query.any { it == '\n' || it == '\r' || it == '@' }) return null
    return MentionQuery(start, end, query)
}

internal fun insertMention(text: String, query: MentionQuery, username: String): Pair<String, Int> {
    val replacement = "@$username "
    return text.replaceRange(query.start, query.end, replacement) to query.start + replacement.length
}
