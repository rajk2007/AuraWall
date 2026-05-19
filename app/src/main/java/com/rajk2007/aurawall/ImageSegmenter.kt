package com.rajk2007.aurawall

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.subject.SubjectSegmentation
import com.google.mlkit.vision.segmentation.subject.SubjectSegmenterOptions
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

object ImageSegmenter {

    suspend fun segment(context: Context, source: Bitmap): Pair<Bitmap, Bitmap> =
        suspendCoroutine { cont ->
            val options = SubjectSegmenterOptions.Builder()
                .enableForegroundConfidenceMask()
                .build()

            val segmenter = SubjectSegmentation.getClient(options)
            val inputImage = InputImage.fromBitmap(source, 0)

            segmenter.process(inputImage)
                .addOnSuccessListener { result ->
                    val mask = result.foregroundConfidenceMask!!
                    val width = source.width
                    val height = source.height

                    // Build foreground bitmap (subject with transparent bg)
                    val fgBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    // Build background bitmap (original, subject area darkened)
                    val bgBitmap = source.copy(Bitmap.Config.ARGB_8888, true)

                    val fgPixels = IntArray(width * height)
                    val bgPixels = IntArray(width * height)
                    source.getPixels(bgPixels, 0, width, 0, 0, width, height)

                    for (i in 0 until width * height) {
                        val confidence = mask.get() // float 0.0 - 1.0
                        val srcPixel = bgPixels[i]
                        if (confidence > 0.5f) {
                            // Foreground: keep pixel with full alpha
                            fgPixels[i] = srcPixel
                            // Background: replace subject with blurred/darkened pixel
                            bgPixels[i] = Color.argb(
                                255,
                                (Color.red(srcPixel) * 0.4f).toInt(),
                                (Color.green(srcPixel) * 0.4f).toInt(),
                                (Color.blue(srcPixel) * 0.4f).toInt()
                            )
                        } else {
                            // Background pixel: transparent on fg layer
                            fgPixels[i] = Color.TRANSPARENT
                        }
                    }
                    mask.rewind()

                    fgBitmap.setPixels(fgPixels, 0, width, 0, 0, width, height)
                    bgBitmap.setPixels(bgPixels, 0, width, 0, 0, width, height)

                    cont.resume(Pair(bgBitmap, fgBitmap))
                }
                .addOnFailureListener { e ->
                    cont.resumeWithException(e)
                }
        }

    // Fast blur simulation: scale down then scale up
    fun blurBitmap(src: Bitmap, factor: Int = 8): Bitmap {
        val small = Bitmap.createScaledBitmap(src, src.width / factor, src.height / factor, true)
        return Bitmap.createScaledBitmap(small, src.width, src.height, true)
    }
}
