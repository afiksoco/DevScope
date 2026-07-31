package com.devscope.core

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import java.lang.ref.WeakReference

/**
 * Shows the DevScope panel on top of whatever Activity is currently resumed.
 *
 * Deliberate design decision: the panel is attached to the Activity's decor
 * view instead of a WindowManager overlay window. A system overlay needs the
 * SYSTEM_ALERT_WINDOW permission, which the user can deny (our "no permission"
 * edge case) — attaching to the decor view needs no permission at all, so that
 * failure mode disappears by design.
 *
 * Tracks the current Activity with ActivityLifecycleCallbacks; on rotation the
 * view is detached with the dying Activity and, because the open/closed state
 * lives in [PanelState] (Application scope), reattached to the new one.
 */
internal class OverlayController(
    private val content: @Composable () -> Unit
) : Application.ActivityLifecycleCallbacks {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var current = WeakReference<Activity>(null)
    private var attachedView: ComposeView? = null

    fun install(app: Application) {
        app.registerActivityLifecycleCallbacks(this)
    }

    /** Safe to call from any thread (the shake sensor thread, for example). */
    fun show() = mainHandler.post { attach() }

    fun hide() = mainHandler.post { detach() }

    private fun attach() {
        val activity = current.get() ?: return
        if (attachedView != null) return
        // The Activity must own a lifecycle for ComposeView to work. Any
        // ComponentActivity (i.e. every Compose/AppCompat app) qualifies; if
        // not, we skip quietly instead of crashing the host.
        if (activity !is LifecycleOwner || activity !is SavedStateRegistryOwner) return

        val decor = activity.window?.decorView as? ViewGroup ?: return
        val view = ComposeView(activity).apply {
            setViewTreeLifecycleOwner(activity)
            setViewTreeSavedStateRegistryOwner(activity)
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setContent { content() }
        }
        decor.addView(
            view,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
        attachedView = view
    }

    private fun detach() {
        val view = attachedView ?: return
        (view.parent as? ViewGroup)?.removeView(view)
        attachedView = null
    }

    // -- ActivityLifecycleCallbacks --------------------------------------

    override fun onActivityResumed(activity: Activity) {
        current = WeakReference(activity)
        // Panel (or bubble) follows the user across screens and rotations.
        attach()
    }

    override fun onActivityPaused(activity: Activity) {
        if (current.get() === activity) detach()
    }

    override fun onActivityDestroyed(activity: Activity) {
        if (current.get() === activity) {
            detach()
            current = WeakReference(null)
        }
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    override fun onActivityStarted(activity: Activity) {}
    override fun onActivityStopped(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
}
