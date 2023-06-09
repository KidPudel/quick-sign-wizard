package com.iggydev.quicksignwizard.presentation.composables

import android.content.ContentResolver
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.iggydev.quicksignwizard.presentation.viewmodels.GenerationViewModel
import org.koin.androidx.compose.getViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenerationScreen(navigationController: NavController) {
    val generationViewModel = getViewModel<GenerationViewModel>()

    var publicKeyQrCode by remember {
        mutableStateOf<ImageBitmap?>(null)
    }

    var signatureQrCode by remember {
        mutableStateOf<ImageBitmap?>(null)
    }

    var hasQrCodeGenerated by remember {
        mutableStateOf(false)
    }

    val width = LocalDensity.current.run {
        LocalConfiguration.current.screenWidthDp.dp.toPx() / 1.2f
    }

    val height = LocalDensity.current.run {
        LocalConfiguration.current.screenHeightDp.dp.toPx() / 1.2f
    }

    var file by remember {
        mutableStateOf<ByteArray?>(null)
    }

    var isGenerationEnabled by remember {
        mutableStateOf(false)
    }

    val context = LocalContext.current

    val chooseFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { receivedPath ->
            println(receivedPath?.path)

            val contentResolver = context.contentResolver

            val fileInputStream = contentResolver.openInputStream(receivedPath!!)

            // read and close (scoped)
            file = fileInputStream?.use { it.readBytes() }

            isGenerationEnabled = true
        }
    )

    val qrCodeDimension = width.coerceAtMost(height).toInt()

    Scaffold(modifier = Modifier.fillMaxSize(), bottomBar = {
        BottomAppBar(contentPadding = PaddingValues(0.dp)) {
            WizardBottomBar(navigationController = navigationController)
        }
    }) {
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(it)
        ) {


            if (!hasQrCodeGenerated) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f), contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isGenerationEnabled) "Генерация QR-кодов" else "Загрузите файл для доступа к генерации QR-кодов",
                        fontSize = 30.sp,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.7f),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Button(
                        onClick = {
                            generationViewModel.generateQrCodeWithPublicKey(dimension = qrCodeDimension)
                            publicKeyQrCode =
                                generationViewModel.state.value.qrCodeImageWithPublicKey
                            hasQrCodeGenerated = true
                        },
                        enabled = isGenerationEnabled,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .height(60.dp)
                            .fillMaxWidth(0.7f),
                        border = BorderStroke(width = 5.dp, color = Color.Black)
                    ) {
                        Text(text = "публичный ключ", fontSize = 20.sp)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = {
                            generationViewModel.generateQrCodeWithSignature(
                                dimension = qrCodeDimension,
                                data = file!!
                            )
                            signatureQrCode =
                                generationViewModel.state.value.qrCodeImageWithSignature
                            hasQrCodeGenerated = true
                        },
                        enabled = isGenerationEnabled,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .height(60.dp)
                            .fillMaxWidth(0.7f),
                        border = BorderStroke(width = 5.dp, color = Color.Black)
                    ) {
                        Text(text = "электронная подпись", fontSize = 20.sp)
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f), contentAlignment = Alignment.Center
                ) {
                    Button(
                        onClick = {
                            chooseFileLauncher.launch("text/plain")
                        },
                        shape = RoundedCornerShape(5.dp),
                        modifier = Modifier
                            .height(50.dp),
                        border = BorderStroke(width = 5.dp, color = Color.Black)
                    ) {
                        Text(text = "выберите файл")
                    }
                }
            } else {
                if (publicKeyQrCode != null) {
                    Text(
                        text = "QR-код с публичным ключом",
                        fontSize = 30.sp,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        bitmap = publicKeyQrCode!!,
                        contentDescription = "qr code with public key inside",
                        tint = Color.Unspecified
                    )
                }
                if (signatureQrCode != null) {
                    Text(
                        text = "QR-код с электронной подписью",
                        fontSize = 30.sp,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        bitmap = signatureQrCode!!,
                        contentDescription = "qr code with digital signature inside",
                        tint = Color.Unspecified
                    )
                }
                Button(
                    onClick = {
                        hasQrCodeGenerated = false
                        publicKeyQrCode = null
                        signatureQrCode = null
                    },
                    shape = RoundedCornerShape(5.dp),
                    modifier = Modifier
                        .height(50.dp)
                        .fillMaxWidth(0.5f),
                    border = BorderStroke(width = 5.dp, color = Color.Black)
                ) {
                    Text(text = "назад", fontSize = 20.sp)
                }
            }
        }

    }
}