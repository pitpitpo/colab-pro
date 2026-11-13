package com.pox.colab

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.pox.colab.ui.screens.details.VmDetailsScreen
import com.pox.colab.ui.screens.home.HomeScreen
import com.pox.colab.ui.screens.home.NewVmDialog
import com.pox.colab.ui.screens.settings.SettingsScreen
import com.pox.colab.ui.theme.ColabM3ExpressiveTheme
import com.pox.colab.ui.viewmodel.ColabViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: ColabViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val palette by viewModel.palette.collectAsState()
            val displayMode by viewModel.displayMode.collectAsState()
            val vms by viewModel.filteredVms.collectAsState()
            val searchQuery by viewModel.searchQuery.collectAsState()
            val alpineState by viewModel.alpineState.collectAsState()
            val accounts by viewModel.accounts.collectAsState()
            val activeAccountId by viewModel.activeAccountId.collectAsState()
            val globalTerminalStyle by viewModel.globalTerminalStyle.collectAsState()
            val perVmTerminalStyle by viewModel.perVmTerminalStyle.collectAsState()
            val remoteFiles by viewModel.remoteFiles.collectAsState()
            val releases by viewModel.releases.collectAsState()

            var currentDestination by remember { mutableStateOf("home") }
            var selectedVmId by remember { mutableStateOf(vms.firstOrNull()?.id.orEmpty()) }
            var selectedVmTab by remember { mutableIntStateOf(0) }
            var showNewVmDialog by remember { mutableStateOf(false) }

            ColabM3ExpressiveTheme(
                palette = palette,
                displayMode = displayMode
            ) {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        NavigationBar {
                            NavigationBarItem(
                                selected = currentDestination == "home",
                                onClick = { currentDestination = "home" },
                                icon = { Icon(Icons.Default.Computer, contentDescription = null) },
                                label = { Text("Instances") }
                            )
                            NavigationBarItem(
                                selected = currentDestination == "details",
                                onClick = {
                                    if (selectedVmId.isEmpty()) {
                                        selectedVmId = vms.firstOrNull()?.id.orEmpty()
                                    }
                                    currentDestination = "details"
                                },
                                icon = { Icon(Icons.Default.Terminal, contentDescription = null) },
                                label = { Text("Console") }
                            )
                            NavigationBarItem(
                                selected = currentDestination == "settings",
                                onClick = { currentDestination = "settings" },
                                icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                                label = { Text("Settings") }
                            )
                        }
                    }
                ) { innerPadding ->
                    when (currentDestination) {
                        "home" -> HomeScreen(
                            vms = vms,
                            searchQuery = searchQuery,
                            onSearchChange = viewModel::updateSearchQuery,
                            onOpenNewVmDialog = { showNewVmDialog = true },
                            onOpenVmDetails = { vmId, tab ->
                                selectedVmId = vmId
                                selectedVmTab = tab
                                currentDestination = "details"
                            },
                            modifier = Modifier.padding(innerPadding)
                        )

                        "details" -> {
                            val activeVm = vms.find { it.id == selectedVmId } ?: vms.firstOrNull()
                            if (activeVm != null) {
                                val effectiveTerminalStyle =
                                    perVmTerminalStyle[activeVm.id] ?: globalTerminalStyle
                                VmDetailsScreen(
                                    vm = activeVm,
                                    initialTab = selectedVmTab,
                                    terminalStyle = effectiveTerminalStyle,
                                    remoteFiles = remoteFiles,
                                    onBack = { currentDestination = "home" },
                                    onTogglePower = { viewModel.toggleVmPower(activeVm.id) },
                                    onAddTerminalTab = { viewModel.addTerminalTab(activeVm.id) },
                                    onCloseTerminalTab = { tabId ->
                                        viewModel.closeTerminalTab(activeVm.id, tabId)
                                    },
                                    onSelectTerminalTab = { tabId ->
                                        viewModel.selectTerminalTab(activeVm.id, tabId)
                                    },
                                    onExecuteCommand = { cmd ->
                                        viewModel.executeTerminalCommand(activeVm.id, cmd)
                                    },
                                    onCreateFolder = viewModel::createRemoteFolder,
                                    onRenameFile = viewModel::renameRemoteNode,
                                    onUploadFile = viewModel::uploadRemoteFile,
                                    onDeleteFile = viewModel::deleteRemoteNode
                                )
                            }
                        }

                        "settings" -> SettingsScreen(
                            alpineState = alpineState,
                            accounts = accounts,
                            activeAccountId = activeAccountId,
                            vms = vms,
                            palette = palette,
                            displayMode = displayMode,
                            globalTerminalStyle = globalTerminalStyle,
                            perVmTerminalStyle = perVmTerminalStyle,
                            releases = releases,
                            onTriggerAlpineUpdate = viewModel::triggerAlpineUpdate,
                            onSelectActiveAccount = viewModel::selectActiveAccount,
                            onToggleAccountEnabled = viewModel::toggleAccountEnabled,
                            onSetAccountTier = viewModel::setAccountTier,
                            onAddAccountPkce = { label, email, tier ->
                                viewModel.addAccountWithPkce(label, email, tier) { oauthUrl ->
                                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(oauthUrl)))
                                }
                            },
                            onSelectPalette = viewModel::setPalette,
                            onSelectDisplayMode = viewModel::setDisplayMode,
                            onSelectTerminalStyle = viewModel::setGlobalTerminalStyle,
                            onSelectVmTerminalStyle = viewModel::setVmTerminalStyle,
                            onRefreshReleases = viewModel::refreshGithubReleases
                        )
                    }

                    if (showNewVmDialog) {
                        NewVmDialog(
                            onDismiss = { showNewVmDialog = false },
                            onDeploy = viewModel::deployVm
                        )
                    }
                }
            }
        }
    }
}
