import SwiftUI
import UIKit
import UIKit.UIGestureRecognizerSubclass

/// Handles three things for the whole screen, whatever is under the finger:
///   1. a happy reaction on every touch
///   2. left/right swipes between mini-games
///   3. the secret 3-second hold in the top-left corner
@MainActor
final class TouchRouter {
    private let state: GameState
    private var startPoints: [ObjectIdentifier: CGPoint] = [:]
    private var lastSwipe = Date.distantPast

    private var cornerTouch: ObjectIdentifier?
    private var cornerStart = CGPoint.zero
    private var cornerTask: Task<Void, Never>?

    private let cornerSize: CGFloat = 90
    private let cornerHoldSeconds = 3.0
    private let swipeDistance: CGFloat = 70

    init(state: GameState) {
        self.state = state
    }

    private var isChildPlaying: Bool {
        state.hasOnboarded && !state.isParentOpen && !state.isAllDone
    }

    func touchBegan(_ touch: UITouch, at point: CGPoint) {
        guard state.hasOnboarded, !state.isParentOpen else { return }

        // The hold also works on the "all done" screen, so a parent can get back in.
        if point.x < cornerSize, point.y < cornerSize, cornerTouch == nil {
            startCornerHold(touch, at: point)
        }

        guard isChildPlaying else { return }
        if startPoints.isEmpty {
            state.touchClaimed = false
        }
        startPoints[ObjectIdentifier(touch)] = point
        state.tapReaction(at: point)
    }

    func touchMoved(_ touch: UITouch, to point: CGPoint) {
        guard cornerTouch == ObjectIdentifier(touch) else { return }
        if hypot(point.x - cornerStart.x, point.y - cornerStart.y) > 40 {
            cancelCornerHold()
        }
    }

    func touchEnded(_ touch: UITouch, at point: CGPoint) {
        let key = ObjectIdentifier(touch)
        if cornerTouch == key {
            cancelCornerHold()
        }
        guard let start = startPoints.removeValue(forKey: key) else { return }
        guard isChildPlaying, !state.touchClaimed else { return }

        let dx = point.x - start.x
        let dy = point.y - start.y
        let isSwipe = abs(dx) > swipeDistance && abs(dx) > abs(dy) * 1.5
        if isSwipe, Date().timeIntervalSince(lastSwipe) > 0.4 {
            lastSwipe = Date()
            state.changePage(forward: dx < 0)
        }
    }

    func touchCancelled(_ touch: UITouch) {
        let key = ObjectIdentifier(touch)
        if cornerTouch == key {
            cancelCornerHold()
        }
        startPoints.removeValue(forKey: key)
    }

    private func startCornerHold(_ touch: UITouch, at point: CGPoint) {
        cornerTouch = ObjectIdentifier(touch)
        cornerStart = point
        let seconds = cornerHoldSeconds
        cornerTask = Task {
            try? await Task.sleep(for: .seconds(seconds))
            guard !Task.isCancelled else { return }
            Sound.shared.stopAll()
            self.state.isParentOpen = true
        }
    }

    private func cancelCornerHold() {
        cornerTask?.cancel()
        cornerTask = nil
        cornerTouch = nil
    }
}

/// Watches every touch in the window without ever blocking one. It never "recognizes"
/// anything, so the games' own gestures carry on as if it weren't there.
final class TouchWatcher: UIGestureRecognizer, UIGestureRecognizerDelegate {
    var router: TouchRouter?
    private var activeTouches = Set<UITouch>()

    override func touchesBegan(_ touches: Set<UITouch>, with event: UIEvent) {
        super.touchesBegan(touches, with: event)
        for touch in touches {
            activeTouches.insert(touch)
            router?.touchBegan(touch, at: touch.location(in: view))
        }
    }

    override func touchesMoved(_ touches: Set<UITouch>, with event: UIEvent) {
        super.touchesMoved(touches, with: event)
        for touch in touches {
            router?.touchMoved(touch, to: touch.location(in: view))
        }
    }

    override func touchesEnded(_ touches: Set<UITouch>, with event: UIEvent) {
        super.touchesEnded(touches, with: event)
        for touch in touches {
            activeTouches.remove(touch)
            router?.touchEnded(touch, at: touch.location(in: view))
        }
        finishIfIdle()
    }

    override func touchesCancelled(_ touches: Set<UITouch>, with event: UIEvent) {
        super.touchesCancelled(touches, with: event)
        for touch in touches {
            activeTouches.remove(touch)
            router?.touchCancelled(touch)
        }
        finishIfIdle()
    }

    private func finishIfIdle() {
        if activeTouches.isEmpty {
            state = .failed
        }
    }

    override func reset() {
        super.reset()
        activeTouches.removeAll()
    }

    override func canPrevent(_ preventedGestureRecognizer: UIGestureRecognizer) -> Bool {
        false
    }

    override func canBePrevented(by preventingGestureRecognizer: UIGestureRecognizer) -> Bool {
        false
    }

    func gestureRecognizer(
        _ gestureRecognizer: UIGestureRecognizer,
        shouldRecognizeSimultaneouslyWith otherGestureRecognizer: UIGestureRecognizer
    ) -> Bool {
        true
    }
}

/// Invisible helper that attaches the TouchWatcher to the app's window.
struct TouchWatcherHost: UIViewRepresentable {
    let state: GameState

    func makeCoordinator() -> TouchRouter {
        TouchRouter(state: state)
    }

    func makeUIView(context: Context) -> WatcherHostView {
        let view = WatcherHostView()
        view.isUserInteractionEnabled = false
        view.router = context.coordinator
        return view
    }

    func updateUIView(_ uiView: WatcherHostView, context: Context) {}
}

final class WatcherHostView: UIView {
    var router: TouchRouter?
    private var watcher: TouchWatcher?

    override func didMoveToWindow() {
        super.didMoveToWindow()
        guard watcher == nil, let window, let router else { return }
        let newWatcher = TouchWatcher(target: nil, action: nil)
        newWatcher.router = router
        newWatcher.cancelsTouchesInView = false
        newWatcher.delaysTouchesBegan = false
        newWatcher.delaysTouchesEnded = false
        newWatcher.delegate = newWatcher
        window.addGestureRecognizer(newWatcher)
        watcher = newWatcher
    }
}
