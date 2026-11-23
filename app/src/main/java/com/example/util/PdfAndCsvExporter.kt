package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.model.BusinessProfile
import com.example.data.model.Customer
import com.example.data.model.Expense
import com.example.data.model.LedgerTransaction
import com.example.data.model.SaleRecord
import com.example.data.model.Supplier
import com.example.data.model.TxnType
import java.io.File
import java.io.FileOutputStream

object PdfAndCsvExporter {

    private fun exportDir(context: Context): File {
        val dir = File(context.cacheDir, "exports")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun uriForFile(context: Context, file: File): Uri {
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    private fun drawLogoTopRight(
        context: Context,
        canvas: Canvas,
        business: BusinessProfile?,
        pageWidth: Int,
        topY: Float = 28f
    ) {
        val bmp = BrandingImageHelper.loadBitmap(context, business?.logoUri) ?: return
        val maxDim = 54f
        val ratio = minOf(maxDim / bmp.width.coerceAtLeast(1), maxDim / bmp.height.coerceAtLeast(1))
        val w = bmp.width * ratio
        val h = bmp.height * ratio
        val left = pageWidth - 36f - w
        val dest = RectF(left, topY, left + w, topY + h)
        canvas.drawBitmap(bmp, null, dest, null)
        bmp.recycle()
    }

    private fun drawSignatureFooter(
        context: Context,
        canvas: Canvas,
        business: BusinessProfile?,
        pageWidth: Int,
        y: Float,
        labelPaint: Paint
    ) {
        val bmp = BrandingImageHelper.loadBitmap(context, business?.signatureUri) ?: return
        val maxW = 128f
        val maxH = 46f
        val ratio = minOf(maxW / bmp.width.coerceAtLeast(1), maxH / bmp.height.coerceAtLeast(1))
        val w = bmp.width * ratio
        val h = bmp.height * ratio
        val left = pageWidth - 36f - 130f
        val dest = RectF(left, y, left + w, y + h)
        canvas.drawBitmap(bmp, null, dest, null)
        bmp.recycle()
        canvas.drawText("Authorized Signatory", left, y + h + 14f, labelPaint)
        business?.businessName?.takeIf { it.isNotBlank() }?.let {
            canvas.drawText("For $it", left, y + h + 28f, labelPaint)
        }
    }

    fun generatePartyStatementPdf(
        context: Context,
        business: BusinessProfile?,
        partyName: String,
        partyPhone: String?,
        currentBalance: Double,
        isCustomer: Boolean,
        transactions: List<LedgerTransaction>
    ): Uri {
        val doc = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        var pageNum = 1
        var page = doc.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create())
        var canvas = page.canvas

        val titlePaint = Paint().apply {
            color = Color.parseColor("#0D4C5E")
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val subtitlePaint = Paint().apply {
            color = Color.DKGRAY
            textSize = 11f
        }
        val boldPaint = Paint().apply {
            color = Color.BLACK
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val bodyPaint = Paint().apply {
            color = Color.BLACK
            textSize = 10f
        }
        val greenPaint = Paint().apply {
            color = Color.parseColor("#059669")
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val redPaint = Paint().apply {
            color = Color.parseColor("#DC2626")
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val linePaint = Paint().apply {
            color = Color.LTGRAY
            strokeWidth = 1f
        }

        var y = 44f
        drawLogoTopRight(context, canvas, business, pageWidth)
        canvas.drawText(business?.businessName ?: "KhataMitra Business", 36f, y, titlePaint)
        y += 18f
        canvas.drawText(
            "Phone: ${business?.phone ?: "-"}   UPI: ${business?.upiId ?: "-"}   GST: ${business?.gstNumber ?: "-"}",
            36f, y, subtitlePaint
        )
        y += 22f
        canvas.drawLine(36f, y, pageWidth - 36f, y, linePaint)
        y += 22f

        canvas.drawText("Ledger Statement: $partyName", 36f, y, boldPaint)
        y += 16f
        canvas.drawText("Mobile: ${partyPhone ?: "N/A"}   Generated: ${Formatters.formatDateTime(System.currentTimeMillis())}", 36f, y, subtitlePaint)
        y += 18f
        val balLabel = when {
            isCustomer && currentBalance > 0 -> "Net Receivable (You Will Get): ${Formatters.formatRupee(currentBalance)}"
            isCustomer && currentBalance < 0 -> "Net Advance (You Will Give): ${Formatters.formatRupee(-currentBalance)}"
            !isCustomer && currentBalance > 0 -> "Net Payable to Supplier: ${Formatters.formatRupee(currentBalance)}"
            !isCustomer && currentBalance < 0 -> "Advance with Supplier: ${Formatters.formatRupee(-currentBalance)}"
            else -> "Net Balance: Settled (₹0)"
        }
        canvas.drawText(balLabel, 36f, y, if (currentBalance >= 0) redPaint else greenPaint)
        y += 24f

        // Table Header
        canvas.drawLine(36f, y, pageWidth - 36f, y, linePaint)
        y += 14f
        canvas.drawText("Date", 36f, y, boldPaint)
        canvas.drawText("Type / Mode", 140f, y, boldPaint)
        canvas.drawText("Notes", 270f, y, boldPaint)
        canvas.drawText("Amount", 490f, y, boldPaint)
        y += 8f
        canvas.drawLine(36f, y, pageWidth - 36f, y, linePaint)
        y += 16f

        for (txn in transactions) {
            if (y > pageHeight - 50) {
                doc.finishPage(page)
                pageNum++
                page = doc.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create())
                canvas = page.canvas
                y = 44f
            }
            val tType = TxnType.fromRaw(txn.txnType)
            canvas.drawText(Formatters.formatShortDay(txn.txnTimeMillis), 36f, y, bodyPaint)
            canvas.drawText("${tType.label} (${txn.paymentMode})", 140f, y, bodyPaint)
            canvas.drawText((txn.description ?: "-").take(34), 270f, y, bodyPaint)
            val amtPaint = if (txn.balanceImpact >= 0) redPaint else greenPaint
            canvas.drawText(Formatters.formatRupee(txn.amount), 490f, y, amtPaint)
            y += 18f
        }

        if (y > pageHeight - 105) {
            doc.finishPage(page)
            pageNum++
            page = doc.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create())
            canvas = page.canvas
            y = 44f
        }
        drawSignatureFooter(context, canvas, business, pageWidth, y + 14f, subtitlePaint)

        doc.finishPage(page)
        val file = File(exportDir(context), "KhataMitra_Statement_${System.currentTimeMillis()}.pdf")
        FileOutputStream(file).use { doc.writeTo(it) }
        doc.close()
        return uriForFile(context, file)
    }

    fun generateSaleInvoicePdf(
        context: Context,
        business: BusinessProfile?,
        sale: SaleRecord
    ): Uri {
        val doc = PdfDocument()
        val page = doc.startPage(PdfDocument.PageInfo.Builder(595, 842, 1).create())
        val canvas = page.canvas
        val titlePaint = Paint().apply {
            color = Color.parseColor("#0D4C5E")
            textSize = 22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val boldPaint = Paint().apply {
            color = Color.BLACK
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val bodyPaint = Paint().apply {
            color = Color.DKGRAY
            textSize = 11f
        }
        val linePaint = Paint().apply {
            color = Color.LTGRAY
            strokeWidth = 1f
        }

        var y = 48f
        drawLogoTopRight(context, canvas, business, 595, topY = 30f)
        canvas.drawText("TAX / RETAIL INVOICE", 36f, y, titlePaint)
        y += 24f
        canvas.drawText(business?.businessName ?: "KhataMitra Business", 36f, y, boldPaint)
        y += 16f
        canvas.drawText("Owner: ${business?.ownerName ?: "-"} | Ph: ${business?.phone ?: "-"}", 36f, y, bodyPaint)
        y += 15f
        canvas.drawText("GSTIN: ${business?.gstNumber ?: "N/A"} | UPI: ${business?.upiId ?: "N/A"}", 36f, y, bodyPaint)
        y += 20f
        canvas.drawLine(36f, y, 559f, y, linePaint)
        y += 22f

        canvas.drawText("Invoice No: ${sale.invoiceNumber}", 36f, y, boldPaint)
        canvas.drawText("Date: ${Formatters.formatDate(sale.saleTimeMillis)}", 380f, y, bodyPaint)
        y += 18f
        canvas.drawText("Billed To: ${sale.customerName}", 36f, y, boldPaint)
        y += 24f

        canvas.drawLine(36f, y, 559f, y, linePaint)
        y += 16f
        canvas.drawText("Item", 36f, y, boldPaint)
        canvas.drawText("Qty", 250f, y, boldPaint)
        canvas.drawText("Rate", 320f, y, boldPaint)
        canvas.drawText("Tax %", 400f, y, boldPaint)
        canvas.drawText("Total", 480f, y, boldPaint)
        y += 10f
        canvas.drawLine(36f, y, 559f, y, linePaint)
        y += 18f

        canvas.drawText(sale.productName.take(30), 36f, y, bodyPaint)
        canvas.drawText(sale.quantity.toString(), 250f, y, bodyPaint)
        canvas.drawText(Formatters.formatRupee(sale.unitPrice), 320f, y, bodyPaint)
        canvas.drawText("${sale.taxPercent}%", 400f, y, bodyPaint)
        canvas.drawText(Formatters.formatRupee(sale.totalAmount), 480f, y, boldPaint)
        y += 28f

        canvas.drawLine(36f, y, 559f, y, linePaint)
        y += 20f
        canvas.drawText("Discount: ${Formatters.formatRupee(sale.discount)}", 360f, y, bodyPaint)
        y += 16f
        canvas.drawText("Paid (${sale.paymentMode}): ${Formatters.formatRupee(sale.paidAmount)}", 360f, y, bodyPaint)
        y += 16f
        canvas.drawText("Balance Due: ${Formatters.formatRupee(sale.remainingBalance)}", 360f, y, boldPaint)
        y += 32f
        drawSignatureFooter(context, canvas, business, 595, y, bodyPaint)
        canvas.drawText("Thank you for your business! Powered by KhataMitra.", 36f, y + 60f, bodyPaint)

        doc.finishPage(page)
        val file = File(exportDir(context), "Invoice_${sale.invoiceNumber}.pdf")
        FileOutputStream(file).use { doc.writeTo(it) }
        doc.close()
        return uriForFile(context, file)
    }

    fun generateBusinessReportPdf(
        context: Context,
        business: BusinessProfile?,
        timeframeLabel: String,
        totalReceivable: Double,
        totalPayable: Double,
        totalSales: Double,
        totalExpenses: Double,
        totalCollected: Double,
        totalCreditGiven: Double
    ): Uri {
        val doc = PdfDocument()
        val page = doc.startPage(PdfDocument.PageInfo.Builder(595, 842, 1).create())
        val canvas = page.canvas
        val titlePaint = Paint().apply {
            color = Color.parseColor("#0D4C5E")
            textSize = 22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val boldPaint = Paint().apply {
            color = Color.BLACK
            textSize = 13f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val bodyPaint = Paint().apply {
            color = Color.DKGRAY
            textSize = 12f
        }

        var y = 48f
        drawLogoTopRight(context, canvas, business, 595, topY = 30f)
        canvas.drawText("${business?.businessName ?: "KhataMitra"} - Business Report", 36f, y, titlePaint)
        y += 22f
        canvas.drawText("Period: $timeframeLabel   Generated: ${Formatters.formatDateTime(System.currentTimeMillis())}", 36f, y, bodyPaint)
        y += 36f

        val rows = listOf(
            "Total Receivable (Udhar)" to Formatters.formatRupee(totalReceivable),
            "Total Payable (Suppliers)" to Formatters.formatRupee(totalPayable),
            "Credit Given ($timeframeLabel)" to Formatters.formatRupee(totalCreditGiven),
            "Collections Received ($timeframeLabel)" to Formatters.formatRupee(totalCollected),
            "Total Sales ($timeframeLabel)" to Formatters.formatRupee(totalSales),
            "Total Expenses ($timeframeLabel)" to Formatters.formatRupee(totalExpenses),
            "Net Operating Cashflow" to Formatters.formatRupee(totalCollected + totalSales - totalExpenses)
        )
        for ((label, value) in rows) {
            canvas.drawText(label, 36f, y, bodyPaint)
            canvas.drawText(value, 400f, y, boldPaint)
            y += 26f
        }
        drawSignatureFooter(context, canvas, business, 595, y + 24f, bodyPaint)

        doc.finishPage(page)
        val file = File(exportDir(context), "KhataMitra_Report_${System.currentTimeMillis()}.pdf")
        FileOutputStream(file).use { doc.writeTo(it) }
        doc.close()
        return uriForFile(context, file)
    }

    private fun csvCell(raw: String?): String {
        val clean = (raw ?: "").replace("\"", "\"\"")
        return "\"$clean\""
    }

    fun exportFullLedgerCsv(
        context: Context,
        customers: List<Customer>,
        suppliers: List<Supplier>,
        transactions: List<LedgerTransaction>,
        expenses: List<Expense>,
        sales: List<SaleRecord>
    ): Uri {
        val sb = StringBuilder()
        sb.appendLine("RecordType,ID,Name,Phone,TypeOrCategory,Amount,BalanceImpact,Mode,Status,Timestamp")
        for (c in customers) {
            sb.appendLine("CUSTOMER,${c.id},${csvCell(c.name)},${csvCell(c.phone)},${csvCell(c.category)},${c.currentBalance},${c.openingBalance},,,")
        }
        for (s in suppliers) {
            sb.appendLine("SUPPLIER,${s.id},${csvCell(s.name)},${csvCell(s.phone)},${csvCell(s.companyName)},${s.currentBalance},${s.openingBalance},,,")
        }
        for (t in transactions) {
            sb.appendLine("TRANSACTION,${t.id},${csvCell(t.partyName)},,${t.txnType},${t.amount},${t.balanceImpact},${t.paymentMode},${t.status},${Formatters.formatDateTime(t.txnTimeMillis)}")
        }
        for (e in expenses) {
            sb.appendLine("EXPENSE,${e.id},${csvCell(e.notes)},,${csvCell(e.category)},${e.amount},,${e.paymentMode},,${Formatters.formatDateTime(e.expenseTimeMillis)}")
        }
        for (s in sales) {
            sb.appendLine("SALE,${s.id},${csvCell(s.customerName)},,${csvCell(s.productName)},${s.totalAmount},${s.remainingBalance},${s.paymentMode},${s.invoiceNumber},${Formatters.formatDateTime(s.saleTimeMillis)}")
        }
        val file = File(exportDir(context), "KhataMitra_Backup_${System.currentTimeMillis()}.csv")
        file.writeText(sb.toString(), Charsets.UTF_8)
        return uriForFile(context, file)
    }

    fun exportQrBitmapUri(context: Context, upiUri: String, businessName: String): Uri {
        val matrix = QrCodeGenerator.encode(upiUri)
        val dim = matrix.size
        val scale = 16
        val padding = 48
        val imgSize = dim * scale + padding * 2
        val bmp = Bitmap.createBitmap(imgSize, imgSize + 60, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.drawColor(Color.WHITE)
        val blackPaint = Paint().apply { color = Color.parseColor("#0D4C5E") }
        for (y in 0 until dim) {
            for (x in 0 until dim) {
                if (matrix[y][x]) {
                    canvas.drawRect(
                        (padding + x * scale).toFloat(),
                        (padding + y * scale).toFloat(),
                        (padding + (x + 1) * scale).toFloat(),
                        (padding + (y + 1) * scale).toFloat(),
                        blackPaint
                    )
                }
            }
        }
        val textPaint = Paint().apply {
            color = Color.BLACK
            textSize = 24f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("Pay $businessName via UPI", imgSize / 2f, (imgSize + 28).toFloat(), textPaint)
        val file = File(exportDir(context), "KhataMitra_UPI_QR_${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        return uriForFile(context, file)
    }
}
