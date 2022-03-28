package xyz.quaver.pupil.sources.hitomi

import android.util.Log
import io.ktor.client.*
import io.ktor.client.features.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.util.*
import kotlinx.coroutines.delay


internal object HitomiPlugin: HttpClientFeature<Unit, HitomiPlugin> {
    override val key = AttributeKey<HitomiPlugin>("HitomiPlugin")

    override fun prepare(block: Unit.() -> Unit) = this

    override fun install(feature: HitomiPlugin, scope: HttpClient) {
        scope.requestPipeline.intercept(HttpRequestPipeline.State) {
            if (context.url.host.endsWith("hitomi.la")) context.header("Referer", "https://hitomi.la/")
        }

        scope[HttpSend].intercept { original, context ->
            val url = original.request.url
            val status = original.response.status

            if (url.host.endsWith("a.hitomi.la") && status == HttpStatusCode.ServiceUnavailable) {
                delay(1000)
                execute(context)
            } else original
        }
    }
}