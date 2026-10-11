package com.tailormyresume.feature.tailor.impl.export

import android.graphics.Bitmap
import android.graphics.Canvas

internal class BitmapPdfPages : PdfPages {

    val pageSizes = mutableListOf<Pair<Int, Int>>()

    private var bitmap: Bitmap? = null

    override fun startPage(width: Int, height: Int, number: Int): Canvas {
        pageSizes += width to height
        val created = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap = created
        return Canvas(created)
    }

    override fun finishPage() {
        bitmap?.recycle()
        bitmap = null
    }
}
