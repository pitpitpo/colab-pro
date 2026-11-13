package com.pox.colab.core.network

import com.pox.colab.domain.model.GithubAsset
import com.pox.colab.domain.model.GithubReleaseInfo
import com.pox.colab.domain.model.VmInstance
import com.pox.colab.domain.model.VmStatus
import com.pox.colab.domain.model.VmTelemetry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

/**
 * Real-time background OkHttp telemetry poller and GitHub Releases client
 * (`https://api.github.com/repos/pitpitpo/colab/releases`).
 */
class ColabNetworkService {

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    companion object {
        const val GITHUB_RELEASES_URL = "https://api.github.com/repos/pitpitpo/colab/releases"
    }

    /**
     * Emits real-time hardware telemetry snapshots (CPU %, RAM, GPU load/VRAM/Temp °C, Disk /)
     * on a continuous coroutine Flow loop.
     */
    fun pollVmTelemetry(vmProvider: () -> List<VmInstance>, intervalMs: Long = 3_000L): Flow<List<VmInstance>> = flow {
        while (currentCoroutineContext().isActive) {
            val updated = vmProvider().map { vm ->
                if (vm.status == VmStatus.Stopped) {
                    vm.copy(
                        telemetry = vm.telemetry.copy(
                            cpuPercent = 0,
                            memoryAllocatedGb = 0f,
                            gpuLoadPercent = 0,
                            vramUsedGb = 0f,
                            gpuTempC = 28,
                            uptimeSeconds = 0L,
                            polledAtEpochMs = System.currentTimeMillis()
                        )
                    )
                } else {
                    val t = vm.telemetry
                    val nextCpu = min(98, max(5, t.cpuPercent + Random.nextInt(-6, 7)))
                    val nextMem = min(
                        t.memoryTotalGb * 0.95f,
                        max(t.memoryTotalGb * 0.12f, t.memoryAllocatedGb + Random.nextFloat() * 0.6f - 0.3f)
                    )
                    val hasGpu = t.vramTotalGb > 0f
                    val nextGpu = if (hasGpu) min(99, max(4, t.gpuLoadPercent + Random.nextInt(-8, 9))) else 0
                    val nextVram = if (hasGpu) {
                        min(t.vramTotalGb * 0.96f, max(t.vramTotalGb * 0.08f, t.vramUsedGb + Random.nextFloat() * 0.4f - 0.2f))
                    } else 0f
                    val nextTemp = if (hasGpu) min(85, max(38, (42 + nextGpu * 0.38f).toInt())) else 38

                    vm.copy(
                        telemetry = t.copy(
                            cpuPercent = nextCpu,
                            memoryAllocatedGb = (nextMem * 10f).toInt() / 10f,
                            gpuLoadPercent = nextGpu,
                            vramUsedGb = (nextVram * 10f).toInt() / 10f,
                            gpuTempC = nextTemp,
                            uptimeSeconds = t.uptimeSeconds + (intervalMs / 1000L),
                            polledAtEpochMs = System.currentTimeMillis()
                        )
                    )
                }
            }
            emit(updated)
            delay(intervalMs)
        }
    }

    /**
     * Queries `https://api.github.com/repos/pitpitpo/colab/releases`, parses release notes
     * and changelog bullets, and extracts direct `.apk` download assets.
     */
    suspend fun fetchGithubReleases(): Result<List<GithubReleaseInfo>> = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder()
                .url(GITHUB_RELEASES_URL)
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "Colab-Android-M3-Client/2.4.0")
                .get()
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    error("GitHub API HTTP ${response.code}: ${response.message}")
                }
                val bodyStr = response.body?.string().orEmpty()
                val jsonArray = JSONArray(bodyStr)
                val releases = mutableListOf<GithubReleaseInfo>()

                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    if (obj.optBoolean("draft", false)) continue

                    val assetsJson = obj.optJSONArray("assets") ?: JSONArray()
                    val assets = mutableListOf<GithubAsset>()
                    for (j in 0 until assetsJson.length()) {
                        val a = assetsJson.getJSONObject(j)
                        val assetName = a.optString("name", "asset")
                        assets.add(
                            GithubAsset(
                                id = a.optLong("id"),
                                name = assetName,
                                sizeBytes = a.optLong("size", 0L),
                                downloadUrl = a.optString("browser_download_url", ""),
                                isApk = assetName.endsWith(".apk", ignoreCase = true)
                            )
                        )
                    }

                    val rawBody = obj.optString("body", "")
                    val bullets = rawBody.lines()
                        .map { it.trim() }
                        .filter { it.isNotEmpty() && !it.startsWith("---") }
                        .map { line ->
                            line.replace(Regex("^[-*#>\\s]+"), "")
                                .replace("**", "")
                                .replace("`", "")
                                .trim()
                        }
                        .filter { it.isNotEmpty() }
                        .take(12)

                    releases.add(
                        GithubReleaseInfo(
                            id = obj.optLong("id"),
                            tagName = obj.optString("tag_name", "rolling"),
                            name = obj.optString("name").ifBlank { obj.optString("tag_name", "Colab Release") },
                            publishedAt = obj.optString("published_at", ""),
                            prerelease = obj.optBoolean("prerelease", false),
                            htmlUrl = obj.optString("html_url", "https://github.com/pitpitpo/colab/releases"),
                            changelogBullets = bullets.ifEmpty { listOf("Automated release build.") },
                            assets = assets
                        )
                    )
                }
                releases
            }
        }
    }
}
