package com.freshsave.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter

/**
 * Draws a scannable QR code. ZXing turns [content] into a grid of on/off cells (a BitMatrix)
 * and we paint each "on" cell as a square, so it stays sharp at any size.
 */
@Composable
fun QrCode(
    content: String,
    modifier: Modifier = Modifier,
    foreground: Color = Color(0xFF1A1F1C),
    background: Color = Color.White,
) {
    // remember(...) means we only re-encode when the text changes, not on every redraw.
    val matrix: BitMatrix = remember(content) {
        QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, 0, 0, mapOf(EncodeHintType.MARGIN to 0))
    }
    Canvas(modifier) {
        drawRect(background)
        val cell = size.minDimension / matrix.width
        for (y in 0 until matrix.height) {
            for (x in 0 until matrix.width) {
                if (matrix[x, y]) {
                    // +0.5px overlap avoids hairline gaps between neighbouring cells.
                    drawRect(foreground, Offset(x * cell, y * cell), Size(cell + 0.5f, cell + 0.5f))
                }
            }
        }
    }
}

@Preview
@Composable
private fun QrPreview() {
    QrCode("FRESHSAVE:o-4f29:4F29", Modifier.size(140.dp))
}
