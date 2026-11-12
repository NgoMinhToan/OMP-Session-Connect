@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.ompsessionconnect.ui.scanner

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn as AndroidXOptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.common.InputImage
import com.ompsessionconnect.R
import com.ompsessionconnect.ui.AppViewModel
import com.ompsessionconnect.ui.components.AddLinkDialog
import com.ompsessionconnect.ui.components.GlassIconButton
import com.ompsessionconnect.ui.theme.LiveGlassShapes
import com.ompsessionconnect.ui.theme.LocalLiveGlass
import com.ompsessionconnect.ui.theme.LocalSessionStatusColors
import androidx.core.view.WindowCompat
import kotlinx.coroutines.launch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

@Composable
fun QrScannerScreen(
  appViewModel: AppViewModel,
  onBack: () -> Unit,
  onAdded: (String) -> Unit,
) {
  val context = LocalContext.current
  val view = LocalView.current
  val tokens = LocalLiveGlass.current
  val status = LocalSessionStatusColors.current
  val invalidMessage = stringResource(R.string.scan_invalid)
  val scope = rememberCoroutineScope()
  var granted by remember {
    mutableStateOf(
      ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
        PackageManager.PERMISSION_GRANTED,
    )
  }
  var error by remember { mutableStateOf<String?>(null) }
  var adding by remember { mutableStateOf(false) }
  var torchOn by remember { mutableStateOf(false) }
  var showAddDialog by remember { mutableStateOf(false) }
  var camera by remember { mutableStateOf<Camera?>(null) }
  val scanEnabled = remember { AtomicBoolean(true) }
  val permissionLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.RequestPermission(),
  ) { granted = it }
  val scannerBackground = remember {
    Brush.verticalGradient(
      listOf(Color(0xFF071318), Color(0xFF0C1230), Color(0xFF1B1030)),
    )
  }

  DisposableEffect(tokens.dark) {
    val window = (context as? Activity)?.window
    val controller = window?.let { WindowCompat.getInsetsController(it, view) }
    controller?.isAppearanceLightStatusBars = false
    controller?.isAppearanceLightNavigationBars = false
    onDispose {
      controller?.isAppearanceLightStatusBars = !tokens.dark
      controller?.isAppearanceLightNavigationBars = !tokens.dark
    }
  }

  LaunchedEffect(Unit) {
    if (!granted) permissionLauncher.launch(Manifest.permission.CAMERA)
  }

  fun submit(link: String) {
    if (adding) return
    adding = true
    error = null
    scope.launch {
      appViewModel.add(link)
        .onSuccess { summary ->
          adding = false
          onAdded(summary.id)
        }
        .onFailure { throwable ->
          adding = false
          error = throwable.message ?: invalidMessage
          scanEnabled.set(true)
        }
    }
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(scannerBackground),
  ) {
    if (granted) {
      CameraPreview(
        onCameraReady = { camera = it },
        onQrCode = { raw ->
          if (scanEnabled.compareAndSet(true, false)) submit(raw)
        },
      )
      ScannerOverlay()
    } else {
      CameraPermissionPrompt(
        onRequest = { permissionLauncher.launch(Manifest.permission.CAMERA) },
      )
    }

    Row(
      modifier = Modifier
        .fillMaxWidth()
        .statusBarsPadding()
        .padding(horizontal = 12.dp, vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      GlassIconButton(
        icon = Icons.AutoMirrored.Outlined.ArrowBack,
        contentDescription = stringResource(R.string.action_back),
        onClick = onBack,
      )
      Text(
        text = stringResource(R.string.scan_title),
        style = MaterialTheme.typography.titleSmall,
        color = Color.White,
        modifier = Modifier.weight(1f),
      )
      if (granted) {
        Surface(
          shape = LiveGlassShapes.Pill,
          color = tokens.glass,
          contentColor = tokens.ink,
          border = BorderStroke(1.dp, tokens.edge),
        ) {
          TextButton(
            onClick = {
              torchOn = !torchOn
              camera?.cameraControl?.enableTorch(torchOn)
            },
          ) {
            Text(
              text = stringResource(
                if (torchOn) R.string.action_torch_off else R.string.action_torch_on,
              ),
              color = tokens.ink,
            )
          }
        }
      }
    }

    Column(
      modifier = Modifier
        .align(Alignment.BottomCenter)
        .navigationBarsPadding()
        .padding(20.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      error?.let { message ->
        Surface(
          shape = LiveGlassShapes.Room,
          color = tokens.sheetOpaque,
          contentColor = status.errorText,
          border = BorderStroke(1.dp, status.error.copy(alpha = 0.4f)),
          modifier = Modifier.padding(bottom = 10.dp),
        ) {
          Text(
            text = message,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            style = MaterialTheme.typography.bodyMedium,
          )
        }
      }
      if (adding) {
        Surface(
          shape = LiveGlassShapes.Pill,
          color = tokens.sheetOpaque,
          contentColor = tokens.ink,
          modifier = Modifier.padding(bottom = 10.dp),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            CircularProgressIndicator(
              modifier = Modifier.size(16.dp),
              strokeWidth = 2.dp,
              color = tokens.primary,
            )
            Text(stringResource(R.string.status_connecting), color = tokens.ink)
          }
        }
      }
      if (granted) {
        Surface(
          shape = LiveGlassShapes.Pill,
          color = tokens.sheetOpaque,
          contentColor = tokens.ink,
          border = BorderStroke(1.dp, tokens.edge),
        ) {
          Text(
            text = stringResource(R.string.scan_hint),
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = tokens.ink,
          )
        }
      }
      Spacer(Modifier.height(8.dp))
      Surface(
        shape = LiveGlassShapes.Pill,
        color = tokens.glass,
        contentColor = tokens.ink,
        border = BorderStroke(1.dp, tokens.edge),
      ) {
        TextButton(onClick = { showAddDialog = true }) {
          Text(stringResource(R.string.action_paste_link), color = tokens.ink)
        }
      }
    }
  }

  if (showAddDialog) {
    AddLinkDialog(
      onDismiss = { showAddDialog = false },
      onAdd = { link -> appViewModel.add(link) },
      onAdded = { sessionId ->
        showAddDialog = false
        onAdded(sessionId)
      },
    )
  }
}

@Composable
private fun CameraPreview(
  onCameraReady: (Camera) -> Unit,
  onQrCode: (String) -> Unit,
) {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current
  val previewView = remember {
    PreviewView(context).apply {
      scaleType = PreviewView.ScaleType.FILL_CENTER
      implementationMode = PreviewView.ImplementationMode.COMPATIBLE
    }
  }
  val analyzer = remember { QrAnalyzer(onQrCode) }
  val executor = remember { Executors.newSingleThreadExecutor() }
  var provider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
  var analysis by remember { mutableStateOf<ImageAnalysis?>(null) }

  DisposableEffect(lifecycleOwner) {
    val future = ProcessCameraProvider.getInstance(context)
    val mainExecutor = ContextCompat.getMainExecutor(context)
    future.addListener({
      val cameraProvider = future.get()
      provider = cameraProvider
      val preview = Preview.Builder().build().also {
        it.surfaceProvider = previewView.surfaceProvider
      }
      val imageAnalysis = ImageAnalysis.Builder()
        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
        .build()
        .also { it.setAnalyzer(executor, analyzer) }
      analysis = imageAnalysis
      runCatching {
        cameraProvider.unbindAll()
        val camera = cameraProvider.bindToLifecycle(
          lifecycleOwner,
          CameraSelector.DEFAULT_BACK_CAMERA,
          preview,
          imageAnalysis,
        )
        onCameraReady(camera)
      }
    }, mainExecutor)

    onDispose {
      analysis?.clearAnalyzer()
      provider?.unbindAll()
      analyzer.close()
      executor.shutdown()
    }
  }

  AndroidView(
    factory = { previewView },
    modifier = Modifier.fillMaxSize(),
  )
}

@Composable
private fun ScannerOverlay() {
  val accent = LocalLiveGlass.current.primary
  Canvas(modifier = Modifier.fillMaxSize()) {
    val boxSize = size.minDimension * 0.68f
    val left = (size.width - boxSize) / 2f
    val top = (size.height - boxSize) / 2.4f
    val rect = Rect(left, top, left + boxSize, top + boxSize)
    val corner = CornerRadius(24.dp.toPx())
    val path = Path().apply {
      fillType = PathFillType.EvenOdd
      addRect(Rect(Offset.Zero, size))
      addRoundRect(RoundRect(rect, corner))
    }
    drawPath(path, Color.Black.copy(alpha = 0.55f))
    drawRoundRect(
      color = accent,
      topLeft = rect.topLeft,
      size = rect.size,
      cornerRadius = corner,
      style = Stroke(width = 3.dp.toPx()),
    )
  }
}

@Composable
private fun CameraPermissionPrompt(onRequest: () -> Unit) {
  val tokens = LocalLiveGlass.current
  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(32.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
  ) {
    Icon(
      painterResource(R.drawable.ic_qr_code),
      contentDescription = null,
      modifier = Modifier.size(64.dp),
      tint = tokens.primary,
    )
    Spacer(Modifier.height(16.dp))
    Text(
      text = stringResource(R.string.scan_permission_title),
      style = MaterialTheme.typography.titleLarge,
      color = Color.White,
    )
    Spacer(Modifier.height(8.dp))
    Text(
      text = stringResource(R.string.scan_permission_body),
      style = MaterialTheme.typography.bodyMedium,
      color = Color.White.copy(alpha = 0.75f),
    )
    Spacer(Modifier.height(20.dp))
    Button(
      onClick = onRequest,
      shape = LiveGlassShapes.Pill,
      colors = androidx.compose.material3.ButtonDefaults.buttonColors(
        containerColor = tokens.primary,
        contentColor = tokens.onPrimary,
      ),
    ) {
      Text(stringResource(R.string.action_allow_camera))
    }
  }
}

@AndroidXOptIn(markerClass = [ExperimentalGetImage::class])
private class QrAnalyzer(
  private val onQrCode: (String) -> Unit,
) : ImageAnalysis.Analyzer {
  private val scanner = BarcodeScanning.getClient(
    BarcodeScannerOptions.Builder()
      .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
      .build(),
  )
  private val main = Handler(Looper.getMainLooper())

  override fun analyze(image: ImageProxy) {
    val mediaImage = image.image
    if (mediaImage == null) {
      image.close()
      return
    }
    scanner.process(InputImage.fromMediaImage(mediaImage, image.imageInfo.rotationDegrees))
      .addOnSuccessListener { barcodes ->
        barcodes.firstOrNull { !it.rawValue.isNullOrBlank() }?.rawValue?.let { value ->
          main.post { onQrCode(value) }
        }
      }
      .addOnCompleteListener { image.close() }
  }

  fun close() {
    scanner.close()
  }
}
