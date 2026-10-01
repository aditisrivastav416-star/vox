package com.example.vox.feature.home

sealed interface HomeAction {
    // Navigation — handled by the route, never by the ViewModel.
    data object OpenRecurring : HomeAction
    data object OpenGroups : HomeAction
    data object CreateGroup : HomeAction
    data class OpenGroup(val groupId: String) : HomeAction
    data class OpenTransaction(val transactionId: String) : HomeAction

    // State changes — handled by HomeViewModel.
    data object PreviousMonth : HomeAction
    data object NextMonth : HomeAction
    data class SelectMode(val mode: SummaryMode) : HomeAction
    data object ToggleBreakdown : HomeAction
}
