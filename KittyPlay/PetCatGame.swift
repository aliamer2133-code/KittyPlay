import SwiftUI

/// Game 1: tap or stroke the cat. She purrs, meows, blinks and rolls over.
struct PetCatGame: View {
    @Environment(GameState.self) private var state

    @GestureState private var isTouching = false
    @State private var isBlinking = false
    @State private var isStroking = false
    @State private var isBouncing = false
    @State private var rollAngle = 0.0
    @State private var lastPoint: CGPoint?
    @State private var strokeLength: CGFloat = 0
    @State private var distanceSinceHeart: CGFloat = 0
    @State private var distanceSinceRoll: CGFloat = 0
    @State private var tapCount = 0
    @State private var lastMeow = Date.distantPast

    var body: some View {
        GeometryReader { geo in
            ZStack {
                background
                CatView(
                    size: min(geo.size.width, geo.size.height) * 0.86,
                    eyesClosed: isBlinking || isStroking,
                    isHappy: isStroking || isBouncing
                )
                .contentShape(Rectangle())
                .gesture(petGesture)
                .scaleEffect(isBouncing ? 1.08 : 1)
                .animation(.spring(response: 0.25, dampingFraction: 0.45), value: isBouncing)
                .rotationEffect(.degrees(rollAngle))
                .position(x: geo.size.width / 2, y: geo.size.height / 2)
            }
        }
        .onChange(of: isTouching) { _, touching in
            if touching {
                fingerLanded()
            } else {
                fingerLifted()
            }
        }
        .task {
            // Blinks on her own every few seconds.
            while !Task.isCancelled {
                try? await Task.sleep(for: .seconds(Double.random(in: 2.5...5)))
                isBlinking = true
                try? await Task.sleep(for: .milliseconds(160))
                isBlinking = false
            }
        }
        .onDisappear {
            Sound.shared.stopPurr()
        }
    }

    private var background: some View {
        LinearGradient(
            colors: [Color(red: 1.0, green: 0.85, blue: 0.92), Color(red: 1.0, green: 0.95, blue: 0.7)],
            startPoint: .top,
            endPoint: .bottom
        )
    }

    private var petGesture: some Gesture {
        DragGesture(minimumDistance: 0, coordinateSpace: .global)
            .updating($isTouching) { _, touching, _ in
                touching = true
            }
            .onChanged { value in
                fingerMoved(to: value.location)
            }
    }

    private func fingerLanded() {
        tapCount += 1
        isBouncing = true
        isBlinking = true
        meow()
        Task {
            try? await Task.sleep(for: .milliseconds(180))
            isBouncing = false
            isBlinking = false
        }
        if tapCount % 6 == 0 {
            rollOver()
        }
    }

    private func fingerMoved(to point: CGPoint) {
        defer { lastPoint = point }
        guard let previous = lastPoint else { return }

        let distance = hypot(point.x - previous.x, point.y - previous.y)
        strokeLength += distance
        guard strokeLength > 12 else { return }

        // This finger is stroking, so it isn't a swipe to the next game.
        state.touchClaimed = true
        isStroking = true
        Sound.shared.startPurr()

        distanceSinceHeart += distance
        if distanceSinceHeart > 70 {
            distanceSinceHeart = 0
            state.addSparkle(at: point, emoji: "💖")
        }

        distanceSinceRoll += distance
        if distanceSinceRoll > 900 {
            distanceSinceRoll = 0
            rollOver()
        }
    }

    private func fingerLifted() {
        lastPoint = nil
        strokeLength = 0
        isStroking = false
        Sound.shared.stopPurr()
    }

    private func meow() {
        guard Date().timeIntervalSince(lastMeow) > 0.8 else { return }
        lastMeow = Date()
        Sound.shared.animalCall(.cat)
    }

    private func rollOver() {
        Sound.shared.play(.giggle)
        withAnimation(.easeInOut(duration: 1.1)) {
            rollAngle += 360
        }
    }
}
