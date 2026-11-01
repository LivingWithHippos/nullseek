package com.github.livingwithhippos.nullseek.core.domain

sealed interface DebridResult<out T> {
    data class Ok<T>(
        val value: T,
    ) : DebridResult<T>

    data class Err(
        val error: DebridError,
    ) : DebridResult<Nothing>
}

sealed interface DebridError {
    data object InvalidToken : DebridError

    /** usually a feature reserved to premium accounts */
    data object PermissionDenied : DebridError

    data object AccountLocked : DebridError

    data object RateLimited : DebridError

    data object FairUsage : DebridError

    data class HosterProblem(
        val reason: String,
    ) : DebridError

    data object NotFound : DebridError

    /** the active provider does not support this operation */
    data object Unsupported : DebridError

    data class Network(
        val cause: Throwable,
    ) : DebridError

    data class Server(
        val code: Int?,
        val message: String?,
    ) : DebridError

    data class Serialization(
        val cause: Throwable,
    ) : DebridError
}

fun DebridError.describe(subject: String): String =
    when (this) {
        DebridError.InvalidToken -> "$subject: Invalid or expired token"
        DebridError.PermissionDenied -> "$subject: Permission denied (premium needed?)"
        DebridError.AccountLocked -> "$subject: Account locked"
        DebridError.RateLimited -> "$subject: Too many requests, slow down"
        DebridError.FairUsage -> "$subject: Fair usage limit reached"
        is DebridError.HosterProblem -> "$subject: Hoster error - $reason"
        DebridError.NotFound -> "$subject: Not found"
        DebridError.Unsupported -> "$subject: Not supported by this provider"
        is DebridError.Network -> "$subject: Network Error - $cause"
        is DebridError.Server -> "$subject: Server Error${code?.let { " $it" } ?: ""}${message?.let { " - $it" } ?: ""}"
        is DebridError.Serialization -> "$subject: Serialization Error - $cause"
    }

inline fun <T, R> DebridResult<T>.map(transform: (T) -> R): DebridResult<R> =
    when (this) {
        is DebridResult.Ok -> DebridResult.Ok(transform(value))
        is DebridResult.Err -> this
    }

fun <T> DebridResult<T>.getOrNull(): T? = (this as? DebridResult.Ok)?.value

val DebridResult<*>.isOk: Boolean get() = this is DebridResult.Ok
