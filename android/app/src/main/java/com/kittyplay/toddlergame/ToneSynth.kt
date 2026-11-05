package com.kittyplay.toddlergame

import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

/** Builds every sound in code, so the game works with no audio files at all. */
object ToneSynth {
    const val SAMPLE_RATE = 22050

    // Simple effects

    private class Note(val startHz: Double, val endHz: Double, val seconds: Double)

    private fun tones(notes: List<Note>): ShortArray {
        val out = ArrayList<Double>()
        for (note in notes) {
            val count = (note.seconds * SAMPLE_RATE).toInt()
            var phase = 0.0
            for (i in 0 until count) {
                val t = i.toDouble() / count
                phase += 2 * PI * (note.startHz + (note.endHz - note.startHz) * t) / SAMPLE_RATE
                val envelope = min(1.0, t * 20) * (1 - t).pow(1.5)
                out.add((sin(phase) + 0.3 * sin(2 * phase)) / 1.3 * envelope * 0.8)
            }
        }
        return toShorts(out.toDoubleArray())
    }

    fun pop() = tones(listOf(Note(500.0, 950.0, 0.09)))
    fun boing() = tones(listOf(Note(260.0, 620.0, 0.2)))
    fun giggle() = tones(listOf(700.0, 820.0, 760.0, 900.0, 840.0, 1000.0).map { Note(it, it * 1.35, 0.09) })
    fun fanfare() = tones(listOf(523.0, 659.0, 784.0, 1047.0).map { Note(it, it, 0.17) })

    /** Two seconds of soft rumbling noise that loops cleanly. */
    fun purr(): ShortArray {
        val count = 2 * SAMPLE_RATE
        val out = DoubleArray(count)
        var filtered = 0.0
        for (i in 0 until count) {
            filtered += 0.18 * (Random.nextDouble(-1.0, 1.0) - filtered)
            val flutter = 0.5 + 0.5 * sin(2 * PI * 24 * i / SAMPLE_RATE)
            out[i] = filtered * flutter * flutter * 2.2
        }
        return toShorts(out)
    }

    // Cartoon animal voices

    /**
     * A small voice model: a buzzy tone, shaped by one resonance that works like the
     * shape of a mouth. Changing the pitch and mouth over time makes "me-ow" or "moo".
     */
    private fun voice(
        seconds: Double,
        pitch: (Double) -> Double,
        mouth: (Double) -> Double,
        breath: Double = 0.0,
        flutterHz: Double = 0.0,
        gain: Double = 0.9,
    ): DoubleArray {
        val count = (seconds * SAMPLE_RATE).toInt()
        val out = DoubleArray(count)
        var phase = 0.0
        var y1 = 0.0
        var y2 = 0.0
        val radius = 0.975
        for (i in 0 until count) {
            val t = i.toDouble() / count
            val hz = pitch(t)
            phase += 2 * PI * hz / SAMPLE_RATE

            var source = 0.0
            var harmonic = 1
            while (harmonic <= 14 && harmonic * hz < SAMPLE_RATE / 2.2) {
                source += sin(harmonic * phase) / harmonic
                harmonic += 1
            }
            source = source * (1 - breath) + Random.nextDouble(-1.0, 1.0) * breath

            val a1 = 2 * radius * cos(2 * PI * mouth(t) / SAMPLE_RATE)
            val a2 = -radius * radius
            val shaped = (1 - radius) * source * 6 + a1 * y1 + a2 * y2
            y2 = y1
            y1 = shaped

            val attack = min(1.0, t / 0.06)
            val release = min(1.0, (1 - t) / 0.25)
            val flutter = if (flutterHz > 0) 0.6 + 0.4 * sin(2 * PI * flutterHz * i / SAMPLE_RATE) else 1.0
            out[i] = (shaped + source * 0.2) * attack * release * flutter
        }

        val peak = out.maxOf { abs(it) }.coerceAtLeast(0.0001)
        return DoubleArray(count) { out[it] / peak * gain }
    }

    private fun silence(seconds: Double) = DoubleArray((seconds * SAMPLE_RATE).toInt())

    /** Moves smoothly through the given values as t goes from 0 to 1. */
    private fun glide(vararg values: Double): (Double) -> Double = { t ->
        val position = t.coerceIn(0.0, 1.0) * (values.size - 1)
        val index = position.toInt().coerceAtMost(values.size - 2)
        val fraction = position - index
        values[index] + (values[index + 1] - values[index]) * fraction
    }

    fun animalCall(animal: Animal): ShortArray {
        val sound: DoubleArray = when (animal) {
            Animal.CAT -> voice(0.6, glide(620.0, 880.0, 760.0, 520.0), glide(800.0, 2200.0, 1600.0, 800.0))
            Animal.DOG -> {
                val woof = voice(0.16, glide(340.0, 230.0), glide(900.0, 600.0), breath = 0.15)
                woof + silence(0.09) + woof
            }
            Animal.COW -> voice(1.0, glide(150.0, 185.0, 170.0, 130.0), glide(380.0, 620.0, 520.0, 340.0))
            Animal.DUCK -> {
                val quack = voice(0.15, glide(420.0, 330.0), glide(1500.0, 1200.0), flutterHz = 45.0)
                quack + silence(0.07) + quack
            }
            // Gentle and short on purpose, so the lion is funny rather than frightening.
            Animal.LION -> voice(0.75, glide(150.0, 210.0, 130.0), glide(650.0, 1000.0, 500.0), breath = 0.3, flutterHz = 28.0, gain = 0.7)
            Animal.SHEEP -> voice(0.7, { t -> 340.0 * (1 + 0.05 * sin(2 * PI * 5.5 * t)) }, glide(800.0, 1150.0, 1000.0))
            Animal.BUNNY -> {
                val nibble = voice(0.06, glide(1100.0, 1400.0), glide(2200.0, 2600.0))
                nibble + silence(0.05) + nibble + silence(0.05) + nibble
            }
            Animal.PIG -> {
                val oink = voice(0.17, glide(230.0, 300.0, 200.0), glide(800.0, 1100.0, 700.0), flutterHz = 38.0)
                oink + silence(0.08) + oink
            }
            Animal.FROG -> {
                val croak = voice(0.22, glide(240.0, 300.0), glide(600.0, 900.0), flutterHz = 32.0)
                croak + silence(0.06) + croak
            }
        }
        return toShorts(sound)
    }

    // File format

    private fun toShorts(values: DoubleArray) = ShortArray(values.size) {
        (values[it].coerceIn(-1.0, 1.0) * 32000).toInt().toShort()
    }

    fun wav(samples: ShortArray): ByteArray {
        val dataSize = samples.size * 2
        val buffer = ByteBuffer.allocate(44 + dataSize).order(ByteOrder.LITTLE_ENDIAN)
        buffer.put("RIFF".toByteArray())
        buffer.putInt(36 + dataSize)
        buffer.put("WAVE".toByteArray())
        buffer.put("fmt ".toByteArray())
        buffer.putInt(16)
        buffer.putShort(1)                   // uncompressed
        buffer.putShort(1)                   // mono
        buffer.putInt(SAMPLE_RATE)
        buffer.putInt(SAMPLE_RATE * 2)       // bytes per second
        buffer.putShort(2)                   // bytes per sample
        buffer.putShort(16)                  // bits per sample
        buffer.put("data".toByteArray())
        buffer.putInt(dataSize)
        samples.forEach { buffer.putShort(it) }
        return buffer.array()
    }
}
