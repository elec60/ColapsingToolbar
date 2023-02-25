package hashem.mousavi.colapsingtoolbar

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.zIndex
import kotlin.math.roundToInt

private val minHeaderHeight = 56.dp
private val maxHeaderHeight = 300.dp


@Composable
fun CollapsingToolbar(modifier: Modifier) {

    val state = rememberLazyListState()

    var progress by remember {
        mutableStateOf(0f)
    }

    val density = LocalDensity.current

    val diffPx = with(density) { (maxHeaderHeight - minHeaderHeight).roundToPx() }

    remember { derivedStateOf { state.layoutInfo } }.value.visibleItemsInfo.forEach {
        if (it.index == 0) {
            progress =
                (1 + it.offset / diffPx.toFloat()).coerceIn(0f, 1f)
        } else if (state.firstVisibleItemIndex > 0) {
            progress = 0f
        }
    }

    //offset = -diff -> progress = 0
    //offset = 0 -> progress = 1
    //progress - 1 = (1/diff)*(offset - 0)
    //progress = 1 + offset / diff

    LaunchedEffect(key1 = Unit) {
        state.scrollToItem(0, diffPx)
    }

    Box(
        modifier = modifier
    ) {
        LazyColumn(
            state = state
        ) {
            items(100) {
                Text(
                    modifier = Modifier
                        .padding(top = if (it == 0) maxHeaderHeight else 0.dp)
                        .padding(16.dp)
                        .fillMaxWidth()
                        .border(1.dp, Color.Blue)
                        .padding(16.dp),
                    text = "Item $it"
                )
            }
        }
        Header(progress = progress)
    }

}

@Composable
fun Header(progress: Float) {
    //p = 0 -> min
    //p = 1 -> max
    //h - max = (max - min)*(p - 1)
    //=> h = max + (max - min)*(p - 1)
    val density = LocalDensity.current
    val height = maxHeaderHeight + (maxHeaderHeight - minHeaderHeight) * (progress - 1)
    val minHeaderHeightPx = with(density) { minHeaderHeight.toPx() }

    var backButtonPosition by remember {
        mutableStateOf(Offset.Zero)
    }

    var backButtonSize by remember {
        mutableStateOf(IntSize.Zero)
    }

    var iconPosition by remember {
        mutableStateOf(Offset.Zero)
    }

    var imageSize by remember {
        mutableStateOf(IntSize.Zero)
    }

    val imageOffset = lerp(
        start = IntOffset(
            x = (backButtonPosition.x + backButtonSize.width).toInt(),
            y = ((minHeaderHeightPx - imageSize.height) / 2).toInt()
        ),
        stop = IntOffset(
            x = iconPosition.x.toInt(),
            y = (backButtonPosition.y + backButtonSize.height).toInt()
        ),
        fraction = if (progress <= 0.5f) 2 * progress else 1f
    )

    val scale = 1 + if (progress < 0.5f) progress else 0.5f

    val isScaleBelowThreshold = scale < 1.5f


    Box(modifier = Modifier
        .fillMaxWidth()
        .height(height)
        .background(Color.Yellow)
        .graphicsLayer {
            shadowElevation = 2f
        }
    ) {
        IconButton(
            modifier = Modifier
                .zIndex(10f)
                .onGloballyPositioned {
                    backButtonSize = it.size
                    backButtonPosition = it.positionInRoot()
                }
                .height(minHeaderHeight)
                .align(Alignment.TopStart),
            onClick = {}
        ) {
            Icon(
                modifier = Modifier.onGloballyPositioned {
                    iconPosition = it.positionInRoot()
                },
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "",
            )
        }

        val animate = animateFloatAsState(targetValue = if (isScaleBelowThreshold) 0f else 1f)

        Image(
            modifier = Modifier
                .onGloballyPositioned {
                    imageSize = it.size
                }
                .offset { if (isScaleBelowThreshold) imageOffset else imageOffset * (1 - animate.value) }
                .graphicsLayer(
                    scaleX = if (isScaleBelowThreshold) scale else 1f,
                    scaleY = if (isScaleBelowThreshold) scale else 1f,
                    transformOrigin = TransformOrigin(
                        pivotFractionX = 0f,
                        pivotFractionY = 0.5f
                    )
                )
                .conditionalFillMaxSize(!isScaleBelowThreshold, fraction = animate.value)
                .background(
                    Color.Red,
                    shape = if (isScaleBelowThreshold) CircleShape else RoundedCornerShape(150.dp * (1 - animate.value))
                ),
            imageVector = Icons.Default.Person,
            contentDescription = ""
        )

    }

}

fun Modifier.conditionalFillMaxSize(fill: Boolean, fraction: Float) =
    run { if (fill) fillMaxSize(fraction) else if (fraction > 0.1f) fillMaxSize(fraction).aspectRatio(1f) else this  }