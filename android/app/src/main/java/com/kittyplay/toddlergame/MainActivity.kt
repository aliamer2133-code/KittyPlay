package com.kittyplay.toddlergame

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.LaunchedEffect
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import kotlin.math.abs
import kotlin.math.hypot

class MainActivity : ComponentActivity() {
    private lateinit var sound: SoundEngine
    private lateinit var state: GameState
    private lateinit var router: TouchRouter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes = window.attributes.apply {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
        hideSystemBars()

        sound = SoundEngine(applicationContext)
        state = GameState(applicationContext, sound)
        router = TouchRouter(state, resources.displayMetrics.density)

        // The back button does nothing, so she can't back out of the game by accident.
        onBackPressedDispatcher.addCallback(this) { }

        setContent {
            MaterialTheme(colorScheme = lightColorScheme()) {
                // Stay awake during play, but let the phone sleep on the sleepy cat screen.
                LaunchedEffect(state.isAllDone) {
                    if (state.isAllDone) {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    } else {
                        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    }
                }
                AppRoot(
                    state = state,
                    pinScreen = { startLockTask() },
                    unpinScreen = { stopLockTask() },
                    isScreenPinned = { isPinned() },
                )
            }
        }
    }

    private fun isPinned(): Boolean {
        val manager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        return manager.lockTaskModeState != ActivityManager.LOCK_TASK_MODE_NONE
    }

    private fun hideSystemBars() {
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemBars()
    }

    override fun onStart() {
        super.onStart()
        state.isAppActive = true
    }

    override fun onStop() {
        super.onStop()
        state.isAppActive = false
        // A fresh play session starts the next time the app is opened.
        state.secondsPlayed = 0
        sound.stopAll()
    }

    override fun onDestroy() {
        super.onDestroy()
        sound.release()
    }

    // Every touch passes through here first, whatever is under the finger.
    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        router.handle(event)
        return super.dispatchTouchEvent(event)
    }
}

/**
 * Handles three things for the whole screen:
 *   1. a happy reaction on every touch
 *   2. left/right swipes between mini-games
 *   3. the secret 3-second hold in the top-left corner
 */
class TouchRouter(private val state: GameState, private val density: Float) {
    private data class Start(val x: Float, val y: Float)

    private val starts = HashMap<Int, Start>()
    private val handler = Handler(Looper.getMainLooper())
    private var cornerPointer: Int? = null
    private var cornerStart = Start(0f, 0f)
    private var lastSwipeAt = 0L

    private val openParent = Runnable {
        cornerPointer = null
        state.sound.stopAll()
        state.isParentOpen = true
    }

    private val isChildPlaying: Boolean
        get() = state.hasOnboarded && !state.isParentOpen && !state.isAllDone

    fun handle(event: MotionEvent) {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                val index = event.actionIndex
                began(event.getPointerId(index), event.getX(index), event.getY(index))
            }
            MotionEvent.ACTION_MOVE -> {
                val pointer = cornerPointer ?: return
                val index = event.findPointerIndex(pointer)
                if (index >= 0) {
                    val moved = hypot(event.getX(index) - cornerStart.x, event.getY(index) - cornerStart.y)
                    if (moved > 40 * density) cancelCornerHold()
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                val index = event.actionIndex
                ended(event.getPointerId(index), event.getX(index), event.getY(index))
            }
            MotionEvent.ACTION_CANCEL -> {
                cancelCornerHold()
                starts.clear()
            }
        }
    }

    private fun began(pointer: Int, x: Float, y: Float) {
        if (!state.hasOnboarded || state.isParentOpen) return

        // The hold also works on the sleepy cat screen, so a parent can get back in.
        val corner = 90 * density
        if (x < corner && y < corner && cornerPointer == null) {
            cornerPointer = pointer
            cornerStart = Start(x, y)
            handler.postDelayed(openParent, 3000)
        }

        if (!isChildPlaying) return
        if (starts.isEmpty()) state.touchClaimed = false
        starts[pointer] = Start(x, y)
        state.tapReaction(x, y)
    }

    private fun ended(pointer: Int, x: Float, y: Float) {
        if (cornerPointer == pointer) cancelCornerHold()
        val start = starts.remove(pointer) ?: return
        if (!isChildPlaying || state.touchClaimed) return

        val dx = x - start.x
        val dy = y - start.y
        val isSwipe = abs(dx) > 70 * density && abs(dx) > abs(dy) * 1.5f
        val now = System.currentTimeMillis()
        if (isSwipe && now - lastSwipeAt > 400) {
            lastSwipeAt = now
            state.changePage(forward = dx < 0)
        }
    }

    private fun cancelCornerHold() {
        handler.removeCallbacks(openParent)
        cornerPointer = null
    }
}
