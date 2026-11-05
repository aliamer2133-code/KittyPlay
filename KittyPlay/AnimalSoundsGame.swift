import SwiftUI

/// Game 2: six big animals. A touch plays the sound, then a voice says the name.
struct AnimalSoundsGame: View {
    private let rows: [[Animal]] = [
        [.cat, .dog],
        [.cow, .duck],
        [.lion, .sheep]
    ]

    var body: some View {
        GeometryReader { geo in
            let size = min(geo.size.width / 2, geo.size.height / 3.4) * 0.92
            ZStack {
                MeadowBackground()
                VStack(spacing: 0) {
                    ForEach(rows, id: \.self) { row in
                        HStack(spacing: 0) {
                            ForEach(row) { animal in
                                SingingAnimal(animal: animal, size: size)
                                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                            }
                        }
                    }
                }
                .padding(.top, 60)
                .padding(.bottom, 30)
            }
        }
    }
}

private struct SingingAnimal: View {
    let animal: Animal
    let size: CGFloat
    @State private var hopCount = 0

    var body: some View {
        AnimalPicture(animal: animal, size: size)
            .onTouchDown {
                hopCount += 1
                Sound.shared.animalCallThenName(animal)
            }
            .hop(trigger: hopCount)
    }
}
