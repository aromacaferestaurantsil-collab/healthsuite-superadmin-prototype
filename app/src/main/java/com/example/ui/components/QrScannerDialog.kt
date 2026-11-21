package com.example.ui.components

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.R
import com.example.ui.theme.CardBorder
import com.example.ui.theme.PrimaryEmerald
import com.example.ui.theme.SidebarBg
import com.example.ui.theme.SurfaceWhite
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

@Composable
fun QrScannerDialog(
    onQrScanned: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = SurfaceWhite,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, CardBorder),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("qr_scanner_dialog")
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Scan WhatsApp / Bridge QR",
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = stringResource(R.string.cd_close_panel),
                            tint = TextPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (hasCameraPermission) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(SidebarBg)
                    ) {
                        CameraQrPreview(onQrDetected = onQrScanned)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Align a WhatsApp Link-Device QR or HTTPS bridge config QR inside the camera frame.",
                        color = TextSecondary,
                        fontSize = 11.5.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.QrCodeScanner,
                            contentDescription = null,
                            tint = PrimaryEmerald,
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Camera permission is required to scan QR codes.",
                            color = TextSecondary,
                            fontSize = 12.5.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PrimaryEmerald,
                                contentColor = Color.White
                            ),
                            modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                        ) {
                            Text("Grant Camera Permission", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
@Composable
private fun CameraQrPreview(
    onQrDetected: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
    val scanner = remember {
        BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                .build()
        )
    }
    var hasEmitted by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        val listener = Runnable {
            val provider = runCatching { cameraProviderFuture.get() }.getOrNull() ?: return@Runnable
            val preview = Preview.Builder().build().also {
                it.surfaceProvider = previewView.surfaceProvider
            }
            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also { imageAnalysis ->
                    imageAnalysis.setAnalyzer(analysisExecutor) { imageProxy ->
                        val mediaImage = imageProxy.image
                        if (mediaImage != null && !hasEmitted) {
                            val inputImage = InputImage.fromMediaImage(
                                mediaImage,
                                imageProxy.imageInfo.rotationDegrees
                            )
                            scanner.process(inputImage)
                                .addOnSuccessListener { barcodes ->
                                    val value = barcodes.firstOrNull()?.rawValue
                                    if (!value.isNullOrBlank() && !hasEmitted) {
                                        hasEmitted = true
                                        onQrDetected(value)
                                    }
                                }
                                .addOnCompleteListener {
                                    imageProxy.close()
                                }
                        } else {
                            imageProxy.close()
                        }
                    }
                }

            runCatching {
                provider.unbindAll()
                provider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    analysis
                )
            }
        }
        cameraProviderFuture.addListener(listener, ContextCompat.getMainExecutor(context))

        onDispose {
            if (cameraProviderFuture.isDone) {
                runCatching { cameraProviderFuture.get().unbindAll() }
            }
            runCatching { scanner.close() }
            analysisExecutor.shutdownNow()
        }
    }

    AndroidView(
        factory = { previewView },
        modifier = Modifier.fillMaxSize()
    )
}
