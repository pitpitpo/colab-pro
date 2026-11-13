package com.pox.colab.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import java.util.UUID

@Composable
fun NewVmDialog(
    onDismiss: () -> Unit,
    onDeploy: (name: String, password: String, accelerator: String, tunnel: String, protocol: String) -> Unit
) {
    var isEasyMode by remember { mutableStateOf(true) }
    var name by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var accelerator by remember { mutableStateOf("") }
    var tunnel by remember { mutableStateOf("") }
    var protocol by remember { mutableStateOf("") }

    val autoName = remember { "colab-a100-${UUID.randomUUID().toString().take(4)}" }
    val autoPass = remember { "Colab#${UUID.randomUUID().toString().take(8)}!" }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        title = {
            Text(
                text = "Deploy New VM",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = isEasyMode,
                        onClick = { isEasyMode = true },
                        label = { Text("Easy Mode") },
                        shape = RoundedCornerShape(12.dp)
                    )
                    FilterChip(
                        selected = !isEasyMode,
                        onClick = { isEasyMode = false },
                        label = { Text("Advanced Mode") },
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                if (isEasyMode) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceContainer
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("One-tap auto-generated profile:", style = MaterialTheme.typography.labelMedium)
                            Text("Instance: $autoName", fontFamily = FontFamily.Monospace)
                            Text("Accelerator: NVIDIA A100")
                            Text("Tunnel: Cloudflare Tunnel")
                            Text("Protocol: SSH / tmux")
                            Text("Password: $autoPass", fontFamily = FontFamily.Monospace)
                        }
                    }
                } else {
                    Text(
                        text = "Blank fields automatically fill with optimal defaults.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Instance Name (Auto if blank)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Root SSH Password (Auto if blank)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = accelerator,
                        onValueChange = { accelerator = it },
                        label = { Text("Accelerator (NVIDIA T4 / L4 / A100)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = tunnel,
                        onValueChange = { tunnel = it },
                        label = { Text("Tunnel (Cloudflare Tunnel / Ngrok / Bore)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = protocol,
                        onValueChange = { protocol = it },
                        label = { Text("Protocol (SSH / tmux / Mosh)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isEasyMode) {
                        onDeploy(autoName, autoPass, "NVIDIA A100", "Cloudflare Tunnel", "SSH / tmux")
                    } else {
                        onDeploy(name, password, accelerator, tunnel, protocol)
                    }
                    onDismiss()
                },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Deploy VM")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
