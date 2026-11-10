package com.example.myapplication.ui.navigation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorControls
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.Shadow
import com.kyant.shapes.Capsule
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val NavInk = Color(0xFF20231F)

@Composable
fun FloatingGlassNav(
    backdrop: com.kyant.backdrop.Backdrop,
    selected: String,
    onSelect: (String) -> Unit,
    onCreate: () -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedIndex = when (selected) { "发现" -> 0; "发布" -> 1; else -> 2 }
    var draggingIndex by remember { mutableStateOf<Float?>(null) }
    var settlingDrag by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val pressProgress by animateFloatAsState(
        targetValue = if (draggingIndex != null) 1f else 0f,
        animationSpec = spring(dampingRatio = .42f, stiffness = 300f),
        label = "glassPress"
    )
    val indicatorPosition = remember { Animatable(selectedIndex.toFloat()) }
    val dragPosition = remember { Animatable(selectedIndex.toFloat()) }
    var downAnimationJob by remember { mutableStateOf<Job?>(null) }
    var settleAnimationJob by remember { mutableStateOf<Job?>(null) }
    var gestureGeneration by remember { mutableIntStateOf(0) }
    LaunchedEffect(selectedIndex, settlingDrag, draggingIndex != null) {
        if (settlingDrag || draggingIndex != null) return@LaunchedEffect
        indicatorPosition.animateTo(selectedIndex.toFloat(), spring(dampingRatio = .55f, stiffness = 280f))
        indicatorPosition.snapTo(selectedIndex.toFloat())
    }
    val visualIndex = if (draggingIndex != null) dragPosition.value else indicatorPosition.value
    val density = LocalDensity.current
    val capsuleContentBackdrop = rememberLayerBackdrop()
    val capsuleSurfaceBackdrop = rememberLayerBackdrop()
    val completeBackdrop = rememberCombinedBackdrop(backdrop, capsuleSurfaceBackdrop, capsuleContentBackdrop)
    BoxWithConstraints(
        modifier = modifier.width(240.dp).height(44.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        val tabWidth = (maxWidth - 10.dp) / 3
        val tabWidthPx = with(density) { tabWidth.toPx() }
        val insetPx = with(density) { 5.dp.toPx() }
        val capsulePhase = kotlin.math.abs(visualIndex - selectedIndex)
        val capsuleScaleX = 1f + capsulePhase * .04f + pressProgress * .250f
        val capsuleScaleY = 1f + capsulePhase * .03f + pressProgress * .450f

        // 1. Bottom: nav gray body
        Box(
            Modifier.matchParentSize().drawBackdrop(
                backdrop = backdrop,
                shape = { Capsule() },
                effects = {
                    vibrancy()
                    lens(
                        refractionHeight = 6.6.dp.toPx(),
                        refractionAmount = 24.dp.toPx(),
                        chromaticAberration = false
                    )
                },
                highlight = { Highlight.Ambient.copy(alpha = .20f) },
                shadow = { Shadow(radius = 10.dp, color = Color.Black.copy(alpha = .08f)) },
                exportedBackdrop = capsuleSurfaceBackdrop,
                onDrawSurface = { drawRect(Color(0xFFE5E9EC).copy(alpha = .75f)) },
                onDrawFront = {
                    drawRoundRect(
                        color = Color.Black.copy(alpha = .9f),
                        cornerRadius = CornerRadius(size.height / 2f),
                        style = Stroke(width = .10.dp.toPx())
                    )
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            listOf(
                                Color.White.copy(alpha = .80f),
                                Color.White.copy(alpha = .18f),
                                Color.White.copy(alpha = .62f)
                            )
                        ),
                        cornerRadius = CornerRadius(size.height / 2f),
                        style = Stroke(width = .8.dp.toPx())
                    )
                }
            )
        )

        // 2. Middle: the base crisp labels (feeds into capsuleContentBackdrop for the pill to refract!)
        Row(
            Modifier.fillMaxSize().padding(horizontal = 5.dp).layerBackdrop(capsuleContentBackdrop),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavTabContent("⌕", "发现", Modifier.weight(1f))
            NavTabContent("＋", "", Modifier.weight(1f))
            NavTabContent("○", "我的", Modifier.weight(1f))
        }
        Box(
            Modifier
                .zIndex(1f)
                .padding(start = 5.dp)
                .width(tabWidth)
                .height(36.dp)
                .graphicsLayer {
                    translationX = with(density) { visualIndex * tabWidth.toPx() }
                }
                .drawBackdrop(
                    backdrop = completeBackdrop,
                    shape = { Capsule() },
                    effects = {
                        val idleEffect = 1f - pressProgress
                        if (idleEffect > 0.01f) {
                            colorControls(saturation = 1f + .5f * idleEffect)
                            blur(.35f.dp.toPx() * idleEffect)
                        }
                        val rHeight = 3.dp.toPx() * (0.6f + 0.4f * pressProgress)
                        val rAmount = 14.dp.toPx() * (0.6f + 0.4f * pressProgress)
                        lens(
                            refractionHeight = rHeight,
                            refractionAmount = rAmount,
                            chromaticAberration = false
                        )
                    },
                    highlight = { Highlight.Default.copy(alpha = .25f * (1f - pressProgress)) },
                    shadow = { Shadow(radius = 7.dp, color = Color.Black.copy(alpha = .06f + pressProgress * .10f)) },
                    layerBlock = {
                        scaleX = capsuleScaleX
                        scaleY = capsuleScaleY
                    },
                    onDrawSurface = { drawRect(Color(0xFFD4DCE2).copy(alpha = .85f * (1f - pressProgress))) }
                )
                .drawWithCache {
                    val rimIntensity = .32f + pressProgress * .68f
                    val rim = Brush.verticalGradient(
                        listOf(
                            Color.White.copy(.8f * rimIntensity),
                            Color.White.copy(.18f * rimIntensity),
                            Color.White.copy(.62f * rimIntensity)
                        )
                    )
                    onDrawWithContent {
                        drawContent()
                        drawRoundRect(
                            brush = rim,
                            cornerRadius = CornerRadius(size.height / 2f),
                            style = Stroke(width = 1.dp.toPx())
                        )
                    }
                }
        )
        Row(
            Modifier.fillMaxSize()
                .zIndex(2f)
                .pointerInput(maxWidth) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        var fingerIndex = ((down.position.x - insetPx - tabWidthPx / 2f) / tabWidthPx)
                            .coerceIn(0f, 2f)
                        gestureGeneration += 1
                        val thisGesture = gestureGeneration
                        settleAnimationJob?.cancel()
                        settleAnimationJob = null
                        downAnimationJob?.cancel()
                        downAnimationJob = null
                        settlingDrag = false
                        draggingIndex = fingerIndex
                        var pointerMoved = false
                        var releasedNormally = false
                        val downTarget = fingerIndex
                        downAnimationJob = scope.launch(start = CoroutineStart.UNDISPATCHED) {
                            indicatorPosition.stop()
                            dragPosition.snapTo(indicatorPosition.value)
                            if (gestureGeneration == thisGesture && !pointerMoved) {
                                dragPosition.animateTo(
                                    downTarget,
                                    spring(dampingRatio = .95f, stiffness = 150f)
                                )
                            }
                        }
                        try {
                            var pointerDown = true
                            while (pointerDown) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                val nextFingerIndex = ((change.position.x - insetPx - tabWidthPx / 2f) / tabWidthPx)
                                    .coerceIn(0f, 2f)
                                fingerIndex = nextFingerIndex
                                draggingIndex = fingerIndex
                                if (change.position.x != change.previousPosition.x) {
                                    pointerMoved = true
                                    downAnimationJob?.cancel()
                                    downAnimationJob = scope.launch(start = CoroutineStart.UNDISPATCHED) {
                                        dragPosition.snapTo(nextFingerIndex)
                                    }
                                }
                                if (change.position != change.previousPosition) {
                                    change.consume()
                                }
                                pointerDown = change.pressed
                                releasedNormally = !pointerDown
                            }

                            if (!releasedNormally) return@awaitEachGesture
                            val index = fingerIndex.roundToInt().coerceIn(0, 2)
                            val releasePosition = if (pointerMoved) fingerIndex else dragPosition.value
                            downAnimationJob?.cancel()
                            downAnimationJob = null
                            settlingDrag = true
                            settleAnimationJob = scope.launch {
                                try {
                                    indicatorPosition.snapTo(releasePosition)
                                    draggingIndex = null
                                    when (index) {
                                        0 -> onSelect("发现")
                                        1 -> onCreate()
                                        else -> onSelect("我的")
                                    }
                                    indicatorPosition.animateTo(index.toFloat(), spring(dampingRatio = .55f, stiffness = 280f))
                                    indicatorPosition.snapTo(index.toFloat())
                                    dragPosition.snapTo(index.toFloat())
                                } finally {
                                    if (gestureGeneration == thisGesture) {
                                        draggingIndex = null
                                        settlingDrag = false
                                        settleAnimationJob = null
                                    }
                                }
                            }
                        } finally {
                            if (!releasedNormally && gestureGeneration == thisGesture) {
                                downAnimationJob?.cancel()
                                downAnimationJob = null
                                settleAnimationJob?.cancel()
                                settleAnimationJob = scope.launch(start = CoroutineStart.UNDISPATCHED) {
                                    try {
                                        indicatorPosition.snapTo(
                                            if (pointerMoved) fingerIndex else dragPosition.value
                                        )
                                        if (gestureGeneration == thisGesture) {
                                            draggingIndex = null
                                            settlingDrag = false
                                        }
                                    } finally {
                                        if (gestureGeneration == thisGesture) {
                                            draggingIndex = null
                                            settlingDrag = false
                                            settleAnimationJob = null
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                .padding(horizontal = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val topTextAlpha = (1f - pressProgress * 1.5f).coerceIn(0f, 1f)
            Row(
                Modifier.fillMaxSize().graphicsLayer { alpha = topTextAlpha },
                verticalAlignment = Alignment.CenterVertically
            ) {
                NavTabContent("⌕", "发现", Modifier.weight(1f))
                NavTabContent("＋", "", Modifier.weight(1f))
                NavTabContent("○", "我的", Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun NavTabContent(icon: String, label: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxHeight(),
        contentAlignment = Alignment.Center
    ) {
        if (label.isEmpty()) {
            Canvas(Modifier.size(width = 30.dp, height = 22.dp)) {
                val center = Offset(size.width / 2f, size.height / 2f)
                drawRoundRect(
                    color = Color(0xFF318CE7),
                    cornerRadius = CornerRadius(size.height / 2f)
                )
                val arm = 4.dp.toPx()
                val width = 2.dp.toPx()
                drawLine(Color.White, Offset(center.x - arm, center.y), Offset(center.x + arm, center.y), width, cap = StrokeCap.Round)
                drawLine(Color.White, Offset(center.x, center.y - arm), Offset(center.x, center.y + arm), width, cap = StrokeCap.Round)
            }
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = icon,
                    color = NavInk,
                    fontSize = 17.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Normal,
                    style = TextStyle(
                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                    )
                )
                Spacer(Modifier.height(1.dp))
                Text(
                    text = label,
                    color = NavInk,
                    fontSize = 9.sp,
                    lineHeight = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    style = TextStyle(
                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                    )
                )
            }
        }
    }
}
