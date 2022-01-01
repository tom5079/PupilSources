package xyz.quaver.pupil.sources.base.util

import com.google.common.util.concurrent.RateLimiter
import kotlinx.coroutines.yield

suspend fun RateLimiter.withRateLimit(block: suspend () -> Unit) {
    while (!tryAcquire()) yield()

    block()
}