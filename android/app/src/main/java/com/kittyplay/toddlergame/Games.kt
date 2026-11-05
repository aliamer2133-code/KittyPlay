package com.kittyplay.toddlergame

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

// ---------- Pager and effects ----------

@Composable
fun GamePager(state: GameState) {
    val bounce by animateFloatAsState(
        targetValue = if (state.isBouncing) 1.015f else 1f,
        animationSpec = spring(dampingRatio = 0.4f, stiffness = 800f),
        label = "page bounce",
    )
    AnimatedContent(
        targetState = state.currentGame,
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                scaleX = bounce
                scaleY = bounce
            },
        transitionSpec = {
            val direction = if (state.slideForward) 1 else -1
            slideInHorizontally(tween(350)) { it * direction } togetherWith
                slideOutHorizontally(tween(350)) { -it * direction }
        },
        label = "game page",
    ) { game ->
        when (game) {
            MiniGame.PET_CAT -> PetCatGame(state)
            MiniGame.ANIMAL_SOUNDS -> AnimalSoundsGame(state)
            MiniGame.FEED -> FeedGame(state)
            MiniGame.PEEKABOO -> PeekabooGame(state)
        }
    }
}

@Composable
fun SparkleLayer(state: GameState) {
    Box(Modifier.fillMaxSize()) {
        for (sparkle in state.sparkles) {
            key(sparkle.id) { SparkleView(sparkle) }
        }
    }
}

@Composable
private fun SparkleView(sparkle: Sparkle) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, tween(700, easing = LinearEasing))
    }
    val half = with(LocalDensity.current) { 32.dp.toPx() }
    Canvas(
        Modifier
            .offset { IntOffset((sparkle.x - half).roundToInt(), (sparkle.y - half).roundToInt()) }
            .size(64.dp)
            .graphicsLayer {
                val t = progress.value
                val scale = 0.4f + 1.1f * t
                scaleX = scale
                scaleY = scale
                alpha = 1f - t
                translationY = -half * 1.5f * t
            }
    ) {
        val cx = size.width / 2
        val cy = size.height / 2
        val r = size.width * 0.3f
        when (sparkle.kind) {
            SparkleKind.STAR -> drawPath(starPath(cx, cy, r), Color(0xFFFF7AC6))
            SparkleKind.GOLD_STAR -> drawPath(starPath(cx, cy, r), Color(0xFFFFC61A))
            SparkleKind.HEART -> drawPath(heartPath(cx, cy, r), Color(0xFFFF4D79))
        }
    }
}

/** Two soft arrows that appear when she hasn't swiped for a while. They never block a touch. */
@Composable
fun SwipeHint() {
    val transition = rememberInfiniteTransition(label = "swipe hint")
    val nudge by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "nudge",
    )
    Canvas(Modifier.fillMaxSize()) {
        val u = size.width / 100f
        val y = size.height * 0.92f
        val color = Color.White.copy(alpha = 0.85f)
        val shift = nudge * 3 * u
        for (side in listOf(-1f, 1f)) {
            val tipX = size.width / 2 + side * (40 * u + shift)
            val backX = tipX - side * 5 * u
            drawLine(color, Offset(backX, y - 5 * u), Offset(tipX, y), 2.2f * u, StrokeCap.Round)
            drawLine(color, Offset(backX, y + 5 * u), Offset(tipX, y), 2.2f * u, StrokeCap.Round)
        }
    }
}

/**
 * Reacts the instant a finger lands, not when it lifts. Toddlers smear and
 * hold their fingers, and an ordinary tap would often be ignored.
 */
fun Modifier.onTouchDown(action: () -> Unit): Modifier = composed {
    val current by rememberUpdatedState(action)
    pointerInput(Unit) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false)
            current()
        }
    }
}

/** Draws an animal that hops and closes its eyes each time `trigger` changes. */
@Composable
private fun HoppingAnimal(animal: Animal, size: Dp, trigger: Int, modifier: Modifier = Modifier) {
    val hop = remember { Animatable(0f) }
    LaunchedEffect(trigger) {
        if (trigger > 0) {
            hop.snapTo(0f)
            hop.animateTo(1f, tween(450, easing = LinearEasing))
            hop.snapTo(0f)
        }
    }
    val t = hop.value
    AnimalArt(
        animal = animal,
        eyesClosed = t > 0f,
        modifier = modifier
            .size(size)
            .graphicsLayer {
                val arc = sin(PI * t).toFloat()
                val scale = 1f + 0.22f * arc
                scaleX = scale
                scaleY = scale
                translationY = -size.toPx() * 0.2f * arc
                rotationZ = 9f * sin(2 * PI * t).toFloat()
            },
    )
}

/** Bright sky and green grass, shared by the outdoor games. */
@Composable
private fun Meadow() {
    Canvas(Modifier.fillMaxSize()) {
        drawRect(Brush.verticalGradient(listOf(Color(0xFF73D1FF), Color(0xFFCCF2FF))))
        val w = size.width
        val h = size.height
        val hill = w * 1.8f
        drawOval(
            Color(0xFF80D959),
            Offset((w - hill) / 2, h * 0.47f),
            Size(hill, h * 0.95f),
        )
        val sun = Offset(w * 0.82f, h * 0.1f)
        drawCircle(Color(0x55FFE066), w * 0.14f, sun)
        drawCircle(Color(0xFFFFD93D), w * 0.09f, sun)
        val cloudY = h * 0.13f
        for ((x, r) in listOf(0.33f to 0.05f, 0.42f to 0.07f, 0.51f to 0.055f)) {
            drawCircle(Color.White, w * r, Offset(w * x, cloudY))
        }
    }
}

// ---------- Game 1: Pet the Cat ----------

@Composable
fun PetCatGame(state: GameState) {
    var isBlinking by remember { mutableStateOf(false) }
    var isStroking by remember { mutableStateOf(false) }
    var isBouncing by remember { mutableStateOf(false) }
    var tapCount by remember { mutableIntStateOf(0) }
    var windowOrigin by remember { mutableStateOf(Offset.Zero) }
    val roll = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    val bounce by animateFloatAsState(
        targetValue = if (isBouncing) 1.08f else 1f,
        animationSpec = spring(dampingRatio = 0.45f, stiffness = 700f),
        label = "cat bounce",
    )

    fun rollOver() {
        state.sound.giggle()
        scope.launch { roll.animateTo(roll.value + 360f, tween(1100, easing = FastOutSlowInEasing)) }
    }

    // She blinks on her own every few seconds.
    LaunchedEffect(Unit) {
        while (true) {
            delay(Random.nextLong(2500, 5000))
            isBlinking = true
            delay(160)
            isBlinking = false
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFFFFD9EB), Color(0xFFFFF2B3)))),
        contentAlignment = Alignment.Center,
    ) {
        BoxWithConstraints(Modifier.fillMaxWidth(0.88f)) {
            Box(
                Modifier
                    .size(maxWidth)
                    .onGloballyPositioned { windowOrigin = it.positionInWindow() }
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)

                            // A touch: meow, blink, bounce, and sometimes a roll.
                            tapCount += 1
                            isBouncing = true
                            isBlinking = true
                            state.sound.animalCall(Animal.CAT)
                            scope.launch {
                                delay(180)
                                isBouncing = false
                                isBlinking = false
                            }
                            if (tapCount % 6 == 0) rollOver()

                            var last = down.position
                            var strokeLength = 0f
                            var sinceHeart = 0f
                            var sinceRoll = 0f
                            val claimAfter = with(density) { 12.dp.toPx() }
                            val heartEvery = with(density) { 70.dp.toPx() }
                            val rollEvery = with(density) { 900.dp.toPx() }
                            try {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                    if (!change.pressed) break
                                    val distance = (change.position - last).getDistance()
                                    last = change.position
                                    strokeLength += distance
                                    if (strokeLength > claimAfter) {
                                        // This finger is stroking, so it isn't a swipe to the next game.
                                        state.touchClaimed = true
                                        isStroking = true
                                        state.sound.startPurr()
                                        sinceHeart += distance
                                        sinceRoll += distance
                                        if (sinceHeart > heartEvery) {
                                            sinceHeart = 0f
                                            val spot = windowOrigin + change.position
                                            state.addSparkle(spot.x, spot.y, SparkleKind.HEART)
                                        }
                                        if (sinceRoll > rollEvery) {
                                            sinceRoll = 0f
                                            rollOver()
                                        }
                                    }
                                }
                            } finally {
                                isStroking = false
                                state.sound.stopPurr()
                            }
                        }
                    },
            ) {
                BigCatArt(
                    eyesClosed = isBlinking || isStroking,
                    isHappy = isStroking || isBouncing,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = bounce
                            scaleY = bounce
                            rotationZ = roll.value
                        },
                )
            }
        }
    }
}

// ---------- Game 2: Animal Sounds ----------

@Composable
fun AnimalSoundsGame(state: GameState) {
    val rows = listOf(
        listOf(Animal.CAT, Animal.DOG),
        listOf(Animal.COW, Animal.DUCK),
        listOf(Animal.LION, Animal.SHEEP),
    )
    Box(Modifier.fillMaxSize()) {
        Meadow()
        BoxWithConstraints(Modifier.fillMaxSize().padding(top = 48.dp, bottom = 40.dp)) {
            val size = minOf(maxWidth / 2, maxHeight / 3) * 0.88f
            Column(Modifier.fillMaxSize()) {
                for (row in rows) {
                    Row(Modifier.fillMaxWidth().weight(1f)) {
                        for (animal in row) {
                            SingingAnimal(state, animal, size, Modifier.weight(1f).fillMaxHeight())
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SingingAnimal(state: GameState, animal: Animal, size: Dp, modifier: Modifier) {
    var hops by remember { mutableIntStateOf(0) }
    Box(modifier, contentAlignment = Alignment.Center) {
        HoppingAnimal(
            animal = animal,
            size = size,
            trigger = hops,
            modifier = Modifier.onTouchDown {
                hops += 1
                state.sound.animalCallThenName(animal)
            },
        )
    }
}

// ---------- Game 3: Feed the Animals ----------

@Composable
fun FeedGame(state: GameState) {
    val animals = listOf(Animal.CAT, Animal.DOG, Animal.BUNNY)
    // Not in the same order as the animals, so the match is more than a straight drag.
    val foods = listOf(Food.CARROT to Animal.BUNNY, Food.FISH to Animal.CAT, Food.BONE to Animal.DOG)
    val hops = remember { mutableStateMapOf<Animal, Int>() }
    val eaten = remember { mutableStateMapOf<Food, Boolean>() }
    val scope = rememberCoroutineScope()

    Box(Modifier.fillMaxSize()) {
        Meadow()
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val density = LocalDensity.current
            val cellDp = maxWidth / 3
            val itemDp = cellDp * 0.95f
            val widthPx = with(density) { maxWidth.toPx() }
            val heightPx = with(density) { maxHeight.toPx() }
            val itemPx = with(density) { itemDp.toPx() }
            val reach = with(density) { 130.dp.toPx() }

            fun animalCenter(index: Int) = Offset(widthPx / 3 * (index + 0.5f), heightPx * 0.33f)
            fun foodCenter(index: Int) = Offset(widthPx / 3 * (index + 0.5f), heightPx * 0.76f)

            fun dropped(food: Food, eater: Animal, point: Offset) {
                val nearest = animals.indices.minByOrNull { (animalCenter(it) - point).getDistance() } ?: return
                val center = animalCenter(nearest)
                if ((center - point).getDistance() > reach) {
                    // Dropped on the grass: the food just bounces home.
                    state.sound.boing()
                    return
                }
                val animal = animals[nearest]
                hops[animal] = (hops[animal] ?: 0) + 1
                if (animal == eater) {
                    state.sound.fanfare()
                    state.sound.say(Words.YUM)
                    state.burst(center.x, center.y)
                    eaten[food] = true
                    scope.launch {
                        delay(450)
                        hops[animal] = (hops[animal] ?: 0) + 1
                        delay(550)
                        eaten[food] = false
                    }
                } else {
                    state.sound.giggle()
                    state.burst(center.x, center.y, SparkleKind.GOLD_STAR, count = 3)
                }
            }

            animals.forEachIndexed { index, animal ->
                val center = animalCenter(index)
                HoppingAnimal(
                    animal = animal,
                    size = itemDp,
                    trigger = hops[animal] ?: 0,
                    modifier = Modifier
                        .offset { IntOffset((center.x - itemPx / 2).roundToInt(), (center.y - itemPx / 2).roundToInt()) }
                        .onTouchDown {
                            hops[animal] = (hops[animal] ?: 0) + 1
                            state.sound.animalCall(animal)
                        },
                )
            }

            foods.forEachIndexed { index, (food, eater) ->
                DraggableFood(
                    state = state,
                    food = food,
                    size = itemDp,
                    home = foodCenter(index),
                    isEaten = eaten[food] == true,
                    onDrop = { point -> dropped(food, eater, point) },
                )
            }
        }
    }
}

@Composable
private fun DraggableFood(
    state: GameState,
    food: Food,
    size: Dp,
    home: Offset,
    isEaten: Boolean,
    onDrop: (Offset) -> Unit,
) {
    val drag = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
    var isHeld by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val currentDrop by rememberUpdatedState(onDrop)
    val half = with(LocalDensity.current) { size.toPx() / 2 }
    val scale by animateFloatAsState(
        targetValue = if (isEaten) 0.01f else if (isHeld) 1.15f else 1f,
        animationSpec = spring(dampingRatio = 0.6f),
        label = "food scale",
    )

    FoodArt(
        food = food,
        modifier = Modifier
            .offset {
                IntOffset(
                    (home.x - half + drag.value.x).roundToInt(),
                    (home.y - half + drag.value.y).roundToInt(),
                )
            }
            .size(size)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    state.touchClaimed = true
                    isHeld = true
                    var total = Offset.Zero
                    var released = false
                    try {
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) {
                                released = true
                                break
                            }
                            // The position is relative to the food, which moves with the finger,
                            // so the change since the last event is the distance moved.
                            total += change.position - change.previousPosition
                            state.touchClaimed = true
                            val target = total
                            scope.launch { drag.snapTo(target) }
                        }
                    } finally {
                        isHeld = false
                        if (released) currentDrop(home + total)
                        // It always springs home, so food can never get stuck.
                        scope.launch { drag.animateTo(Offset.Zero, spring(dampingRatio = 0.6f)) }
                    }
                }
            },
    )
}

// ---------- Game 4: Peekaboo ----------

@Composable
fun PeekabooGame(state: GameState) {
    Box(Modifier.fillMaxSize()) {
        Meadow()
        BoxWithConstraints(Modifier.fillMaxSize().padding(top = 96.dp, bottom = 30.dp)) {
            val size = minOf(maxWidth / 2, maxHeight / 2) * 0.92f
            Column(Modifier.fillMaxSize()) {
                repeat(2) {
                    Row(Modifier.fillMaxWidth().weight(1f)) {
                        repeat(2) {
                            Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                                BushSpot(state, size)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BushSpot(state: GameState, size: Dp) {
    var animal by remember { mutableStateOf(Animal.entries.random()) }
    var isOut by remember { mutableStateOf(false) }
    var touches by remember { mutableIntStateOf(0) }
    var hops by remember { mutableIntStateOf(0) }
    val reveal by animateFloatAsState(
        targetValue = if (isOut) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 300f),
        label = "peekaboo",
    )

    // Hides again a little while after the last touch.
    LaunchedEffect(touches) {
        if (touches > 0) {
            delay(3000)
            isOut = false
        }
    }

    Box(
        Modifier
            .size(size)
            .onTouchDown {
                touches += 1
                if (isOut) {
                    hops += 1
                    state.sound.animalCall(animal)
                } else {
                    animal = Animal.entries.random()
                    isOut = true
                    state.sound.animalCall(animal)
                    state.sound.giggle(0.7f)
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        val sizePx = with(LocalDensity.current) { size.toPx() }
        Box(
            Modifier.graphicsLayer {
                alpha = reveal.coerceIn(0f, 1f)
                val scale = 0.5f + 0.5f * reveal
                scaleX = scale
                scaleY = scale
                translationY = sizePx * (0.1f - 0.42f * reveal)
            }
        ) {
            HoppingAnimal(animal = animal, size = size * 0.8f, trigger = hops)
        }
        BushArt(Modifier.fillMaxSize().graphicsLayer { translationY = sizePx * 0.12f })
    }
}

// ---------- The sleepy cat ----------

/**
 * The calm end of play time. No sparkles, no sounds on touch, nothing to re-excite.
 * It stays here until a grown-up opens the parent menu and taps Play Again.
 */
@Composable
fun SleepyCatScreen(state: GameState) {
    LaunchedEffect(Unit) {
        state.sound.stopAll()
        // The voice engine can take a moment to wake up after a fresh launch.
        delay(500)
        state.sound.sayAllDone()
    }
    val transition = rememberInfiniteTransition(label = "breathing")
    val breath by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2500, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "breath",
    )

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF1A2159), Color(0xFF4D4080)))),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            // Moon
            drawCircle(Color(0xFFFFF0B3), w * 0.11f, Offset(w * 0.78f, h * 0.13f))
            drawCircle(Color(0xFF1F2660), w * 0.095f, Offset(w * 0.73f, h * 0.12f))
            // Stars
            for ((x, y, r) in listOf(
                Triple(0.18f, 0.12f, 0.035f),
                Triple(0.42f, 0.2f, 0.022f),
                Triple(0.1f, 0.27f, 0.018f),
                Triple(0.58f, 0.08f, 0.018f),
                Triple(0.9f, 0.3f, 0.02f),
            )) {
                drawPath(starPath(w * x, h * y, w * r), Color(0xFFFFE680))
            }
            // A floating "z"
            val zSize = w * (0.05f + 0.01f * breath)
            val zx = w * 0.72f
            val zy = h * (0.34f - 0.015f * breath)
            val zColor = Color.White.copy(alpha = 0.35f + 0.6f * breath)
            val stroke = w * 0.012f
            drawLine(zColor, Offset(zx, zy), Offset(zx + zSize, zy), stroke, StrokeCap.Round)
            drawLine(zColor, Offset(zx + zSize, zy), Offset(zx, zy + zSize), stroke, StrokeCap.Round)
            drawLine(zColor, Offset(zx, zy + zSize), Offset(zx + zSize, zy + zSize), stroke, StrokeCap.Round)
        }
        BigCatArt(
            eyesClosed = true,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(top = 80.dp)
                .fillMaxWidth(0.82f)
                .aspectRatio(1f)
                .graphicsLayer {
                    val scale = 0.98f + 0.05f * breath
                    scaleX = scale
                    scaleY = scale
                },
        )
    }
}
