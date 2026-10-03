package com.example.vox.data.remote

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import com.example.vox.domain.model.*
import com.example.vox.domain.repository.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

private fun JSONObject.money() = Money(getString("amountMinor").toLong(), getString("currencyCode"))
private fun JSONObject.transaction() = Transaction(
    getString("id"), TransactionType.valueOf(getString("type")), money(), getString("title"),
    nullable("category")?.let(Category::valueOf), PaymentMethod.valueOf(getString("paymentMethod")),
    Instant.parse(getString("timestamp")), TransactionSource.valueOf(getString("source")),
    nullable("groupId"), nullable("notes"),
)

class RemoteTransactionRepository(private val api: Api) : TransactionRepository {
    private val rows = MutableStateFlow<List<Transaction>>(emptyList())
    fun clear() { rows.value = emptyList() }
    override fun observeTransactions(): Flow<List<Transaction>> = rows
    override fun observeTransaction(id: String): Flow<Transaction?> = rows.map { list -> list.find { it.id == id } }
    suspend fun refresh() {
        val all = mutableListOf<Transaction>()
        var cursor: String? = null
        do {
            val response = api.request("GET", "/transactions?limit=100" + (cursor?.let { "&cursor=" + java.net.URLEncoder.encode(it, "UTF-8") } ?: ""))
            all += response.getJSONArray("items").objects().map { it.transaction() }
            cursor = response.nullable("nextCursor")
        } while (cursor != null)
        rows.value = all.sortedByDescending { it.timestamp }
    }
    override suspend fun upsert(transaction: Transaction) {
        val current = runCatching { api.request("GET", "/transactions/${transaction.id}") }.getOrNull()
        val body = json(
            "id" to transaction.id, "type" to transaction.type.name, "amountMinor" to transaction.amount.amountMinor.toString(),
            "currencyCode" to transaction.amount.currencyCode, "title" to transaction.title, "category" to transaction.category?.name,
            "paymentMethod" to transaction.paymentMethod.name, "timestamp" to transaction.timestamp.toString(),
            "source" to transaction.source.name, "groupId" to transaction.groupId, "notes" to transaction.notes,
        )
        if (current == null) api.request("POST", "/transactions", body)
        else api.request("PUT", "/transactions/${transaction.id}", body.put("version", current.getInt("version")))
        refresh()
    }
    override suspend fun delete(id: String) {
        val row = api.request("GET", "/transactions/$id")
        api.request("DELETE", "/transactions/$id?version=${row.getInt("version")}")
        refresh()
    }
    override suspend fun categorize(id: String, category: Category) {
        upsert(rows.value.first { it.id == id }.copy(category = category))
    }
}

class RemoteUserRepository(private val api: Api, private val onSignedIn: suspend () -> Unit, private val onSignedOut: () -> Unit) : UserRepository {
    private val profile = MutableStateFlow<UserProfile?>(null)
    override fun observeProfile(): Flow<UserProfile?> = profile
    suspend fun refresh() {
        if (!api.hasSession) return
        profile.value = api.request("GET", "/users/me").profile()
    }
    private fun JSONObject.profile() = UserProfile(getString("name"), getString("currencyCode"), nullable("phone"))
    override suspend fun saveProfile(profile: UserProfile) {
        this.profile.value = api.request("PUT", "/users/me", json(
            "name" to profile.name, "currencyCode" to profile.currencyCode, "phone" to profile.phone
        )).profile()
    }
    override suspend fun signOut() {
        runCatching { api.request("POST", "/auth/logout") }
        api.clearSession()
        profile.value = null
        onSignedOut()
    }
    override suspend fun login(identifier: String, password: String) {
        val result = api.request("POST", "/auth/login", json("identifier" to identifier, "password" to password))
        onSignedOut()
        api.saveSession(result)
        onSignedIn()
    }
    override suspend fun startSignup(identifier: String) {
        api.request("POST", "/auth/signup/start", json("identifier" to identifier))
    }
    override suspend fun verifySignup(identifier: String, code: String) {
        api.request("POST", "/auth/signup/verify", json("identifier" to identifier, "code" to code))
    }
    override suspend fun finishSignup(identifier: String, code: String, password: String, confirmPassword: String) {
        val result = api.request("POST", "/auth/register", json(
            "identifier" to identifier, "code" to code, "password" to password, "confirmPassword" to confirmPassword
        ))
        onSignedOut()
        api.saveSession(result)
        onSignedIn()
    }
}

class RemoteBudgetRepository(private val api: Api) : BudgetRepository {
    private val budgets = MutableStateFlow<Map<Pair<String, String>, Long>>(emptyMap())
    fun clear() { budgets.value = emptyMap() }
    override fun observeBudgets(): Flow<Map<Pair<String, String>, Long>> = budgets
    suspend fun refresh() {
        budgets.value = api.request("GET", "/budgets").getJSONArray("items").objects()
            .associate { (it.getString("month") to it.getString("currencyCode")) to it.getString("amountMinor").toLong() }
    }
    override suspend fun setBudget(month: String, currencyCode: String, amountMinor: Long) {
        api.request("PUT", "/budgets", json("month" to month, "currencyCode" to currencyCode, "amountMinor" to amountMinor.toString()))
        refresh()
    }
}

class RemoteRecurringRepository(private val api: Api) : RecurringPaymentRepository {
    private val rows = MutableStateFlow<List<RecurringPayment>>(emptyList())
    fun clear() { rows.value = emptyList() }
    override fun observePayments(): Flow<List<RecurringPayment>> = rows
    suspend fun refresh() {
        rows.value = api.request("GET", "/recurring-payments").getJSONArray("items").objects().map { r ->
            RecurringPayment(r.getString("id"), r.getString("name"), Category.valueOf(r.getString("category")),
                r.nullable("amountMinor")?.let { Money(it.toLong(), r.getString("currencyCode")) },
                Frequency.valueOf(r.getString("frequency")), if (r.isNull("customIntervalDays")) null else r.getInt("customIntervalDays"),
                LocalDate.parse(r.getString("nextDueDate")), r.getBoolean("remindDaysBefore"), r.getBoolean("remindOnDueDate"))
        }
    }
    override suspend fun upsert(payment: RecurringPayment) {
        val currency = payment.amount?.currencyCode ?: api.request("GET", "/users/me").getString("currencyCode")
        val body = json(
            "id" to payment.id, "name" to payment.name, "category" to payment.category.name,
            "amountMinor" to payment.amount?.amountMinor?.toString(), "currencyCode" to currency,
            "frequency" to payment.frequency.name, "customIntervalDays" to payment.customIntervalDays,
            "nextDueDate" to payment.nextDueDate.toString(), "remindDaysBefore" to payment.remindDaysBefore,
            "remindOnDueDate" to payment.remindOnDueDate,
        )
        val exists = rows.value.any { it.id == payment.id }
        api.request(if (exists) "PUT" else "POST", if (exists) "/recurring-payments/${payment.id}" else "/recurring-payments", body)
        refresh()
    }
}

class RemoteGroupRepository(private val context: Context, private val api: Api) : GroupRepository {
    private val groups = MutableStateFlow<List<Group>>(emptyList())
    private val contacts = MutableStateFlow<List<Person>>(emptyList())
    fun clear() { groups.value = emptyList(); contacts.value = emptyList() }
    override fun observeGroups(): Flow<List<Group>> = groups
    override fun observeGroup(id: String): Flow<Group?> = groups.map { list -> list.find { it.id == id } }
    override fun observeContacts(): Flow<List<Person>> = contacts

    private fun own(id: String) = if (id == api.userId) CURRENT_USER_ID else id
    private fun server(id: String) = if (id == CURRENT_USER_ID) api.userId else id
    suspend fun refresh() {
        val list = api.request("GET", "/groups").getJSONArray("items").objects()
        groups.value = list.map { item ->
            val g = api.request("GET", "/groups/${item.getString("id")}")
            val currency = g.getString("currencyCode")
            Group(
                g.getString("id"), g.getString("name"), GroupType.valueOf(g.getString("type")), currency,
                g.getJSONArray("members").objects().map { m ->
                    val u = m.getJSONObject("user")
                    Person(own(u.getString("id")), u.getString("name"), u.nullable("email"))
                },
                g.getJSONArray("expenses").objects().map { e ->
                    val shares = e.getJSONObject("shares")
                    GroupExpense(e.getString("id"), e.getString("description"),
                        Money(e.getString("amountMinor").toLong(), currency), own(e.getString("paidById")),
                        shares.keys().asSequence().associate { own(it) to shares.getString(it).toLong() },
                        Instant.parse(e.getString("timestamp")))
                },
                g.getJSONArray("settlements").objects().map { s ->
                    Settlement(own(s.getString("fromId")), own(s.getString("toId")), s.getString("amountMinor").toLong())
                },
            )
        }
    }
    override suspend fun syncContacts() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            contacts.value = emptyList()
            return
        }
        val device = mutableMapOf<String, String>()
        context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER),
            null, null, null,
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                val name = cursor.getString(0).orEmpty()
                val raw = cursor.getString(1).orEmpty().replace(Regex("[^+0-9]"), "")
                val number = if (raw.length == 10) "+91$raw" else raw
                if (number.startsWith("+") && number.length in 9..16) device[number] = name
            }
        }
        context.contentResolver.query(
            ContactsContract.CommonDataKinds.Email.CONTENT_URI,
            arrayOf(ContactsContract.CommonDataKinds.Email.DISPLAY_NAME, ContactsContract.CommonDataKinds.Email.ADDRESS),
            null, null, null,
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                val email = cursor.getString(1).orEmpty().trim().lowercase()
                if (email.contains('@')) device[email] = cursor.getString(0).orEmpty()
            }
        }
        val matched = device.keys.chunked(100).flatMap { batch ->
            api.request("POST", "/groups/contacts/lookup", json("identifiers" to JSONArray(batch)))
                .getJSONArray("items").objects()
        }
        contacts.value = matched.filter { it.getString("id") != api.userId }.map { u ->
            val identifier = u.getString("identifier")
            Person(u.getString("id"), device[identifier] ?: u.getString("name").ifBlank { identifier }, identifier)
        }.distinctBy { it.id }
    }
    override suspend fun createGroup(name: String, type: GroupType, memberIds: List<String>): String {
        val currency = api.request("GET", "/users/me").getString("currencyCode")
        val id = api.request("POST", "/groups", json(
            "name" to name, "type" to type.name, "currencyCode" to currency, "memberIds" to JSONArray(memberIds)
        )).getString("id")
        refresh()
        return id
    }
    override suspend fun addExpense(groupId: String, expense: GroupExpense) {
        val shares = JSONObject().apply { expense.shares.forEach { (id, value) -> put(server(id), value.toString()) } }
        api.request("POST", "/groups/$groupId/expenses", json(
            "id" to expense.id, "description" to expense.description, "amountMinor" to expense.total.amountMinor.toString(),
            "paidById" to server(expense.paidById), "timestamp" to expense.timestamp.toString(), "shares" to shares
        ))
        refresh()
    }
    override suspend fun settleUp(groupId: String) {
        val group = groups.value.first { it.id == groupId }
        group.otherMembers.forEach { member ->
            val amount = group.balanceWith(member.id).amountMinor
            if (amount != 0L) api.request("POST", "/groups/$groupId/settlements", json(
                "fromId" to server(if (amount > 0) member.id else CURRENT_USER_ID),
                "toId" to server(if (amount > 0) CURRENT_USER_ID else member.id),
                "amountMinor" to kotlin.math.abs(amount).toString(),
            ))
        }
        refresh()
    }
    override suspend fun deleteGroup(groupId: String) {
        api.request("DELETE", "/groups/$groupId")
        refresh()
    }
}

class UnavailableReceiptRepository : ReceiptRepository {
    override suspend fun scan(imageUri: String?): Receipt = error("Receipt scanning is not configured on the backend")
    override fun observeReceipt(id: String): Flow<Receipt?> = MutableStateFlow(null)
}

class UnavailableVoiceAssistant : VoiceAssistant {
    override suspend fun listen(): VoiceResult = error("Voice recognition is not configured")
}
