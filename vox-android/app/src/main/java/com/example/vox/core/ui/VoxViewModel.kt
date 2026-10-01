package com.example.vox.core.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.vox.app.AppContainer
import com.example.vox.app.VoxApplication

/** Creates a ViewModel with access to the app's dependencies and its navigation arguments. */
@Composable
inline fun <reified VM : ViewModel> voxViewModel(
    crossinline create: AppContainer.(SavedStateHandle) -> VM,
): VM = viewModel(
    factory = viewModelFactory {
        initializer { (this[APPLICATION_KEY] as VoxApplication).container.create(createSavedStateHandle()) }
    },
)
