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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
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
private fun MainScreen(viewModel: MainViewModel = viewModel()) {
    var selected by rememberSaveable { mutableStateOf("发现") }
    var isRegistering by rememberSaveable { mutableStateOf(false) }
    val userPosts by viewModel.userPosts.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val matchingPosts by viewModel.matchingPosts.collectAsStateWithLifecycle()
    val allUsers by viewModel.allUsers.collectAsStateWithLifecycle()
    val pageStateHolder = rememberSaveableStateHolder()
    val backdrop = rememberLayerBackdrop()
    Box(Modifier.fillMaxSize()) {
        if (isRegistering) {
            RegisterScreen(
                backdrop = backdrop,
                onBack = { isRegistering = false },
                onCheckAvailability = { studentId, idCard ->
                    viewModel.checkRegistrationAvailability(studentId, idCard)
                },
                onRegisterSuccess = { profile ->
                    viewModel.saveUser(profile)
                    isRegistering = false
                }
            )
        } else {
            pageStateHolder.SaveableStateProvider(selected) {
                when (selected) {
                    "发现" -> DiscoverScreen(
                        backdrop = backdrop,
                        userPosts = userPosts,
                        currentUser = currentUser,
                        onUpdatePostStatus = { id, status -> viewModel.updatePostStatus(id, status) },
                        onStartPublish = { selected = "发布" }
                    )
                    "发布" -> PublishScreen(
                        backdrop = backdrop,
                        currentUser = currentUser,
                        allUsers = allUsers,
                        onLogin = { studentId, password, callback -> viewModel.login(studentId, password, callback) },
                        onResetPassword = { studentId, eduroamPass, newPass, callback ->
                            viewModel.resetPasswordViaEduroam(studentId, eduroamPass, newPass, callback)
                        },
                        onNavigateToRegister = { isRegistering = true },
                        onSaveUser = { profile -> viewModel.saveUser(profile) },
                        onSubmit = { post ->
                            viewModel.publishPost(post)
                            selected = "发现"
                        }
                    )
                    else -> ProfileScreen(
                        backdrop = backdrop,
                        userPosts = userPosts,
                        currentUser = currentUser,
                        matchingPosts = matchingPosts,
                        allUsers = allUsers,
                        onLogin = { studentId, password, callback -> viewModel.login(studentId, password, callback) },
                        onResetPassword = { studentId, eduroamPass, newPass, callback ->
                            viewModel.resetPasswordViaEduroam(studentId, eduroamPass, newPass, callback)
                        },
                        onNavigateToRegister = { isRegistering = true },
                        onLogout = { viewModel.logout() },
                        onSaveUser = { profile ->
                            viewModel.updateUserContact(profile.studentId, profile.phone, profile.campus)
                        },
                        onUpdatePostStatus = { id, status -> viewModel.updatePostStatus(id, status) }
                    )
                }
            }
            FloatingGlassNav(
                backdrop,
                selected,
                onSelect = { selected = it },
                onCreate = { selected = "发布" },
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 38.dp)
            )
        }
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
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2f),
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
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2f),
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
            NavTabContent("⌕", "发现", Modifier.weight(1f), selected = true)
            NavTabContent("＋", "", Modifier.weight(1f), selected = true)
            NavTabContent("○", "我的", Modifier.weight(1f), selected = true)
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
                        // Continuous optical lens refraction so pill always bends the text underneath!
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
                    // When not pressed, capsule has gray tint; when pressed, gray disappears completely so it becomes 100% pure glass!
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
            // When idle (pressProgress = 0), this top layer renders the crisp text above the capsule!
            // When pressed (pressProgress -> 1), this top layer fades out completely (alpha -> 0),
            // so only the bottom layer's text is refracted by the pure glass capsule!
            val topTextAlpha = (1f - pressProgress * 1.5f).coerceIn(0f, 1f)
            Row(
                Modifier.fillMaxSize().graphicsLayer { alpha = topTextAlpha },
                verticalAlignment = Alignment.CenterVertically
            ) {
                NavTabContent("⌕", "发现", Modifier.weight(1f), selected = true)
                NavTabContent("＋", "", Modifier.weight(1f), selected = true)
                NavTabContent("○", "我的", Modifier.weight(1f), selected = true)
            }
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

