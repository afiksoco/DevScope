package com.devscope.demo.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.devscope.demo.DemoApplication
import com.devscope.demo.data.User
import kotlinx.coroutines.launch
import timber.log.Timber

private val NAMES = listOf("Noa", "Tamar", "Yossi", "Dana", "Omer", "Shira", "Eitan", "Maya")

/**
 * Room demo screen: rows added here appear immediately in DevScope's DB tab
 * (`SELECT * FROM users`).
 */
@Composable
fun UsersScreen() {
    val app = LocalContext.current.applicationContext as DemoApplication
    val scope = rememberCoroutineScope()
    val users by app.database.userDao().observeAll().collectAsState(initial = emptyList())

    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Users (Room)", style = MaterialTheme.typography.headlineMedium)
        Button(
            onClick = {
                scope.launch {
                    val user = User(name = NAMES.random())
                    app.database.userDao().insert(user)
                    Timber.i("Inserted user %s", user.name)
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Add random user") }

        LazyColumn {
            items(users, key = { it.id }) { user ->
                ListItem(
                    headlineContent = { Text(user.name) },
                    supportingContent = { Text("id ${user.id}") },
                    trailingContent = {
                        TextButton(onClick = {
                            scope.launch {
                                app.database.userDao().delete(user)
                                Timber.i("Deleted user %s", user.name)
                            }
                        }) { Text("Delete") }
                    },
                )
            }
        }
        if (users.isEmpty()) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                Text("No users yet — add a few and check the DB tab", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
