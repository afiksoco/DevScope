package com.devscope.nav

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import com.devscope.core.DevScopeModule
import com.devscope.core.ModuleRegistry
import com.devscope.core.RingBuffer
import com.devscope.ui.NavTab
import kotlinx.coroutines.flow.MutableStateFlow

/** One recorded destination change. */
data class NavEvent(
    val timeMs: Long,
    val route: String,
    val args: String?,
)

/**
 * Navigation history. Listens to destination changes on the host's
 * NavController and keeps the current route plus a timeline of recent moves.
 *
 * Navigation-Compose is compileOnly: an app without it never registers this
 * module (missing-dependency edge case).
 */
internal class NavigationModule(private val registry: ModuleRegistry) : DevScopeModule {

    private companion object {
        const val CAPACITY = 100
    }

    override val id = "nav"
    override val title = "Nav"

    val history = RingBuffer<NavEvent>(CAPACITY)
    val currentRoute = MutableStateFlow("(not attached)")

    fun attach(navController: NavController) {
        navController.addOnDestinationChangedListener { _, destination, arguments ->
            // Fail-safe edge case: a listener failure disables the tab, not the app.
            registry.guard(id) {
                val route = destination.route ?: destination.displayName
                currentRoute.value = route
                history.add(
                    NavEvent(
                        timeMs = System.currentTimeMillis(),
                        route = route,
                        args = arguments?.keySet()
                            ?.joinToString { key -> "$key=${arguments.get(key)}" }
                            ?.ifEmpty { null },
                    )
                )
            }
        }
    }

    override fun onClear() = history.clear()

    @Composable
    override fun Content() = NavTab(this)
}
