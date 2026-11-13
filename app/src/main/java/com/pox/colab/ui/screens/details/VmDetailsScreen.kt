package com.pox.colab.ui.screens.details

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pox.colab.domain.model.RemoteFileNode
import com.pox.colab.domain.model.TerminalBackgroundStyle
import com.pox.colab.domain.model.VmInstance
import com.pox.colab.ui.components.ListItemPlaceholder
import com.pox.colab.ui.components.StatusIndicatorDot
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VmDetailsScreen(
    vm: VmInstance,
    initialTab: Int = 0,
    terminalStyle: TerminalBackgroundStyle,
    remoteFiles: List<RemoteFileNode>,
    onBack: () -> Unit,
    onTogglePower: () -> Unit,
    onAddTerminalTab: () -> Unit,
    onCloseTerminalTab: (String) -> Unit,
    onSelectTerminalTab: (String) -> Unit,
    onExecuteCommand: (String) -> Unit,
    onCreateFolder: (String) -> Unit,
    onRenameFile: (oldName: String, newName: String) -> Unit,
    onUploadFile: (fileName: String, content: String) -> Unit,
    onDeleteFile: (String) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(initialTab) }
    val tabs = listOf("Dashboard", "Console", "File Manager")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(vm.name, fontWeight = FontWeight.Bold)
                        StatusIndicatorDot(status = vm.status)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onTogglePower) {
                        Icon(Icons.Default.PowerSettingsNew, contentDescription = "Toggle VM Power")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            PrimaryTabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            when (selectedTab) {
                0 -> DashboardTabContent(vm = vm, onTogglePower = onTogglePower)
                1 -> ConsoleTabContent(
                    vm = vm,
                    terminalStyle = terminalStyle,
                    onAddTab = onAddTerminalTab,
                    onCloseTab = onCloseTerminalTab,
                    onSelectTab = onSelectTerminalTab,
                    onExecuteCommand = onExecuteCommand
                )
                2 -> FileManagerTabContent(
                    files = remoteFiles,
                    onCreateFolder = onCreateFolder,
                    onRenameFile = onRenameFile,
                    onUploadFile = onUploadFile,
                    onDeleteFile = onDeleteFile
                )
            }
        }
    }
}

@Composable
private fun DashboardTabContent(
    vm: VmInstance,
    onTogglePower: () -> Unit
) {
    val clipboard = LocalClipboardManager.current
    val t = vm.telemetry
    val deploymentScript = remember(vm.name, vm.password, vm.sshHost) {
        """
        #!/usr/bin/env sh
        export COLAB_HOME="/home/alpine/.colab-instances/${vm.accountId}"
        colab runtime create --name "${vm.name}" --accelerator "${vm.accelerator}"
        echo "ClientAliveInterval 10" | sudo tee -a /etc/ssh/sshd_config
        echo "TCPKeepAlive yes" | sudo tee -a /etc/ssh/sshd_config
        echo "root:${vm.password}" | sudo chpasswd
        tmux has-session -t colab-0 2>/dev/null || tmux new-session -d -s colab-0
        """.trimIndent()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TelemetryGaugeCard(
                    title = "CPU Load",
                    progress = t.cpuPercent / 100f,
                    valueText = "${t.cpuPercent}% (${t.cpuCores} vCPU)",
                    modifier = Modifier.weight(1f)
                )
                TelemetryGaugeCard(
                    title = "Memory",
                    progress = if (t.memoryTotalGb > 0f) t.memoryAllocatedGb / t.memoryTotalGb else 0f,
                    valueText = "${t.memoryAllocatedGb} / ${t.memoryTotalGb} GB",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TelemetryGaugeCard(
                    title = "GPU & VRAM",
                    progress = t.gpuLoadPercent / 100f,
                    valueText = "${t.vramUsedGb}/${t.vramTotalGb} GB · ${t.gpuTempC}°C",
                    modifier = Modifier.weight(1f)
                )
                TelemetryGaugeCard(
                    title = "Disk (/)",
                    progress = if (t.diskTotalGb > 0f) t.diskUsedGb / t.diskTotalGb else 0f,
                    valueText = "${t.diskUsedGb} / ${t.diskTotalGb} GB",
                    modifier = Modifier.weight(1f)
                )
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
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("VM Deployment Script", style = MaterialTheme.typography.titleMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilledTonalButton(onClick = onTogglePower) {
                                Text(if (vm.status.name == "Stopped") "Power On" else "Power Off")
                            }
                            Button(
                                onClick = { clipboard.setText(AnnotatedString(deploymentScript)) }
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("Copy")
                            }
                        }
                    }

                    Text(
                        text = deploymentScript,
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                            .padding(12.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun TelemetryGaugeCard(
    title: String,
    progress: Float,
    valueText: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CircularProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                strokeWidth = 5.dp
            )
            Column {
                Text(title, style = MaterialTheme.typography.labelMedium)
                Text(
                    valueText,
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun ConsoleTabContent(
    vm: VmInstance,
    terminalStyle: TerminalBackgroundStyle,
    onAddTab: () -> Unit,
    onCloseTab: (String) -> Unit,
    onSelectTab: (String) -> Unit,
    onExecuteCommand: (String) -> Unit
) {
    var input by remember { mutableStateOf("") }
    val activeTab = vm.terminalTabs.find { it.id == vm.activeTabId } ?: vm.terminalTabs.firstOrNull()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Multi-tab tmux session bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            vm.terminalTabs.forEach { tab ->
                FilledTonalButton(
                    onClick = { onSelectTab(tab.id) },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(tab.title, fontFamily = FontFamily.Monospace)
                    if (vm.terminalTabs.size > 1) {
                        Spacer(Modifier.width(6.dp))
                        IconButton(onClick = { onCloseTab(tab.id) }) {
                            Icon(Icons.Default.Close, contentDescription = "Close tab")
                        }
                    }
                }
            }
            IconButton(onClick = onAddTab) {
                Icon(Icons.Default.Add, contentDescription = "New tmux tab")
            }
        }

        // Terminal Buffer with Theme Hub dynamic background
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(terminalStyle.colorHex))
                .padding(14.dp)
        ) {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(activeTab?.bufferLines.orEmpty()) { line ->
                    Text(
                        text = line,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFFF4F4F6),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        // Accessory keys row (Ctrl, Tab, Esc, Arrows)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf("Ctrl", "Tab", "Esc", "↑", "↓", "←", "→", "|", "/", "~").forEach { keyLabel ->
                OutlinedButton(
                    onClick = {
                        when (keyLabel) {
                            "Esc" -> input = ""
                            "Tab" -> input += "  "
                            else -> input += keyLabel
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(keyLabel, fontFamily = FontFamily.Monospace)
                }
            }
        }

        // Input Field
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                singleLine = true,
                placeholder = { Text("Command (nvidia-smi, ls, tmux ls)...") }
            )
            Button(
                onClick = {
                    onExecuteCommand(input)
                    input = ""
                },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Run")
            }
        }
    }
}

@Composable
private fun FileManagerTabContent(
    files: List<RemoteFileNode>,
    onCreateFolder: (String) -> Unit,
    onRenameFile: (oldName: String, newName: String) -> Unit,
    onUploadFile: (fileName: String, content: String) -> Unit,
    onDeleteFile: (String) -> Unit
) {
    val clipboard = LocalClipboardManager.current
    var currentPath by remember { mutableStateOf("/content") }
    var isFetching by remember { mutableStateOf(false) }

    var showNewFolderDialog by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }

    var showUploadDialog by remember { mutableStateOf(false) }
    var uploadFileName by remember { mutableStateOf("") }
    var uploadFileBody by remember { mutableStateOf("") }

    var renameTarget by remember { mutableStateOf<RemoteFileNode?>(null) }
    var renameValue by remember { mutableStateOf("") }

    var previewNode by remember { mutableStateOf<RemoteFileNode?>(null) }

    LaunchedEffect(currentPath) {
        isFetching = true
        delay(220)
        isFetching = false
    }

    val breadcrumbs = remember(currentPath) {
        currentPath.split("/").filter { it.isNotBlank() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Breadcrumb Navigation & Action Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                breadcrumbs.forEachIndexed { index, segment ->
                    val targetPath = "/" + breadcrumbs.take(index + 1).joinToString("/")
                    AssistChip(
                        onClick = { currentPath = targetPath },
                        label = { Text(segment, fontFamily = FontFamily.Monospace) },
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilledTonalButton(
                    onClick = { showUploadDialog = true },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Upload, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Upload")
                }

                Button(
                    onClick = { showNewFolderDialog = true },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.CreateNewFolder, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Folder")
                }

                IconButton(onClick = { isFetching = true }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh directory")
                }
            }
        }

        // Shimmer Skeleton Loading Placeholders during directory fetches
        if (isFetching) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ListItemPlaceholder()
                ListItemPlaceholder()
                ListItemPlaceholder()
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(files, key = { it.path }) { node ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (node.isDirectory) {
                                    currentPath = node.path
                                } else {
                                    previewNode = node
                                }
                            },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(node.name, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Medium)
                                Text(
                                    if (node.isDirectory) "Directory" else "${node.sizeBytes} B",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Row {
                                if (!node.isDirectory) {
                                    IconButton(onClick = { previewNode = node }) {
                                        Icon(Icons.Default.Visibility, contentDescription = "Preview ${node.name}")
                                    }
                                    IconButton(
                                        onClick = {
                                            clipboard.setText(AnnotatedString(node.previewContent.orEmpty()))
                                        }
                                    ) {
                                        Icon(Icons.Default.Download, contentDescription = "Download ${node.name}")
                                    }
                                }
                                IconButton(
                                    onClick = {
                                        renameTarget = node
                                        renameValue = node.name
                                    }
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = "Rename ${node.name}")
                                }
                                IconButton(onClick = { onDeleteFile(node.name) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete ${node.name}")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showNewFolderDialog) {
        AlertDialog(
            onDismissRequest = { showNewFolderDialog = false },
            shape = RoundedCornerShape(28.dp),
            title = { Text("Create Folder") },
            text = {
                OutlinedTextField(
                    value = newFolderName,
                    onValueChange = { newFolderName = it },
                    label = { Text("Folder name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onCreateFolder(newFolderName)
                        newFolderName = ""
                        showNewFolderDialog = false
                    }
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewFolderDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showUploadDialog) {
        AlertDialog(
            onDismissRequest = { showUploadDialog = false },
            shape = RoundedCornerShape(28.dp),
            title = { Text("Upload Remote File") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = uploadFileName,
                        onValueChange = { uploadFileName = it },
                        label = { Text("Filename (e.g. script.py)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = uploadFileBody,
                        onValueChange = { uploadFileBody = it },
                        label = { Text("File contents") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUploadFile(uploadFileName, uploadFileBody)
                        uploadFileName = ""
                        uploadFileBody = ""
                        showUploadDialog = false
                    }
                ) {
                    Text("Upload")
                }
            },
            dismissButton = {
                TextButton(onClick = { showUploadDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    renameTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            shape = RoundedCornerShape(28.dp),
            title = { Text("Rename ${target.name}") },
            text = {
                OutlinedTextField(
                    value = renameValue,
                    onValueChange = { renameValue = it },
                    label = { Text("New name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onRenameFile(target.name, renameValue)
                        renameTarget = null
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { renameTarget = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    previewNode?.let { node ->
        AlertDialog(
            onDismissRequest = { previewNode = null },
            shape = RoundedCornerShape(28.dp),
            title = { Text(node.path, fontFamily = FontFamily.Monospace) },
            text = {
                Text(
                    text = node.previewContent ?: "(empty file)",
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodySmall
                )
            },
            confirmButton = {
                TextButton(onClick = { previewNode = null }) {
                    Text("Close")
                }
            }
        )
    }
}
