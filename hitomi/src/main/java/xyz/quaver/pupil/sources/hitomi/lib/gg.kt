package xyz.quaver.pupil.sources.hitomi.lib

import android.webkit.WebView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import org.kodein.di.DIAware
import org.kodein.di.instance

private suspend inline fun <reified T> WebView.evaluate(script: String, timeoutMillis: Long = 1000): T {
    var result: T? = null

    withTimeout(timeoutMillis) {
        while (progress != 100) yield()

        evaluateJavascript(script) {
            result = when (T::class) {
                Int::class -> it.toInt()
                String::class -> it.replace("\"", "")
                else -> error("Unsupported type ${T::class}")
            } as T
        }

        while (result == null) yield()
    }

    return result!!
}

suspend fun DIAware.gg_m(g: Int): Int = withContext(Dispatchers.Main) {
    val webView: WebView by instance()
    webView.evaluate("gg.m($g)")
}

suspend fun DIAware.gg_b(): String = withContext(Dispatchers.Main) {
    val webView: WebView by instance()
    webView.evaluate("gg.b")
}

suspend fun DIAware.gg_s(h: String): String = withContext(Dispatchers.Main) {
    val webView: WebView by instance()
    webView.evaluate("gg.s('$h')")
}