package com.ronin.ai.core.device

import android.content.Context
import com.ronin.ai.core.domain.repository.ExperienceRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Global crash protection: uncaught exceptions are recorded into the
 * experience system (so RONIN "learns" from crashes) before the default
 * handler terminates the process. The app never shows a raw stack trace.
 */
@Singleton
class CrashGuard @Inject constructor(
    @ApplicationContext private val context: Context,
    private val experienceRepository: ExperienceRepository
) {

    @Volatile
    private var installed = false

    fun install() {
        // Guard against double-installation wrapping the handler twice.
        if (installed) return
        installed = true

        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching {
                // The process is about to die, so the write must be synchronous
                // — but it is bounded: a hung database must not stop the
                // platform's crash handling (which would surface as an ANR
                // instead of a clean crash).
                runBlocking {
                    withTimeout(CRASH_WRITE_TIMEOUT_MS) {
                        experienceRepository.recordError(
                            title = "Unexpected crash",
                            detail = throwable.message ?: throwable.javaClass.simpleName,
                            context = throwable.stackTraceToString().take(400)
                        )
                    }
                }
            }.onFailure { failure ->
                if (failure !is TimeoutCancellationException) {
                    // Nothing else we can do — never mask the original crash.
                    failure.printStackTrace()
                }
            }
            // Always hand the original failure to the platform handler.
            previous?.uncaughtException(thread, throwable)
        }
    }

    private companion object {
        const val CRASH_WRITE_TIMEOUT_MS = 1_500L
    }
}
