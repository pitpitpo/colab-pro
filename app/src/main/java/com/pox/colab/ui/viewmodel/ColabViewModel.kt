package com.pox.colab.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pox.colab.core.alpine.AlpineContainerManager
import com.pox.colab.core.network.ColabNetworkService
import com.pox.colab.core.ssh.ResilientSshSessionManager
import com.pox.colab.domain.model.AccountTier
import com.pox.colab.domain.model.AlpineContainerState
import com.pox.colab.domain.model.ColabAccount
import com.pox.colab.domain.model.ColabPalette
import com.pox.colab.domain.model.DisplayMode
import com.pox.colab.domain.model.GithubReleaseInfo
import com.pox.colab.domain.model.RemoteFileNode
import com.pox.colab.domain.model.TerminalBackgroundStyle
import com.pox.colab.domain.model.TerminalSessionTab
import com.pox.colab.domain.model.VmInstance
import com.pox.colab.domain.model.VmStatus
import com.pox.colab.domain.model.VmTelemetry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class ColabViewModel(application: Application) : AndroidViewModel(application) {

    private val alpineManager = AlpineContainerManager(application.applicationContext)
    private val networkService = ColabNetworkService()
    private val sshManager = ResilientSshSessionManager(viewModelScope)

    val alpineState: StateFlow<AlpineContainerState> = alpineManager.state

    private val _accounts = MutableStateFlow(
        listOf(
            ColabAccount(
                id = "acc-pro-1",
                label = "Primary Research",
                email = "ravtx12best@gmail.com",
                tier = AccountTier.Pro,
                enabled = true,
                alpineNamespace = "/home/alpine/.colab-instances/ravtx12best-1"
            ),
            ColabAccount(
                id = "acc-free-2",
                label = "Sandbox Tier",
                email = "ml-sandbox@gmail.com",
                tier = AccountTier.Free,
                enabled = true,
                alpineNamespace = "/home/alpine/.colab-instances/ml-sandbox-2"
            )
        )
    )
    val accounts: StateFlow<List<ColabAccount>> = _accounts.asStateFlow()

    private val _activeAccountId = MutableStateFlow("acc-pro-1")
    val activeAccountId: StateFlow<String> = _activeAccountId.asStateFlow()

    private val _vms = MutableStateFlow(
        listOf(
            VmInstance(
                id = "vm-a100-llm",
                name = "colab-a100-llm-ft",
                password = "Colab#A100!94x",
                accelerator = "NVIDIA A100",
                tunnel = "Cloudflare Tunnel",
                protocol = "SSH / tmux",
                accountId = "acc-pro-1",
                status = VmStatus.Running,
                region = "us-central1",
                sshHost = "a100-llm-ft.trycloudflare.com",
                sshPort = 22,
                telemetry = VmTelemetry(
                    cpuPercent = 64,
                    cpuCores = 12,
                    memoryAllocatedGb = 48.2f,
                    memoryTotalGb = 83.5f,
                    gpuLoadPercent = 86,
                    vramUsedGb = 31.4f,
                    vramTotalGb = 40.0f,
                    gpuTempC = 71,
                    diskUsedGb = 92.4f,
                    diskTotalGb = 201.2f,
                    uptimeSeconds = 13840L
                ),
                terminalTabs = listOf(
                    TerminalSessionTab(
                        id = "tab-1",
                        title = "colab-0",
                        tmuxSessionName = "colab-0",
                        cwd = "/content/lora_finetune",
                        isConnected = true,
                        reattachCount = 1,
                        bufferLines = listOf(
                            "SSH tunnel established via Cloudflare Tunnel -> a100-llm-ft.trycloudflare.com",
                            "tmux session [colab-0] reattached (ClientAliveInterval 10, TCPKeepAlive yes)",
                            "$ nvidia-smi",
                            "GPU 0: NVIDIA A100-SXM4-40GB | Temp: 71C | VRAM: 31.4 / 40.0 GiB | Load: 86%"
                        )
                    )
                ),
                activeTabId = "tab-1"
            ),
            VmInstance(
                id = "vm-t4-vision",
                name = "colab-t4-vision",
                password = "Vision#T4!88k",
                accelerator = "NVIDIA T4",
                tunnel = "Ngrok",
                protocol = "SSH / tmux",
                accountId = "acc-free-2",
                status = VmStatus.Running,
                region = "europe-west4",
                sshHost = "0.tcp.ngrok.io",
                sshPort = 14822,
                telemetry = VmTelemetry(
                    cpuPercent = 38,
                    cpuCores = 4,
                    memoryAllocatedGb = 6.8f,
                    memoryTotalGb = 15.0f,
                    gpuLoadPercent = 52,
                    vramUsedGb = 7.2f,
                    vramTotalGb = 15.0f,
                    gpuTempC = 61,
                    diskUsedGb = 39.1f,
                    diskTotalGb = 112.6f,
                    uptimeSeconds = 7410L
                ),
                terminalTabs = listOf(
                    TerminalSessionTab(
                        id = "tab-t4",
                        title = "colab-0",
                        tmuxSessionName = "colab-0",
                        cwd = "/content",
                        isConnected = true,
                        bufferLines = listOf("Connected via SSH / tmux (Ngrok) -> 0.tcp.ngrok.io:14822")
                    )
                ),
                activeTabId = "tab-t4"
            )
        )
    )
    val vms: StateFlow<List<VmInstance>> = _vms.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val filteredVms: StateFlow<List<VmInstance>> = combine(_vms, _searchQuery) { list, query ->
        val q = query.trim().lowercase()
        if (q.isEmpty()) list
        else list.filter { vm ->
            vm.name.lowercase().contains(q) ||
                vm.accelerator.lowercase().contains(q) ||
                vm.protocol.lowercase().contains(q) ||
                vm.tunnel.lowercase().contains(q)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), _vms.value)

    private val _palette = MutableStateFlow(ColabPalette.ColabAmber)
    val palette: StateFlow<ColabPalette> = _palette.asStateFlow()

    private val _displayMode = MutableStateFlow(DisplayMode.AmoledPureBlack)
    val displayMode: StateFlow<DisplayMode> = _displayMode.asStateFlow()

    private val _globalTerminalStyle = MutableStateFlow(TerminalBackgroundStyle.PureBlack)
    val globalTerminalStyle: StateFlow<TerminalBackgroundStyle> = _globalTerminalStyle.asStateFlow()

    private val _perVmTerminalStyle = MutableStateFlow<Map<String, TerminalBackgroundStyle>>(emptyMap())
    val perVmTerminalStyle: StateFlow<Map<String, TerminalBackgroundStyle>> = _perVmTerminalStyle.asStateFlow()

    private val _releases = MutableStateFlow<List<GithubReleaseInfo>>(emptyList())
    val releases: StateFlow<List<GithubReleaseInfo>> = _releases.asStateFlow()

    private val _isLoadingReleases = MutableStateFlow(false)
    val isLoadingReleases: StateFlow<Boolean> = _isLoadingReleases.asStateFlow()

    private val _remoteFiles = MutableStateFlow(
        listOf(
            RemoteFileNode("lora_finetune", "/content/lora_finetune", isDirectory = true),
            RemoteFileNode("datasets", "/content/datasets", isDirectory = true),
            RemoteFileNode(
                name = "train_qlora.py",
                path = "/content/train_qlora.py",
                isDirectory = false,
                sizeBytes = 3420L,
                previewContent = "import torch\nprint('CUDA available:', torch.cuda.is_available())"
            ),
            RemoteFileNode(
                name = "tmux-resilient.sh",
                path = "/content/tmux-resilient.sh",
                isDirectory = false,
                sizeBytes = 215L,
                previewContent = "#!/bin/sh\ntmux attach-session -t colab-0 || tmux new-session -s colab-0"
            )
        )
    )
    val remoteFiles: StateFlow<List<RemoteFileNode>> = _remoteFiles.asStateFlow()

    init {
        // Trigger Alpine container boot & auto-update on launch
        viewModelScope.launch {
            alpineManager.bootAndAutoUpdate()
        }
        // Start background OkHttp telemetry poller
        viewModelScope.launch {
            networkService.pollVmTelemetry({ _vms.value }, intervalMs = 3_000L).collect { updated ->
                _vms.value = updated
            }
        }
        refreshGithubReleases()
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun triggerAlpineUpdate() {
        viewModelScope.launch {
            alpineManager.bootAndAutoUpdate()
        }
    }

    fun deployVm(
        name: String,
        password: String,
        accelerator: String,
        tunnel: String,
        protocol: String
    ) {
        val activeAcc = _accounts.value.find { it.id == _activeAccountId.value } ?: _accounts.value.first()
        val defaultAccel = if (activeAcc.tier == AccountTier.Pro) "NVIDIA A100" else "NVIDIA T4"
        val resolvedAccel = accelerator.ifBlank { defaultAccel }
        val resolvedName = name.ifBlank {
            "colab-${resolvedAccel.lowercase().replace("nvidia ", "").replace(" ", "-")}-${UUID.randomUUID().toString().take(4)}"
        }
        val resolvedPassword = password.ifBlank { "Colab#${UUID.randomUUID().toString().take(8)}!" }
        val resolvedTunnel = tunnel.ifBlank { "Cloudflare Tunnel" }
        val resolvedProtocol = protocol.ifBlank { "SSH / tmux" }

        val tabId = "tab-${System.currentTimeMillis()}"
        val newVm = VmInstance(
            id = "vm-${System.currentTimeMillis()}",
            name = resolvedName,
            password = resolvedPassword,
            accelerator = resolvedAccel,
            tunnel = resolvedTunnel,
            protocol = resolvedProtocol,
            accountId = activeAcc.id,
            status = VmStatus.Running,
            sshHost = "${resolvedName.lowercase()}.trycloudflare.com",
            sshPort = 22,
            telemetry = VmTelemetry(
                cpuPercent = 18,
                cpuCores = if (resolvedAccel.contains("A100") || resolvedAccel.contains("L4")) 12 else 4,
                memoryAllocatedGb = 12.4f,
                memoryTotalGb = if (resolvedAccel.contains("A100")) 83.5f else 15.0f,
                gpuLoadPercent = 14,
                vramUsedGb = 4.2f,
                vramTotalGb = if (resolvedAccel.contains("A100")) 40.0f else 15.0f,
                gpuTempC = 44
            ),
            terminalTabs = listOf(
                TerminalSessionTab(
                    id = tabId,
                    title = "colab-0",
                    tmuxSessionName = "colab-0",
                    bufferLines = listOf(
                        "Connected via $resolvedProtocol ($resolvedTunnel)",
                        "tmux session [colab-0] attached · KeepAlive 10s"
                    )
                )
            ),
            activeTabId = tabId
        )

        _vms.value = listOf(newVm) + _vms.value
    }

    fun toggleVmPower(vmId: String) {
        _vms.value = _vms.value.map { vm ->
            if (vm.id != vmId) vm
            else {
                val nextStatus = if (vm.status == VmStatus.Stopped) VmStatus.Running else VmStatus.Stopped
                if (nextStatus == VmStatus.Stopped) sshManager.disconnect(vmId)
                vm.copy(status = nextStatus)
            }
        }
    }

    fun addTerminalTab(vmId: String) {
        _vms.value = _vms.value.map { vm ->
            if (vm.id != vmId) vm
            else {
                val idx = vm.terminalTabs.size
                val newTab = TerminalSessionTab(
                    id = "tab-${System.currentTimeMillis()}",
                    title = "colab-$idx",
                    tmuxSessionName = "colab-$idx",
                    bufferLines = listOf("tmux new-session -s colab-$idx · SSH KeepAlive (10s) active")
                )
                vm.copy(
                    terminalTabs = vm.terminalTabs + newTab,
                    activeTabId = newTab.id
                )
            }
        }
    }

    fun closeTerminalTab(vmId: String, tabId: String) {
        _vms.value = _vms.value.map { vm ->
            if (vm.id != vmId || vm.terminalTabs.size <= 1) vm
            else {
                val remaining = vm.terminalTabs.filterNot { it.id == tabId }
                val nextActive = if (vm.activeTabId == tabId) remaining.last().id else vm.activeTabId
                vm.copy(terminalTabs = remaining, activeTabId = nextActive)
            }
        }
    }

    fun selectTerminalTab(vmId: String, tabId: String) {
        _vms.value = _vms.value.map { vm ->
            if (vm.id == vmId) vm.copy(activeTabId = tabId) else vm
        }
    }

    fun executeTerminalCommand(vmId: String, command: String) {
        val cmd = command.trim()
        if (cmd.isEmpty()) return
        _vms.value = _vms.value.map { vm ->
            if (vm.id != vmId) vm
            else {
                val updatedTabs = vm.terminalTabs.map { tab ->
                    if (tab.id != vm.activeTabId) tab
                    else {
                        if (cmd == "clear") {
                            tab.copy(bufferLines = emptyList())
                        } else {
                            val output = when {
                                cmd.startsWith("nvidia-smi") ->
                                    "GPU 0: ${vm.accelerator} | Temp: ${vm.telemetry.gpuTempC}C | VRAM: ${vm.telemetry.vramUsedGb}/${vm.telemetry.vramTotalGb} GiB | Load: ${vm.telemetry.gpuLoadPercent}%"
                                cmd == "ls" ->
                                    _remoteFiles.value.joinToString("  ") { if (it.isDirectory) "${it.name}/" else it.name }
                                cmd.startsWith("tmux") ->
                                    "${tab.tmuxSessionName}: 1 windows (attached) [ClientAliveInterval 10, TCPKeepAlive yes]"
                                else -> "Executed in Alpine/SSH tmux [${tab.tmuxSessionName}]: $cmd"
                            }
                            tab.copy(bufferLines = tab.bufferLines + listOf("${tab.cwd} $ $cmd", output))
                        }
                    }
                }
                vm.copy(terminalTabs = updatedTabs)
            }
        }
    }

    fun createRemoteFolder(folderName: String) {
        val clean = folderName.trim()
        if (clean.isEmpty()) return
        _remoteFiles.value = _remoteFiles.value + RemoteFileNode(
            name = clean,
            path = "/content/$clean",
            isDirectory = true
        )
    }

    fun deleteRemoteNode(name: String) {
        _remoteFiles.value = _remoteFiles.value.filterNot { it.name == name }
    }

    fun renameRemoteNode(oldName: String, newName: String) {
        val clean = newName.trim()
        if (clean.isEmpty()) return
        _remoteFiles.value = _remoteFiles.value.map { node ->
            if (node.name == oldName) node.copy(name = clean, path = "/content/$clean") else node
        }
    }

    fun uploadRemoteFile(fileName: String, content: String) {
        val clean = fileName.trim()
        if (clean.isEmpty()) return
        val newNode = RemoteFileNode(
            name = clean,
            path = "/content/$clean",
            isDirectory = false,
            sizeBytes = content.toByteArray().size.toLong(),
            previewContent = content
        )
        _remoteFiles.value = _remoteFiles.value.filterNot { it.name == clean } + newNode
    }

    fun selectActiveAccount(accountId: String) {
        _activeAccountId.value = accountId
    }

    fun toggleAccountEnabled(accountId: String) {
        _accounts.value = _accounts.value.map { acc ->
            if (acc.id == accountId) acc.copy(enabled = !acc.enabled) else acc
        }
    }

    fun setAccountTier(accountId: String, tier: AccountTier) {
        _accounts.value = _accounts.value.map { acc ->
            if (acc.id == accountId) acc.copy(tier = tier) else acc
        }
    }

    fun addAccountWithPkce(label: String, email: String, tier: AccountTier, onPkceReady: (String) -> Unit) {
        viewModelScope.launch {
            val pkce = alpineManager.initializeIsolatedAccountPkce(label.ifBlank { email }, email)
            val newAcc = ColabAccount(
                id = "acc-${System.currentTimeMillis()}",
                label = label.ifBlank { email.substringBefore("@") },
                email = email,
                tier = tier,
                enabled = true,
                alpineNamespace = pkce.alpineNamespace,
                pkceAuthUrl = pkce.oauthUrl
            )
            _accounts.value = _accounts.value + newAcc
            _activeAccountId.value = newAcc.id
            onPkceReady(pkce.oauthUrl)
        }
    }

    fun setPalette(palette: ColabPalette) {
        _palette.value = palette
    }

    fun setDisplayMode(mode: DisplayMode) {
        _displayMode.value = mode
    }

    fun setGlobalTerminalStyle(style: TerminalBackgroundStyle) {
        _globalTerminalStyle.value = style
    }

    fun setVmTerminalStyle(vmId: String, style: TerminalBackgroundStyle?) {
        val next = _perVmTerminalStyle.value.toMutableMap()
        if (style == null) next.remove(vmId) else next[vmId] = style
        _perVmTerminalStyle.value = next
    }

    fun refreshGithubReleases() {
        viewModelScope.launch {
            _isLoadingReleases.value = true
            networkService.fetchGithubReleases().onSuccess { list ->
                _releases.value = list
            }
            _isLoadingReleases.value = false
        }
    }
}
