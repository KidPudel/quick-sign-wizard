package com.iggydev.quicksignwizard.presentation.composables

import android.Manifest
import android.content.pm.PackageManager
import android.hardware.camera2.CameraMetadata.LENS_FACING_BACK
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.iggydev.quicksignwizard.data.utilities.QrCodeAnalyzer

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

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { permissionGranted ->
            hasCameraPermission = permissionGranted
        }
    )

    LaunchedEffect(Unit) {
        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // field to draw a camera (view)
        if (hasCameraPermission) {
            AndroidView(factory = { contextView ->
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
                                TODO(reason = "handle anayzed qr code")
                            })
                    }

                try {
                    // the state of lifecycle determines should camera open, started, stopped and closed
                    // binds lifecycle to receive data (for preview and for analyzer)
                    cameraProviderFuture.get()
                        .bindToLifecycle(lifecycleOwner, selector, preview, imageAnalyzer)
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                previewView
            })

        }

    }
}