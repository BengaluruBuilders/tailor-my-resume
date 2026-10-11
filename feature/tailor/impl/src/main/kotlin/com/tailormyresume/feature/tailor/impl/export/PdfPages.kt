package com.tailormyresume.feature.tailor.impl.export

import android.graphics.Canvas
import android.graphics.pdf.PdfDocument
import java.io.OutputStream

internal interface PdfPages {
    fun startPage(width: Int, height: Int, number: Int): Canvas

    fun finishPage()
}

internal class AndroidPdfPages(private val pdf: PdfDocument = PdfDocument()) : PdfPages {

    private var page: PdfDocument.Page? = null

    override fun startPage(width: Int, height: Int, number: Int): Canvas {
        val started = pdf.startPage(PdfDocument.PageInfo.Builder(width, height, number).create())
        page = started
        return started.canvas
    }

    override fun finishPage() {
        page?.let(pdf::finishPage)
        page = null
    }

    fun writeTo(out: OutputStream) = pdf.writeTo(out)

    fun close() = pdf.close()
}
