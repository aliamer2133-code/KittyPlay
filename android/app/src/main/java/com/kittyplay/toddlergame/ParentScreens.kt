package com.kittyplay.toddlergame

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/** The root screen. Decides what is showing. */
@Composable
fun AppRoot(
    state: GameState,
    pinScreen: () -> Unit,
    unpinScreen: () -> Unit,
    isScreenPinned: () -> Boolean,
) {
    // The clock: one tick per second for as long as the app is on screen.
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            state.tick()
        }
    }

    Box(Modifier.fillMaxSize().background(Color.White)) {
        when {
            !state.hasOnboarded -> OnboardingScreen(state, pinScreen)
            state.isParentOpen -> ParentGate(state, pinScreen, unpinScreen, isScreenPinned)
            state.isAllDone -> SleepyCatScreen(state)
            else -> {
                GamePager(state)
                if (state.showSwipeHint) SwipeHint()
                SparkleLayer(state)
            }
        }
    }
}

@Composable
fun PinPad(
    title: String,
    message: String = "",
    onCancel: (() -> Unit)? = null,
    onComplete: (String) -> Unit,
) {
    var digits by remember { mutableStateOf("") }

    fun enter(digit: String) {
        if (digits.length >= 4) return
        digits += digit
        if (digits.length == 4) {
            val entered = digits
            digits = ""
            onComplete(entered)
        }
    }

    Column(
        Modifier.fillMaxWidth().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(title, fontSize = 24.sp, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurface)
        Text(message, color = Color(0xFFC62828))
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            repeat(4) { index ->
                Box(
                    Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(if (index < digits.length) Color.DarkGray else Color.LightGray)
                )
            }
        }
        for (row in listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"))) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                for (digit in row) {
                    FilledTonalButton(onClick = { enter(digit) }, modifier = Modifier.size(76.dp)) {
                        Text(digit, fontSize = 26.sp)
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(76.dp), contentAlignment = Alignment.Center) {
                if (onCancel != null) TextButton(onClick = onCancel) { Text("Close") }
            }
            FilledTonalButton(onClick = { enter("0") }, modifier = Modifier.size(76.dp)) {
                Text("0", fontSize = 26.sp)
            }
            Box(Modifier.size(76.dp), contentAlignment = Alignment.Center) {
                TextButton(onClick = { digits = digits.dropLast(1) }) { Text("Undo") }
            }
        }
    }
}

@Composable
private fun ParentGate(
    state: GameState,
    pinScreen: () -> Unit,
    unpinScreen: () -> Unit,
    isScreenPinned: () -> Boolean,
) {
    var isUnlocked by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }

    Box(Modifier.fillMaxSize().safeDrawingPadding(), contentAlignment = Alignment.Center) {
        if (isUnlocked) {
            ParentMenu(state, pinScreen, unpinScreen, isScreenPinned)
        } else {
            PinPad(
                title = "Grown-ups only",
                message = message,
                onCancel = { state.isParentOpen = false },
                onComplete = { entered ->
                    if (entered == state.pin) isUnlocked = true else message = "That PIN didn't match."
                },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ParentMenu(
    state: GameState,
    pinScreen: () -> Unit,
    unpinScreen: () -> Unit,
    isScreenPinned: () -> Boolean,
) {
    var isChangingPin by remember { mutableStateOf(false) }
    var showGuide by remember { mutableStateOf(false) }

    if (isChangingPin) {
        NewPinFlow(
            title = "Enter a new PIN",
            onCancel = { isChangingPin = false },
            onChosen = { chosen ->
                state.pin = chosen
                isChangingPin = false
            },
        )
        return
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Parent Menu", fontSize = 28.sp)

        if (state.isAllDone) {
            Button(
                onClick = {
                    state.playAgain()
                    state.isParentOpen = false
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Play Again") }
            Text("The sleepy cat screen stays until you tap this.", fontSize = 13.sp)
        }

        HorizontalDivider()
        SwitchRow("Sound on", state.soundOn) { state.soundOn = it }

        HorizontalDivider()
        Text("Mini-games", fontSize = 18.sp)
        for (game in MiniGame.entries) {
            val isOn = state.isGameEnabled(game)
            SwitchRow(
                label = game.parentLabel,
                checked = isOn,
                // At least one game always stays on.
                enabled = !(isOn && state.enabledGames.size == 1),
            ) { state.setGameEnabled(game, it) }
        }

        HorizontalDivider()
        Text("Play-time limit", fontSize = 18.sp)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (minutes in listOf(0, 5, 10, 15, 20, 30, 45, 60)) {
                FilterChip(
                    selected = state.limitMinutes == minutes,
                    onClick = { state.limitMinutes = minutes },
                    label = { Text(if (minutes == 0) "No limit" else "$minutes min") },
                )
            }
        }
        Text(
            "Played so far: ${state.secondsPlayed / 60} min. The clock restarts when the app is closed and reopened.",
            fontSize = 13.sp,
        )
        OutlinedButton(onClick = { state.secondsPlayed = 0 }) { Text("Restart the play clock") }

        HorizontalDivider()
        Text("Locking the phone", fontSize = 18.sp)
        if (isScreenPinned()) {
            OutlinedButton(onClick = unpinScreen) { Text("Unpin this game") }
        } else {
            OutlinedButton(onClick = pinScreen) { Text("Pin this game now") }
        }
        OutlinedButton(onClick = { showGuide = !showGuide }) {
            Text(if (showGuide) "Hide pinning help" else "How to set up screen pinning")
        }
        if (showGuide) PinningGuide()
        OutlinedButton(onClick = { isChangingPin = true }) { Text("Change PIN") }

        Spacer(Modifier.height(8.dp))
        Button(onClick = { state.isParentOpen = false }, modifier = Modifier.fillMaxWidth()) { Text("Done") }
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, enabled: Boolean = true, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange, enabled = enabled)
    }
}

@Composable
private fun NewPinFlow(title: String, onCancel: (() -> Unit)?, onChosen: (String) -> Unit) {
    var first by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf("") }
    PinPad(
        title = if (first == null) title else "Enter it once more",
        message = message,
        onCancel = onCancel,
        onComplete = { entered ->
            val earlier = first
            when {
                earlier == null -> {
                    first = entered
                    message = ""
                }
                earlier == entered -> onChosen(entered)
                else -> {
                    first = null
                    message = "Those didn't match. Try again."
                }
            }
        },
    )
}

@Composable
private fun OnboardingScreen(state: GameState, pinScreen: () -> Unit) {
    var chosenPin by remember { mutableStateOf<String?>(null) }
    val pin = chosenPin

    Box(Modifier.fillMaxSize().safeDrawingPadding(), contentAlignment = Alignment.Center) {
        if (pin == null) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "Hold the top-left corner for 3 seconds during play to open the parent menu. It asks for this PIN.",
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
                NewPinFlow(title = "Choose a parent PIN", onCancel = null, onChosen = { chosenPin = it })
            }
        } else {
            Column(Modifier.fillMaxSize().padding(20.dp)) {
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                    PinningGuide()
                }
                OutlinedButton(
                    onClick = {
                        state.pin = pin
                        state.hasOnboarded = true
                        pinScreen()
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Pin this game and play") }
                Button(
                    onClick = {
                        state.pin = pin
                        state.hasOnboarded = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Got it, start playing") }
            }
        }
    }
}

@Composable
private fun PinningGuide() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Lock the phone to this game", fontSize = 22.sp)
        Text("Screen pinning keeps her inside this game until you unpin it.")
        val steps = listOf(
            "Open Settings and search for \"pin\". Depending on your phone it is called App pinning, Screen pinning or Pin app.",
            "Turn it on. Also turn on \"Ask for PIN before unpinning\" (or similar), so only you can leave.",
            "Come back to this game and tap \"Pin this game\". Accept the phone's message.",
            "To finish, hold the corner to open the parent menu and tap \"Unpin this game\". Or hold the Back and Recents buttons together, or swipe up and hold on phones without buttons.",
        )
        steps.forEachIndexed { index, step -> Text("${index + 1}. $step") }
        Text("Tip: turn on Airplane Mode first, so calls and messages can't interrupt.", fontSize = 13.sp)
    }
}
