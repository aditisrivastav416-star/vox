package com.example.vox.app

import android.app.Application
import android.content.Context
import com.example.vox.data.remote.*
import com.example.vox.domain.repository.*

class AppContainer(context: Context) {
    private val api = Api(context)
    val transactionRepository = RemoteTransactionRepository(api)
    val groupRepository = RemoteGroupRepository(context, api)
    val recurringPaymentRepository = RemoteRecurringRepository(api)
    val budgetRepository = RemoteBudgetRepository(api)
    val userRepository = RemoteUserRepository(api, ::refresh, ::clear)
    val receiptRepository: ReceiptRepository = UnavailableReceiptRepository()
    val voiceAssistant: VoiceAssistant = UnavailableVoiceAssistant()

    suspend fun restoreSession(): Boolean {
        if (!api.hasSession) return false
        return runCatching { refresh(); true }.getOrDefault(false)
    }

    private suspend fun refresh() {
        userRepository.refresh()
        transactionRepository.refresh()
        groupRepository.refresh()
        recurringPaymentRepository.refresh()
        budgetRepository.refresh()
    }

    private fun clear() {
        transactionRepository.clear()
        groupRepository.clear()
        recurringPaymentRepository.clear()
        budgetRepository.clear()
    }
}

class VoxApplication : Application() {
    val container by lazy { AppContainer(this) }
}
