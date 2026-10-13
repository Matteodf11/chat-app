package com.matteo.chatapp.ui.screens

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import com.matteo.chatapp.repository.DraftStorage

internal data class DraftSubmission(val conversation: String, val text: String, val revision: Long)

/** Owns the actual editor states, so queued callbacks never capture a previous editor's setter. */
internal class TextDrafts(private val account: String,
    private val read: (String, String) -> String = DraftStorage::load,
    private val write: (String, String, String) -> Unit = DraftStorage::save) {
    private class Entry(val text: MutableState<String>, var revision: Long = 0)
    private val entries = mutableMapOf<String, Entry>()
    private fun entry(conversation: String) = entries.getOrPut(conversation) {
        Entry(mutableStateOf(read(account, conversation)))
    }
    fun state(conversation: String): MutableState<String> = entry(conversation).text
    fun update(conversation: String, text: String) {
        val draft = entry(conversation)
        write(account, conversation, text)
        draft.text.value = text
        draft.revision++
    }
    fun capture(conversation: String): DraftSubmission = entry(conversation).let {
        DraftSubmission(conversation, it.text.value, it.revision)
    }
    fun clearIfUnchanged(submission: DraftSubmission): Boolean {
        val draft = entry(submission.conversation)
        if (draft.revision != submission.revision || draft.text.value != submission.text) return false
        update(submission.conversation, "")
        return true
    }
}
