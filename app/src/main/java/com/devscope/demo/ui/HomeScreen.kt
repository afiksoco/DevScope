package com.devscope.demo.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.devscope.DevScope
import com.devscope.demo.DemoApplication
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Request
import okhttp3.Response
import timber.log.Timber
import java.io.IOException

/**
 * Everything on this screen exists to feed the DevScope tabs:
 * log buttons -> Logs, HTTP buttons -> Network, crash button -> Crash.
 */
@Composable
fun HomeScreen(navController: NavController) {
    val app = LocalContext.current.applicationContext as DemoApplication

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("DevScope Demo", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Shake the device (or use the buttons) and open the DevScope panel to watch everything below being captured.",
            style = MaterialTheme.typography.bodyMedium,
        )

        Text("Logs", style = MaterialTheme.typography.titleMedium)
        Button(onClick = ::generateLogs, modifier = Modifier.fillMaxWidth()) {
            Text("Generate logs (D/I/W/E)")
        }

        Text("Network", style = MaterialTheme.typography.titleMedium)
        Button(onClick = { call(app, "https://httpbin.org/get") }, modifier = Modifier.fillMaxWidth()) {
            Text("GET 200 — httpbin.org/get")
        }
        Button(onClick = { call(app, "https://httpbin.org/status/500") }, modifier = Modifier.fillMaxWidth()) {
            Text("GET 500 — httpbin.org/status/500")
        }
        Button(onClick = { call(app, "https://no-such-host.invalid/") }, modifier = Modifier.fillMaxWidth()) {
            Text("GET fail — unknown host")
        }

        Text("Navigation", style = MaterialTheme.typography.titleMedium)
        Button(onClick = { navController.navigate(Routes.USERS) }, modifier = Modifier.fillMaxWidth()) {
            Text("Users screen (Room)")
        }
        Button(onClick = { navController.navigate(Routes.greeting("Afik")) }, modifier = Modifier.fillMaxWidth()) {
            Text("Greeting screen (with argument)")
        }

        Text("Panel & crash", style = MaterialTheme.typography.titleMedium)
        OutlinedButton(onClick = { DevScope.open() }, modifier = Modifier.fillMaxWidth()) {
            Text("Open DevScope panel")
        }
        OutlinedButton(
            onClick = { throw IllegalStateException("Demo crash — pressed the crash button") },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Crash now (reopen app → Crash tab)")
        }
    }
}

private fun generateLogs() {
    Timber.d("Debug message — something small happened")
    Timber.i("Info message — user pressed the demo button")
    Timber.w("Warning message — this looks suspicious")
    Timber.e(RuntimeException("demo exception"), "Error message — with a stack trace")
}

/** Fire-and-forget GET; the result lands in Logs and Network tabs. */
private fun call(app: DemoApplication, url: String) {
    Timber.i("Requesting %s", url)
    app.httpClient.newCall(Request.Builder().url(url).build()).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            Timber.w("Request failed: %s", e.message)
        }

        override fun onResponse(call: Call, response: Response) {
            response.use { Timber.i("Response %d from %s", it.code, url) }
        }
    })
}
