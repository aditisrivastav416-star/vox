package com.example.vox.domain.model

import java.time.Instant

const val CURRENT_USER_ID = "me"

data class Person(
    val id: String,
    val name: String,
    val contact: String? = null,
    val isOnVox: Boolean = true,
)

enum class GroupType { TRIP, HOME, FRIENDS, OTHER }

data class GroupExpense(
    val id: String,
    val description: String,
    val total: Money,
    val paidById: String,
    /** Person id -> share in minor units. Shares sum to [total]. */
    val shares: Map<String, Long>,
    val timestamp: Instant,
)

/** [fromId] paid [toId] back [amountMinor]. */
data class Settlement(
    val fromId: String,
    val toId: String,
    val amountMinor: Long,
)

data class Group(
    val id: String,
    val name: String,
    val type: GroupType,
    val currencyCode: String,
    /** Includes the current user. */
    val members: List<Person>,
    val expenses: List<GroupExpense> = emptyList(),
    val settlements: List<Settlement> = emptyList(),
) {
    /** Positive: [memberId] owes the current user. Negative: the current user owes them. */
    fun balanceWith(memberId: String): Money {
        var owedToMe = 0L
        expenses.forEach { expense ->
            when (expense.paidById) {
                CURRENT_USER_ID -> owedToMe += expense.shares[memberId] ?: 0
                memberId -> owedToMe -= expense.shares[CURRENT_USER_ID] ?: 0
            }
        }
        settlements.forEach { s ->
            if (s.fromId == memberId && s.toId == CURRENT_USER_ID) owedToMe -= s.amountMinor
            if (s.fromId == CURRENT_USER_ID && s.toId == memberId) owedToMe += s.amountMinor
        }
        return Money(owedToMe, currencyCode)
    }

    val otherMembers: List<Person> get() = members.filter { it.id != CURRENT_USER_ID }

    /** Positive: the group owes the current user overall. */
    val netBalance: Money
        get() = Money(otherMembers.sumOf { balanceWith(it.id).amountMinor }, currencyCode)
}
