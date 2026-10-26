package com.example.vox.app

import android.app.Application
import android.content.Context
import com.example.vox.data.remote.*
import com.example.vox.data.local.*
import com.example.vox.domain.repository.*

class AppContainer(context: Context) {
    private val store = DeviceStore(context)
    private val api = Api(context)
    val transactionRepository = LocalTransactionRepository(store)
    val groupRepository = LocalGroupRepository(context, store)
    val recurringPaymentRepository = LocalRecurringRepository(store)
    val budgetRepository = LocalBudgetRepository(store)
    val userRepository = LocalUserRepository(store)
    val receiptRepository: ReceiptRepository = LocalReceiptRepository(context, store)
    val voiceAssistant: VoiceAssistant = GnaniVoiceAssistant(context, api)

    suspend fun restoreSession(): Boolean = userRepository.hasProfile()
}

class VoxApplication : Application() {
    val container by lazy { AppContainer(this) }
}
