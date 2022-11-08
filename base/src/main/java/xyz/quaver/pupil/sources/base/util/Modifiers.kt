package xyz.quaver.pupil.sources.base.util

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.launch
import xyz.quaver.graphics.subsampledimage.SubSampledImageState

fun Modifier.doubleClickCycleZoom(
    state: SubSampledImageState,
    scale: Float = 2f,
    animationSpec: AnimationSpec<Rect> = spring(),
    onTap: ((Offset) -> Unit)? = null
) = composed {
    val initialImageRect by produceState<Rect?>(null, state.canvasSize, state.imageSize) {
        state.canvasSize?.let { canvasSize ->
            state.imageSize?.let { imageSize ->
                value = state.bound(state.scaleType(canvasSize, imageSize), canvasSize)
            } }
    }

    val coroutineScope = rememberCoroutineScope()

    pointerInput(Unit) {
        detectTapGestures(
            onTap = onTap,
            onDoubleTap = { centroid ->
                val imageRect = state.imageRect
                coroutineScope.launch {
                    if (imageRect == null || imageRect != initialImageRect)
                        state.resetImageRect(animationSpec)
                    else {
                        state.setImageRectWithBound(
                            Rect(
                                Offset(
                                    centroid.x - (centroid.x - imageRect.left) * scale,
                                    centroid.y - (centroid.y - imageRect.top) * scale
                                ),
                                imageRect.size * scale
                            ), animationSpec
                        )
                    }
                }
            }
        )
    }
}
