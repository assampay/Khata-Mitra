package com.example.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.extensions.ExtensionMode
import androidx.camera.extensions.ExtensionsManager
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import com.example.util.BrandingImageHelper
import com.example.util.ScanFilterMode
import com.example.util.ShareAndIntents
import java.io.File
import java.util.concurrent.Executors

enum class CameraCaptureMode {
    PROFILE_PHOTO,
    DOCUMENT_SCAN
}

/**
 * Full-screen Material 3 CameraX dialog supporting both circular Profile Photo capture
 * and enhanced Document Scanning (with live viewfinder guides, torch, lens toggle, and
 * B&W / Crisp Color document scan filters).
 */
@Composable
fun CameraScannerDialog(
    mode: CameraCaptureMode,
    targetFolder: String = if (mode == CameraCaptureMode.PROFILE_PHOTO) "photos" else "scans",
    filePrefix: String = if (mode == CameraCaptureMode.PROFILE_PHOTO) "profile" else "doc",
    maxDimPx: Int = if (mode == CameraCaptureMode.PROFILE_PHOTO) 512 else 1440,
    onDismiss: () -> Unit,
    onImageSaved: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var permissionRequested by remember { mutableStateOf(false) }

    val permLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        permissionRequested = true
    }

    LaunchedEffect(Unit) {
        if (!hasPermission && !permissionRequested) {
            permLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    var useFrontCamera by remember { mutableStateOf(false) }
    var torchEnabled by remember { mutableStateOf(false) }
    var isCapturing by remember { mutableStateOf(false) }
    var capturedFile by remember { mutableStateOf<File?>(null) }
    var filterMode by remember {
        mutableStateOf(
            if (mode == CameraCaptureMode.DOCUMENT_SCAN) ScanFilterMode.CRISP_DOCUMENT
            else ScanFilterMode.ORIGINAL
        )
    }
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var boundCamera by remember { mutableStateOf<Camera?>(null) }
    var cameraError by remember { mutableStateOf<String?>(null) }

    val captureExecutor = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(Unit) {
        onDispose {
            captureExecutor.shutdown()
            previewBitmap?.recycle()
        }
    }

    LaunchedEffect(capturedFile, filterMode) {
        val file = capturedFile
        if (file != null) {
            previewBitmap?.recycle()
            previewBitmap = BrandingImageHelper.decodeCapturedPreview(
                sourceFile = file,
                maxDimPx = maxDimPx,
                filterMode = filterMode,
                squareCrop = mode == CameraCaptureMode.PROFILE_PHOTO
            )
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        BackHandler(onBack = onDismiss)

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFF0A0F14)
        ) {
            if (!hasPermission) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = if (mode == CameraCaptureMode.DOCUMENT_SCAN) Icons.Filled.DocumentScanner else Icons.Filled.PhotoCamera,
                        contentDescription = null,
                        tint = Color(0xFF2DD4BF),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = if (mode == CameraCaptureMode.DOCUMENT_SCAN) "Camera Permission for Document Scan" else "Camera Permission for Profile Photo",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "KhataMitra uses your camera only to capture customer/business photos or scan paper bills & receipts locally.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFCBD5E1),
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(24.dp))
                    Button(
                        onClick = { permLauncher.launch(Manifest.permission.CAMERA) },
                        modifier = Modifier.fillMaxWidth().height(50.dp)
                    ) {
                        Text("Allow Camera Access")
                    }
                    Spacer(Modifier.height(10.dp))
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = Color(0xFF94A3B8))
                    }
                }
            } else if (capturedFile != null && previewBitmap != null) {
                // Review & Enhance captured image
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.White)
                        }
                        Text(
                            text = if (mode == CameraCaptureMode.DOCUMENT_SCAN) "Review Scanned Document" else "Review Profile Photo",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.size(48.dp))
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val bmp = previewBitmap!!
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "Captured preview",
                            contentScale = if (mode == CameraCaptureMode.PROFILE_PHOTO) ContentScale.Crop else ContentScale.Fit,
                            modifier = if (mode == CameraCaptureMode.PROFILE_PHOTO) {
                                Modifier
                                    .size(260.dp)
                                    .clip(CircleShape)
                                    .border(3.dp, Color(0xFF2DD4BF), CircleShape)
                            } else {
                                Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(16.dp))
                                    .border(1.dp, Color(0xFF334155), RoundedCornerShape(16.dp))
                            }
                        )
                    }

                    if (mode == CameraCaptureMode.DOCUMENT_SCAN) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
                        ) {
                            ScanFilterMode.entries.forEach { f ->
                                FilterChip(
                                    selected = filterMode == f,
                                    onClick = { filterMode = f },
                                    label = { Text(f.label) }
                                )
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                capturedFile?.delete()
                                capturedFile = null
                                previewBitmap?.recycle()
                                previewBitmap = null
                            },
                            modifier = Modifier.weight(1f).height(50.dp)
                        ) {
                            Icon(Icons.Filled.Refresh, contentDescription = null, tint = Color.White)
                            Spacer(Modifier.width(6.dp))
                            Text("Retake", color = Color.White)
                        }
                        Button(
                            onClick = {
                                val src = capturedFile ?: return@Button
                                val savedUri = BrandingImageHelper.saveCapturedFileToInternal(
                                    context = context,
                                    sourceFile = src,
                                    folder = targetFolder,
                                    prefix = filePrefix,
                                    maxDimPx = maxDimPx,
                                    filterMode = filterMode,
                                    squareCrop = mode == CameraCaptureMode.PROFILE_PHOTO
                                )
                                src.delete()
                                if (savedUri != null) {
                                    onImageSaved(savedUri)
                                } else {
                                    Toast.makeText(context, "Unable to save image", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .semantics(mergeDescendants = true) {}
                                .testTag("camera_use_photo_button")
                        ) {
                            Icon(Icons.Filled.Check, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text(if (mode == CameraCaptureMode.DOCUMENT_SCAN) "Save Scan" else "Use Photo")
                        }
                    }
                }
            } else {
                // Live CameraX Preview
                val previewView = remember {
                    PreviewView(context).apply {
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                    }
                }

                LaunchedEffect(useFrontCamera) {
                    val providerFuture = ProcessCameraProvider.getInstance(context)
                    providerFuture.addListener({
                        try {
                            val cameraProvider = providerFuture.get()
                            val baseSelector = if (useFrontCamera) {
                                CameraSelector.DEFAULT_FRONT_CAMERA
                            } else {
                                CameraSelector.DEFAULT_BACK_CAMERA
                            }

                            val preview = Preview.Builder().build().also {
                                it.surfaceProvider = previewView.surfaceProvider
                            }
                            val capture = ImageCapture.Builder()
                                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                                .build()

                            val extFuture = ExtensionsManager.getInstanceAsync(context, cameraProvider)
                            extFuture.addListener({
                                val finalSelector = try {
                                    val extMgr = extFuture.get()
                                    if (extMgr.isExtensionAvailable(baseSelector, ExtensionMode.AUTO)) {
                                        extMgr.getExtensionEnabledCameraSelector(baseSelector, ExtensionMode.AUTO)
                                    } else {
                                        baseSelector
                                    }
                                } catch (_: Throwable) {
                                    baseSelector
                                }

                                try {
                                    cameraProvider.unbindAll()
                                    val cam = cameraProvider.bindToLifecycle(
                                        lifecycleOwner,
                                        finalSelector,
                                        preview,
                                        capture
                                    )
                                    boundCamera = cam
                                    imageCapture = capture
                                    cam.cameraControl.enableTorch(torchEnabled && !useFrontCamera)
                                    cameraError = null
                                } catch (e: Exception) {
                                    cameraError = e.localizedMessage ?: "Camera unavailable on this device"
                                }
                            }, ContextCompat.getMainExecutor(context))
                        } catch (e: Exception) {
                            cameraError = e.localizedMessage ?: "Camera initialization failed"
                        }
                    }, ContextCompat.getMainExecutor(context))
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    AndroidView(
                        factory = { previewView },
                        modifier = Modifier.fillMaxSize()
                    )

                    // Viewfinder overlay guide
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        if (mode == CameraCaptureMode.PROFILE_PHOTO) {
                            val radius = size.minDimension * 0.34f
                            drawCircle(
                                color = Color(0xCC2DD4BF),
                                radius = radius,
                                center = center,
                                style = Stroke(width = 4.dp.toPx())
                            )
                        } else {
                            val rectW = size.width * 0.82f
                            val rectH = size.height * 0.56f
                            val left = (size.width - rectW) / 2f
                            val top = (size.height - rectH) / 2f
                            drawRoundRect(
                                color = Color(0xCC2DD4BF),
                                topLeft = Offset(left, top),
                                size = Size(rectW, rectH),
                                cornerRadius = CornerRadius(18.dp.toPx(), 18.dp.toPx()),
                                style = Stroke(width = 3.dp.toPx())
                            )
                        }
                    }

                    // Top controls
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .align(Alignment.TopCenter),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Filled.Close, contentDescription = "Close Camera", tint = Color.White)
                        }
                        Text(
                            text = if (mode == CameraCaptureMode.DOCUMENT_SCAN) "Align Document / Bill Inside Frame" else "Align Face / Logo in Circle",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White,
                            modifier = Modifier
                                .background(Color(0x88000000), RoundedCornerShape(20.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                        if (!useFrontCamera) {
                            IconButton(onClick = {
                                torchEnabled = !torchEnabled
                                boundCamera?.cameraControl?.enableTorch(torchEnabled)
                            }) {
                                Icon(
                                    imageVector = if (torchEnabled) Icons.Filled.FlashOn else Icons.Filled.FlashOff,
                                    contentDescription = "Toggle Flash",
                                    tint = if (torchEnabled) Color(0xFFFACC15) else Color.White
                                )
                            }
                        } else {
                            Spacer(Modifier.size(48.dp))
                        }
                    }

                    if (cameraError != null) {
                        Text(
                            text = cameraError ?: "",
                            color = Color(0xFFFCA5A5),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .background(Color(0xAA000000), RoundedCornerShape(12.dp))
                                .padding(16.dp)
                        )
                    }

                    // Bottom shutter bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 24.dp, vertical = 24.dp)
                            .align(Alignment.BottomCenter),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { useFrontCamera = !useFrontCamera },
                            modifier = Modifier
                                .size(52.dp)
                                .background(Color(0x66000000), CircleShape)
                        ) {
                            Icon(
                                Icons.Filled.Cameraswitch,
                                contentDescription = "Switch Camera",
                                tint = Color.White
                            )
                        }

                        Button(
                            onClick = {
                                val cap = imageCapture ?: return@Button
                                if (isCapturing) return@Button
                                isCapturing = true
                                val tmpFile = File(context.cacheDir, "cam_${System.currentTimeMillis()}.jpg")
                                val opts = ImageCapture.OutputFileOptions.Builder(tmpFile).build()
                                cap.takePicture(
                                    opts,
                                    captureExecutor,
                                    object : ImageCapture.OnImageSavedCallback {
                                        override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                            ContextCompat.getMainExecutor(context).execute {
                                                isCapturing = false
                                                capturedFile = tmpFile
                                            }
                                        }

                                        override fun onError(exception: ImageCaptureException) {
                                            ContextCompat.getMainExecutor(context).execute {
                                                isCapturing = false
                                                Toast.makeText(
                                                    context,
                                                    "Capture failed: ${exception.message}",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }
                                    }
                                )
                            },
                            shape = CircleShape,
                            modifier = Modifier
                                .size(72.dp)
                                .border(4.dp, Color.White, CircleShape)
                                .testTag("camera_shutter_button")
                        ) {
                            if (isCapturing) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    modifier = Modifier.size(28.dp),
                                    strokeWidth = 3.dp
                                )
                            } else {
                                Icon(
                                    imageVector = if (mode == CameraCaptureMode.DOCUMENT_SCAN) Icons.Filled.DocumentScanner else Icons.Filled.PhotoCamera,
                                    contentDescription = "Capture"
                                )
                            }
                        }

                        Spacer(Modifier.size(52.dp))
                    }
                }
            }
        }
    }
}

/**
 * Displays a stored bill/receipt/document with Storage Access Framework ("Save Copy")
 * and FileProvider Share actions.
 */
@Composable
fun DocumentPreviewDialog(
    title: String,
    imageUri: String,
    suggestedFileName: String = "KhataMitra_Document_${System.currentTimeMillis()}.png",
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    val saveLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("image/png")
    ) { destUri ->
        if (destUri != null) {
            val ok = BrandingImageHelper.exportToSafDocument(context, imageUri, destUri)
            Toast.makeText(
                context,
                if (ok) "Document saved to device storage" else "Unable to save document",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                AsyncImage(
                    model = imageUri,
                    contentDescription = title,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(340.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { saveLauncher.launch(suggestedFileName) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.Download, contentDescription = null, Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Save Copy", maxLines = 1)
                    }
                    OutlinedButton(
                        onClick = {
                            val shareUri = BrandingImageHelper.getShareableUri(context, imageUri)
                            if (shareUri != null) {
                                ShareAndIntents.shareFileUri(context, shareUri, "image/png", title)
                            } else {
                                Toast.makeText(context, "Document file unavailable", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.Share, contentDescription = null, Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Share", maxLines = 1)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}
