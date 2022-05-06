package xyz.quaver.pupil.sources.manatoki.networking

import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.http.*
import io.ktor.util.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow

object ManatokiRateLimiter : HttpClientPlugin<Unit, ManatokiRateLimiter> {

    private val rateLimiter = MutableSharedFlow<Unit>()

    init {
        CoroutineScope(Dispatchers.Unconfined).launch {
            rateLimiter.collect {
                delay(1000)
            }
        }
    }

    override fun install(plugin: ManatokiRateLimiter, scope: HttpClient) {
        scope.plugin(HttpSend).intercept { request ->
            if (request.url.encodedPathSegments.getOrNull(1) == "comic") rateLimiter.emit(Unit)
            execute(request)
        }
    }

    override val key = AttributeKey<ManatokiRateLimiter>("ManatokiRateLimiter")
    override fun prepare(block: Unit.() -> Unit) = this

}