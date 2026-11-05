# Kitty Play: build it on Windows and put it on your iPhone

You don't need a Mac or Xcode for this. GitHub's online Macs build the app, and a Windows tool called Sideloadly installs it on your iPhone. Allow an hour or two the first time.

The online build compiles the app for you. If you ever change the code and it stops with an error, paste the red message to Claude and it can be fixed.

## What you need

- Your Windows PC, and a cable that connects your iPhone to it.
- An iPhone on iOS 17 or newer.
- A free GitHub account.
- A free Apple ID. **Use a separate one rather than your main one,** because you'll type its password into Sideloadly.

### Limits to expect

- **7 days:** the app stops opening 7 days after each install. Reinstalling takes a few minutes and needs your PC and the cable (see Part 5).
- **3 apps:** a free Apple ID can have 3 of these self-installed apps on a device at once.
- **Blank icon:** the app doesn't have its own home-screen icon yet.

---

## Part 1: Put the project on GitHub

1. Right-click `KittyPlay.zip` and choose **Extract All**. Extract it to a new folder at `Documents\KittyPlay`. Inside you should see `project.yml`, `GUIDE.md`, and folders named `KittyPlay` and `.github`.
2. Create an account at [github.com](https://github.com) if you don't have one.
3. Install [GitHub Desktop](https://desktop.github.com) and sign in.
4. Choose **File > Add Local Repository** and select `Documents\KittyPlay`.
5. It will say this isn't a repository yet. Click **create a repository**, then **Create Repository**. Leave the other settings alone.
6. Click **Publish repository**.
7. **Untick "Keep this code private"**, then click **Publish Repository**.

**Why public?** [GitHub's Macs are free for public projects](https://docs.github.com/en/billing/concepts/product-billing/github-actions). Private projects get a limited monthly allowance. The project contains only game code, but anyone can see a public project, so:

- **Don't add family recordings or photos to a public project.** If you want those, make it private instead. The allowance should still cover plenty of builds, though [one forum report](https://github.com/orgs/community/discussions/210649) says macOS builds can be blocked on an account with a $0 spending limit.
- To keep your email address off the project, check **Keep my email addresses private** in GitHub's email settings before publishing.

## Part 2: Build the app

1. Open your project on github.com and click the **Actions** tab. A run called **Build iPhone app** should have started by itself. If not, click **Build iPhone app** on the left, then **Run workflow**.
2. Wait for it to finish. This usually takes several minutes.
   - **Green tick:** go to step 3.
   - **Red cross:** click into the run, then the step with the red cross. Copy the lines containing `error:` and paste them to Claude. After a fix, replace the file in `Documents\KittyPlay`. In GitHub Desktop, type a short summary, click **Commit to main**, then **Push origin**. That starts a new build.
3. Click the finished run, and under **Artifacts** download **KittyPlay-app**.
4. Unzip the download. Move `KittyPlay.ipa` somewhere safe, such as `Documents\KittyPlay-app`. You'll reuse this file for each weekly reinstall.

## Part 3: Install it on your iPhone

1. Install **iTunes from [Apple's website](https://www.apple.com/itunes/)**. [The Microsoft Store version won't work with Sideloadly](https://sideloadly.io/faq), so uninstall it first if you have it. Then install the desktop version of iCloud from Apple's website too, if Sideloadly asks for it.
2. Install [Sideloadly](https://sideloadly.io) from its official site.
3. Connect the iPhone by cable and unlock it. Tap **Trust This Computer** if asked.
4. Open Sideloadly. Check your iPhone is selected, then drag `KittyPlay.ipa` onto the window.
5. Enter your separate Apple ID and click **Start**. Enter its password, and the two-factor code if asked.
6. Wait for it to say **Done**. The app is now on your iPhone.

### Allow it to open

1. On the iPhone, go to **Settings > General > VPN & Device Management**. Tap your Apple ID and tap **Trust**.
2. Go to **Settings > Privacy & Security > Developer Mode**, turn it on, and restart when asked. Confirm when the phone asks again after restarting.
3. Open **Kitty Play**, choose a parent PIN, and read through the guide.

### If something goes wrong

| What you see | What to do |
|---|---|
| Sideloadly can't see the iPhone | Unlock the phone, tap Trust, and check iTunes is the website version. Try another cable. |
| Bundle identifier or App ID error | Open Sideloadly's advanced options and let it use an automatic bundle ID. |
| "Maximum number of apps" | Delete another self-installed app from the iPhone. |
| "Untrusted Developer" | Do step 1 under "Allow it to open". |
| Developer Mode isn't listed | Install the app first, then check again. |
| The app won't open a week later | The 7 days are up. See Part 5. |

Sideloadly's screens can change, so if something doesn't match, follow what its window says or paste the message to Claude.

## Part 4: Lock the phone with Guided Access

The app shows these steps on first launch too, and they're in the parent menu.

1. Go to **Settings > Accessibility > Guided Access** and turn it on.
2. Tap **Passcode Settings > Set Guided Access Passcode**. Don't forget it.
3. Set **Display Auto-Lock** to **Never** if your iPhone offers it.
4. Open Kitty Play, then **triple-click the side button** and tap **Start**.
5. To finish, triple-click the side button, enter the passcode, and tap **End**.

Turn on **Airplane Mode** first, so calls and messages can't interrupt her.

## Part 5: The weekly reinstall

Try to do this **before** the 7 days run out, so she doesn't find a game that won't open.

1. Connect the iPhone to your PC.
2. Open Sideloadly and drag in the same `KittyPlay.ipa`.
3. Use the **same Apple ID** and click **Start**.

Don't delete the app first. [Sideloadly says you can install over it with the same Apple ID](https://sideloadly.io), and that should keep your PIN and settings. You don't need to rebuild on GitHub unless the game itself has changed.

## Part 6: Test it before she plays

Sit down with the phone and check each of these:

- [ ] Tap the screen in lots of places. Each tap gives a sparkle, a pop and a small bounce.
- [ ] Swipe left and right through all 4 games, starting from different places.
- [ ] Stroke the cat without changing the page. It purrs, closes its eyes and rolls over after a long stroke.
- [ ] Drag each food to each animal, and also drop it on the grass.
- [ ] Press with **several fingers and a flat palm**, the way a 2-year-old does. Nothing should freeze or get stuck.
- [ ] Hold the top-left corner for 3 seconds, enter the PIN, and try every setting.
- [ ] Set the limit to 5 minutes, let it run out, and check the sleepy cat appears. Then use the corner hold and tap **Play Again**.
- [ ] Leave it alone for a few minutes. The screen shouldn't lock during play.

If anything looks or behaves wrong, describe it to Claude. After a fix, repeat the commit and push from Part 2, download the new app, and install it as in Part 5.

## Using the game

- **Parent menu:** hold the top-left corner for 3 seconds, then enter the PIN.
- **Swiping on Pet the Cat:** a swipe that starts on the cat counts as stroking. Start above or below the cat to change the game.
- **Play-time limit:** it starts at 15 minutes, and you can change it. The sleepy cat screen stays until you tap **Play Again** in the parent menu, even if the app is closed.
- **The clock restarts** whenever the app is closed and reopened. With Guided Access on, she can't do that herself.
- **Forgotten parent PIN:** delete the app and install it again. That also clears the settings. The PIN keeps a toddler out but isn't strong security.
- **Sound:** the game plays even when the ringer switch is on silent, so check the phone's volume first.

---

## Part 7: Better pictures and sounds (optional)

The game already works without any of this. It uses emoji for animals, a code-drawn cat, code-generated effects and the iPhone's built-in voice, all offline. The built-in voice saying "Moo!" is the weakest part, so real animal sounds are the best upgrade.

### Where to find free files

Check the license on **each file** before you download it, because licenses vary within a site. CC0 means free for any use with no credit needed. Your project is public, so these files will be public too.

| Site | Good for | Notes |
|---|---|---|
| [Kenney](https://kenney.nl/assets/animal-pack-redux) | Cartoon animals | Listed as CC0. Check that it has the animals you want before relying on it; [Cube Pets](https://kenney.nl/assets/cube-pets) is another option. |
| [OpenGameArt](https://opengameart.org/content/cat-purr-meow) | Art and sounds | This cat purr and meow is CC0, though it has a little background noise. |
| [Freesound](https://freesound.org) | Real animal sounds | Filter by the Creative Commons 0 license. Needs a free account. |
| [Pixabay Sound Effects](https://pixabay.com/sound-effects/) | Animal sounds | Has its own license, so read its terms. |
| [Mixkit](https://mixkit.co/free-sound-effects/animals/) | Animal sounds | Has its own license, so read its terms. |

Keep sounds short (1 to 2 seconds) and check they aren't scary. A real lion's roar may be too much for a 2-year-old, so look for a soft or cartoon one.

### How to name them

The app finds files by name, so spelling matters. Use all lowercase. Any file you don't add just keeps its built-in version.

| What | File name (`.mp3`, `.m4a`, `.wav` or `.caf`) |
|---|---|
| Animal sound | `sound_cat`, `sound_dog`, `sound_cow`, `sound_duck`, `sound_lion`, `sound_sheep`, `sound_bunny`, `sound_pig`, `sound_frog` |
| Voice saying the name | `name_cat`, `name_dog`, and so on |
| Effects | `fx_pop`, `fx_boing`, `fx_giggle`, `fx_chime`, `fx_fanfare`, `fx_purr` |
| Bedtime line | `say_alldone` |
| Picture (PNG with a transparent background) | `cat.png`, `dog.png`, `cow.png`, `duck.png`, `lion.png`, `sheep.png`, `bunny.png`, `pig.png`, `frog.png` |

**To add files:** create a folder at `Documents\KittyPlay\KittyPlay\Media` and put them in it. Then commit and push from GitHub Desktop, as in Part 2, and install the new build as in Part 5.

**Another language:** change the words and `voiceLanguage` at the top of `KittyPlay\Animals.swift`. For example, `"ar-SA"` selects Arabic.

---

## The Android version

The Android app lives in the `android` folder. GitHub builds it and plays it on an emulator each time it changes. You don't need Android Studio, and there's no 7-day limit.

### Install it

1. Copy `Documents\KittyPlay-app\KittyPlay.apk` to the phone, by USB cable or Google Drive.
2. On the phone, tap the file in the Files app.
3. If Android asks, allow that app to install unknown apps, and confirm. If Play Protect warns about an unknown developer, choose to install anyway. That's expected for an app you built yourself.
4. Open **Kitty Play**. Android may show a one-time "Viewing full screen" message, so tap **Got it**. Then choose your parent PIN.

### Lock the phone with screen pinning

1. In Settings, search for "pin". Depending on the phone, it's called App pinning, Screen pinning or Pin app. Turn it on.
2. Also turn on **Ask for PIN before unpinning** (or similar), so only you can leave.
3. In the game, tap **Pin this game and play**, or use **Pin this game now** in the parent menu, and accept the phone's message.
4. To finish, open the parent menu and tap **Unpin this game**.

### Differences from the iPhone version

- The animals are drawn rather than emoji, and the animal sounds are generated rather than spoken.
- The app has an icon, and arrows appear if she stays on one game for a while.
- The app has no internet permission, so it can't go online.
- Custom sounds go in `android/app/src/main/assets/media`, using the names in Part 7. Custom pictures aren't supported yet.

### Updating it

Until the app has a permanent signing key, each new build uses a different key. To install an update, uninstall the old version first, which resets the PIN and settings.
