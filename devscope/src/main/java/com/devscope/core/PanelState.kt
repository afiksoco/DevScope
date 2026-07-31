package com.devscope.core

import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Panel state lives here, at Application scope, and not inside any Activity —
 * this is what makes the panel survive screen rotation (lifecycle edge case):
 * the Activity and its ComposeView die, but the open/selected-tab state and all
 * module buffers don't, so the panel reattaches to the next Activity as it was.
 */
internal object PanelState {
    val isOpen = MutableStateFlow(false)
    val selectedTab = MutableStateFlow(0)
}
