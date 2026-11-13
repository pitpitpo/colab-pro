package com.pox.colab.ui.screens.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pox.colab.domain.model.AccountTier
import com.pox.colab.domain.model.AlpineContainerState
import com.pox.colab.domain.model.ColabAccount
import com.pox.colab.domain.model.ColabPalette
import com.pox.colab.domain.model.DisplayMode
import com.pox.colab.domain.model.GithubReleaseInfo
import com.pox.colab.domain.model.TerminalBackgroundStyle
import com.pox.colab.domain.model.VmInstance

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    alpineState: AlpineContainerState,
    accounts: List<ColabAccount>,
    activeAccountId: String,
    vms: List<VmInstance>,
    palette: ColabPalette,
    displayMode: DisplayMode,
    globalTerminalStyle: TerminalBackgroundStyle,
    perVmTerminalStyle: Map<String, TerminalBackgroundStyle>,
    releases: List<GithubReleaseInfo>,
    onTriggerAlpineUpdate: () -> Unit,
    onSelectActiveAccount: (String) -> Unit,
    onToggleAccountEnabled: (String) -> Unit,
    onSetAccountTier: (String, AccountTier) -> Unit,
    onAddAccountPkce: (label: String, email: String, tier: AccountTier) -> Unit,
    onSelectPalette: (ColabPalette) -> Unit,
    onSelectDisplayMode: (DisplayMode) -> Unit,
    onSelectTerminalStyle: (TerminalBackgroundStyle) -> Unit,
    onSelectVmTerminalStyle: (vmId: String, style: TerminalBackgroundStyle?) -> Unit,
    onRefreshReleases: () -> Unit
) {
    var selectedHub by remember { mutableIntStateOf(0) }
    val hubs = listOf("Accounts Hub", "Theme Hub", "Updates")
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize()) {
        PrimaryTabRow(selectedTabIndex = selectedHub) {
            hubs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedHub == index,
                    onClick = { selectedHub = index },
                    text = { Text(title) }
                )
            }
        }

        when (selectedHub) {
            0 -> {
                var newEmail by remember { mutableStateOf("") }
                var newLabel by remember { mutableStateOf("") }
                var newTier by remember { mutableStateOf(AccountTier.Pro) }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainer
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("Embedded Alpine Linux Runtime", fontWeight = FontWeight.Bold)
                                Text(
                                    "${alpineState.distroVersion} · colab-cli ${alpineState.colabCliVersion}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Button(
                                    onClick = onTriggerAlpineUpdate,
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(if (alpineState.isUpdating) "Updating..." else "Run Boot Auto-Update")
                                }
                            }
                        }
                    }

                    item {
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainer
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("Add Account (Alpine PKCE colab auth login)", fontWeight = FontWeight.Bold)
                                OutlinedTextField(
                                    value = newLabel,
                                    onValueChange = { newLabel = it },
                                    label = { Text("Account Label") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = newEmail,
                                    onValueChange = { newEmail = it },
                                    label = { Text("Google Account Email") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    AccountTier.entries.forEach { tier ->
                                        FilterChip(
                                            selected = newTier == tier,
                                            onClick = { newTier = tier },
                                            label = { Text("Colab ${tier.name}") }
                                        )
                                    }
                                }
                                Button(
                                    onClick = {
                                        if (newEmail.isNotBlank()) {
                                            onAddAccountPkce(newLabel, newEmail, newTier)
                                            newLabel = ""
                                            newEmail = ""
                                        }
                                    },
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Generate PKCE OAuth Link")
                                }
                            }
                        }
                    }

                    items(accounts, key = { it.id }) { acc ->
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainer
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(
                                        selected = acc.id == activeAccountId,
                                        onClick = { onSelectActiveAccount(acc.id) }
                                    )
                                    Column {
                                        Text(acc.label, fontWeight = FontWeight.SemiBold)
                                        Text(acc.email, style = MaterialTheme.typography.bodySmall)
                                        Text(
                                            acc.alpineNamespace,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    AccountTier.entries.forEach { tier ->
                                        FilterChip(
                                            selected = acc.tier == tier,
                                            onClick = { onSetAccountTier(acc.id, tier) },
                                            label = { Text(tier.name) }
                                        )
                                    }
                                    Switch(
                                        checked = acc.enabled,
                                        onCheckedChange = { onToggleAccountEnabled(acc.id) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            1 -> {
                var selectedVmIdForStyle by remember(vms) {
                    mutableStateOf(vms.firstOrNull()?.id.orEmpty())
                }
                val activeVmOverride = perVmTerminalStyle[selectedVmIdForStyle]

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Text("M3 Expressive Palettes", style = MaterialTheme.typography.titleMedium)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ColabPalette.entries.forEach { item ->
                                FilterChip(
                                    selected = palette == item,
                                    onClick = { onSelectPalette(item) },
                                    label = { Text(item.displayName) }
                                )
                            }
                        }
                    }

                    item {
                        Text("Display Modes", style = MaterialTheme.typography.titleMedium)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            DisplayMode.entries.forEach { mode ->
                                FilterChip(
                                    selected = displayMode == mode,
                                    onClick = { onSelectDisplayMode(mode) },
                                    label = { Text(mode.label) }
                                )
                            }
                        }
                    }

                    item {
                        Text("Global Terminal Background", style = MaterialTheme.typography.titleMedium)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TerminalBackgroundStyle.entries.forEach { bg ->
                                FilterChip(
                                    selected = globalTerminalStyle == bg,
                                    onClick = { onSelectTerminalStyle(bg) },
                                    label = { Text(bg.label) }
                                )
                            }
                        }
                    }

                    if (vms.isNotEmpty()) {
                        item {
                            Card(
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            "Granular Per-VM Terminal Styling",
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        if (activeVmOverride != null) {
                                            TextButton(
                                                onClick = {
                                                    onSelectVmTerminalStyle(selectedVmIdForStyle, null)
                                                }
                                            ) {
                                                Text("Reset to Global")
                                            }
                                        }
                                    }

                                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        vms.forEach { vm ->
                                            FilterChip(
                                                selected = selectedVmIdForStyle == vm.id,
                                                onClick = { selectedVmIdForStyle = vm.id },
                                                label = { Text(vm.name) }
                                            )
                                        }
                                    }

                                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        TerminalBackgroundStyle.entries.forEach { bg ->
                                            FilterChip(
                                                selected = activeVmOverride == bg,
                                                onClick = {
                                                    onSelectVmTerminalStyle(selectedVmIdForStyle, bg)
                                                },
                                                label = { Text(bg.label) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            2 -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("GitHub Releases (pitpitpo/colab)", style = MaterialTheme.typography.titleMedium)
                            Button(onClick = onRefreshReleases, shape = RoundedCornerShape(12.dp)) {
                                Text("Check Updates")
                            }
                        }
                    }

                    items(releases, key = { it.id }) { rel ->
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainer
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("${rel.name} (${rel.tagName})", fontWeight = FontWeight.Bold)
                                rel.changelogBullets.forEach { bullet ->
                                    Text("• $bullet", style = MaterialTheme.typography.bodySmall)
                                }
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    rel.assets.forEach { asset ->
                                        Button(
                                            onClick = {
                                                context.startActivity(
                                                    Intent(Intent.ACTION_VIEW, Uri.parse(asset.downloadUrl))
                                                )
                                            },
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Text(asset.name)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
