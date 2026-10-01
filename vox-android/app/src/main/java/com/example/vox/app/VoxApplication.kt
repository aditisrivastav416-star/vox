package com.example.vox.app

import android.app.Application
import com.example.vox.data.fake.FakeGroupRepository
import com.example.vox.data.fake.FakeReceiptRepository
import com.example.vox.data.fake.FakeRecurringPaymentRepository
import com.example.vox.data.fake.FakeTransactionRepository
import com.example.vox.data.fake.FakeUserRepository
import com.example.vox.data.fake.FakeVoiceAssistant
import com.example.vox.domain.repository.GroupRepository
import com.example.vox.domain.repository.ReceiptRepository
import com.example.vox.domain.repository.RecurringPaymentRepository
import com.example.vox.domain.repository.TransactionRepository
import com.example.vox.domain.repository.UserRepository
import com.example.vox.domain.repository.VoiceAssistant

/** Manual DI. Swap a Fake* for a Room/API implementation here; nothing above the domain layer changes. */
class AppContainer {
    val transactionRepository: TransactionRepository = FakeTransactionRepository()
    val groupRepository: GroupRepository = FakeGroupRepository()
    val recurringPaymentRepository: RecurringPaymentRepository = FakeRecurringPaymentRepository()
    val receiptRepository: ReceiptRepository = FakeReceiptRepository()
    val userRepository: UserRepository = FakeUserRepository()
    val voiceAssistant: VoiceAssistant = FakeVoiceAssistant()
}

class VoxApplication : Application() {
    val container by lazy { AppContainer() }
}
