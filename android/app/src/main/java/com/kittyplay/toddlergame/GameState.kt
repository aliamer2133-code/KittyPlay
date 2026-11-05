package com.kittyplay.toddlergame

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

enum class MiniGame(val parentLabel: String) {
    PET_CAT("Pet the Cat"),
    ANIMAL_SOUNDS("Animal Sounds"),
    FEED("Feed the Animals"),
    PEEKABOO("Peekaboo"),
}

enum class SparkleKind { STAR, HEART, GOLD_STAR }

class Sparkle(val id: Long, val x: Float, val y: Float, val kind: SparkleKind)

/** All of the app's shared state. Parent settings are saved between launches. */
class GameState(context: Context, val sound: SoundEngine) {
    private val prefs = context.getSharedPreferences("kittyplay", Context.MODE_PRIVATE)
    private val scope = MainScope()
    private var nextSparkleId = 0L

    // Saved settings

    private var _hasOnboarded by mutableStateOf(prefs.getBoolean("hasOnboarded", false))
    var hasOnboarded: Boolean
        get() = _hasOnboarded
        set(value) {
            _hasOnboarded = value
            prefs.edit().putBoolean("hasOnboarded", value).apply()
        }

    private var _pin by mutableStateOf(prefs.getString("pin", "") ?: "")
    var pin: String
        get() = _pin
        set(value) {
            _pin = value
            prefs.edit().putString("pin", value).apply()
        }

    private var _soundOn by mutableStateOf(prefs.getBoolean("soundOn", true))
    var soundOn: Boolean
        get() = _soundOn
        set(value) {
            _soundOn = value
            sound.enabled = value
            prefs.edit().putBoolean("soundOn", value).apply()
        }

    private var _enabledGames by mutableStateOf(
        prefs.getStringSet("enabledGames", null)?.toSet() ?: MiniGame.entries.map { it.name }.toSet()
    )
    val enabledGames: List<MiniGame>
        get() = MiniGame.entries.filter { it.name in _enabledGames }.ifEmpty { listOf(MiniGame.PET_CAT) }

    /** Play-time limit in minutes. 0 means no limit. */
    private var _limitMinutes by mutableIntStateOf(prefs.getInt("limitMinutes", 15))
    var limitMinutes: Int
        get() = _limitMinutes
        set(value) {
            _limitMinutes = value
            prefs.edit().putInt("limitMinutes", value).apply()
        }

    /** Saved so that closing the app doesn't skip the sleepy cat screen. */
    private var _isAllDone by mutableStateOf(prefs.getBoolean("isAllDone", false))
    var isAllDone: Boolean
        get() = _isAllDone
        set(value) {
            _isAllDone = value
            prefs.edit().putBoolean("isAllDone", value).apply()
        }

    // Live state

    var isParentOpen by mutableStateOf(false)
    var isAppActive by mutableStateOf(true)
    var pageIndex by mutableIntStateOf(0)
    var slideForward by mutableStateOf(true)
    var secondsPlayed by mutableIntStateOf(0)
    var isBouncing by mutableStateOf(false)
    var showSwipeHint by mutableStateOf(false)
    val sparkles = mutableStateListOf<Sparkle>()

    /**
     * Set by a game when it owns the current finger (stroking the cat, dragging
     * food), so that movement doesn't also count as a page swipe.
     */
    var touchClaimed = false

    private var secondsOnPage = 0

    init {
        sound.enabled = _soundOn
    }

    // Pages

    val currentGame: MiniGame
        get() {
            val games = enabledGames
            return games[pageIndex % games.size]
        }

    fun isGameEnabled(game: MiniGame) = game.name in _enabledGames

    fun setGameEnabled(game: MiniGame, enabled: Boolean) {
        val updated = if (enabled) _enabledGames + game.name else _enabledGames - game.name
        // At least one game always stays on, so she never reaches an empty screen.
        if (updated.isEmpty()) return
        _enabledGames = updated
        pageIndex = 0
        prefs.edit().putStringSet("enabledGames", updated).apply()
    }

    /** Pages wrap around, so she can always keep swiping. */
    fun changePage(forward: Boolean) {
        val count = enabledGames.size
        if (count <= 1) return
        sound.stopAll()
        slideForward = forward
        pageIndex = (pageIndex % count + (if (forward) 1 else -1) + count) % count
        secondsOnPage = 0
        showSwipeHint = false
    }

    // Happy reactions

    /** Runs for every touch anywhere on screen. */
    fun tapReaction(x: Float, y: Float) {
        addSparkle(x, y, if (Random.nextBoolean()) SparkleKind.STAR else SparkleKind.GOLD_STAR)
        sound.pop()
        isBouncing = true
        scope.launch {
            delay(120)
            isBouncing = false
        }
    }

    fun addSparkle(x: Float, y: Float, kind: SparkleKind) {
        if (sparkles.size >= 24) sparkles.removeAt(0)
        val sparkle = Sparkle(nextSparkleId++, x, y, kind)
        sparkles.add(sparkle)
        scope.launch {
            delay(800)
            sparkles.remove(sparkle)
        }
    }

    fun burst(x: Float, y: Float, kind: SparkleKind = SparkleKind.HEART, count: Int = 8, spread: Float = 240f) {
        repeat(count) {
            addSparkle(
                x + Random.nextFloat() * spread * 2 - spread,
                y + Random.nextFloat() * spread * 2 - spread,
                kind,
            )
        }
    }

    // Once-a-second clock

    fun tick() {
        if (!isAppActive || !hasOnboarded || isParentOpen || isAllDone) return

        // If she hasn't changed game for a while, gently show that she can swipe.
        secondsOnPage += 1
        if (secondsOnPage >= 25 && enabledGames.size > 1) showSwipeHint = true

        if (limitMinutes > 0) {
            secondsPlayed += 1
            if (secondsPlayed >= limitMinutes * 60) isAllDone = true
        }
    }

    fun playAgain() {
        secondsPlayed = 0
        secondsOnPage = 0
        isAllDone = false
    }
}
