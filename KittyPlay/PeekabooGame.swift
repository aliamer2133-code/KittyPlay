import SwiftUI

/// Game 4: touch a bush and an animal pops out with a sound and a giggle.
struct PeekabooGame: View {
    var body: some View {
        GeometryReader { geo in
            let size = min(geo.size.width / 2, geo.size.height / 3) * 0.9
            ZStack {
                MeadowBackground()
                VStack(spacing: 0) {
                    ForEach(0..<2, id: \.self) { _ in
                        HStack(spacing: 0) {
                            ForEach(0..<2, id: \.self) { _ in
                                BushSpot(size: size)
                                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                            }
                        }
                    }
                }
                .padding(.top, geo.size.height * 0.12)
                .padding(.bottom, 20)
            }
        }
    }
}

private struct BushSpot: View {
    let size: CGFloat

    @State private var animal = Animal.peekabooCast.randomElement() ?? .cat
    @State private var isOut = false
    @State private var hopCount = 0
    @State private var hideTask: Task<Void, Never>?

    var body: some View {
        ZStack {
            AnimalPicture(animal: animal, size: size * 0.85)
                .hop(trigger: hopCount)
                .scaleEffect(isOut ? 1 : 0.5)
                .opacity(isOut ? 1 : 0)
                .offset(y: isOut ? -size * 0.42 : size * 0.1)
            Bush(size: size)
                .offset(y: size * 0.12)
        }
        .frame(width: size, height: size)
        .onTouchDown {
            bushTouched()
        }
        .onDisappear {
            hideTask?.cancel()
        }
    }

    private func bushTouched() {
        if isOut {
            hopCount += 1
            Sound.shared.animalCall(animal)
        } else {
            animal = Animal.peekabooCast.randomElement() ?? .cat
            withAnimation(.spring(response: 0.35, dampingFraction: 0.55)) {
                isOut = true
            }
            Sound.shared.animalCall(animal)
            Sound.shared.play(.giggle, volume: 0.7)
        }
        scheduleHide()
    }

    private func scheduleHide() {
        hideTask?.cancel()
        hideTask = Task {
            try? await Task.sleep(for: .seconds(3))
            guard !Task.isCancelled else { return }
            withAnimation(.easeInOut(duration: 0.3)) {
                isOut = false
            }
        }
    }
}

private struct Bush: View {
    let size: CGFloat

    private let dark = Color(red: 0.2, green: 0.62, blue: 0.25)
    private let light = Color(red: 0.32, green: 0.74, blue: 0.3)

    var body: some View {
        ZStack {
            Circle()
                .fill(dark)
                .frame(width: size * 0.56, height: size * 0.56)
                .offset(x: -size * 0.24, y: size * 0.06)
            Circle()
                .fill(dark)
                .frame(width: size * 0.56, height: size * 0.56)
                .offset(x: size * 0.24, y: size * 0.06)
            Circle()
                .fill(light)
                .frame(width: size * 0.66, height: size * 0.66)
                .offset(y: -size * 0.04)
            Text("🌼")
                .font(.system(size: size * 0.14))
                .offset(x: -size * 0.14, y: size * 0.02)
            Text("🌸")
                .font(.system(size: size * 0.14))
                .offset(x: size * 0.18, y: -size * 0.08)
        }
    }
}
