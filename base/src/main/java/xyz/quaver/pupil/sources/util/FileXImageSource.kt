package xyz.quaver.pupil.sources.util

import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toAndroidRect
import xyz.quaver.graphics.subsampledimage.ImageSource
import xyz.quaver.graphics.subsampledimage.newBitmapRegionDecoder
import xyz.quaver.io.FileX
import xyz.quaver.io.util.inputStream

class FileXImageSource(val file: FileX): ImageSource {
    private val decoder by lazy {
        file.inputStream()!!.use {
            newBitmapRegionDecoder(it)
        }
    }

    override val imageSize by lazy { Size(decoder.width.toFloat(), decoder.height.toFloat()) }

    override fun decodeRegion(region: Rect, sampleSize: Int): ImageBitmap =
        decoder.decodeRegion(region.toAndroidRect(), BitmapFactory.Options().apply {
            inSampleSize = sampleSize
        }).asImageBitmap()
}

@Composable
fun rememberFileXImageSource(file: FileX) = remember {
    FileXImageSource(file)
}