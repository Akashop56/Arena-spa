package com.ronin.ai.core.common

/**
 * Simple, explicit result wrapper used across use cases and repositories.
 * Keeps failure reasons user-readable instead of relying on exceptions.
 */
sealed class Outcome<out T> {
    data class Success<T>(val value: T) : Outcome<T>()
    data class Failure(val message: String, val cause: Throwable? = null) : Outcome<Nothing>()

    fun getOrNull(): T? = (this as? Success)?.value
    fun failureOrNull(): Failure? = this as? Failure
}

inline fun <T> runOutcome(block: () -> T): Outcome<T> = try {
    Outcome.Success(block())
} catch (t: Throwable) {
    Outcome.Failure(t.message ?: t.javaClass.simpleName, t)
}

suspend inline fun <T> runOutcomeSuspend(crossinline block: suspend () -> T): Outcome<T> = try {
    Outcome.Success(block())
} catch (t: Throwable) {
    Outcome.Failure(t.message ?: t.javaClass.simpleName, t)
}

fun <T> Outcome<T>.onSuccess(block: (T) -> Unit): Outcome<T> {
    if (this is Outcome.Success) block(value)
    return this
}

fun <T> Outcome<T>.onFailure(block: (Outcome.Failure) -> Unit): Outcome<T> {
    if (this is Outcome.Failure) block(this)
    return this
}
