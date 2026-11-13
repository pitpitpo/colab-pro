package com.pox.colab.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class VmStatus {
    Running,
    Stopped,
    Booting,
    Reconnecting
}

@Serializable
enum class AccountTier {
    Pro,
    Free
}

@Serializable
enum class ColabPalette(val displayName: String, val primaryHex: Long) {
    ColabAmber("Colab Amber", 0xFFF9AB00),
    MatrixGreen("Matrix Green", 0xFF22C55E),
    CyberCyan("Cyber Cyan", 0xFF06B6D4),
    NeonViolet("Neon Violet", 0xFFA855F7),
    Monochrome("Monochrome", 0xFFF4F4F5)
}

@Serializable
enum class DisplayMode(val label: String) {
    AmoledPureBlack("AMOLED Pure Black (#000000)"),
    DarkMode("Dark Mode"),
    DynamicMaterialYou("Dynamic Color (Material You)")
}

@Serializable
enum class TerminalBackgroundStyle(val label: String, val colorHex: Long) {
    PureBlack("Pure Black", 0xFF000000),
    CarbonMatrix("Carbon Matrix", 0xFF060B07),
    AmberPhosphor("Amber Phosphor", 0xFF0D0902),
    DeepSlate("Deep Slate", 0xFF0B0F17),
    CyberGrid("Cyber Grid", 0xFF05080D)
}

@Serializable
data class VmTelemetry(
    val cpuPercent: Int = 0,
    val cpuCores: Int = 4,
    val memoryAllocatedGb: Float = 0f,
    val memoryTotalGb: Float = 15f,
    val gpuLoadPercent: Int = 0,
    val vramUsedGb: Float = 0f,
    val vramTotalGb: Float = 15f,
    val gpuTempC: Int = 38,
    val diskUsedGb: Float = 24.6f,
    val diskTotalGb: Float = 112.6f,
    val uptimeSeconds: Long = 0L,
    val polledAtEpochMs: Long = System.currentTimeMillis()
)

@Serializable
data class TerminalSessionTab(
    val id: String,
    val title: String,
    val tmuxSessionName: String,
    val cwd: String = "/content",
    val isConnected: Boolean = true,
    val reattachCount: Int = 0,
    val bufferLines: List<String> = emptyList()
)

@Serializable
data class RemoteFileNode(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val sizeBytes: Long = 0L,
    val modifiedEpochMs: Long = System.currentTimeMillis(),
    val previewContent: String? = null
)

@Serializable
data class VmInstance(
    val id: String,
    val name: String,
    val password: String,
    val accelerator: String,
    val tunnel: String,
    val protocol: String,
    val accountId: String,
    val status: VmStatus = VmStatus.Running,
    val region: String = "us-central1",
    val sshHost: String,
    val sshPort: Int = 22,
    val telemetry: VmTelemetry = VmTelemetry(),
    val terminalTabs: List<TerminalSessionTab> = emptyList(),
    val activeTabId: String = ""
)

@Serializable
data class ColabAccount(
    val id: String,
    val label: String,
    val email: String,
    val tier: AccountTier = AccountTier.Free,
    val enabled: Boolean = true,
    val alpineNamespace: String,
    val pkceAuthUrl: String = "",
    val authenticatedAtIso: String = ""
)

@Serializable
data class AlpineContainerState(
    val isBooted: Boolean = false,
    val isUpdating: Boolean = false,
    val distroVersion: String = "Alpine Linux 3.20.3 (aarch64)",
    val colabCliVersion: String = "0.4.2",
    val tmuxVersion: String = "3.4-r1",
    val opensshVersion: String = "9.7_p1-r4",
    val bootLogs: List<String> = emptyList()
)

@Serializable
data class GithubAsset(
    val id: Long,
    val name: String,
    val sizeBytes: Long,
    val downloadUrl: String,
    val isApk: Boolean
)

@Serializable
data class GithubReleaseInfo(
    val id: Long,
    val tagName: String,
    val name: String,
    val publishedAt: String,
    val prerelease: Boolean,
    val htmlUrl: String,
    val changelogBullets: List<String>,
    val assets: List<GithubAsset>
)
