package com.example.vox.navigation

import android.net.Uri

/** Routes and arguments. Only IDs and small primitives travel through navigation. */
object VoxDestination {
    const val ARG_TRANSACTION_ID = "transactionId"
    const val ARG_GROUP_ID = "groupId"
    const val ARG_RECEIPT_ID = "receiptId"
    const val ARG_AMOUNT = "amount"
    const val ARG_TITLE = "title"
    const val ARG_CATEGORY = "category"
    const val ARG_TYPE = "type"

    const val SPLASH = "splash"
    const val WELCOME = "welcome"
    const val PROFILE_SETUP = "profile_setup"
    const val HOME = "home"
    const val ACTIVITY = "activity"
    const val VOX = "vox"
    const val SETTINGS = "settings"
    const val GROUPS = "groups"
    const val GROUP_CREATE = "groups/create"
    const val GROUP_DETAIL = "group/{$ARG_GROUP_ID}"
    const val RECURRING = "recurring"
    const val RECURRING_CREATE = "recurring/create"
    const val RECEIPT_SCAN = "receipt_scan"
    const val RECEIPT_RESULT = "receipt_result/{$ARG_RECEIPT_ID}"
    const val TRANSACTION_DETAIL = "transaction/{$ARG_TRANSACTION_ID}"
    const val TRANSACTION_FORM = "transaction_form?$ARG_TRANSACTION_ID={$ARG_TRANSACTION_ID}" +
        "&$ARG_AMOUNT={$ARG_AMOUNT}&$ARG_TITLE={$ARG_TITLE}&$ARG_CATEGORY={$ARG_CATEGORY}&$ARG_TYPE={$ARG_TYPE}"
    const val SPLIT = "split?$ARG_RECEIPT_ID={$ARG_RECEIPT_ID}&$ARG_GROUP_ID={$ARG_GROUP_ID}"

    fun groupDetail(groupId: String) = "group/${Uri.encode(groupId)}"
    fun receiptResult(receiptId: String) = "receipt_result/${Uri.encode(receiptId)}"
    fun transactionDetail(transactionId: String) = "transaction/${Uri.encode(transactionId)}"

    fun transactionForm(
        transactionId: String? = null,
        amountMinor: Long? = null,
        title: String? = null,
        category: String? = null,
        type: String? = null,
    ) = query(
        "transaction_form",
        ARG_TRANSACTION_ID to transactionId,
        ARG_AMOUNT to amountMinor?.toString(),
        ARG_TITLE to title,
        ARG_CATEGORY to category,
        ARG_TYPE to type,
    )

    fun split(receiptId: String? = null, groupId: String? = null) =
        query("split", ARG_RECEIPT_ID to receiptId, ARG_GROUP_ID to groupId)

    private fun query(base: String, vararg args: Pair<String, String?>): String {
        val present = args.filter { it.second != null }
        if (present.isEmpty()) return base
        return base + "?" + present.joinToString("&") { (k, v) -> "$k=${Uri.encode(v)}" }
    }
}
