package xyz.quaver.pupil.sources.manatoki.networking

import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.http.*
import io.ktor.util.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume

object ManatokiCaptcha: HttpClientPlugin<Unit, ManatokiCaptcha> {
    private val captchaMutex = Mutex()
    private val captchaCont = mutableListOf<Continuation<Unit>>()

    private val _captchaRequestFlow = MutableStateFlow(false)
    val captchaRequestFlow = _captchaRequestFlow as StateFlow<Boolean>

    override val key = AttributeKey<ManatokiCaptcha>("ManatokiCaptcha")

    override fun prepare(block: Unit.() -> Unit) = this

    override fun install(plugin: ManatokiCaptcha, scope: HttpClient) {
        scope.plugin(HttpSend).intercept { request ->
            var call = execute(request)

            while (
                request.url.pathSegments.last() != "captcha.php" &&
                call.response.headers[HttpHeaders.Location]?.contains("captcha") == true
            ) {
                captchaMutex.lock()
                _captchaRequestFlow.emit(true)
                suspendCancellableCoroutine<Unit> { cont ->
                    captchaCont.add(cont)
                    captchaMutex.unlock()
                }
                call = execute(request)
            }

            call
        }
    }

    suspend fun resume() {
        captchaMutex.withLock {
            _captchaRequestFlow.emit(false)
            captchaCont.forEach {
                it.resume(Unit)
            }
            captchaCont.clear()
        }
    }
}