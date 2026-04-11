package com.xxyangyoulin.jbforum

import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object CodeMetadataScraper {
    enum class Status { PENDING, RUNNING, SUCCESS, FAILED }

    private const val MAX_PARALLEL = 2
    private const val SCRAPE_TIMEOUT_MS = 25_000L

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val queueMutex = Mutex()
    private val inFlight = ConcurrentHashMap.newKeySet<String>()
    private val queued = ConcurrentHashMap.newKeySet<String>()
    private val taskQueue = ArrayDeque<ScrapeTask>()
    private val _statuses = MutableStateFlow<Map<String, Status>>(emptyMap())
    val statuses: StateFlow<Map<String, Status>> = _statuses.asStateFlow()
    private var activeWorkers = 0

    private data class ScrapeTask(
        val code: String,
        val server: String,
        val token: String
    )

    fun enqueue(codes: Set<String>, server: String, token: String) {
        if (server.isBlank() || codes.isEmpty()) return
        scope.launch {
            queueMutex.withLock {
                codes.forEach { raw ->
                    val code = raw.trim().uppercase()
                    if (code.isBlank()) return@forEach
                    if (inFlight.contains(code) || queued.contains(code)) return@forEach
                    queued.add(code)
                    taskQueue.addLast(ScrapeTask(code = code, server = server, token = token))
                    _statuses.update { it + (code to Status.PENDING) }
                }
                while (activeWorkers < MAX_PARALLEL && taskQueue.isNotEmpty()) {
                    activeWorkers += 1
                    scope.launch { runWorker() }
                }
            }
        }
    }

    fun clearStatuses() {
        scope.launch {
            queueMutex.withLock {
                inFlight.clear()
                queued.clear()
                taskQueue.clear()
                _statuses.value = emptyMap()
            }
        }
    }

    private suspend fun runWorker() {
        try {
            while (true) {
                val task = queueMutex.withLock {
                    val next = taskQueue.removeFirstOrNull()
                    if (next != null) {
                        queued.remove(next.code)
                        inFlight.add(next.code)
                        _statuses.update { it + (next.code to Status.RUNNING) }
                    }
                    next
                } ?: break
                processTask(task)
            }
        } finally {
            queueMutex.withLock {
                activeWorkers = (activeWorkers - 1).coerceAtLeast(0)
                while (activeWorkers < MAX_PARALLEL && taskQueue.isNotEmpty()) {
                    activeWorkers += 1
                    scope.launch { runWorker() }
                }
            }
        }
    }

    private suspend fun processTask(task: ScrapeTask) {
        val timeoutJob = scope.launch {
            delay(SCRAPE_TIMEOUT_MS)
            if (inFlight.remove(task.code)) {
                _statuses.update { it + (task.code to Status.FAILED) }
            }
        }
        try {
            val metadata = MetaTubeApiClient.fetchCodeMetadata(task.server, task.token, task.code)
            if (!inFlight.contains(task.code)) return
            if (metadata != null) {
                val cachedCoverUrl = MetaTubeImageCache.cache(metadata.coverUrl, task.token)
                val cachedBackdropUrl = MetaTubeImageCache.cache(
                    metadata.backdropUrl,
                    task.token
                )
                LocalCodeMetadataStore.upsert(
                    metadata.copy(
                        code = metadata.code.trim().uppercase(),
                        coverUrl = cachedCoverUrl,
                        backdropUrl = cachedBackdropUrl
                    )
                )
                _statuses.update { it + (task.code to Status.SUCCESS) }
            } else {
                _statuses.update { it + (task.code to Status.FAILED) }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Throwable) {
            _statuses.update { it + (task.code to Status.FAILED) }
        } finally {
            timeoutJob.cancel()
            inFlight.remove(task.code)
        }
    }
}
