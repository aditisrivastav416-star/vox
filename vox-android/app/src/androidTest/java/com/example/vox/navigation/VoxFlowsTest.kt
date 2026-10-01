package com.example.vox.navigation

import androidx.annotation.StringRes
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.vox.R
import com.example.vox.app.MainActivity
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** PRD §26 flows, driven through semantics (text / content descriptions), never coordinates. */
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class VoxFlowsTest {
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    private fun s(@StringRes id: Int) = rule.activity.getString(id)
    private fun click(text: String) = rule.onNodeWithText(text).performClick()
    private fun clickDescription(@StringRes id: Int) = rule.onNodeWithContentDescription(s(id)).performClick()
    private fun waitFor(text: String) = rule.waitUntil(5_000) { rule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }
    private fun scrollTo(text: String) =
        rule.onAllNodes(hasScrollToNodeAction()).onFirst().performScrollToNode(hasText(text))
    private fun type(label: String, value: String) =
        rule.onNode(hasSetTextAction() and hasText(label)).performTextInput(value)

    @Before
    fun enterApp() {
        click(s(R.string.welcome_skip))
        waitFor(s(R.string.mode_spending))
    }

    @Test
    fun home_group_groupDetails() {
        scrollTo("Goa Trip")
        click("Goa Trip")
        waitFor(s(R.string.group_settle_up))
        rule.onNodeWithText(s(R.string.member_owes_you).format("Rahul")).assertExists()
    }

    @Test
    fun home_upcoming_recurring_create() {
        scrollTo(s(R.string.upcoming_payments))
        rule.onAllNodesWithText(s(R.string.see_all)).onFirst().performClick()
        rule.onNodeWithText(s(R.string.recurring_title)).assertExists()
        clickDescription(R.string.recurring_new)
        type(s(R.string.field_name), "Gym")
        type(s(R.string.field_amount), "1500")
        rule.onNodeWithText(s(R.string.save)).performScrollTo().performClick()
        waitFor("Gym")
    }

    @Test
    fun vox_manualTransaction_showsInActivity() {
        clickDescription(R.string.cd_open_vox)
        click(s(R.string.vox_type_instead))
        type(s(R.string.field_amount), "250")
        type(s(R.string.field_description), "Bookstore")
        // The top bar title shares this text; target the button.
        rule.onNode(hasText(s(R.string.add_transaction)) and hasClickAction()).performScrollTo().performClick()
        waitFor(s(R.string.nav_activity))
        click(s(R.string.nav_activity))
        waitFor("Bookstore")
    }

    @Test
    fun vox_scanReceipt_split() {
        clickDescription(R.string.cd_open_vox)
        click(s(R.string.scan_receipt))
        clickDescription(R.string.cd_capture_receipt)
        waitFor(s(R.string.receipt_found))
        click(s(R.string.receipt_split))
        click(s(R.string.split_equally))
        click(s(R.string.split_confirm))
        waitFor(s(R.string.mode_spending))
    }

    @Test
    fun activity_transactionDetails_edit() {
        click(s(R.string.nav_activity))
        waitFor("Swiggy")
        click("Swiggy")
        waitFor(s(R.string.source_sms))
        click(s(R.string.edit_transaction))
        waitFor(s(R.string.save_changes))
    }

    @Test
    fun group_addExpense_split() {
        scrollTo("Flat 402")
        click("Flat 402")
        click(s(R.string.group_add_expense))
        type(s(R.string.field_description), "Pizza night")
        type(s(R.string.field_amount), "900")
        click(s(R.string.split_equally))
        click(s(R.string.split_confirm))
        waitFor("Pizza night")
    }

}
