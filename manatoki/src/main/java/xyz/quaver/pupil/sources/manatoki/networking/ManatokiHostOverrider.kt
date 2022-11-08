package xyz.quaver.pupil.sources.manatoki.networking

import android.util.Log
import androidx.compose.animation.scaleOut
import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.util.*
import java.net.URL

object ManatokiHostOverrider : HttpClientPlugin<Unit, ManatokiHostOverrider> {

    private var host: String? = null

    override fun install(plugin: ManatokiHostOverrider, scope: HttpClient) {
        scope.plugin(HttpSend).intercept { request ->
            if (host == null) {
                val r = execute(request {
                    url("https://manatoki.net")
                }).response

                if (r.status == HttpStatusCode.Found) {
                    host = URL(r.headers["location"]).host
                }
            }

            host?.let { host ->
                request.host = host
            }

            execute(request)
        }
    }

    override val key = AttributeKey<ManatokiHostOverrider>("ManatokiHostOverrider")
    override fun prepare(block: Unit.() -> Unit) = this

}