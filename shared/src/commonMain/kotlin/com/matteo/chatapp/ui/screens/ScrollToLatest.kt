package com.matteo.chatapp.ui.screens

import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.withFrameNanos

/** A tall final photo must end at its bottom, rather than aligning its top with the viewport. */
internal suspend fun LazyListState.scrollToLatest(index: Int, animated: Boolean = true) {
    // If the last row is already visible, adjust its bottom directly. Aligning its
    // top first would briefly jump when a photo grows after decoding.
    if (layoutInfo.visibleItemsInfo.none { it.index == index }) {
        if (animated) animateScrollToItem(index) else scrollToItem(index)
        withFrameNanos { }
    }
    val layout = layoutInfo
    val last = layout.visibleItemsInfo.lastOrNull()?.takeIf { it.index == index } ?: return
    val remaining = (last.offset + last.size + layout.afterContentPadding - layout.viewportEndOffset).coerceAtLeast(0)
    if (remaining > 0) {
        if (animated) animateScrollBy(remaining.toFloat(), tween(120)) else scrollBy(remaining.toFloat())
    }
}
