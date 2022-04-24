package xyz.quaver.pupil.sources.hitomi

import android.util.Log
import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.util.*
import kotlinx.coroutines.delay

internal object HitomiPlugin: HttpClientPlugin<Unit, HitomiPlugin> {
    override val key = AttributeKey<HitomiPlugin>("HitomiPlugin")

    override fun prepare(block: Unit.() -> Unit) = this

    override fun install(plugin: HitomiPlugin, scope: HttpClient) {
        scope.requestPipeline.intercept(HttpRequestPipeline.State) {
            if (context.url.host.endsWith("hitomi.la")) context.header("Referer", "https://hitomi.la/")
        }

        scope.plugin(HttpSend).intercept { request ->
            var call = execute(request)

            val url = request.url

            if (url.host.endsWith("a.hitomi.la")) {
                while (call.response.status == HttpStatusCode.ServiceUnavailable) {
                    delay(1000)
                    call = execute(request)
                }
            }

            call
        }
    }
}