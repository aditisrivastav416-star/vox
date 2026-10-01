package com.example.vox.core.ui

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.vox.R
import com.example.vox.core.designsystem.icon.VoxIcons
import com.example.vox.domain.model.CURRENT_USER_ID
import com.example.vox.domain.model.Category
import com.example.vox.domain.model.Frequency
import com.example.vox.domain.model.GroupType
import com.example.vox.domain.model.PaymentMethod
import com.example.vox.domain.model.Person
import com.example.vox.domain.model.TransactionSource
import com.example.vox.domain.model.TransactionType

/** Maps domain enums to resources so the domain layer stays Android-free. */

@get:StringRes
val Category.labelRes: Int
    get() = when (this) {
        Category.FOOD -> R.string.category_food
        Category.GROCERIES -> R.string.category_groceries
        Category.SHOPPING -> R.string.category_shopping
        Category.TRAVEL -> R.string.category_travel
        Category.TRANSPORT -> R.string.category_transport
        Category.BILLS -> R.string.category_bills
        Category.RENT -> R.string.category_rent
        Category.ENTERTAINMENT -> R.string.category_entertainment
        Category.EDUCATION -> R.string.category_education
        Category.HEALTH -> R.string.category_health
        Category.OTHER -> R.string.category_other
        Category.SALARY -> R.string.category_salary
        Category.GIFT -> R.string.category_gift
        Category.OTHER_INCOME -> R.string.category_other_income
    }

val Category?.icon: ImageVector
    get() = when (this) {
        Category.FOOD -> VoxIcons.Food
        Category.GROCERIES -> VoxIcons.Groceries
        Category.SHOPPING -> VoxIcons.Shopping
        Category.TRAVEL -> VoxIcons.Travel
        Category.TRANSPORT -> VoxIcons.Transport
        Category.BILLS -> VoxIcons.Bills
        Category.RENT -> VoxIcons.Home
        Category.ENTERTAINMENT -> VoxIcons.Entertainment
        Category.EDUCATION -> VoxIcons.Education
        Category.HEALTH -> VoxIcons.Health
        Category.SALARY -> VoxIcons.Salary
        Category.GIFT -> VoxIcons.Gift
        Category.OTHER_INCOME -> VoxIcons.Income
        Category.OTHER, null -> VoxIcons.Other
    }

@get:StringRes
val PaymentMethod.labelRes: Int
    get() = when (this) {
        PaymentMethod.UPI -> R.string.payment_upi
        PaymentMethod.CARD -> R.string.payment_card
        PaymentMethod.CASH -> R.string.payment_cash
        PaymentMethod.BANK -> R.string.payment_bank
    }

@get:StringRes
val TransactionSource.labelRes: Int
    get() = when (this) {
        TransactionSource.MANUAL -> R.string.source_manual
        TransactionSource.VOICE -> R.string.source_voice
        TransactionSource.SMS -> R.string.source_sms
        TransactionSource.RECEIPT -> R.string.source_receipt
        TransactionSource.GROUP -> R.string.source_group
    }

@get:StringRes
val TransactionType.labelRes: Int
    get() = when (this) {
        TransactionType.EXPENSE -> R.string.type_expense
        TransactionType.INCOME -> R.string.type_income
    }

@get:StringRes
val GroupType.labelRes: Int
    get() = when (this) {
        GroupType.TRIP -> R.string.group_type_trip
        GroupType.HOME -> R.string.group_type_home
        GroupType.FRIENDS -> R.string.group_type_friends
        GroupType.OTHER -> R.string.group_type_other
    }

val GroupType.icon: ImageVector
    get() = when (this) {
        GroupType.TRIP -> VoxIcons.Trip
        GroupType.HOME -> VoxIcons.Home
        GroupType.FRIENDS, GroupType.OTHER -> VoxIcons.Groups
    }

@get:StringRes
val Frequency.labelRes: Int
    get() = when (this) {
        Frequency.DAILY -> R.string.frequency_daily
        Frequency.WEEKLY -> R.string.frequency_weekly
        Frequency.MONTHLY -> R.string.frequency_monthly
        Frequency.QUARTERLY -> R.string.frequency_quarterly
        Frequency.YEARLY -> R.string.frequency_yearly
        Frequency.CUSTOM -> R.string.frequency_custom
    }

@Composable
fun Person.displayName(): String = if (id == CURRENT_USER_ID) stringResource(R.string.you) else name
