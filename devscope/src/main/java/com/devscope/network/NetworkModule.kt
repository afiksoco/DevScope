package com.devscope.network

import androidx.compose.runtime.Composable
import com.devscope.core.DevScopeModule
import com.devscope.core.ModuleRegistry
import com.devscope.core.RingBuffer
import com.devscope.ui.NetworkTab
import okhttp3.Interceptor
import okhttp3.Response

/**
 * HTTP inspector. The host app adds [interceptor] to its OkHttpClient; every
 * request/response is recorded into a ring buffer and shown in the panel.
 *
 * OkHttp is a compileOnly dependency of DevScope: an app that doesn't use
 * OkHttp simply never touches this class (missing-dependency edge case).
 */
internal class NetworkModule(private val registry: ModuleRegistry) : DevScopeModule {

    private companion object {
        const val CAPACITY = 500

        /** Memory edge case: response bodies are previewed up to this size. */
        const val MAX_BODY_BYTES = 250_000L

        val TEXTUAL_TYPES = listOf("json", "xml", "text", "html", "form-urlencoded")
    }

    override val id = "network"
    override val title = "Network"

    val entries = RingBuffer<NetworkEntry>(CAPACITY)

    val interceptor: Interceptor = Interceptor { chain ->
        val request = chain.request()
        val startedAt = System.currentTimeMillis()
        val response: Response
        try {
            response = chain.proceed(request)
        } catch (e: Exception) {
            // Failed call (no route, timeout...) — record it, then rethrow so
            // the app sees exactly the same behavior as without DevScope.
            record(
                NetworkEntry(
                    timeMs = startedAt,
                    method = request.method,
                    url = request.url.toString(),
                    code = null,
                    durationMs = System.currentTimeMillis() - startedAt,
                    responseBody = "",
                    error = "${e.javaClass.simpleName}: ${e.message}",
                )
            )
            throw e
        }
        record(
            NetworkEntry(
                timeMs = startedAt,
                method = request.method,
                url = request.url.toString(),
                code = response.code,
                durationMs = System.currentTimeMillis() - startedAt,
                responseBody = bodyPreview(response),
            )
        )
        response
    }

    override fun onClear() = entries.clear()

    @Composable
    override fun Content() = NetworkTab(entries)

    private fun record(entry: NetworkEntry) = registry.guard(id) { entries.add(entry) }

    /**
     * Reads a *copy* of the body via peekBody, so the app can still consume
     * the real one. Truncated at [MAX_BODY_BYTES]; binary bodies are shown as
     * their size only (huge/binary body edge case).
     */
    private fun bodyPreview(response: Response): String = try {
        val contentType = response.header("Content-Type").orEmpty().lowercase()
        val length = response.header("Content-Length")?.toLongOrNull() ?: -1L
        val isTextual = TEXTUAL_TYPES.any { contentType.contains(it) }
        when {
            !isTextual && contentType.isNotEmpty() -> "(binary $contentType, $length bytes)"
            else -> {
                val preview = response.peekBody(MAX_BODY_BYTES).string()
                if (length > MAX_BODY_BYTES) "$preview\n… (truncated, $length bytes total)" else preview
            }
        }
    } catch (e: Exception) {
        "(body unavailable: ${e.message})"
    }
}
