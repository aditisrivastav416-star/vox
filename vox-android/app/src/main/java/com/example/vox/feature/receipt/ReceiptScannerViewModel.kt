package com.example.vox.feature.receipt

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vox.R
import com.example.vox.core.common.UiText
import com.example.vox.domain.repository.ReceiptRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ReceiptScannerUiState(
    val isScanning: Boolean = false,
    val error: UiText? = null,
)

sealed interface ReceiptScannerAction {
    data class ImagePicked(val uri: String) : ReceiptScannerAction
}

sealed interface ReceiptScannerEvent {
    data class Scanned(val receiptId: String) : ReceiptScannerEvent
}

class ReceiptScannerViewModel(private val receiptRepository: ReceiptRepository) : ViewModel() {
    private val _state = MutableStateFlow(ReceiptScannerUiState())
    val state: StateFlow<ReceiptScannerUiState> = _state.asStateFlow()

    private val _events = Channel<ReceiptScannerEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun onAction(action: ReceiptScannerAction) {
        when (action) { is ReceiptScannerAction.ImagePicked -> scan(action.uri) }
    }

    private fun scan(uri: String?) {
        if (_state.value.isScanning) return
        _state.value = ReceiptScannerUiState(isScanning = true)
        viewModelScope.launch {
            try {
                val receipt = receiptRepository.scan(uri)
                _state.update { it.copy(isScanning = false) }
                _events.send(ReceiptScannerEvent.Scanned(receipt.id))
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _state.value = ReceiptScannerUiState(error = UiText.Res(R.string.error_scan_failed))
            }
        }
    }
}
