package com.devscope.network

/** One captured HTTP call. */
data class NetworkEntry(
    val timeMs: Long,
    val method: String,
    val url: String,
    val code: Int?,          // null when the call failed before a response
    val durationMs: Long,
    val responseBody: String,
    val error: String? = null,
) {
    val isError: Boolean get() = error != null || (code ?: 0) >= 400
}
