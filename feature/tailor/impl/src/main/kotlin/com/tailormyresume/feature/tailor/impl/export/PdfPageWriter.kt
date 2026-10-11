package com.tailormyresume.feature.tailor.impl.export

import android.graphics.Canvas
import android.graphics.Paint
import android.text.StaticLayout
import com.tailormyresume.core.model.PageSize

internal class PdfPageWriter(private val pages: PdfPages, pageSize: PageSize) {

    private val pageWidth: Float = when (pageSize) {
        PageSize.A4 -> A4_WIDTH
        PageSize.LETTER -> LETTER_WIDTH
    }
    private val pageHeight: Float = when (pageSize) {
        PageSize.A4 -> A4_HEIGHT
        PageSize.LETTER -> LETTER_HEIGHT
    }

    private var pageNumber = 0
    private var cursorY = MARGIN
    private var canvas: Canvas = startPage()

    val pageCount: Int get() = pageNumber

    val contentWidth: Int get() = (pageWidth - 2 * MARGIN).toInt()

    fun drawBlock(layout: StaticLayout, keepWithNext: Float = 0f) {
        drawLines(layout, MARGIN, marker = null, keepWithNext = keepWithNext)
    }

    fun drawHanging(marker: StaticLayout, body: StaticLayout, indent: Float) {
        drawLines(body, MARGIN + indent, marker = marker, keepWithNext = 0f)
    }

    fun drawRule(paint: Paint) {
        canvas.drawLine(MARGIN, cursorY, pageWidth - MARGIN, cursorY, paint)
    }

    fun space(points: Float) {
        cursorY += points
    }

    fun finish() {
        pages.finishPage()
    }

    private fun drawLines(layout: StaticLayout, x: Float, marker: StaticLayout?, keepWithNext: Float) {
        if (layout.lineCount == 0) return
        if (Pagination.shouldBreakBefore(pageSpace(), layout.height.toFloat(), keepWithNext)) nextPage()
        val chunks = Pagination.planChunks(
            lineCount = layout.lineCount,
            space = pageSpace(),
            lineTop = { layout.getLineTop(it).toFloat() },
            lineBottom = { layout.getLineBottom(it).toFloat() },
        )
        chunks.forEachIndexed { index, chunk ->
            if (chunk.startsNewPage) nextPage()
            drawChunk(layout, chunk, x, if (index == 0) marker else null)
        }
    }

    private fun drawChunk(layout: StaticLayout, chunk: LineChunk, x: Float, marker: StaticLayout?) {
        val top = layout.getLineTop(chunk.firstLine).toFloat()
        val bottom = layout.getLineBottom(chunk.endLine - 1).toFloat()
        marker?.let {
            canvas.save()
            canvas.translate(MARGIN, cursorY)
            it.draw(canvas)
            canvas.restore()
        }
        canvas.save()
        canvas.translate(x, cursorY - top)
        canvas.clipRect(0f, top, layout.width.toFloat(), bottom)
        layout.draw(canvas)
        canvas.restore()
        cursorY += bottom - top
    }

    private fun pageSpace(): PageSpace = PageSpace(
        cursorY = cursorY,
        pageTop = MARGIN,
        pageBottom = pageHeight - MARGIN,
    )

    private fun nextPage() {
        pages.finishPage()
        canvas = startPage()
    }

    private fun startPage(): Canvas {
        pageNumber += 1
        cursorY = MARGIN
        return pages.startPage(pageWidth.toInt(), pageHeight.toInt(), pageNumber)
    }

    companion object {
        const val A4_WIDTH = 595f
        const val A4_HEIGHT = 842f
        const val LETTER_WIDTH = 612f
        const val LETTER_HEIGHT = 792f
        const val MARGIN = 48f
    }
}
