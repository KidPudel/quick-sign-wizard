package com.iggydev.quicksignwizard.presentation.composables

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import android.util.Base64
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.iggydev.quicksignwizard.common.Constants
import com.iggydev.quicksignwizard.data.utilities.QrCodeAnalyzer
import com.iggydev.quicksignwizard.presentation.Screens
import kotlinx.coroutines.launch
import java.io.ByteArrayInputStream
import java.math.BigInteger
import java.security.AlgorithmParameters
import java.security.KeyFactory
import java.security.KeyStore
import java.security.MessageDigest
import java.security.PublicKey
import java.security.Signature
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.security.interfaces.ECPublicKey
import java.security.spec.ECGenParameterSpec
import java.security.spec.ECParameterSpec
import java.security.spec.ECPrivateKeySpec

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerScreen(navigationController: NavController) {
    val context = LocalContext.current
    val cameraProviderFuture = remember {
        // binds camera lifecycle to the lifecycle owner, which lets work with camera
        ProcessCameraProvider.getInstance(context)
    }
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val keyStore by remember {
        mutableStateOf(KeyStore.getInstance("AndroidKeyStore").apply { load(null) })
    }


    var file by remember {
        mutableStateOf<ByteArray?>(null)
    }

    val scannedData = remember {
        mutableStateListOf<String>()
    }

    val chooseFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { receivedPath ->
            val contentResolver = context.contentResolver

            val fileInputStream = contentResolver.openInputStream(receivedPath!!)

            // read and close (scoped)
            file = fileInputStream?.use { it.readBytes() }
        }
    )

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { permissionGranted ->
            hasCameraPermission = permissionGranted
            // next launch
            chooseFileLauncher.launch("text/plain")
        }
    )

    val goToSettingsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = {
        }
    )

    LaunchedEffect(key1 = true) {
        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    val scannerCoroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember {
        SnackbarHostState()
    }

    Scaffold(
        bottomBar = {
            BottomAppBar(contentPadding = PaddingValues(0.dp)) {
                WizardBottomBar(navigationController = navigationController)
            }
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(it)
        ) {
            // field to draw a camera (view)
            if (hasCameraPermission) {
                AndroidView(
                    factory = { contextView ->
                        // set a preview of the camera
                        val previewView = PreviewView(contextView)
                        val preview = Preview.Builder().build()

                        // bind previewView to preview to tell camera that usecase (preview where to render + qr code analyzer) is ready to receive data (images)
                        preview.setSurfaceProvider(previewView.surfaceProvider)

                        // set camera selector
                        val selector = CameraSelector.Builder()
                            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                            .build()

                        // setup image analyzer of qr code scanning
                        val imageAnalyzer = ImageAnalysis.Builder()
                            .setTargetResolution(Size(previewView.width, previewView.height))
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build().apply {
                                setAnalyzer(
                                    ContextCompat.getMainExecutor(contextView),
                                    QrCodeAnalyzer { qrCodeData ->
                                        // decode data
                                        val byteArrayData =
                                            Base64.decode(qrCodeData, Base64.NO_WRAP)

                                        if (!scannedData.contains(qrCodeData)) {
                                            if (!keyStore.isCertificateEntry(Constants.recipientAlias)) {
                                                val certificateFactory =
                                                    CertificateFactory.getInstance("X.509")
                                                val inputStream =
                                                    ByteArrayInputStream(byteArrayData)
                                                val certificate =
                                                    certificateFactory.generateCertificate(
                                                        inputStream
                                                    )

                                                keyStore.setCertificateEntry(
                                                    Constants.recipientAlias,
                                                    certificate
                                                )
                                                scannerCoroutineScope.launch {
                                                    snackbarHostState.showSnackbar(message = "new qr code detected")
                                                }
                                            } else {
                                                val publicKey =
                                                    keyStore.getCertificate(Constants.recipientAlias).publicKey

                                                // calculate hash value
                                                val digest = MessageDigest.getInstance("SHA-256")
                                                    .run { digest(file) }

                                                val verification =
                                                    Signature.getInstance("SHA256withECDSA").apply {
                                                        initVerify(publicKey)
                                                        update(digest)
                                                    }

                                                // compare hash values
                                                val isValid = verification.verify(byteArrayData)

                                                scannerCoroutineScope.launch {
                                                    snackbarHostState.showSnackbar(message = if (isValid) "data integrity verified" else "possibility of corrupted data")
                                                }

                                                // after usage of public key, delete it (temporary)
                                                keyStore.deleteEntry(Constants.recipientAlias)
                                            }
                                            scannedData.add(qrCodeData)
                                        }
                                    })
                            }

                        try {
                            // the state of lifecycle determines should camera open, started, stopped and closed
                            // binds lifecycle to receive data (for preview and for analyzer)
                            cameraProviderFuture.get()
                                .bindToLifecycle(
                                    lifecycleOwner,
                                    selector,
                                    preview,
                                    imageAnalyzer
                                )
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        previewView
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                AlertDialog(onDismissRequest = { }, confirmButton = {
                    Button(
                        onClick = {
                            // go to settings
                            val intentSettings = Intent(Settings.ACTION_SETTINGS)
                            goToSettingsLauncher.launch(intentSettings)
                        },
                        shape = RoundedCornerShape(5.dp),
                        border = BorderStroke(width = 5.dp, color = Color.Black)
                    ) {
                        Text(text = "OK")
                    }
                }, dismissButton = {
                    Button(
                        onClick = {
                            navigationController.popBackStack(
                                route = Screens.GenerationScreen.route,
                                inclusive = false
                            )
                        },
                        shape = RoundedCornerShape(5.dp),
                        border = BorderStroke(width = 5.dp, color = Color.Black)
                    ) {
                        Text(text = "К генерации")
                    }
                }, title = {
                    Text(text = "Для сканирования дайте разрешение для камеры")
                })
            }

        }
    }
}


fun getPublicKeyFromRaw(key: BigInteger, curveName: String): ECPublicKey {
    val ecParameterSpec = getParametersForCurve(curveName)
    val publicKeySpec = ECPrivateKeySpec(key, ecParameterSpec)
    val keyFactory = KeyFactory.getInstance("EC")
    return keyFactory.generatePublic(publicKeySpec) as ECPublicKey
}

fun getParametersForCurve(curveName: String): ECParameterSpec {
    val params = AlgorithmParameters.getInstance("EC")
    params.init(ECGenParameterSpec(curveName))
    return params.getParameterSpec(ECParameterSpec::class.java)
}