package xyz.quaver.pupil.sources.hitomi

import android.util.Log
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.util.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.math.abs

private val String.isImageServer: Boolean
    get() = this.endsWith("a.hitomi.la")

internal object HitomiPlugin: HttpClientPlugin<Unit, HitomiPlugin> {
    override val key = AttributeKey<HitomiPlugin>("HitomiPlugin")

    private val viewerMutex = Mutex()

    private var viewerImages: Map<String, Int>? = null
    private var viewerPosition: Int = 0

    fun setViewerImages(images: List<String>?) = CoroutineScope(Dispatchers.Unconfined).launch {
        viewerMutex.withLock {
            viewerImages = buildMap {
                images?.forEachIndexed { index, image ->
                    put(image, index)
                }
            }
            viewerPosition = 0
        }
    }

    fun setViewerPosition(position: Int) = CoroutineScope(Dispatchers.Unconfined).launch {
        viewerMutex.withLock {
            viewerPosition = position
        }
    }

    private val callPriority: (String) -> Int = callPriority@{ image ->
        val index = viewerImages?.get(image) ?: return@callPriority Int.MAX_VALUE

        abs(viewerPosition - index)
    }

    private val callMutex = Mutex()
    private val readyCalls = mutableMapOf<String, Continuation<Unit>>()
    private val runningCalls = mutableSetOf<String>()
    private const val maxRunningCall = 5

    private fun promoteAndExecute() {
        if (runningCalls.size >= maxRunningCall) return

        val nextCalls = readyCalls.keys.sortedBy(callPriority).take(maxRunningCall - runningCalls.size)

        nextCalls.forEach { call ->
            val cont = readyCalls.remove(call) ?: error("Missing continuation")
            runningCalls.add(call)
            cont.resume(Unit)
        }
    }

    private suspend fun Sender.enqueue(request: HttpRequestBuilder): HttpClientCall {
        val url = request.url.buildString()

        callMutex.lock()
        suspendCoroutine<Unit> { cont ->
            readyCalls[url] = cont
            promoteAndExecute()
            callMutex.unlock()
        }

        val call = execute(request)

        callMutex.withLock {
            runningCalls.remove(url)
            promoteAndExecute()
        }

        return call
    }

    override fun prepare(block: Unit.() -> Unit) = this

    private fun installHeaderPlugin(client: HttpClient) {
        client.requestPipeline.intercept(HttpRequestPipeline.State) {
            if (context.url.host.endsWith("hitomi.la")) context.header("Referer", "https://hitomi.la/")
        }
    }

    private fun installRetryPlugin(client: HttpClient) {
        client.plugin(HttpSend).intercept { request ->
            var call = execute(request)

            val url = request.url

            if (url.host.isImageServer) {
                while (call.response.status == HttpStatusCode.ServiceUnavailable) {
                    delay(1000)
                    call = execute(request)
                }
            }

            call
        }
    }

    private fun installPriorityPlugin(client: HttpClient) {
        client.plugin(HttpSend).intercept { request ->
            if (request.url.host.isImageServer) enqueue(request) else execute(request)
        }
    }

    override fun install(plugin: HitomiPlugin, scope: HttpClient) {
        installHeaderPlugin(scope)
        installRetryPlugin(scope)
        installPriorityPlugin(scope)
    }
}