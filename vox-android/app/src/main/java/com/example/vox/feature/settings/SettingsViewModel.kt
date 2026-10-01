package com.example.vox.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vox.core.ui.UiState
import com.example.vox.core.ui.stateInUi
import com.example.vox.domain.model.UserProfile
import com.example.vox.domain.model.Group
import com.example.vox.domain.model.RecurringPayment
import com.example.vox.domain.repository.GroupRepository
import com.example.vox.domain.repository.RecurringPaymentRepository
import com.example.vox.domain.repository.UserRepository
import com.example.vox.feature.auth.SupportedCurrencies
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

data class SettingsUiModel(
    val profile: UserProfile?,
    val currencies: List<String> = SupportedCurrencies,
    val groups: List<Group> = emptyList(),
    val recurring: List<RecurringPayment> = emptyList(),
)

sealed interface SettingsAction {
    data class CurrencyChanged(val code: String) : SettingsAction
    data object SignOut : SettingsAction
    data object OpenGroups : SettingsAction
    data object OpenRecurring : SettingsAction
}

sealed interface SettingsEvent {
    data object SignedOut : SettingsEvent
}

class SettingsViewModel(
    private val userRepository: UserRepository,
    groupRepository: GroupRepository,
    recurringPaymentRepository: RecurringPaymentRepository,
) : ViewModel() {
    val state: StateFlow<UiState<SettingsUiModel>> =
        combine(userRepository.observeProfile(), groupRepository.observeGroups(), recurringPaymentRepository.observePayments()) { profile, groups, recurring ->
            SettingsUiModel(profile, groups = groups, recurring = recurring)
        }.stateInUi(viewModelScope)

    private val _events = Channel<SettingsEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun onAction(action: SettingsAction) {
        when (action) {
            is SettingsAction.CurrencyChanged -> viewModelScope.launch {
                val profile = userRepository.observeProfile().first() ?: return@launch
                userRepository.saveProfile(profile.copy(currencyCode = action.code))
            }
            SettingsAction.SignOut -> viewModelScope.launch {
                userRepository.signOut()
                _events.send(SettingsEvent.SignedOut)
            }
            SettingsAction.OpenGroups, SettingsAction.OpenRecurring -> Unit // navigation
        }
    }
}
