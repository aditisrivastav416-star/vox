package com.example.vox.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vox.domain.model.UserProfile
import com.example.vox.domain.repository.UserRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
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
    val stage: SignupStage = SignupStage.LOGIN,
    val identifier: String = "",
    val code: String = "",
    val error: String? = null,
    val busy: Boolean = false,
)

enum class SignupStage { LOGIN, CONTACT, CODE, PASSWORD }

sealed interface AuthAction {
    data class Login(val identifier: String, val password: String) : AuthAction
    data object StartSignup : AuthAction
    data class IdentifierChanged(val value: String) : AuthAction
    data class CodeChanged(val value: String) : AuthAction
    data object RequestCode : AuthAction
    data object VerifyCode : AuthAction
    data class FinishSignup(val password: String, val confirmPassword: String) : AuthAction
    data class NameChanged(val value: String) : AuthAction
    data class CurrencyChanged(val code: String) : AuthAction
    data class PhotoPicked(val uri: String) : AuthAction
    data object Save : AuthAction
}

sealed interface AuthEvent {
    data object SignedIn : AuthEvent
    data object Registered : AuthEvent
}

internal val SupportedCurrencies = listOf("INR", "USD", "EUR", "GBP", "AED", "SGD", "AUD", "CAD", "JPY")

/** India-first default (PRD); the user picks another during setup or in settings. */
private fun defaultCurrency(): String = "INR"

class AuthViewModel(private val userRepository: UserRepository) : ViewModel() {
    private val _state = MutableStateFlow(ProfileSetupUiState())
    val state: StateFlow<ProfileSetupUiState> = _state.asStateFlow()

    private val _events = Channel<AuthEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            userRepository.observeProfile().first { it != null }?.let { profile ->
                _state.update { it.copy(name = profile.name, currencyCode = profile.currencyCode, phone = profile.phone.orEmpty()) }
            }
        }
    }

    fun onAction(action: AuthAction) {
        when (action) {
            is AuthAction.Login -> runAuth { userRepository.login(action.identifier, action.password); _events.send(AuthEvent.SignedIn) }
            AuthAction.StartSignup -> _state.update { it.copy(stage = SignupStage.CONTACT, error = null) }
            is AuthAction.IdentifierChanged -> _state.update { it.copy(identifier = action.value, error = null) }
            is AuthAction.CodeChanged -> _state.update { it.copy(code = action.value, error = null) }
            AuthAction.RequestCode -> runAuth {
                userRepository.startSignup(_state.value.identifier)
                _state.update { it.copy(stage = SignupStage.CODE) }
            }
            AuthAction.VerifyCode -> runAuth {
                userRepository.verifySignup(_state.value.identifier, _state.value.code)
                _state.update { it.copy(stage = SignupStage.PASSWORD) }
            }
            is AuthAction.FinishSignup -> runAuth {
                require(action.password == action.confirmPassword) { "Passwords do not match" }
                userRepository.finishSignup(_state.value.identifier, _state.value.code, action.password, action.confirmPassword)
                _events.send(AuthEvent.Registered)
            }
            is AuthAction.NameChanged -> _state.update { it.copy(name = action.value, showNameError = false) }
            is AuthAction.CurrencyChanged -> _state.update { it.copy(currencyCode = action.code) }
            is AuthAction.PhotoPicked -> _state.update { it.copy(photoUri = action.uri) }
            AuthAction.Save -> {
                val s = _state.value
                if (s.name.isBlank()) {
                    _state.update { it.copy(showNameError = true) }
                } else {
                    save(UserProfile(s.name.trim(), s.currencyCode))
                }
            }
        }
    }

    private fun runAuth(block: suspend () -> Unit) = viewModelScope.launch {
        _state.update { it.copy(busy = true, error = null) }
        try { block() } catch (e: Exception) { _state.update { it.copy(error = e.message ?: "Request failed") } }
        finally { _state.update { it.copy(busy = false) } }
    }

    private fun save(profile: UserProfile) = viewModelScope.launch {
        try {
            val current = userRepository.observeProfile().first()
            userRepository.saveProfile(profile.copy(phone = current?.phone))
            _events.send(AuthEvent.SignedIn)
        } catch (e: Exception) { _state.update { it.copy(error = e.message ?: "Could not save profile") } }
    }
}
