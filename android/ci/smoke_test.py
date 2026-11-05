#!/usr/bin/env python3
"""Plays the installed app on an emulator like a person would, taking screenshots.

Usage: smoke_test.py <apk> <screenshot folder>
Fails if the app crashes, leaves the foreground or a parent screen doesn't appear.
"""
import re
import subprocess
import sys
import time
from pathlib import Path

PACKAGE = "com.kittyplay.toddlergame"
apk, shots = sys.argv[1], Path(sys.argv[2])
shots.mkdir(parents=True, exist_ok=True)
failures = []
number = 0


def adb(*args, check=True):
    done = subprocess.run(["adb", *args], capture_output=True, text=True, timeout=120)
    if check and done.returncode != 0:
        raise RuntimeError(f"adb {args} failed: {done.stderr}")
    return done.stdout


def shot(name):
    global number
    number += 1
    path = shots / f"{number:02d}_{name}.png"
    with path.open("wb") as out:
        subprocess.run(["adb", "exec-out", "screencap", "-p"], stdout=out, check=True, timeout=60)
    print("screenshot:", path.name)


def tap(x, y):
    adb("shell", "input", "tap", str(int(x)), str(int(y)))


def swipe(x1, y1, x2, y2, ms=300):
    adb("shell", "input", "swipe", str(int(x1)), str(int(y1)), str(int(x2)), str(int(y2)), str(ms))


def screen_nodes():
    for _ in range(5):
        adb("shell", "uiautomator", "dump", "/sdcard/ui.xml", check=False)
        xml = adb("exec-out", "cat", "/sdcard/ui.xml", check=False)
        if "<hierarchy" in xml:
            return re.findall(r'text="([^"]*)"[^>]*?bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', xml)
        time.sleep(1)
    return []


def has_text(text):
    return any(node[0] == text for node in screen_nodes())


def tap_text(text, required=True):
    for attempt in range(3):
        for label, x1, y1, x2, y2 in screen_nodes():
            if label == text:
                tap((int(x1) + int(x2)) / 2, (int(y1) + int(y2)) / 2)
                time.sleep(0.5)
                return True
        if attempt == 0:
            # It may be further down a scrolling screen.
            swipe(width / 2, height * 0.8, width / 2, height * 0.3, 400)
        time.sleep(1)
    if required:
        failures.append(f"could not find on screen: {text}")
    return False


def expect(text):
    if not has_text(text):
        failures.append(f"expected to see: {text}")


def enter_pin():
    for digit in "1234":
        tap_text(digit)


def check_foreground(step):
    state = adb("shell", "dumpsys", "activity", "activities", check=False)
    if not re.search(r"(mResumedActivity|topResumedActivity|ResumedActivity).*" + PACKAGE, state):
        failures.append(f"app not in foreground after: {step}")


def hold_corner():
    swipe(50, 150, 50, 150, 3500)
    time.sleep(1)


adb("install", "-r", apk)
adb("logcat", "-c", check=False)
size = re.search(r"(\d+)x(\d+)", adb("shell", "wm", "size"))
width, height = int(size.group(1)), int(size.group(2))
print(f"screen {width}x{height}")

adb("shell", "am", "start", "-W", "-n", f"{PACKAGE}/.MainActivity")
time.sleep(4)

# First launch
shot("choose_pin")
expect("Choose a parent PIN")
enter_pin()
time.sleep(1)
expect("Enter it once more")
enter_pin()
time.sleep(1)
shot("pinning_guide")
tap_text("Got it, start playing")
time.sleep(2)

BOTTOM = height * 0.93


def next_game():
    swipe(width * 0.85, BOTTOM, width * 0.15, BOTTOM, 300)
    time.sleep(1.2)


# Game 1: stroking the cat must not change the page.
shot("pet_cat")
tap(width / 2, height / 2)
time.sleep(0.25)
shot("pet_cat_tapped")
swipe(width * 0.3, height * 0.55, width * 0.7, height * 0.55, 700)
time.sleep(0.3)
swipe(width * 0.7, height * 0.55, width * 0.3, height * 0.55, 700)
time.sleep(0.2)
shot("pet_cat_after_stroking_still_here")

# The back button must not leave the game.
adb("shell", "input", "keyevent", "4")
time.sleep(1)
check_foreground("back button")

# Game 2
next_game()
shot("animal_sounds")
tap(width * 0.25, height * 0.25)
time.sleep(0.2)
shot("animal_sounds_tapped")
for x, y in [(0.75, 0.25), (0.25, 0.5), (0.75, 0.5), (0.25, 0.75), (0.75, 0.75)]:
    tap(width * x, height * y)
    time.sleep(0.5)
check_foreground("tapping animals")

# Game 3: the fish is the middle food and the cat is the left animal.
next_game()
shot("feed")
swipe(width * 0.5, height * 0.76, width / 6, height * 0.33, 800)
time.sleep(0.3)
shot("feed_correct_match")
time.sleep(2)
swipe(width * 0.5, height * 0.76, width * 0.5, height * 0.33, 800)
time.sleep(0.3)
shot("feed_wrong_match")
time.sleep(2)
swipe(width / 6, height * 0.76, width * 0.5, height * 0.6, 500)
time.sleep(1.5)
shot("feed_food_back_home")

# Game 4
next_game()
shot("peekaboo")
for x, y in [(0.25, 0.4), (0.75, 0.4), (0.25, 0.75), (0.75, 0.75)]:
    tap(width * x, height * y)
    time.sleep(0.3)
time.sleep(0.6)
shot("peekaboo_revealed")

# Wraps around to the first game, and back the other way.
next_game()
shot("wrapped_to_pet_cat")
swipe(width * 0.15, BOTTOM, width * 0.85, BOTTOM, 300)
time.sleep(1.2)
next_game()

# A roughly 25 second wait shows the gentle swipe hint.
time.sleep(27)
shot("swipe_hint")

# Palm test: several fingers can't be sent with adb, so rapid tapping stands in.
for i in range(30):
    tap(width * ((i * 7) % 10 + 0.5) / 10, height * ((i * 3) % 10 + 0.5) / 10)
time.sleep(1)
shot("after_mashing")
check_foreground("rapid tapping")

# Parent menu: wrong PIN first, then the right one.
hold_corner()
shot("parent_gate")
expect("Grown-ups only")
for digit in "9999":
    tap_text(digit)
time.sleep(0.5)
expect("That PIN didn't match.")
enter_pin()
time.sleep(1)
shot("parent_menu")
expect("Parent Menu")
tap_text("5 min")
time.sleep(0.5)
shot("parent_menu_limit_set")
tap_text("Done")
time.sleep(1)

# Stay on a game until the limit runs out.
print("waiting for the 5 minute limit")
time.sleep(310)
shot("sleepy_cat")
check_foreground("time limit")
window = adb("shell", "dumpsys", "window", "windows", check=False)
print("keep-screen-on still set on sleepy screen:", "KEEP_SCREEN_ON" in window)

# Touches stay quiet here, and the corner hold still gets a parent back in.
tap(width / 2, height / 2)
time.sleep(0.3)
shot("sleepy_cat_after_touch")
hold_corner()
enter_pin()
time.sleep(1)
shot("parent_menu_play_again")
tap_text("Play Again")
time.sleep(2)
shot("playing_again")

# Force-close and reopen: the PIN and settings must have been kept.
adb("shell", "am", "force-stop", PACKAGE)
adb("shell", "am", "start", "-W", "-n", f"{PACKAGE}/.MainActivity")
time.sleep(3)
shot("reopened_goes_straight_to_game")
if has_text("Choose a parent PIN"):
    failures.append("settings were lost after reopening")

log = adb("logcat", "-d", check=False)
for match in re.finditer(r"FATAL EXCEPTION.*(?:\n.*){0,25}", log):
    if PACKAGE in match.group(0):
        failures.append("crash:\n" + match.group(0))
(shots / "logcat.txt").write_text(log[-400000:])

if failures:
    print("\nFAILED:")
    for failure in failures:
        print(" -", failure)
    sys.exit(1)
print("\nAll checks passed")
