package com.iggydev.quicksignwizard.presentation.viewmodels

import androidx.lifecycle.ViewModel
import com.iggydev.quicksignwizard.data.utilities.DigitalSignature
import com.iggydev.quicksignwizard.presentation.states.GenerationState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class GenerationViewModel : ViewModel() {
    private val digitalSignature: DigitalSignature = DigitalSignature()

    private val _state = MutableStateFlow(GenerationState())
    val state: StateFlow<GenerationState>
        get() = _state

    fun generateQrCodeWithPublicKey(dimension: Int) {
        _state.update {
            it.copy(qrCodeImageWithPublicKey = digitalSignature.generateQrCodePublicKey(dimension = dimension))
        }
    }

    fun generateQrCodeWithSignature(dimension: Int, data: ByteArray) {
        _state.update {
            it.copy(
                qrCodeImageWithSignature = digitalSignature.generateQrCodeSignature(
                    dimension = dimension,
                    data = data
                )
            )
        }
    }
}