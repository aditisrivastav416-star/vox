package com.example.vox.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vox.domain.model.UserProfile
import com.example.vox.domain.repository.UserRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileSetupUiState(
    val name: String = "",
    val currencyCode: String = defaultCurrency(),
    val phone: String = "",
    val photoUri: String? = null,
    val showNameError: Boolean = false,
    val currencies: List<String> = SupportedCurrencies,
)

sealed interface AuthAction {
    /** Sign-in is simulated: every entry point leads into the app. */
    data object SkipSignIn : AuthAction
    data class NameChanged(val value: String) : AuthAction
    data class CurrencyChanged(val code: String) : AuthAction
    data class PhoneChanged(val value: String) : AuthAction
    data class PhotoPicked(val uri: String) : AuthAction
    data object Save : AuthAction
}

sealed interface AuthEvent {
    data object SignedIn : AuthEvent
}

internal val SupportedCurrencies = listOf("INR", "USD", "EUR", "GBP", "AED", "SGD", "AUD", "CAD", "JPY")

/** India-first default (PRD); the user picks another during setup or in settings. */
private fun defaultCurrency(): String = "INR"

class AuthViewModel(private val userRepository: UserRepository) : ViewModel() {
    private val _state = MutableStateFlow(ProfileSetupUiState())
    val state: StateFlow<ProfileSetupUiState> = _state.asStateFlow()

    private val _events = Channel<AuthEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun onAction(action: AuthAction) {
        when (action) {
            AuthAction.SkipSignIn -> save(UserProfile(name = "", currencyCode = _state.value.currencyCode))
            is AuthAction.NameChanged -> _state.update { it.copy(name = action.value, showNameError = false) }
            is AuthAction.CurrencyChanged -> _state.update { it.copy(currencyCode = action.code) }
            is AuthAction.PhoneChanged -> _state.update { it.copy(phone = action.value) }
            is AuthAction.PhotoPicked -> _state.update { it.copy(photoUri = action.uri) }
            AuthAction.Save -> {
                val s = _state.value
                if (s.name.isBlank()) {
                    _state.update { it.copy(showNameError = true) }
                } else {
                    save(UserProfile(s.name.trim(), s.currencyCode, s.phone.trim().ifBlank { null }))
                }
            }
        }
    }

    private fun save(profile: UserProfile) = viewModelScope.launch {
        userRepository.saveProfile(profile)
        _events.send(AuthEvent.SignedIn)
    }
}
