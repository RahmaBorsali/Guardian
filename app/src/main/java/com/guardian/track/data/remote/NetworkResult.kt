package com.guardian.track.data.remote

/**
 * Sealed class modeling network operation results.
 * Provides type-safe state management for loading, success, and error states.
 * Required by specification for Retrofit response handling.
 */
sealed class NetworkResult<T> {
    data class Success<T>(val data: T) : NetworkResult<T>()
    data class Error<T>(val message: String, val code: Int? = null) : NetworkResult<T>()
    class Loading<T> : NetworkResult<T>()
}
