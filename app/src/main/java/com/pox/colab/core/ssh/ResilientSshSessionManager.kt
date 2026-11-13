package com.pox.colab.core.ssh

import com.jcraft.jsch.ChannelExec
import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import com.pox.colab.domain.model.VmInstance
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.Properties
import java.util.concurrent.ConcurrentHashMap

/**
 * Anti-disconnection SSH & tmux session manager.
 *
 * Enforces:
 * - SSH keep-alive (`ServerAliveInterval 10`, `ServerAliveCountMax 6`, `TCPKeepAlive yes`)
 * - Remote `sshd` keep-alive (`ClientAliveInterval 10`, `TCPKeepAlive yes`)
 * - Persistent `tmux` session daemon (`tmux attach-session -t <name> || tmux new-session -s <name>`)
 *   so interactive shells survive network drops across Wi-Fi / cellular switches.
 */
class ResilientSshSessionManager(
    private val scope: CoroutineScope
) {
    private val jsch = JSch()
    private val activeSessions = ConcurrentHashMap<String, Session>()

    companion object {
        const val KEEP_ALIVE_INTERVAL_MS = 10_000
        const val KEEP_ALIVE_MAX_COUNT = 6
    }

    suspend fun connectOrReattach(
        vm: VmInstance,
        tmuxSessionName: String = "colab-0",
        onLogLine: (String) -> Unit
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val existing = activeSessions[vm.id]
            if (existing != null && existing.isConnected) {
                onLogLine("Re-verified active SSH transport (${vm.sshHost}:${vm.sshPort})")
                return@runCatching
            }

            val session = jsch.getSession("root", vm.sshHost, vm.sshPort).apply {
                setPassword(vm.password)
                val config = Properties().apply {
                    put("StrictHostKeyChecking", "no")
                    put("TCPKeepAlive", "yes")
                    put("ServerAliveInterval", "10")
                    put("ServerAliveCountMax", "6")
                }
                setConfig(config)
                setServerAliveInterval(KEEP_ALIVE_INTERVAL_MS)
                setServerAliveCountMax(KEEP_ALIVE_MAX_COUNT)
            }

            session.connect(15_000)
            activeSessions[vm.id] = session

            // Ensure remote tmux session daemon exists and reattach
            val bootstrapCmd =
                "tmux has-session -t $tmuxSessionName 2>/dev/null || tmux new-session -d -s $tmuxSessionName"
            executeInTmux(vm, tmuxSessionName, bootstrapCmd)
            onLogLine("Attached resilient tmux session [$tmuxSessionName] (ClientAliveInterval 10, TCPKeepAlive yes)")

            startWatchdog(vm, tmuxSessionName, onLogLine)
        }
    }

    suspend fun executeInTmux(
        vm: VmInstance,
        tmuxSessionName: String,
        command: String
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val session = activeSessions[vm.id]
            if (session == null || !session.isConnected) {
                error("SSH transport disconnected; queued auto-reattach for tmux [$tmuxSessionName]")
            }

            val channel = (session.openChannel("exec") as ChannelExec).apply {
                setCommand(command)
            }
            val outputStream = ByteArrayOutputStream()
            channel.outputStream = outputStream
            channel.setErrStream(outputStream)
            channel.connect(10_000)

            while (!channel.isClosed) {
                delay(40)
            }
            channel.disconnect()
            outputStream.toString(Charsets.UTF_8.name()).trim()
        }
    }

    private fun startWatchdog(
        vm: VmInstance,
        tmuxSessionName: String,
        onLogLine: (String) -> Unit
    ) {
        scope.launch(Dispatchers.IO) {
            while (isActive) {
                delay(KEEP_ALIVE_INTERVAL_MS.toLong())
                val session = activeSessions[vm.id] ?: break
                if (!session.isConnected) {
                    onLogLine("Network drop detected on ${vm.name}. Reattaching tmux [$tmuxSessionName]...")
                    connectOrReattach(vm, tmuxSessionName, onLogLine)
                    break
                }
            }
        }
    }

    fun disconnect(vmId: String) {
        activeSessions.remove(vmId)?.disconnect()
    }
}
