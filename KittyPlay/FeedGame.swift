import SwiftUI

private struct Food: Identifiable {
    let id: String
    let emoji: String
    let eater: Animal
}

/// Game 3: drag food to an animal. Every attempt is happy, and the right match
/// gets a bigger celebration. Nothing ever counts as a mistake.
struct FeedGame: View {
    @Environment(GameState.self) private var state

    private let animals: [Animal] = [.cat, .dog, .bunny]
    // Not in the same order as the animals, so the match is more than a straight drag.
    private let foods: [Food] = [
        Food(id: "carrot", emoji: "🥕", eater: .bunny),
        Food(id: "fish", emoji: "🐟", eater: .cat),
        Food(id: "bone", emoji: "🦴", eater: .dog)
    ]

    @State private var hopCounts: [String: Int] = [:]
    @State private var eatenFoodIDs: Set<String> = []

    var body: some View {
        GeometryReader { geo in
            let cell = geo.size.width / 3
            ZStack {
                MeadowBackground()

                ForEach(Array(animals.enumerated()), id: \.element.id) { index, animal in
                    AnimalPicture(animal: animal, size: cell * 0.95)
                        .onTouchDown {
                            hopCounts[animal.id, default: 0] += 1
                            Sound.shared.animalCall(animal)
                        }
                        .hop(trigger: hopCounts[animal.id, default: 0])
                        .position(animalPosition(index, in: geo.size))
                }

                ForEach(Array(foods.enumerated()), id: \.element.id) { index, food in
                    DraggableFood(
                        emoji: food.emoji,
                        size: cell * 0.95,
                        isEaten: eatenFoodIDs.contains(food.id),
                        onDrop: { point in
                            dropped(food, at: point, in: geo.size)
                        }
                    )
                    .position(x: cell * (CGFloat(index) + 0.5), y: geo.size.height * 0.76)
                }
            }
            .frame(width: geo.size.width, height: geo.size.height)
            .coordinateSpace(.named("feed"))
        }
    }

    private func animalPosition(_ index: Int, in size: CGSize) -> CGPoint {
        CGPoint(x: size.width / 3 * (CGFloat(index) + 0.5), y: size.height * 0.33)
    }

    private func dropped(_ food: Food, at point: CGPoint, in size: CGSize) {
        var nearestIndex = 0
        var nearestDistance = CGFloat.greatestFiniteMagnitude
        for index in animals.indices {
            let position = animalPosition(index, in: size)
            let distance = hypot(point.x - position.x, point.y - position.y)
            if distance < nearestDistance {
                nearestIndex = index
                nearestDistance = distance
            }
        }

        guard nearestDistance < 130 else {
            // Dropped on the grass: the food just bounces home.
            Sound.shared.play(.boing, volume: 0.7)
            return
        }

        let animal = animals[nearestIndex]
        let position = animalPosition(nearestIndex, in: size)
        hopCounts[animal.id, default: 0] += 1

        if animal == food.eater {
            celebrate(food, animal: animal, at: position)
        } else {
            Sound.shared.play(.giggle)
            state.burst(at: position, emoji: "⭐️", count: 3)
        }
    }

    private func celebrate(_ food: Food, animal: Animal, at position: CGPoint) {
        Sound.shared.play(.fanfare)
        Sound.shared.say(Words.yum)
        state.burst(at: position)
        eatenFoodIDs.insert(food.id)
        Task {
            try? await Task.sleep(for: .milliseconds(350))
            hopCounts[animal.id, default: 0] += 1
            try? await Task.sleep(for: .milliseconds(650))
            eatenFoodIDs.remove(food.id)
        }
    }
}

private struct DraggableFood: View {
    @Environment(GameState.self) private var state

    let emoji: String
    let size: CGFloat
    let isEaten: Bool
    let onDrop: (CGPoint) -> Void

    // Resets by itself when the finger lifts, so food can never get stuck.
    @GestureState private var dragOffset = CGSize.zero

    var body: some View {
        Text(emoji)
            .font(.system(size: size * 0.8))
            .frame(width: size, height: size)
            .contentShape(Rectangle())
            .gesture(dragGesture)
            .scaleEffect(isEaten ? 0.01 : (dragOffset == .zero ? 1 : 1.15))
            .offset(dragOffset)
            .animation(.spring(response: 0.3, dampingFraction: 0.6), value: dragOffset)
            .animation(.spring(response: 0.3, dampingFraction: 0.6), value: isEaten)
    }

    private var dragGesture: some Gesture {
        DragGesture(minimumDistance: 0, coordinateSpace: .named("feed"))
            .updating($dragOffset) { value, offset, _ in
                offset = value.translation
            }
            .onChanged { _ in
                state.touchClaimed = true
            }
            .onEnded { value in
                onDrop(value.location)
            }
    }
}
