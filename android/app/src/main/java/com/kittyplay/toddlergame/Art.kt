package com.kittyplay.toddlergame

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Everything spoken aloud lives here, so changing language is a one-file job. */
object Words {
    const val VOICE_LANGUAGE = "en-US"
    const val ALL_DONE = "Sleepy cat. All done."
    const val YUM = "Yum yum!"
}

/** `id` also names optional custom files: sound_cat.mp3 in assets/media. */
enum class Animal(val id: String, val spokenName: String) {
    CAT("cat", "Cat"),
    DOG("dog", "Dog"),
    COW("cow", "Cow"),
    DUCK("duck", "Duck"),
    LION("lion", "Lion"),
    SHEEP("sheep", "Sheep"),
    BUNNY("bunny", "Bunny"),
    PIG("pig", "Pig"),
    FROG("frog", "Frog"),
}

enum class Food { FISH, BONE, CARROT }

private val Ink = Color(0xFF3F2619)
private val Blush = Color(0xFFFF8CA6)
private val Cream = Color(0xFFFFEBC7)

/** Draws on a 100 x 100 grid, whatever the real size is. */
private class Pen(val scope: DrawScope) {
    val u = scope.size.minDimension / 100f

    fun circle(color: Color, x: Float, y: Float, r: Float) {
        scope.drawCircle(color, r * u, Offset(x * u, y * u))
    }

    /** An oval centred on x, y. */
    fun oval(color: Color, x: Float, y: Float, w: Float, h: Float) {
        scope.drawOval(color, Offset((x - w / 2) * u, (y - h / 2) * u), Size(w * u, h * u))
    }

    fun tri(color: Color, x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float) {
        val path = Path().apply {
            moveTo(x1 * u, y1 * u)
            lineTo(x2 * u, y2 * u)
            lineTo(x3 * u, y3 * u)
            close()
        }
        scope.drawPath(path, color)
    }

    fun line(color: Color, x1: Float, y1: Float, x2: Float, y2: Float, width: Float) {
        scope.drawLine(color, Offset(x1 * u, y1 * u), Offset(x2 * u, y2 * u), width * u, StrokeCap.Round)
    }

    /** A curved line. A positive bend sags downward, like a smile. */
    fun curve(color: Color, x1: Float, x2: Float, y: Float, bend: Float, width: Float) {
        val path = Path().apply {
            moveTo(x1 * u, y * u)
            quadraticBezierTo((x1 + x2) / 2 * u, (y + bend * 2) * u, x2 * u, y * u)
        }
        scope.drawPath(path, color, style = Stroke(width * u, cap = StrokeCap.Round))
    }

    fun eye(x: Float, y: Float, closed: Boolean, size: Float = 5.5f) {
        if (closed) {
            curve(Ink, x - size, x + size, y, -size * 0.5f, 2.2f)
        } else {
            circle(Ink, x, y, size)
            circle(Color.White, x + size * 0.35f, y - size * 0.35f, size * 0.36f)
        }
    }

    fun eyes(y: Float, closed: Boolean, gap: Float = 14f, size: Float = 5.5f) {
        eye(50 - gap, y, closed, size)
        eye(50 + gap, y, closed, size)
    }

    fun cheeks(y: Float, gap: Float = 23f) {
        oval(Blush.copy(alpha = 0.55f), 50 - gap, y, 11f, 7f)
        oval(Blush.copy(alpha = 0.55f), 50 + gap, y, 11f, 7f)
    }

    /** The little "w" mouth used by cats and bunnies. */
    fun catMouth(y: Float) {
        curve(Ink, 42f, 50f, y, 1.6f, 1.8f)
        curve(Ink, 50f, 58f, y, 1.6f, 1.8f)
    }
}

/** A cartoon animal face. Drawn rather than an emoji, so it can blink on every phone. */
@Composable
fun AnimalArt(animal: Animal, modifier: Modifier = Modifier, eyesClosed: Boolean = false) {
    Canvas(modifier) {
        val pen = Pen(this)
        when (animal) {
            Animal.CAT -> pen.cat(eyesClosed)
            Animal.DOG -> pen.dog(eyesClosed)
            Animal.COW -> pen.cow(eyesClosed)
            Animal.DUCK -> pen.duck(eyesClosed)
            Animal.LION -> pen.lion(eyesClosed)
            Animal.SHEEP -> pen.sheep(eyesClosed)
            Animal.BUNNY -> pen.bunny(eyesClosed)
            Animal.PIG -> pen.pig(eyesClosed)
            Animal.FROG -> pen.frog(eyesClosed)
        }
    }
}

private fun Pen.cat(closed: Boolean) {
    val fur = Color(0xFFFFA63D)
    tri(fur, 18f, 46f, 22f, 10f, 46f, 30f)
    tri(fur, 82f, 46f, 78f, 10f, 54f, 30f)
    tri(Blush, 25f, 35f, 26f, 19f, 38f, 29f)
    tri(Blush, 75f, 35f, 74f, 19f, 62f, 29f)
    circle(fur, 50f, 56f, 36f)
    val stripe = Color(0xFFE5731A)
    line(stripe, 41f, 23f, 41f, 31f, 3.5f)
    line(stripe, 50f, 21f, 50f, 31f, 3.5f)
    line(stripe, 59f, 23f, 59f, 31f, 3.5f)
    eyes(50f, closed)
    cheeks(64f, 24f)
    tri(Blush, 45f, 60f, 55f, 60f, 50f, 66f)
    catMouth(68f)
    line(Ink, 8f, 58f, 25f, 61f, 1.2f)
    line(Ink, 8f, 69f, 25f, 67f, 1.2f)
    line(Ink, 92f, 58f, 75f, 61f, 1.2f)
    line(Ink, 92f, 69f, 75f, 67f, 1.2f)
}

private fun Pen.dog(closed: Boolean) {
    val brown = Color(0xFF9C6237)
    circle(Color(0xFFD9A066), 50f, 52f, 36f)
    oval(brown, 15f, 50f, 22f, 50f)
    oval(brown, 85f, 50f, 22f, 50f)
    circle(brown, 66f, 40f, 14f)
    oval(Cream, 50f, 69f, 40f, 30f)
    eyes(46f, closed)
    oval(Ink, 50f, 61f, 14f, 10f)
    line(Ink, 50f, 66f, 50f, 72f, 1.8f)
    oval(Blush, 50f, 80f, 12f, 12f)
    curve(Ink, 38f, 62f, 73f, 2.5f, 1.8f)
}

private fun Pen.cow(closed: Boolean) {
    tri(Cream, 24f, 26f, 18f, 5f, 36f, 18f)
    tri(Cream, 76f, 26f, 82f, 5f, 64f, 18f)
    oval(Color(0xFF55443C), 12f, 38f, 24f, 13f)
    oval(Color(0xFF55443C), 88f, 38f, 24f, 13f)
    oval(Color.White, 50f, 50f, 68f, 76f)
    circle(Color(0xFF55443C), 32f, 30f, 14f)
    circle(Color(0xFF55443C), 72f, 50f, 8f)
    eyes(44f, closed)
    oval(Color(0xFFFFB7C5), 50f, 72f, 54f, 34f)
    oval(Color(0xFFD9798E), 38f, 70f, 7f, 8f)
    oval(Color(0xFFD9798E), 62f, 70f, 7f, 8f)
    curve(Ink, 40f, 60f, 80f, 2.2f, 1.8f)
}

private fun Pen.duck(closed: Boolean) {
    val feather = Color(0xFFFFD93D)
    circle(feather, 44f, 16f, 7f)
    circle(feather, 53f, 13f, 8f)
    circle(feather, 62f, 18f, 6f)
    circle(feather, 50f, 54f, 38f)
    eyes(44f, closed, gap = 16f)
    cheeks(58f, 27f)
    val bill = Color(0xFFFF9326)
    oval(bill, 50f, 67f, 42f, 15f)
    oval(Color(0xFFE5731A), 50f, 73f, 32f, 9f)
    circle(bill, 50f, 66f, 3f)
}

private fun Pen.lion(closed: Boolean) {
    val mane = Color(0xFFD9721E)
    for (i in 0 until 12) {
        val angle = i * 2 * PI / 12
        circle(mane, 50f + 34f * cos(angle).toFloat(), 52f + 34f * sin(angle).toFloat(), 14f)
    }
    circle(mane, 50f, 52f, 36f)
    val face = Color(0xFFFFC94D)
    circle(face, 24f, 29f, 8f)
    circle(face, 76f, 29f, 8f)
    circle(Blush, 24f, 29f, 4f)
    circle(Blush, 76f, 29f, 4f)
    circle(face, 50f, 52f, 29f)
    eyes(46f, closed, gap = 12f, size = 5f)
    oval(Cream, 50f, 65f, 28f, 19f)
    tri(Ink, 44f, 57f, 56f, 57f, 50f, 63f)
    catMouth(68f)
}

private fun Pen.sheep(closed: Boolean) {
    val wool = Color.White
    for (i in 0 until 11) {
        val angle = i * 2 * PI / 11
        circle(wool, 50f + 30f * cos(angle).toFloat(), 48f + 30f * sin(angle).toFloat(), 16f)
    }
    circle(wool, 50f, 48f, 30f)
    val skin = Color(0xFFFFDDBD)
    oval(Color(0xFFE8BC96), 19f, 52f, 22f, 11f)
    oval(Color(0xFFE8BC96), 81f, 52f, 22f, 11f)
    oval(skin, 50f, 60f, 42f, 46f)
    circle(wool, 42f, 37f, 8f)
    circle(wool, 50f, 34f, 9f)
    circle(wool, 58f, 37f, 8f)
    eyes(55f, closed, gap = 11f, size = 5f)
    tri(Blush, 45f, 66f, 55f, 66f, 50f, 71f)
    curve(Ink, 42f, 58f, 74f, 2.2f, 1.8f)
}

private fun Pen.bunny(closed: Boolean) {
    val fur = Color(0xFFFFF8F0)
    oval(fur, 35f, 24f, 18f, 48f)
    oval(fur, 65f, 24f, 18f, 48f)
    oval(Blush, 35f, 25f, 8f, 34f)
    oval(Blush, 65f, 25f, 8f, 34f)
    circle(fur, 50f, 62f, 33f)
    eyes(56f, closed, gap = 13f)
    cheeks(68f, 22f)
    tri(Blush, 46f, 65f, 54f, 65f, 50f, 70f)
    catMouth(72f)
    oval(Color.White, 50f, 79f, 9f, 8f)
    line(Color(0xFFCCBBAA), 50f, 75f, 50f, 83f, 0.8f)
}

private fun Pen.pig(closed: Boolean) {
    val pink = Color(0xFFFFB0C4)
    val dark = Color(0xFFEF8AA5)
    tri(dark, 18f, 38f, 16f, 12f, 40f, 24f)
    tri(dark, 82f, 38f, 84f, 12f, 60f, 24f)
    circle(pink, 50f, 54f, 37f)
    eyes(44f, closed, gap = 16f)
    oval(dark, 50f, 62f, 30f, 21f)
    oval(Color(0xFFB85C75), 43f, 62f, 6f, 9f)
    oval(Color(0xFFB85C75), 57f, 62f, 6f, 9f)
    curve(Ink, 38f, 62f, 78f, 2.5f, 1.8f)
}

private fun Pen.frog(closed: Boolean) {
    val green = Color(0xFF6BCB3F)
    circle(green, 28f, 32f, 16f)
    circle(green, 72f, 32f, 16f)
    oval(green, 50f, 60f, 84f, 62f)
    if (closed) {
        eye(28f, 30f, true, 7f)
        eye(72f, 30f, true, 7f)
    } else {
        circle(Color.White, 28f, 30f, 11f)
        circle(Color.White, 72f, 30f, 11f)
        eye(28f, 30f, false, 6.5f)
        eye(72f, 30f, false, 6.5f)
    }
    cheeks(62f, 30f)
    curve(Ink, 26f, 74f, 66f, 6f, 2.4f)
}

/** The big cat, with its whole body, for Pet the Cat and the sleepy screen. */
@Composable
fun BigCatArt(modifier: Modifier = Modifier, eyesClosed: Boolean = false, isHappy: Boolean = false) {
    Canvas(modifier) {
        val fur = Color(0xFFFFA63D)
        val stripe = Color(0xFFE5731A)
        Pen(this).run {
            // Tail
            line(stripe, 72f, 86f, 90f, 54f, 12f)
            // Body and paws
            oval(fur, 50f, 73f, 58f, 54f)
            oval(Cream, 50f, 77f, 34f, 36f)
            oval(fur, 34f, 96f, 18f, 11f)
            oval(fur, 66f, 96f, 18f, 11f)
            // Ears
            tri(fur, 13f, 34f, 18f, 3f, 42f, 20f)
            tri(fur, 87f, 34f, 82f, 3f, 58f, 20f)
            tri(Blush, 21f, 25f, 22f, 11f, 33f, 19f)
            tri(Blush, 79f, 25f, 78f, 11f, 67f, 19f)
            // Head
            oval(fur, 50f, 34f, 74f, 60f)
            line(stripe, 41f, 9f, 41f, 17f, 3.5f)
            line(stripe, 50f, 7f, 50f, 17f, 3.5f)
            line(stripe, 59f, 9f, 59f, 17f, 3.5f)
            // Face
            eyes(29f, eyesClosed, gap = 16f, size = 7.5f)
            cheeks(41f, 25f)
            tri(Blush, 46f, 37f, 54f, 37f, 50f, 42f)
            if (isHappy) oval(Blush, 50f, 48f, 8f, 8f)
            catMouth(44f)
            line(Ink, 6f, 34f, 22f, 37f, 1.2f)
            line(Ink, 6f, 44f, 22f, 42f, 1.2f)
            line(Ink, 94f, 34f, 78f, 37f, 1.2f)
            line(Ink, 94f, 44f, 78f, 42f, 1.2f)
        }
    }
}

@Composable
fun FoodArt(food: Food, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        Pen(this).run {
            when (food) {
                Food.FISH -> {
                    val blue = Color(0xFF4DA6FF)
                    tri(Color(0xFF2E86DE), 70f, 50f, 96f, 26f, 96f, 74f)
                    oval(blue, 45f, 50f, 70f, 44f)
                    tri(Color(0xFF2E86DE), 40f, 32f, 58f, 16f, 60f, 36f)
                    circle(Color.White, 26f, 44f, 7f)
                    circle(Ink, 25f, 44f, 4f)
                    curve(Ink, 15f, 25f, 58f, 1.5f, 1.8f)
                }
                Food.BONE -> {
                    val bone = Color(0xFFFFF4DC)
                    val edge = Color(0xFFE0CDA8)
                    line(edge, 26f, 74f, 74f, 26f, 22f)
                    line(bone, 26f, 74f, 74f, 26f, 18f)
                    circle(bone, 15f, 72f, 12f)
                    circle(bone, 28f, 85f, 12f)
                    circle(bone, 72f, 15f, 12f)
                    circle(bone, 85f, 28f, 12f)
                }
                Food.CARROT -> {
                    val leaf = Color(0xFF4CAF50)
                    oval(leaf, 50f, 16f, 11f, 28f)
                    tri(leaf, 48f, 30f, 28f, 8f, 42f, 8f)
                    tri(leaf, 52f, 30f, 72f, 8f, 58f, 8f)
                    tri(Color(0xFFFF8A1F), 28f, 28f, 72f, 28f, 50f, 97f)
                    oval(Color(0xFFFF8A1F), 50f, 29f, 44f, 12f)
                    line(Color(0xFFE5731A), 38f, 42f, 50f, 44f, 1.8f)
                    line(Color(0xFFE5731A), 62f, 56f, 50f, 58f, 1.8f)
                }
            }
        }
    }
}

@Composable
fun BushArt(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val dark = Color(0xFF339E40)
        val light = Color(0xFF52BD4C)
        Pen(this).run {
            circle(dark, 26f, 66f, 28f)
            circle(dark, 74f, 66f, 28f)
            circle(light, 50f, 56f, 33f)
            circle(Color(0xFFFFD93D), 34f, 58f, 3f)
            circle(Color.White, 34f, 52f, 2.6f)
            circle(Color.White, 34f, 64f, 2.6f)
            circle(Color.White, 28f, 58f, 2.6f)
            circle(Color.White, 40f, 58f, 2.6f)
            circle(Color(0xFFFFD93D), 68f, 46f, 3f)
            circle(Blush, 68f, 40f, 2.6f)
            circle(Blush, 68f, 52f, 2.6f)
            circle(Blush, 62f, 46f, 2.6f)
            circle(Blush, 74f, 46f, 2.6f)
        }
    }
}

fun starPath(cx: Float, cy: Float, radius: Float): Path = Path().apply {
    for (i in 0 until 10) {
        val r = if (i % 2 == 0) radius else radius * 0.45f
        val angle = -PI / 2 + i * PI / 5
        val x = cx + r * cos(angle).toFloat()
        val y = cy + r * sin(angle).toFloat()
        if (i == 0) moveTo(x, y) else lineTo(x, y)
    }
    close()
}

fun heartPath(cx: Float, cy: Float, r: Float): Path = Path().apply {
    moveTo(cx, cy + r * 0.9f)
    cubicTo(cx - r * 1.5f, cy - r * 0.1f, cx - r * 0.7f, cy - r * 1.1f, cx, cy - r * 0.35f)
    cubicTo(cx + r * 0.7f, cy - r * 1.1f, cx + r * 1.5f, cy - r * 0.1f, cx, cy + r * 0.9f)
    close()
}
