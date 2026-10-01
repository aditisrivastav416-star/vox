package com.example.vox.feature.groups

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vox.core.ui.UiState
import com.example.vox.core.ui.stateInUi
import com.example.vox.domain.model.Group
import com.example.vox.domain.model.Money
import com.example.vox.domain.model.Person
import com.example.vox.domain.repository.GroupRepository
import com.example.vox.domain.repository.UserRepository
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine

data class MemberBalanceUi(val person: Person, val balance: Money)

data class GroupListItemUi(
    val group: Group,
    val balance: Money,
    val memberBalances: List<MemberBalanceUi>,
)

data class GroupsUiModel(
    val owedToYou: Money,
    val youOwe: Money,
    val groups: List<GroupListItemUi>,
)

/** Pairwise balances between you and each other member, skipping settled pairs. */
fun Group.memberBalances(): List<MemberBalanceUi> =
    otherMembers.map { MemberBalanceUi(it, balanceWith(it.id)) }.filter { it.balance.amountMinor != 0L }

class GroupsViewModel(groupRepository: GroupRepository, userRepository: UserRepository) : ViewModel() {
    val state: StateFlow<UiState<GroupsUiModel>> = combine(
        groupRepository.observeGroups(),
        userRepository.observeProfile(),
    ) { groups, profile ->
        val currency = profile?.currencyCode ?: groups.firstOrNull()?.currencyCode ?: "INR"
        val items = groups.map { GroupListItemUi(it, it.netBalance, it.memberBalances()) }
        // ponytail: totals only add up groups in your currency; FX conversion comes with multi-currency groups.
        val balances = items.flatMap { it.memberBalances }.map { it.balance }.filter { it.currencyCode == currency }
        GroupsUiModel(
            owedToYou = Money(balances.filter { it.amountMinor > 0 }.sumOf { it.amountMinor }, currency),
            youOwe = Money(balances.filter { it.amountMinor < 0 }.sumOf { -it.amountMinor }, currency),
            groups = items,
        )
    }.stateInUi(viewModelScope)
}
