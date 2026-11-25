package id.ac.rekaptf.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.annotation.StringRes
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import id.ac.rekaptf.BuildConfig
import id.ac.rekaptf.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.Locale

internal class EvidenceError(@StringRes val text: Int) : IOException()

internal class EvidenceReader(context: Context) {
    private val resolver = context.applicationContext.contentResolver

    suspend fun preview(uri: Uri): Bitmap = withContext(Dispatchers.IO) {
        decode(uri, maxSide = 1400, maxPixels = 1_500_000L)
    }

    suspend fun extract(uri: Uri, repository: RekapRepository): Income {
        if (!BuildConfig.REKAP_LOCAL_OCR) {
            val image = withContext(Dispatchers.IO) { originalImage(uri) }
            return repository.extractEvidence(image.bytes, image.mimeType)
        }
        val bitmap = withContext(Dispatchers.IO) { decode(uri, maxSide = 8192, maxPixels = 5_000_000L) }
        val text = recognize(bitmap)
        val relevant = withContext(Dispatchers.Default) { relevantEvidenceText(text) }
        return repository.extractEvidence(relevant)
    }

    private fun decode(uri: Uri, maxSide: Int, maxPixels: Long): Bitmap = try {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) throw EvidenceError(R.string.evidence_read_error)

        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight).toLong() > maxSide.toLong() * sample ||
            bounds.outWidth.toLong() * bounds.outHeight > maxPixels * sample * sample
        ) {
            sample *= 2
        }
        val options = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val decoded = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
            ?: throw EvidenceError(R.string.evidence_read_error)
        val matrix = orientation(uri)
        if (matrix.isIdentity) {
            decoded
        } else {
            try {
                Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true).also {
                    if (it !== decoded) decoded.recycle()
                }
            } catch (error: IllegalArgumentException) {
                decoded.recycle()
                throw EvidenceError(R.string.evidence_read_error)
            }
        }
    } catch (error: EvidenceError) {
        throw error
    } catch (_: IOException) {
        throw EvidenceError(R.string.evidence_read_error)
    } catch (_: SecurityException) {
        throw EvidenceError(R.string.evidence_read_error)
    }

    private fun orientation(uri: Uri): Matrix {
        val value = try {
            resolver.openInputStream(uri)?.use {
                ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            }
        } catch (_: IOException) {
            null
        } catch (_: SecurityException) {
            null
        }
        return Matrix().apply {
            when (value) {
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> setScale(-1f, 1f)
                ExifInterface.ORIENTATION_ROTATE_180 -> setRotate(180f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> setScale(1f, -1f)
                ExifInterface.ORIENTATION_TRANSPOSE -> {
                    setRotate(90f)
                    postScale(-1f, 1f)
                }
                ExifInterface.ORIENTATION_ROTATE_90 -> setRotate(90f)
                ExifInterface.ORIENTATION_TRANSVERSE -> {
                    setRotate(270f)
                    postScale(-1f, 1f)
                }
                ExifInterface.ORIENTATION_ROTATE_270 -> setRotate(270f)
            }
        }
    }

    private fun originalImage(uri: Uri): EvidenceUpload {
        val bytes = try {
            resolver.openInputStream(uri)?.use { it.readAtMost(4 * 1024 * 1024) }
                ?: throw EvidenceError(R.string.evidence_read_error)
        } catch (_: FileLimitExceeded) {
            throw EvidenceError(R.string.evidence_size_error)
        } catch (_: IOException) {
            throw EvidenceError(R.string.evidence_read_error)
        } catch (_: SecurityException) {
            throw EvidenceError(R.string.evidence_read_error)
        }
        val format = when {
            bytes.startsWith(JpegSignature) -> "image/jpeg"
            bytes.startsWith(PngSignature) -> "image/png"
            else -> throw EvidenceError(R.string.evidence_format_error)
        }
        return EvidenceUpload(bytes, format)
    }

    private fun ByteArray.startsWith(signature: ByteArray): Boolean =
        size >= signature.size && signature.indices.all { this[it] == signature[it] }

    private suspend fun recognize(image: Bitmap): String = suspendCancellableCoroutine { continuation ->
        if (!continuation.isActive) {
            image.recycle()
            return@suspendCancellableCoroutine
        }
        val recognizer = try {
            TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        } catch (_: Exception) {
            image.recycle()
            continuation.resumeWith(Result.failure(EvidenceError(R.string.evidence_ocr_error)))
            return@suspendCancellableCoroutine
        }
        try {
            recognizer.process(InputImage.fromBitmap(image, 0)).addOnCompleteListener { task ->
                recognizer.close()
                image.recycle()
                if (continuation.isActive) {
                    val result = if (task.isSuccessful) {
                        Result.success(task.result.text)
                    } else {
                        Result.failure(EvidenceError(R.string.evidence_ocr_error))
                    }
                    continuation.resumeWith(result)
                }
            }
        } catch (_: Exception) {
            recognizer.close()
            image.recycle()
            if (continuation.isActive) {
                continuation.resumeWith(Result.failure(EvidenceError(R.string.evidence_ocr_error)))
            }
        }
    }

    private companion object {
        val JpegSignature = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte())
        val PngSignature = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
    }
}

private val successPattern = Regex("""\b(?:transfer(?:\s+bi[- ]fast)?\s+(?:berhasil|sukses|successful)|successful\s+transfer)\b""")
private val failurePattern = Regex("""\b(?:(?:tidak|belum|not)\s+(?:berhasil|sukses|successful)|gagal|failed|unsuccessful|tertunda|pending)\b""")
private val amountPattern = Regex("""\b(?:idr|rp)\.?\s*\d[\d.,]{0,24}""", RegexOption.IGNORE_CASE)
private val datePattern = Regex("""\b(?:\d{4}[-/]\d{1,2}[-/]\d{1,2}|\d{1,2}[-/]\d{1,2}[-/]\d{2,4}|\d{1,2}\s+\p{L}{3,12}\s+\d{4})\b""")
private val amountLabel = Regex("""^(?:jumlah|nominal|total)(?: transfer)?\b\s*[:\-]?\s*(.*)$""", RegexOption.IGNORE_CASE)
private val payerLabel = Regex("""^(?:nama )?(?:pengirim|pembayar|dari|sumber dana)\b\s*[:\-]?\s*(.*)$""", RegexOption.IGNORE_CASE)
private val referenceLabel = Regex("""^(?:(?:no\.?|nomor)\s+)?(?:referensi|ref)\b\.?\s*[:\-]?\s*(.*)$""", RegexOption.IGNORE_CASE)
private val bareAmount = Regex("""\d[\d.,]{0,24}""")
private val payerName = Regex("""[\p{L}][\p{L}\p{M} .’'\-]{0,119}""")
private val reference = Regex("""[A-Za-z0-9][A-Za-z0-9\-/.]{2,79}""")

private fun relevantEvidenceText(text: String): String {
    if (text.isBlank()) throw EvidenceError(R.string.evidence_ocr_error)
    val lines = text.take(32_768).lineSequence().map(String::trim).filter(String::isNotBlank).toList()
    val joined = lines.joinToString(" ").lowercase(Locale.ROOT)
    if ("livin" !in joined || !successPattern.containsMatchIn(joined) || failurePattern.containsMatchIn(joined)) {
        throw EvidenceError(R.string.evidence_unsupported_error)
    }
    val fields = linkedSetOf<String>()
    for ((index, line) in lines.withIndex()) {
        val lower = line.lowercase(Locale.ROOT)
        if (listOf("biaya", "fee", "saldo").any { it in lower }) continue
        fun labeled(label: Regex, format: Regex): String? {
            val value = label.matchEntire(line)?.groupValues?.get(1)?.trim() ?: return null
            return value.ifBlank { lines.getOrNull(index + 1).orEmpty() }.takeIf(format::matches)
        }
        val amount = amountPattern.find(line)?.value ?: labeled(amountLabel, bareAmount)
        amount?.let { fields += "Nominal: $it" }
        datePattern.find(line)?.value?.let { fields += "Tanggal: $it" }
        labeled(payerLabel, payerName)?.takeUnless { name ->
            val key = name.lowercase(Locale.ROOT)
            key == "ke" || listOf("rekening", "bank", "mandiri", "livin", "tujuan", "penerima", "nomor",
                "referensi", "jumlah", "nominal", "tanggal", "transfer", "ke ", "email", "telepon").any(key::startsWith)
        }?.let { fields += "Pengirim: $it" }
        labeled(referenceLabel, reference)?.let { fields += "Referensi: $it" }
        if (fields.size >= 24) break
    }
    if (fields.isEmpty()) throw EvidenceError(R.string.evidence_ocr_error)
    return (listOf("Livin’ by Mandiri", "Transfer Berhasil") + fields.take(24)).joinToString("\n")
}
