package com.devscope

import android.app.Application
import android.content.pm.ApplicationInfo
import android.util.Log
import androidx.navigation.NavController
import androidx.room.RoomDatabase
import com.devscope.core.ModuleRegistry
import com.devscope.core.OverlayController
import com.devscope.core.PanelState
import com.devscope.core.ShakeDetector
import com.devscope.crash.CrashModule
import com.devscope.crash.CrashSink
import com.devscope.db.DatabaseModule
import com.devscope.log.LogsModule
import com.devscope.nav.NavigationModule
import com.devscope.network.NetworkModule
import com.devscope.ui.DevScopeRoot
import okhttp3.Interceptor

/** How the panel is opened. */
enum class Trigger {
    /** Shake the device. Falls back to [BUBBLE] when there is no accelerometer. */
    SHAKE,

    /** A small floating bubble that is always on screen. */
    BUBBLE,

    /** Only via [DevScope.open] / [DevScope.toggle]. */
    MANUAL,
}

/**
 * DevScope — a drop-in debug overlay for Android.
 *
 * ```
 * // Application.onCreate()
 * DevScope.install(this)
 *     .trackDatabase(appDatabase)
 *     .openOn(Trigger.SHAKE)
 * ```
 *
 * Release edge case: [install] is a no-op when the app is not debuggable, so
 * shipping the call in production code costs nothing and exposes nothing.
 */
object DevScope {

    private const val TAG = "DevScope"

    private val registry = ModuleRegistry()
    private var overlay: OverlayController? = null
    private var crashModule: CrashModule? = null
    private var networkModule: NetworkModule? = null
    private var navModule: NavigationModule? = null

    /** True once [install] ran in a debuggable build. */
    var isInstalled: Boolean = false
        private set

    private var bubbleFallback = false

    /**
     * Installs DevScope. Call once from [Application.onCreate].
     * Calling again is a no-op (double-install edge case); calling in a
     * non-debuggable (release) build does nothing at all.
     */
    fun install(app: Application): Installer {
        val debuggable = app.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
        if (!debuggable) {
            Log.i(TAG, "Non-debuggable build — DevScope disabled")
            return Installer(app, enabled = false)
        }
        if (isInstalled) return Installer(app, enabled = true)
        isInstalled = true

        LogsModule(registry).also {
            registry.register(it)
            it.install()
        }
        CrashModule(app).also {
            crashModule = it
            registry.register(it)
            it.install()
        }
        overlay = OverlayController {
            DevScopeRoot(registry, showBubble = bubbleFallback)
        }.also { it.install(app) }

        return Installer(app, enabled = true)
    }

    /**
     * OkHttp interceptor that records every call into the Network tab.
     * Add it to your client: `OkHttpClient.Builder().addInterceptor(DevScope.networkInterceptor)`.
     * In release builds it's a pass-through that records nothing.
     */
    val networkInterceptor: Interceptor
        get() {
            if (!isInstalled) return Interceptor { chain -> chain.proceed(chain.request()) }
            val module = networkModule ?: NetworkModule(registry).also {
                networkModule = it
                registry.register(it)
            }
            return module.interceptor
        }

    /** Records destination changes of [navController] into the Nav tab. */
    fun trackNavigation(navController: NavController) {
        if (!isInstalled) return
        val module = navModule ?: NavigationModule(registry).also {
            navModule = it
            registry.register(it)
        }
        module.attach(navController)
    }

    /** Opens the panel (main-thread safe from anywhere). */
    fun open() {
        if (isInstalled) PanelState.isOpen.value = true
    }

    fun close() {
        PanelState.isOpen.value = false
    }

    fun toggle() {
        if (isInstalled) PanelState.isOpen.value = !PanelState.isOpen.value
    }

    /** Fluent configuration returned by [install]. */
    class Installer internal constructor(
        private val app: Application,
        private val enabled: Boolean,
    ) {

        /** Adds the DB tab for [db]. Apps without Room simply never call this. */
        fun trackDatabase(db: RoomDatabase, name: String = "room"): Installer {
            if (enabled) registry.register(DatabaseModule(db, name))
            return this
        }

        /**
         * Sends crash reports to [sink] on the next launch — the demo app backs
         * this with Firebase Firestore. DevScope itself stays cloud-agnostic.
         */
        fun uploadCrashesTo(sink: CrashSink): Installer {
            if (enabled) crashModule?.attachSink(sink)
            return this
        }

        /** Chooses how the panel opens; default is [Trigger.SHAKE]. */
        fun openOn(trigger: Trigger): Installer {
            if (!enabled) return this
            when (trigger) {
                Trigger.SHAKE -> {
                    val started = ShakeDetector { toggle() }.start(app)
                    // No accelerometer (emulator edge case) -> bubble fallback.
                    if (!started) bubbleFallback = true
                }
                Trigger.BUBBLE -> bubbleFallback = true
                Trigger.MANUAL -> Unit
            }
            return this
        }
    }
}
