@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.ompsessionconnect.ui.scanner

import android.Manifest
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
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
          error = throwable.message ?: context.getString(R.string.scan_invalid)
          scanEnabled.set(true)
        }
    }
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text(stringResource(R.string.scan_title)) },
        navigationIcon = {
          IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.action_back))
          }
        },
        actions = {
          if (granted) {
            TextButton(
              onClick = {
                torchOn = !torchOn
                camera?.cameraControl?.enableTorch(torchOn)
              },
            ) {
              Text(
                stringResource(
                  if (torchOn) R.string.action_torch_off else R.string.action_torch_on,
                ),
              )
            }
          }
        },
      )
    },
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding),
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

      Column(
        modifier = Modifier
          .align(Alignment.BottomCenter)
          .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        error?.let { message ->
          Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.padding(bottom = 12.dp),
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
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.padding(bottom = 12.dp),
          ) {
            Surface(
              color = Color.Transparent,
            ) {
              androidx.compose.foundation.layout.Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                CircularProgressIndicator(
                  modifier = Modifier.size(16.dp),
                  strokeWidth = 2.dp,
                )
                Text(stringResource(R.string.status_connecting))
              }
            }
          }
        }
        if (granted) {
          Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
            contentColor = MaterialTheme.colorScheme.onSurface,
          ) {
            Text(
              text = stringResource(R.string.scan_hint),
              modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
              style = MaterialTheme.typography.bodyMedium,
            )
          }
        }
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = { showAddDialog = true }) {
          Text(stringResource(R.string.action_paste_link))
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
  val accent = MaterialTheme.colorScheme.primary
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
      tint = MaterialTheme.colorScheme.primary,
    )
    Spacer(Modifier.height(16.dp))
    Text(
      text = stringResource(R.string.scan_permission_title),
      style = MaterialTheme.typography.titleLarge,
    )
    Spacer(Modifier.height(8.dp))
    Text(
      text = stringResource(R.string.scan_permission_body),
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(20.dp))
    Button(onClick = onRequest) {
      Text(stringResource(R.string.action_allow_camera))
    }
  }
}

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
