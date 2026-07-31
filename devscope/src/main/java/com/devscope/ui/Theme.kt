package com.devscope.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * DevScope draws with its own fixed dark palette, independent of the host
 * app's theme — the panel must look the same (and stay readable) no matter
 * what app it is dropped into.
 */
internal object DsColors {
    val ink = Color(0xFF0E1116)
    val panel = Color(0xFF171B22)
    val panel2 = Color(0xFF1F242E)
    val line = Color(0xFF2A313C)
    val text = Color(0xFFE7EAF0)
    val muted = Color(0xFF8B93A7)
    val faint = Color(0xFF5A6273)
    val info = Color(0xFF58B3F0)
    val warn = Color(0xFFF4B740)
    val error = Color(0xFFFF6B6B)
    val live = Color(0xFF43D9A3)

    /** Log level -> color, same coding as Logcat. */
    fun forLevel(level: Char): Color = when (level) {
        'E', 'A' -> error
        'W' -> warn
        'I' -> info
        else -> muted
    }

    /** HTTP status -> color. */
    fun forStatus(code: Int?): Color = when {
        code == null -> error
        code >= 500 -> error
        code >= 400 -> warn
        else -> live
    }
}

internal val MonoFont = FontFamily.Monospace

private val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)

internal fun formatTime(timeMs: Long): String = timeFormat.format(Date(timeMs))
