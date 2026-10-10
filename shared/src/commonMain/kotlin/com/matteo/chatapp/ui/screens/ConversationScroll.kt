package com.matteo.chatapp.ui.screens

// Item counts cannot measure proximity: a single photo may fill the entire viewport.
internal fun isNearConversationEnd(lastVisibleIndex: Int?, totalItems: Int,
    lastVisibleBottom: Int, viewportBottom: Int, tolerancePx: Float): Boolean =
    totalItems == 0 || (lastVisibleIndex == totalItems - 1 &&
        lastVisibleBottom <= viewportBottom + tolerancePx)
