import SwiftUI
import UIKit

/// Everything spoken aloud lives here, so changing language is a one-file job.
/// Example: use "ar-SA" with Arabic words, or "es-ES" with Spanish words.
enum Words {
    static let voiceLanguage = "en-US"
    static let allDone = "Sleepy cat. All done."
    static let yum = "Yum yum!"
}

struct Animal: Identifiable, Hashable {
    /// Also the name of the picture and sound files: cat.png, sound_cat.mp3, name_cat.m4a
    let id: String
    let emoji: String
    let name: String
    let call: String
    let pitch: Float

    static let cat = Animal(id: "cat", emoji: "🐱", name: "Cat", call: "Meow!", pitch: 1.6)
    static let dog = Animal(id: "dog", emoji: "🐶", name: "Dog", call: "Woof woof!", pitch: 1.1)
    static let cow = Animal(id: "cow", emoji: "🐮", name: "Cow", call: "Moooo!", pitch: 0.7)
    static let duck = Animal(id: "duck", emoji: "🦆", name: "Duck", call: "Quack quack!", pitch: 1.5)
    static let lion = Animal(id: "lion", emoji: "🦁", name: "Lion", call: "Roar!", pitch: 0.6)
    static let sheep = Animal(id: "sheep", emoji: "🐑", name: "Sheep", call: "Baa baa!", pitch: 1.3)
    static let bunny = Animal(id: "bunny", emoji: "🐰", name: "Bunny", call: "Hop hop!", pitch: 1.7)
    static let pig = Animal(id: "pig", emoji: "🐷", name: "Pig", call: "Oink oink!", pitch: 1.2)
    static let frog = Animal(id: "frog", emoji: "🐸", name: "Frog", call: "Ribbit!", pitch: 0.9)

    static let peekabooCast: [Animal] = [.cat, .dog, .duck, .bunny, .pig, .frog, .sheep, .lion]
}

/// Draws an animal: your own picture if you added one, otherwise the emoji.
struct AnimalPicture: View {
    let animal: Animal
    let size: CGFloat

    var body: some View {
        if let image = UIImage(named: animal.id) {
            Image(uiImage: image)
                .resizable()
                .scaledToFit()
                .frame(width: size, height: size)
        } else {
            Text(animal.emoji)
                .font(.system(size: size * 0.8))
                .frame(width: size, height: size)
        }
    }
}
