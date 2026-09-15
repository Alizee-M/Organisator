package com.organisator.print3d.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.organisator.print3d.data.PhotoCrop
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private const val FRAME_RATIO = 16f / 10f
private const val MAX_ZOOM = 8f

/**
 * Cadrage d'une photo avant de la retenir. L'image s'affiche d'abord en entier :
 * on voit ce qu'on va garder avant de valider, puis on pince pour zoomer et on
 * glisse pour déplacer. Seule la portion visible dans le cadre est conservée.
 */
@Composable
fun PhotoFramingDialog(
    path: String,
    onCancel: () -> Unit,
    onConfirm: (PhotoCrop) -> Unit
) {
    val image by produceState<ImageBitmap?>(initialValue = null, key1 = path) {
        value = withContext(Dispatchers.IO) {
            val file = File(path)
            if (!file.exists()) null
            else runCatching { BitmapFactory.decodeFile(path)?.asImageBitmap() }.getOrNull()
        }
    }

    var zoom by remember(path) { mutableStateOf(1f) }
    var offset by remember(path) { mutableStateOf(Offset.Zero) }
    var frame by remember(path) { mutableStateOf(IntSize.Zero) }

    Dialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Cadrer la photo", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Pincez pour zoomer, glissez pour déplacer. Seule la partie " +
                        "visible dans le cadre sera gardée.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(14.dp))

                val current = image
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(FRAME_RATIO)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outline,
                            shape = RoundedCornerShape(14.dp)
                        )
                        .clipToBounds()
                        .onSizeChanged { frame = it },
                    contentAlignment = Alignment.Center
                ) {
                    if (current == null) {
                        CircularProgressIndicator()
                    } else {
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(FRAME_RATIO)
                                .pointerInput(current, frame) {
                                    detectTransformGestures { _, pan, gestureZoom, _ ->
                                        zoom = (zoom * gestureZoom).coerceIn(1f, MAX_ZOOM)
                                        offset = clampOffset(
                                            offset + pan,
                                            current,
                                            frame,
                                            zoom
                                        )
                                    }
                                }
                        ) {
                            val layout = layoutOf(current, frame, zoom, offset) ?: return@Canvas
                            drawImage(
                                image = current,
                                dstOffset = IntOffset(
                                    layout.left.roundToInt(),
                                    layout.top.roundToInt()
                                ),
                                dstSize = IntSize(
                                    layout.width.roundToInt(),
                                    layout.height.roundToInt()
                                )
                            )
                        }
                    }
                }

                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    TextButton(onClick = {
                        zoom = 1f
                        offset = Offset.Zero
                    }) { Text("Tout afficher") }
                }

                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        onClick = onCancel,
                        modifier = Modifier.weight(1f)
                    ) { Text("Annuler") }
                    Button(
                        onClick = {
                            val source = current
                            if (source == null) onCancel()
                            else onConfirm(cropOf(source, frame, zoom, offset))
                        },
                        enabled = current != null,
                        modifier = Modifier.weight(1f)
                    ) { Text("Valider") }
                }

                if (current == null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Chargement de l'image…",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

/** Position et taille de l'image dessinée, en pixels du cadre. */
private data class ImageLayout(
    val left: Float,
    val top: Float,
    val width: Float,
    val height: Float,
    val scale: Float
)

/**
 * À zoom 1, l'image tient tout entière dans le cadre : c'est l'état initial,
 * celui qui permet de juger le cadrage avant de resserrer.
 */
private fun layoutOf(
    image: ImageBitmap,
    frame: IntSize,
    zoom: Float,
    offset: Offset
): ImageLayout? {
    if (frame.width <= 0 || frame.height <= 0) return null
    val fit = min(
        frame.width.toFloat() / image.width,
        frame.height.toFloat() / image.height
    )
    val scale = fit * zoom
    val width = image.width * scale
    val height = image.height * scale
    return ImageLayout(
        left = (frame.width - width) / 2f + offset.x,
        top = (frame.height - height) / 2f + offset.y,
        width = width,
        height = height,
        scale = scale
    )
}

/** Empêche de faire glisser l'image hors du cadre ; recentre si elle est plus petite. */
private fun clampOffset(
    offset: Offset,
    image: ImageBitmap,
    frame: IntSize,
    zoom: Float
): Offset {
    val layout = layoutOf(image, frame, zoom, Offset.Zero) ?: return Offset.Zero
    val maxX = max(0f, (layout.width - frame.width) / 2f)
    val maxY = max(0f, (layout.height - frame.height) / 2f)
    return Offset(
        x = offset.x.coerceIn(-maxX, maxX),
        y = offset.y.coerceIn(-maxY, maxY)
    )
}

/** Portion de l'image source visible dans le cadre, en fractions de 0 à 1. */
private fun cropOf(
    image: ImageBitmap,
    frame: IntSize,
    zoom: Float,
    offset: Offset
): PhotoCrop {
    val layout = layoutOf(image, frame, zoom, offset)
        ?: return PhotoCrop(0f, 0f, 1f, 1f)
    val scale = layout.scale
    val left = max(0f, -layout.left / scale)
    val top = max(0f, -layout.top / scale)
    val right = min(image.width.toFloat(), (frame.width - layout.left) / scale)
    val bottom = min(image.height.toFloat(), (frame.height - layout.top) / scale)
    return PhotoCrop(
        left = left / image.width,
        top = top / image.height,
        right = right / image.width,
        bottom = bottom / image.height
    )
}
