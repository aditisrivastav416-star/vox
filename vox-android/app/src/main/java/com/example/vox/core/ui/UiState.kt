package com.example.vox.core.ui

import com.example.vox.R
import com.example.vox.core.common.UiText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: UiText) : UiState<Nothing>
}

/** Standard ViewModel pipeline: Loading → Success, or Error if the source fails. */
fun <T> Flow<T>.stateInUi(scope: CoroutineScope): StateFlow<UiState<T>> =
    map<T, UiState<T>> { UiState.Success(it) }
        .catch { emit(UiState.Error(UiText.Res(R.string.error_generic))) }
        .stateIn(scope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)
