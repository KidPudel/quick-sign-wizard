package com.iggydev.quicksignwizard.presentation.composables

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraMetadata.LENS_FACING_BACK
import android.provider.Settings
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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.iggydev.quicksignwizard.data.utilities.QrCodeAnalyzer
import com.iggydev.quicksignwizard.presentation.Screens
import org.w3c.dom.Text

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

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { permissionGranted ->
            hasCameraPermission = permissionGranted
        }
    )

    val goToSettingsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = {
            it.resultCode
        }
    )


    LaunchedEffect(key1 = true) {
        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    Scaffold(bottomBar = {
        BottomAppBar(contentPadding = PaddingValues(0.dp)) {
            WizardBottomBar(navigationController = navigationController)
        }
    }) {

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
                        Text(text = "return to generation")
                    }
                }, title = {
                    Text(text = "To scan, please give a permission for the camera")
                })
            }

        }
    }
}