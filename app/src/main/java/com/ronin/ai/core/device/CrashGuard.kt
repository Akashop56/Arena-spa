package com.ronin.ai.core.device

import android.content.Context
import com.ronin.ai.core.domain.repository.ExperienceRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.runBlocking
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

    fun install() {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching {
                runBlocking {
                    experienceRepository.recordError(
                        title = "Unexpected crash",
                        detail = throwable.message ?: throwable.javaClass.simpleName,
                        context = throwable.stackTraceToString().take(400)
                    )
                }
            }
            previous?.uncaughtException(thread, throwable)
        }
    }
}
