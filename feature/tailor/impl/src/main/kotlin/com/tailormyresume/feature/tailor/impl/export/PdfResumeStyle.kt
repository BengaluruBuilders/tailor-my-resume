package com.tailormyresume.feature.tailor.impl.export

import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.text.TextPaint

internal enum class FitPass(val bodySize: Float, val lineSpacing: Float) {
    Regular(bodySize = 10.5f, lineSpacing = 1.12f),
    Compact(bodySize = 9.5f, lineSpacing = 1.04f),
}

internal class PdfResumeStyle(pass: FitPass = FitPass.Regular) {
    val lineSpacing: Float = pass.lineSpacing
    val name: TextPaint = textPaint(size = 22f, bold = true, color = INK)
    val headline: TextPaint = textPaint(size = 11f, bold = false, color = INK)
    val contact: TextPaint = textPaint(size = 10f, bold = false, color = MUTED)
    val sectionHeading: TextPaint = textPaint(size = 12.5f, bold = true, color = INK)
    val entryTitle: TextPaint = textPaint(size = 11f, bold = true, color = INK)
    val entryDetail: TextPaint = textPaint(size = 10f, bold = false, color = MUTED)
    val body: TextPaint = textPaint(size = pass.bodySize, bold = false, color = INK)
    val rule: Paint = Paint().apply {
        color = RULE
        strokeWidth = 0.8f
    }

    private fun textPaint(size: Float, bold: Boolean, color: Int): TextPaint =
        TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = size
            this.color = color
            typeface = Typeface.create(Typeface.SANS_SERIF, if (bold) Typeface.BOLD else Typeface.NORMAL)
        }

    private companion object {
        val INK = Color.rgb(17, 17, 17)
        val MUTED = Color.rgb(85, 85, 85)
        val RULE = Color.rgb(170, 170, 170)
    }
}
