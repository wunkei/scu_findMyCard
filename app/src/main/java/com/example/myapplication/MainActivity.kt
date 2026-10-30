package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.colorControls
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.Shadow
import com.kyant.shapes.Capsule
import com.example.myapplication.ui.theme.MyApplicationTheme

private val Ink = Color(0xFF20231F)
private val Muted = Color(0xFF898D84)
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { MyApplicationTheme { MainScreen() } }
    }
}

@Composable
private fun MainScreen() {
    var selected by remember { mutableStateOf("发现") }
    var publishType by rememberSaveable { mutableStateOf("") }
    var userPosts by remember { mutableStateOf(emptyList<LostFoundPost>()) }
    val pageStateHolder = rememberSaveableStateHolder()
    val backdrop = rememberLayerBackdrop()
    Box(Modifier.fillMaxSize()) {
        pageStateHolder.SaveableStateProvider(selected) {
            when (selected) {
                "发现" -> DiscoverScreen(
                    backdrop = backdrop,
                    userPosts = userPosts,
                    onStartPublish = { type ->
                        publishType = type
                        selected = "发布"
                    }
                )
                "发布" -> PublishScreen(
                    backdrop = backdrop,
                    selectedType = publishType,
                    onTypeSelected = { publishType = it },
                    onSubmit = { post ->
                        userPosts = listOf(post) + userPosts
                        publishType = ""
                        selected = "发现"
                    }
                )
                else -> ProfileScreen(backdrop = backdrop, userPosts = userPosts)
            }
        }
        FloatingGlassNav(
            backdrop,
            selected,
            onSelect = { selected = it },
            onCreate = { publishType = ""; selected = "发布" },
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 38.dp)
        )
    }
}

private fun capsuleWindowPath(
    size: androidx.compose.ui.geometry.Size,
    tabWidthPx: Float,
    insetPx: Float,
    visualIndex: Float,
    capsuleHeightPx: Float,
    capsuleScaleX: Float,
    capsuleScaleY: Float,
    outerScaleX: Float = 1f,
    outerScaleY: Float = 1f
): Path {
    val centerX = insetPx + visualIndex * tabWidthPx + tabWidthPx / 2f
    val centerY = size.height / 2f
    val halfWidth = tabWidthPx * capsuleScaleX / 2f
    val halfHeight = capsuleHeightPx * capsuleScaleY / 2f
    val outerCenterX = size.width / 2f
    val outerCenterY = size.height / 2f
    val left = outerCenterX + (centerX - halfWidth - outerCenterX) / outerScaleX
    val right = outerCenterX + (centerX + halfWidth - outerCenterX) / outerScaleX
    val top = outerCenterY + (centerY - halfHeight - outerCenterY) / outerScaleY
    val bottom = outerCenterY + (centerY + halfHeight - outerCenterY) / outerScaleY
    val baseRadius = minOf(tabWidthPx, capsuleHeightPx) / 2f
    val radiusX = baseRadius * capsuleScaleX / outerScaleX
    val radiusY = baseRadius * capsuleScaleY / outerScaleY
    return Path().apply {
        addRoundRect(
            androidx.compose.ui.geometry.RoundRect(
                androidx.compose.ui.geometry.Rect(left, top, right, bottom),
                androidx.compose.ui.geometry.CornerRadius(radiusX, radiusY)
            )
        )
    }
}

@Composable
private fun FloatingGlassNav(backdrop: com.kyant.backdrop.Backdrop, selected: String, onSelect: (String) -> Unit, onCreate: () -> Unit, modifier: Modifier = Modifier) {
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
        // A fresh touch interrupts any in-flight selection animation. The drag
        // animation then starts from indicatorPosition's current visible value.
        if (settlingDrag || draggingIndex != null) return@LaunchedEffect
        indicatorPosition.animateTo(selectedIndex.toFloat(), spring(dampingRatio = .55f, stiffness = 280f))
        indicatorPosition.snapTo(selectedIndex.toFloat())
    }
    val visualIndex = if (draggingIndex != null) dragPosition.value else indicatorPosition.value
    val density = LocalDensity.current
    val capsuleContentBackdrop = rememberLayerBackdrop()
    val capsuleSurfaceBackdrop = rememberLayerBackdrop()
    // The pill should refract one stationary copy of the nav content. Including
    // the enlarged nav copy here samples the same labels twice and creates text ghosts.
    val completeBackdrop = rememberCombinedBackdrop(backdrop, capsuleSurfaceBackdrop, capsuleContentBackdrop)
    BoxWithConstraints(
        modifier = modifier.width(240.dp).height(44.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        val tabWidth = (maxWidth - 10.dp) / 3
        val tabWidthPx = with(density) { tabWidth.toPx() }
        val insetPx = with(density) { 5.dp.toPx() }
        val navScaleX = 1f + pressProgress * .04f
        val navScaleY = 1f + pressProgress * .08f
        val capsulePhase = kotlin.math.abs(visualIndex - selectedIndex)
        val capsuleScaleX = 1f + capsulePhase * .04f + pressProgress * .250f
        val capsuleScaleY = 1f + capsulePhase * .03f + pressProgress * .450f
        val capsuleHeightPx = with(density) { 36.dp.toPx() }

        // Keep a stationary copy of the nav surface and labels under the glass pill.
        Box(
            Modifier.matchParentSize().drawWithCache {
                val window = capsuleWindowPath(
                    size, tabWidthPx, insetPx, visualIndex, capsuleHeightPx,
                    capsuleScaleX, capsuleScaleY
                )
                onDrawWithContent {
                    val layerContent = this
                    clipPath(window, ClipOp.Intersect) { layerContent.drawContent() }
                }
            }
        ) {
            Box(
                Modifier.matchParentSize().drawBackdrop(
                    backdrop = backdrop,
                    shape = { Capsule() },
                    effects = {
                        vibrancy()
                        blur(1.dp.toPx())
                        lens(refractionHeight = 3.dp.toPx(), refractionAmount = 6.dp.toPx())
                    },
                    highlight = { Highlight.Ambient.copy(alpha = .32f) },
                    shadow = { Shadow(radius = 10.dp, color = Color.Black.copy(alpha = .14f)) },
                    exportedBackdrop = capsuleSurfaceBackdrop,
                    onDrawSurface = { drawRect(Color.White.copy(alpha = .10f)) },
                    onDrawFront = {
                        drawRoundRect(
                            color = Color.White.copy(alpha = .28f),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2f),
                            style = Stroke(width = .8.dp.toPx())
                        )
                    }
                )
            )
            Row(
                Modifier.fillMaxSize().padding(horizontal = 5.dp).layerBackdrop(capsuleContentBackdrop),
                verticalAlignment = Alignment.CenterVertically
            ) {
                NavTabContent("⌕", "发现", Modifier.weight(1f), selected = true)
                NavTabContent("＋", "", Modifier.weight(1f), selected = true)
                NavTabContent("○", "我的", Modifier.weight(1f), selected = true)
            }
        }

        // The nav enlarges everywhere except the area currently covered by the pill.
        Box(
            Modifier.matchParentSize()
                .graphicsLayer {
                    scaleX = navScaleX
                    scaleY = navScaleY
                }
                .drawWithCache {
                    val window = capsuleWindowPath(
                        size, tabWidthPx, insetPx, visualIndex, capsuleHeightPx,
                        capsuleScaleX, capsuleScaleY, navScaleX, navScaleY
                    )
                    onDrawWithContent {
                        val layerContent = this
                        clipPath(window, ClipOp.Difference) { layerContent.drawContent() }
                    }
                }
        ) {
            Box(
                Modifier.matchParentSize().drawBackdrop(
                    backdrop = backdrop,
                    shape = { Capsule() },
                    effects = {
                        vibrancy()
                        blur(1.dp.toPx())
                        lens(refractionHeight = 3.dp.toPx(), refractionAmount = 6.dp.toPx())
                    },
                    highlight = { Highlight.Ambient.copy(alpha = .32f) },
                    shadow = { Shadow(radius = 10.dp, color = Color.Black.copy(alpha = .14f)) },
                    onDrawSurface = { drawRect(Color.White.copy(alpha = .10f)) },
                    onDrawFront = {
                        drawRoundRect(
                            color = Color.White.copy(alpha = .28f),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2f),
                            style = Stroke(width = .8.dp.toPx())
                        )
                    }
                )
            )
            Row(
                Modifier.fillMaxSize().padding(horizontal = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                NavTabContent("⌕", "发现", Modifier.weight(1f), selected = true)
                NavTabContent("＋", "", Modifier.weight(1f), selected = true)
                NavTabContent("○", "我的", Modifier.weight(1f), selected = true)
            }
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
                        if (pressProgress > 0.01f) {
                            val rHeight = 3.dp.toPx() * pressProgress
                            val rAmount = 14.dp.toPx() * pressProgress
                            lens(
                                refractionHeight = rHeight,
                                refractionAmount = rAmount,
                                chromaticAberration = false
                            )
                        }
                    },
                    highlight = { Highlight.Default.copy(alpha = .25f * (1f - pressProgress)) },
                    shadow = { Shadow(radius = 7.dp, color = Color.Black.copy(alpha = .06f + pressProgress * .10f)) },
                    layerBlock = {
                        scaleX = capsuleScaleX
                        scaleY = capsuleScaleY
                    },
                    onDrawSurface = { drawRect(Color.White.copy(alpha = .07f * (1f - pressProgress))) }
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
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2f),
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
            // This top row only receives gestures. The two clipped rendering
            // layers above already draw the labels; drawing them here as well
            // puts a sharp copy over the refracted copy while the pill is held.
            repeat(3) { Box(Modifier.weight(1f).fillMaxHeight()) }
        }
    }
}

@Composable
private fun NavTabContent(icon: String, label: String, modifier: Modifier = Modifier, selected: Boolean = true) {
    Box(
        modifier = modifier.fillMaxHeight(),
        contentAlignment = Alignment.Center
    ) {
        if (label.isEmpty()) {
            Canvas(Modifier.size(width = 30.dp, height = 22.dp)) {
                val center = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2f)
                drawRoundRect(
                    color = Color(0xFF318CE7),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2f)
                )
                val arm = 4.dp.toPx()
                val width = 2.dp.toPx()
                drawLine(Color.White, androidx.compose.ui.geometry.Offset(center.x - arm, center.y), androidx.compose.ui.geometry.Offset(center.x + arm, center.y), width, cap = StrokeCap.Round)
                drawLine(Color.White, androidx.compose.ui.geometry.Offset(center.x, center.y - arm), androidx.compose.ui.geometry.Offset(center.x, center.y + arm), width, cap = StrokeCap.Round)
            }
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = icon,
                    color = if (selected) Ink else Muted,
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
                    color = if (selected) Ink else Muted,
                    fontSize = 9.sp,
                    lineHeight = 10.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    style = TextStyle(
                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                    )
                )
            }
        }
    }
}

