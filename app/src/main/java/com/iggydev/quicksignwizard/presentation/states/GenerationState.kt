package com.iggydev.quicksignwizard.presentation.states

import androidx.compose.ui.graphics.ImageBitmap

data class GenerationState(
    val qrCodeImageWithPublicKey: ImageBitmap? = null,
    val qrCodeImageWithSignature: ImageBitmap? = null
)
