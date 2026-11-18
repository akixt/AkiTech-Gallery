package com.akitech.gallery.ai

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.view.Surface
import com.akitech.gallery.domain.model.AiVideoAspectRatio
import com.akitech.gallery.domain.model.PhotoMotionPlan
import java.io.File
import kotlin.math.max

/**
 * Encodes a real hardware/AVC H.264 MP4 motion video from a source [Bitmap]
 * driven by a [PhotoMotionPlan] directed by Gemini via Firebase AI Logic.
 */
object PhotoMotionVideoEncoder {

    private const val VIDEO_MIME = MediaFormat.MIMETYPE_VIDEO_AVC
    private const val FPS = 24
    private const val BITRATE = 4_200_000
    private const val I_FRAME_INTERVAL = 1
    private const val TIMEOUT_USEC = 10_000L

    fun encode(
        bitmap: Bitmap,
        plan: PhotoMotionPlan,
        aspectRatio: AiVideoAspectRatio,
        durationSeconds: Int,
        outputFile: File,
        onProgress: (Int) -> Unit = {},
    ) {
        val width = if (aspectRatio == AiVideoAspectRatio.PORTRAIT_9_16) 720 else 1280
        val height = if (aspectRatio == AiVideoAspectRatio.PORTRAIT_9_16) 1280 else 720
        val clampedDuration = durationSeconds.coerceIn(3, 15)
        val totalFrames = clampedDuration * FPS

        if (outputFile.exists()) outputFile.delete()

        val format = MediaFormat.createVideoFormat(VIDEO_MIME, width, height).apply {
            setInteger(
                MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface,
            )
            setInteger(MediaFormat.KEY_BIT_RATE, BITRATE)
            setInteger(MediaFormat.KEY_FRAME_RATE, FPS)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, I_FRAME_INTERVAL)
        }

        var codec: MediaCodec? = null
        var inputSurface: Surface? = null
        var muxer: MediaMuxer? = null
        var muxerStarted = false
        var trackIndex = -1

        try {
            codec = MediaCodec.createEncoderByType(VIDEO_MIME)
            codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            inputSurface = codec.createInputSurface()
            codec.start()

            muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

            val bufferInfo = MediaCodec.BufferInfo()
            var outputFrameIndex = 0L
            val frameDurationUs = 1_000_000L / FPS

            fun drainEncoder(endOfStream: Boolean) {
                if (endOfStream) {
                    codec.signalEndOfInputStream()
                }
                while (true) {
                    val outputBufferId = codec.dequeueOutputBuffer(bufferInfo, TIMEOUT_USEC)
                    if (outputBufferId == MediaCodec.INFO_TRY_AGAIN_LATER) {
                        if (!endOfStream) break
                    } else if (outputBufferId == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        if (!muxerStarted) {
                            trackIndex = muxer.addTrack(codec.outputFormat)
                            muxer.start()
                            muxerStarted = true
                        }
                    } else if (outputBufferId >= 0) {
                        val encodedData = codec.getOutputBuffer(outputBufferId)
                        if (encodedData != null) {
                            if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
                                bufferInfo.size = 0
                            }
                            if (bufferInfo.size > 0 && muxerStarted) {
                                encodedData.position(bufferInfo.offset)
                                encodedData.limit(bufferInfo.offset + bufferInfo.size)
                                bufferInfo.presentationTimeUs = outputFrameIndex * frameDurationUs
                                muxer.writeSampleData(trackIndex, encodedData, bufferInfo)
                                outputFrameIndex++
                            }
                        }
                        codec.releaseOutputBuffer(outputBufferId, false)
                        if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                            break
                        }
                    }
                }
            }

            val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
                colorFilter = buildColorMoodFilter(plan.colorMood)
            }
            val vignettePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = RadialGradient(
                    width / 2f,
                    height / 2f,
                    max(width, height) * 0.72f,
                    intArrayOf(Color.TRANSPARENT, Color.argb(115, 0, 0, 0)),
                    floatArrayOf(0.55f, 1f),
                    Shader.TileMode.CLAMP,
                )
            }
            val fadePaint = Paint()
            val matrix = Matrix()

            val baseScale = max(
                width.toFloat() / bitmap.width.coerceAtLeast(1),
                height.toFloat() / bitmap.height.coerceAtLeast(1),
            )
            val startScale = plan.startScale.coerceIn(1.0f, 1.45f)
            val endScale = plan.endScale.coerceIn(1.0f, 1.55f)
            val startPanX = plan.panStartX.coerceIn(-0.4f, 0.4f)
            val endPanX = plan.panEndX.coerceIn(-0.4f, 0.4f)
            val startPanY = plan.panStartY.coerceIn(-0.4f, 0.4f)
            val endPanY = plan.panEndY.coerceIn(-0.4f, 0.4f)

            for (frame in 0 until totalFrames) {
                drainEncoder(endOfStream = false)

                val rawT = if (totalFrames > 1) frame.toFloat() / (totalFrames - 1) else 1f
                // Smooth cubic ease-in-out
                val t = if (rawT < 0.5f) {
                    4f * rawT * rawT * rawT
                } else {
                    1f - ((-2f * rawT + 2f).let { it * it * it } / 2f)
                }

                val currentZoom = startScale + (endScale - startScale) * t
                val currentPanX = startPanX + (endPanX - startPanX) * t
                val currentPanY = startPanY + (endPanY - startPanY) * t

                val scale = baseScale * currentZoom
                val scaledW = bitmap.width * scale
                val scaledH = bitmap.height * scale
                val slackX = (scaledW - width).coerceAtLeast(0f)
                val slackY = (scaledH - height).coerceAtLeast(0f)

                val drawX = (width - scaledW) / 2f + currentPanX * slackX
                val drawY = (height - scaledH) / 2f + currentPanY * slackY

                val canvas: Canvas = inputSurface.lockCanvas(null)
                try {
                    canvas.drawColor(Color.BLACK)
                    matrix.reset()
                    matrix.postScale(scale, scale)
                    matrix.postTranslate(drawX, drawY)
                    canvas.drawBitmap(bitmap, matrix, bitmapPaint)
                    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), vignettePaint)

                    // Smooth fade-in and fade-out
                    val fadeAlpha = when {
                        frame < FPS / 2 -> ((1f - frame.toFloat() / (FPS / 2f)) * 180f).toInt()
                        frame > totalFrames - FPS / 2 -> {
                            (((frame - (totalFrames - FPS / 2)).toFloat() / (FPS / 2f)) * 180f).toInt()
                        }
                        else -> 0
                    }.coerceIn(0, 220)
                    if (fadeAlpha > 0) {
                        fadePaint.color = Color.argb(fadeAlpha, 0, 0, 0)
                        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), fadePaint)
                    }
                } finally {
                    inputSurface.unlockCanvasAndPost(canvas)
                }

                if (frame % 12 == 0 || frame == totalFrames - 1) {
                    onProgress(((frame + 1) * 100 / totalFrames).coerceIn(1, 100))
                }
            }

            drainEncoder(endOfStream = true)
        } finally {
            runCatching { codec?.stop() }
            runCatching { codec?.release() }
            runCatching { inputSurface?.release() }
            if (muxerStarted) {
                runCatching { muxer?.stop() }
            }
            runCatching { muxer?.release() }
        }
    }

    private fun buildColorMoodFilter(colorMood: String): ColorMatrixColorFilter? {
        val cm = ColorMatrix()
        when (colorMood.lowercase()) {
            "noir_cinema" -> {
                cm.setSaturation(0f)
                val contrast = ColorMatrix(
                    floatArrayOf(
                        1.18f, 0f, 0f, 0f, -14f,
                        0f, 1.18f, 0f, 0f, -14f,
                        0f, 0f, 1.18f, 0f, -14f,
                        0f, 0f, 0f, 1f, 0f,
                    ),
                )
                cm.postConcat(contrast)
            }
            "warm_gold" -> {
                cm.setSaturation(1.12f)
                cm.postConcat(
                    ColorMatrix(
                        floatArrayOf(
                            1.08f, 0f, 0f, 0f, 6f,
                            0f, 1.02f, 0f, 0f, 2f,
                            0f, 0f, 0.90f, 0f, -4f,
                            0f, 0f, 0f, 1f, 0f,
                        ),
                    ),
                )
            }
            "cool_cyan" -> {
                cm.setSaturation(1.06f)
                cm.postConcat(
                    ColorMatrix(
                        floatArrayOf(
                            0.92f, 0f, 0f, 0f, -4f,
                            0f, 1.02f, 0f, 0f, 2f,
                            0f, 0f, 1.12f, 0f, 8f,
                            0f, 0f, 0f, 1f, 0f,
                        ),
                    ),
                )
            }
            else -> return null
        }
        return ColorMatrixColorFilter(cm)
    }
}
