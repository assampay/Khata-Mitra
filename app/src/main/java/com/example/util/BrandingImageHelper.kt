package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.documentfile.provider.DocumentFile
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.FileOutputStream

enum class ScanFilterMode(val label: String) {
    ORIGINAL("Original"),
    CRISP_DOCUMENT("Crisp Color"),
    BW_DOCUMENT("B&W Scan")
}

object BrandingImageHelper {

    private fun storageDir(context: Context, folder: String): File {
        val dir = File(context.filesDir, folder)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    /**
     * Copies, EXIF-orients, and safely downsamples an image selected from Android Photo Picker
     * into app-private scoped storage (filesDir/<folder>/) so it persists reliably across restarts.
     */
    fun savePickedImageToInternal(
        context: Context,
        sourceUri: Uri,
        prefix: String,
        maxDimPx: Int = 512,
        folder: String = "branding"
    ): String? {
        return try {
            val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                BitmapFactory.decodeStream(input, null, boundsOpts)
            }
            if (boundsOpts.outWidth <= 0 || boundsOpts.outHeight <= 0) return null

            var sampleSize = 1
            while (boundsOpts.outWidth / sampleSize > maxDimPx * 2 ||
                boundsOpts.outHeight / sampleSize > maxDimPx * 2
            ) {
                sampleSize *= 2
            }

            val orientation = try {
                context.contentResolver.openInputStream(sourceUri)?.use { input ->
                    ExifInterface(input).getAttributeInt(
                        ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_NORMAL
                    )
                } ?: ExifInterface.ORIENTATION_NORMAL
            } catch (_: Exception) {
                ExifInterface.ORIENTATION_NORMAL
            }

            val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            val decoded = context.contentResolver.openInputStream(sourceUri)?.use { input ->
                BitmapFactory.decodeStream(input, null, decodeOpts)
            } ?: return null

            val oriented = applyExifOrientation(decoded, orientation)
            if (oriented !== decoded) decoded.recycle()

            val scaled = scaleDownIfNeeded(oriented, maxDimPx)
            if (scaled !== oriented) oriented.recycle()

            val outFile = File(storageDir(context, folder), "${prefix}_${System.currentTimeMillis()}.png")
            FileOutputStream(outFile).use { fos ->
                scaled.compress(Bitmap.CompressFormat.PNG, 95, fos)
            }
            scaled.recycle()
            Uri.fromFile(outFile).toString()
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Decodes a CameraX captured JPEG with EXIF rotation correction, optional square crop,
     * and optional document scan contrast/grayscale enhancement.
     */
    fun decodeCapturedPreview(
        sourceFile: File,
        maxDimPx: Int = 1280,
        filterMode: ScanFilterMode = ScanFilterMode.ORIGINAL,
        squareCrop: Boolean = false
    ): Bitmap? {
        if (!sourceFile.exists()) return null
        return try {
            val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(sourceFile.absolutePath, boundsOpts)
            if (boundsOpts.outWidth <= 0 || boundsOpts.outHeight <= 0) return null

            var sampleSize = 1
            while (boundsOpts.outWidth / sampleSize > maxDimPx * 2 ||
                boundsOpts.outHeight / sampleSize > maxDimPx * 2
            ) {
                sampleSize *= 2
            }

            val exifOrientation = try {
                ExifInterface(sourceFile.absolutePath).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
            } catch (_: Exception) {
                ExifInterface.ORIENTATION_NORMAL
            }

            val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            val decoded = BitmapFactory.decodeFile(sourceFile.absolutePath, decodeOpts) ?: return null
            val oriented = applyExifOrientation(decoded, exifOrientation)
            if (oriented !== decoded) decoded.recycle()

            val cropped = if (squareCrop) {
                val side = minOf(oriented.width, oriented.height)
                val x = (oriented.width - side) / 2
                val y = (oriented.height - side) / 2
                val sq = Bitmap.createBitmap(oriented, x, y, side, side)
                if (sq !== oriented) oriented.recycle()
                sq
            } else {
                oriented
            }

            val scaled = scaleDownIfNeeded(cropped, maxDimPx)
            if (scaled !== cropped) cropped.recycle()

            if (filterMode == ScanFilterMode.ORIGINAL) {
                scaled
            } else {
                val enhanced = applyDocumentFilter(scaled, filterMode)
                if (enhanced !== scaled) scaled.recycle()
                enhanced
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Saves a CameraX captured photo or scanned document permanently to app-private storage.
     */
    fun saveCapturedFileToInternal(
        context: Context,
        sourceFile: File,
        folder: String,
        prefix: String,
        maxDimPx: Int = 1280,
        filterMode: ScanFilterMode = ScanFilterMode.ORIGINAL,
        squareCrop: Boolean = false
    ): String? {
        val processed = decodeCapturedPreview(sourceFile, maxDimPx, filterMode, squareCrop) ?: return null
        return try {
            val outFile = File(storageDir(context, folder), "${prefix}_${System.currentTimeMillis()}.png")
            FileOutputStream(outFile).use { fos ->
                processed.compress(Bitmap.CompressFormat.PNG, 95, fos)
            }
            processed.recycle()
            Uri.fromFile(outFile).toString()
        } catch (_: Exception) {
            processed.recycle()
            null
        }
    }

    /**
     * Exports a stored photo/document to a user-selected SAF destination URI using DocumentFile.
     */
    fun exportToSafDocument(
        context: Context,
        sourceUriOrPath: String,
        destUri: Uri
    ): Boolean {
        return try {
            val docFile = DocumentFile.fromSingleUri(context, destUri)
            if (docFile != null && !docFile.canWrite() && docFile.exists()) {
                return false
            }
            val bmp = loadBitmap(context, sourceUriOrPath) ?: return false
            context.contentResolver.openOutputStream(destUri)?.use { out ->
                bmp.compress(Bitmap.CompressFormat.PNG, 100, out)
            } ?: run {
                bmp.recycle()
                return false
            }
            bmp.recycle()
            true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Converts an internal file:// path or content:// URI into a FileProvider-backed content:// URI
     * suitable for external sharing intents.
     */
    fun getShareableUri(context: Context, uriOrPath: String): Uri? {
        return try {
            val parsed = Uri.parse(uriOrPath)
            if (parsed.scheme == "content") {
                parsed
            } else {
                val path = parsed.path ?: uriOrPath
                val file = File(path)
                if (!file.exists()) return null
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Renders drawn signature strokes into a crisp transparent-background PNG inside filesDir.
     */
    fun saveSignatureStrokesToInternal(
        context: Context,
        strokes: List<List<Pair<Float, Float>>>,
        canvasWidth: Float,
        canvasHeight: Float
    ): String? {
        if (strokes.isEmpty() || canvasWidth <= 0f || canvasHeight <= 0f) return null
        return try {
            val outW = 600
            val outH = 220
            val bmp = Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)
            canvas.drawColor(Color.TRANSPARENT)

            val scaleX = outW / canvasWidth
            val scaleY = outH / canvasHeight
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#0D4C5E")
                style = Paint.Style.STROKE
                strokeWidth = 6.5f
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
            }

            for (stroke in strokes) {
                if (stroke.isEmpty()) continue
                if (stroke.size == 1) {
                    val p = stroke.first()
                    canvas.drawPoint(p.first * scaleX, p.second * scaleY, paint)
                    continue
                }
                val path = Path()
                path.moveTo(stroke.first().first * scaleX, stroke.first().second * scaleY)
                for (i in 1 until stroke.size) {
                    val prev = stroke[i - 1]
                    val curr = stroke[i]
                    val midX = (prev.first + curr.first) / 2f * scaleX
                    val midY = (prev.second + curr.second) / 2f * scaleY
                    path.quadTo(prev.first * scaleX, prev.second * scaleY, midX, midY)
                }
                val last = stroke.last()
                path.lineTo(last.first * scaleX, last.second * scaleY)
                canvas.drawPath(path, paint)
            }

            val outFile = File(storageDir(context, "branding"), "sig_${System.currentTimeMillis()}.png")
            FileOutputStream(outFile).use { fos ->
                bmp.compress(Bitmap.CompressFormat.PNG, 100, fos)
            }
            bmp.recycle()
            Uri.fromFile(outFile).toString()
        } catch (_: Exception) {
            null
        }
    }

    fun loadBitmap(context: Context, uriOrPath: String?): Bitmap? {
        if (uriOrPath.isNullOrBlank()) return null
        return try {
            val parsed = Uri.parse(uriOrPath)
            when (parsed.scheme) {
                "file", null -> {
                    val path = parsed.path ?: uriOrPath
                    val file = File(path)
                    if (file.exists()) BitmapFactory.decodeFile(file.absolutePath) else null
                }
                else -> {
                    context.contentResolver.openInputStream(parsed)?.use { input ->
                        BitmapFactory.decodeStream(input)
                    }
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun applyDocumentFilter(src: Bitmap, mode: ScanFilterMode): Bitmap {
        val result = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val matrix = ColorMatrix()
        when (mode) {
            ScanFilterMode.ORIGINAL -> Unit
            ScanFilterMode.CRISP_DOCUMENT -> {
                val contrast = 1.45f
                val translate = (-0.5f * contrast + 0.5f) * 255f + 16f
                matrix.set(
                    floatArrayOf(
                        contrast, 0f, 0f, 0f, translate,
                        0f, contrast, 0f, 0f, translate,
                        0f, 0f, contrast, 0f, translate,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            }
            ScanFilterMode.BW_DOCUMENT -> {
                val gray = ColorMatrix().apply { setSaturation(0f) }
                val contrast = 1.85f
                val translate = (-0.5f * contrast + 0.5f) * 255f + 22f
                val contrastMatrix = ColorMatrix(
                    floatArrayOf(
                        contrast, 0f, 0f, 0f, translate,
                        0f, contrast, 0f, 0f, translate,
                        0f, 0f, contrast, 0f, translate,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                matrix.set(gray)
                matrix.postConcat(contrastMatrix)
            }
        }
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(matrix)
        }
        canvas.drawBitmap(src, 0f, 0f, paint)
        return result
    }

    private fun applyExifOrientation(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.preScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.postRotate(90f)
                matrix.preScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.postRotate(270f)
                matrix.preScale(-1f, 1f)
            }
            else -> return bitmap
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun scaleDownIfNeeded(src: Bitmap, maxDim: Int): Bitmap {
        val w = src.width
        val h = src.height
        if (w <= maxDim && h <= maxDim) return src
        val ratio = minOf(maxDim.toFloat() / w, maxDim.toFloat() / h)
        val newW = (w * ratio).toInt().coerceAtLeast(1)
        val newH = (h * ratio).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(src, newW, newH, true)
    }
}
