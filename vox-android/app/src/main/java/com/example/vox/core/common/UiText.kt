package com.example.vox.core.common

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

/** Text produced outside Compose (e.g. ViewModels) without touching Android resources there. */
sealed interface UiText {
    data class Res(@param:StringRes val id: Int, val args: List<Any> = emptyList()) : UiText

    @Composable
    fun asString(): String = when (this) {
        is Res -> stringResource(id, *args.toTypedArray())
    }
}
