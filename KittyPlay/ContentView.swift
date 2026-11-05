import SwiftUI
import UIKit

/// The root screen. Decides what is showing and keeps the screen awake.
struct ContentView: View {
    @Environment(GameState.self) private var state
    @Environment(\.scenePhase) private var scenePhase

    var body: some View {
        ZStack {
            if !state.hasOnboarded {
                OnboardingView()
            } else if state.isAllDone {
                SleepyCatView()
            } else {
                GamePager()
            }

            SparkleLayer()

            if state.isParentOpen {
                ParentGateView()
                    .zIndex(10)
            }
        }
        .background(TouchWatcherHost(state: state))
        .statusBarHidden(true)
        .persistentSystemOverlays(.hidden)
        .defersSystemGestures(on: .all)
        .onChange(of: scenePhase, initial: true) { _, newPhase in
            state.isAppActive = (newPhase == .active)
            if newPhase == .background {
                // A fresh play session starts the next time the app is opened.
                state.secondsPlayed = 0
                Sound.shared.stopAll()
            }
            updateScreenAwake()
        }
        .onChange(of: state.isAllDone, initial: true) { _, _ in
            updateScreenAwake()
        }
        .task {
            // The play-time clock: one tick per second for the app's whole life.
            while !Task.isCancelled {
                try? await Task.sleep(for: .seconds(1))
                state.tick()
            }
        }
    }

    /// Stay awake during play, but let the phone sleep on the "all done" screen.
    private func updateScreenAwake() {
        UIApplication.shared.isIdleTimerDisabled = (scenePhase == .active) && !state.isAllDone
    }
}

/// Shows the current mini-game. Swipes are detected in TouchRouter.
struct GamePager: View {
    @Environment(GameState.self) private var state

    var body: some View {
        ZStack {
            gameView(for: state.currentGame)
                .id(state.currentGame)
                .transition(pageTransition)
        }
        .scaleEffect(state.isBouncing ? 1.015 : 1)
        .animation(.spring(response: 0.2, dampingFraction: 0.4), value: state.isBouncing)
        .ignoresSafeArea()
    }

    private var pageTransition: AnyTransition {
        let forward = state.slideForward
        return .asymmetric(
            insertion: .move(edge: forward ? .trailing : .leading),
            removal: .move(edge: forward ? .leading : .trailing)
        )
    }

    @ViewBuilder
    private func gameView(for game: MiniGame) -> some View {
        switch game {
        case .petCat: PetCatGame()
        case .animalSounds: AnimalSoundsGame()
        case .feed: FeedGame()
        case .peekaboo: PeekabooGame()
        }
    }
}
