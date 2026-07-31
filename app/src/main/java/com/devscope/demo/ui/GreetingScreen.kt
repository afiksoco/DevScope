package com.devscope.demo.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Exists to demo navigation arguments in DevScope's Nav tab. */
@Composable
fun GreetingScreen(name: String) {
    Column(
        Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Hello, $name!", style = MaterialTheme.typography.headlineLarge)
        Text(
            "Open DevScope → Nav to see this destination and its argument.",
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
