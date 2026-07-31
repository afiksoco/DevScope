package com.devscope.core

import androidx.compose.runtime.Composable

/**
 * A single tab in the DevScope panel.
 *
 * Every capability (logs, network, database...) is its own class implementing
 * this interface, so modules stay isolated: a failure in one never touches the
 * others, and adding a new capability means adding one class and registering it.
 */
interface DevScopeModule {

    /** Stable identifier, used to mark a module as failed. */
    val id: String

    /** Short label shown on the tab. */
    val title: String

    /** The tab's UI. */
    @Composable
    fun Content()

    /** Called when the user taps "clear" while this tab is selected. */
    fun onClear() {}
}
