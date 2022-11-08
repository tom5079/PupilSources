package xyz.quaver.pupil.sources.manatoki.networking

import io.ktor.client.plugins.cookies.*
import io.ktor.http.*
import io.ktor.util.*
import io.ktor.util.date.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import xyz.quaver.pupil.sources.manatoki.CookieDao
import xyz.quaver.pupil.sources.manatoki.CookieEntry
import xyz.quaver.pupil.sources.manatoki.ExtensionMap
import xyz.quaver.pupil.sources.manatoki.ManatokiDatabase

class ManatokiCookiesStorage(
    private val cookieDao: CookieDao
) : CookiesStorage {
    private val mutex = Mutex()
    private var oldestCookie: Long? = null

    override suspend fun addCookie(requestUrl: Url, cookie: Cookie) {
        mutex.withLock {
            if (cookie.name.isBlank()) return

            val requestedPath = let {
                val pathInRequest = requestUrl.encodedPath
                if (pathInRequest.endsWith('/')) pathInRequest else "$pathInRequest/"
            }

            with (cookie.fillDefaults(requestUrl)) {
                cookieDao.removeDuplicates(
                    name,
                    requestUrl.host.toLowerCasePreservingASCIIRules(),
                    hostIsIp(requestUrl.host),
                    requestedPath,
                    requestUrl.protocol.isSecure()
                )
                cookieDao.addCookie(CookieEntry(
                    requestUrl.toString(),
                    name, value, encoding, maxAge, expires,
                    domain, path, secure, httpOnly, ExtensionMap(extensions)
                ))
            }

            if (oldestCookie == null) oldestCookie = cookieDao.oldestCookie()

            cookie.expires?.timestamp?.let { expires ->
                if (oldestCookie == null || oldestCookie!! > expires) {
                    oldestCookie = expires
                }
            }
        }
    }

    override suspend fun get(requestUrl: Url): List<Cookie> = mutex.withLock {
        if (oldestCookie == null) oldestCookie = cookieDao.oldestCookie()

        val date = GMTDate()
        if (oldestCookie != null && date.timestamp >= oldestCookie!!) {
            cookieDao.cleanup(date.timestamp)
            oldestCookie = cookieDao.oldestCookie()
        }

        val requestedPath = let {
            val pathInRequest = requestUrl.encodedPath
            if (pathInRequest.endsWith('/')) pathInRequest else "$pathInRequest/"
        }

        cookieDao.get(
            requestUrl.host.toLowerCasePreservingASCIIRules(),
            hostIsIp(requestUrl.host),
            requestedPath,
            requestUrl.protocol.isSecure()
        ).map { entry ->
            with (entry) {
                Cookie(
                    name, value, encoding, maxAge, expires,
                    domain, path, secure, httpOnly, extensions.extensions
                )
            }
        }
    }

    override fun close() = Unit
}

internal fun Cookie.fillDefaults(requestUrl: Url): Cookie {
    val domain = (if (domain.isNullOrBlank()) requestUrl.host else domain)
        ?.toLowerCasePreservingASCIIRules()

    val path =
        (if (path?.startsWith("/") != true) requestUrl.encodedPath else path)!!.let { path ->
            if (path.endsWith('/')) path else "$path/"
        }

    return this.copy(domain = domain, path = path)
}
