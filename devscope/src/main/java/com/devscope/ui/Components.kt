package com.devscope.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Small pill used for filters ("D", "I", "W"...) and actions. */
@Composable
internal fun DsChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Text(
        text = label,
        fontSize = 12.sp,
        fontFamily = MonoFont,
        color = if (selected) DsColors.ink else DsColors.muted,
        modifier = Modifier
            .background(
                if (selected) DsColors.warn else DsColors.panel2,
                RoundedCornerShape(999.dp),
            )
            .border(1.dp, if (selected) DsColors.warn else DsColors.line, RoundedCornerShape(999.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

/** Message shown when a tab has nothing to display (or a module failed). */
@Composable
internal fun DsEmpty(message: String) {
    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(message, color = DsColors.faint, fontSize = 13.sp, fontFamily = MonoFont)
    }
}

/** Single-line mono text field on the panel's dark background. */
@Composable
internal fun DsTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = TextStyle(color = DsColors.text, fontSize = 13.sp, fontFamily = MonoFont),
        cursorBrush = SolidColor(DsColors.warn),
        modifier = modifier
            .background(DsColors.ink, RoundedCornerShape(8.dp))
            .border(1.dp, DsColors.line, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        decorationBox = { inner ->
            Box {
                if (value.isEmpty()) {
                    Text(placeholder, color = DsColors.faint, fontSize = 13.sp, fontFamily = MonoFont)
                }
                inner()
            }
        },
    )
}

/** Expandable list row scaffold shared by the Network and Crash tabs. */
@Composable
internal fun DsExpandableRow(
    expanded: Boolean,
    onToggle: () -> Unit,
    header: @Composable () -> Unit,
    details: @Composable () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        header()
        if (expanded) details()
    }
}
