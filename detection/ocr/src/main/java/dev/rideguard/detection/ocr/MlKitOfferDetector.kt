package dev.rideguard.detection.ocr

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import dev.rideguard.core.model.OfferParser
import dev.rideguard.core.model.RawOffer
import dev.rideguard.core.model.RidePlatform
import java.io.Closeable

class MlKitOfferDetector(
    private val parsers: List<OfferParser>,
) : Closeable {
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    fun detect(
        bitmap: Bitmap,
        preferredPlatform: RidePlatform? = null,
        callback: (Result<RawOffer?>) -> Unit,
    ) {
        recognizer.process(InputImage.fromBitmap(bitmap, 0))
            .addOnSuccessListener { text ->
                val orderedParsers = parsers.sortedBy { parser ->
                    if (parser.platform == preferredPlatform) 0 else 1
                }
                callback(Result.success(orderedParsers.firstNotNullOfOrNull { it.parse(text.text) }))
            }
            .addOnFailureListener { error -> callback(Result.failure(error)) }
    }

    override fun close() {
        recognizer.close()
    }
}
