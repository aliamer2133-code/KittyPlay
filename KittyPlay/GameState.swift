import SwiftUI
import Observation

enum MiniGame: String, CaseIterable, Identifiable {
    case petCat, animalSounds, feed, peekaboo

    var id: String { rawValue }

    /// Shown only in the parent menu. The child never sees text.
    var parentLabel: String {
        switch self {
        case .petCat: return "Pet the Cat"
        case .animalSounds: return "Animal Sounds"
        case .feed: return "Feed the Animals"
        case .peekaboo: return "Peekaboo"
        }
    }
}

struct Sparkle: Identifiable {
    let id = UUID()
    let point: CGPoint
    let emoji: String
}

/// All of the app's shared state. Parent settings are saved between launches.
@Observable
@MainActor
final class GameState {
    // MARK: Saved settings

    var hasOnboarded: Bool {
        didSet { UserDefaults.standard.set(hasOnboarded, forKey: "hasOnboarded") }
    }
    var pin: String {
        didSet { UserDefaults.standard.set(pin, forKey: "pin") }
    }
    var soundOn: Bool {
        didSet {
            UserDefaults.standard.set(soundOn, forKey: "soundOn")
            Sound.shared.isEnabled = soundOn
        }
    }
    var enabledGameIDs: Set<String> {
        didSet { UserDefaults.standard.set(Array(enabledGameIDs), forKey: "enabledGames") }
    }
    /// Play-time limit in minutes. 0 means no limit.
    var limitMinutes: Int {
        didSet { UserDefaults.standard.set(limitMinutes, forKey: "limitMinutes") }
    }
    /// Saved so that force-quitting the app doesn't skip the "all done" screen.
    var isAllDone: Bool {
        didSet { UserDefaults.standard.set(isAllDone, forKey: "isAllDone") }
    }

    // MARK: Live state

    var isParentOpen = false
    var isAppActive = true
    var pageIndex = 0
    var slideForward = true
    var secondsPlayed = 0
    var sparkles: [Sparkle] = []
    var isBouncing = false
    /// Set by a game when it owns the current finger (stroking the cat, dragging
    /// food), so that movement doesn't also count as a page swipe.
    var touchClaimed = false

    private static let sparkleEmoji = ["✨", "⭐️", "💖", "🌟", "🐾"]

    init() {
        let defaults = UserDefaults.standard
        hasOnboarded = defaults.bool(forKey: "hasOnboarded")
        pin = defaults.string(forKey: "pin") ?? ""
        soundOn = (defaults.object(forKey: "soundOn") as? Bool) ?? true
        let savedGames = defaults.stringArray(forKey: "enabledGames")
        enabledGameIDs = Set(savedGames ?? MiniGame.allCases.map { $0.rawValue })
        limitMinutes = (defaults.object(forKey: "limitMinutes") as? Int) ?? 15
        isAllDone = defaults.bool(forKey: "isAllDone")
        Sound.shared.isEnabled = soundOn
    }

    // MARK: Pages

    var enabledGames: [MiniGame] {
        let games = MiniGame.allCases.filter { enabledGameIDs.contains($0.rawValue) }
        return games.isEmpty ? [.petCat] : games
    }

    var currentGame: MiniGame {
        let games = enabledGames
        return games[pageIndex % games.count]
    }

    /// Pages wrap around, so she can always keep swiping.
    func changePage(forward: Bool) {
        let count = enabledGames.count
        guard count > 1 else { return }
        Sound.shared.stopAll()
        slideForward = forward
        let step = forward ? 1 : -1
        withAnimation(.easeInOut(duration: 0.35)) {
            pageIndex = (pageIndex % count + step + count) % count
        }
    }

    // MARK: Happy reactions

    /// Runs for every touch anywhere on screen.
    func tapReaction(at point: CGPoint) {
        addSparkle(at: point)
        Sound.shared.pop()
        isBouncing = true
        Task {
            try? await Task.sleep(for: .milliseconds(120))
            self.isBouncing = false
        }
    }

    func addSparkle(at point: CGPoint, emoji: String? = nil) {
        if sparkles.count >= 24 {
            sparkles.removeFirst()
        }
        let chosen = emoji ?? Self.sparkleEmoji.randomElement() ?? "✨"
        let sparkle = Sparkle(point: point, emoji: chosen)
        sparkles.append(sparkle)
        Task {
            try? await Task.sleep(for: .milliseconds(800))
            self.sparkles.removeAll { $0.id == sparkle.id }
        }
    }

    func burst(at point: CGPoint, emoji: String = "💖", count: Int = 8) {
        for _ in 0..<count {
            let spot = CGPoint(
                x: point.x + CGFloat.random(in: -90...90),
                y: point.y + CGFloat.random(in: -90...90)
            )
            addSparkle(at: spot, emoji: emoji)
        }
    }

    // MARK: Play-time limit

    /// Called once per second.
    func tick() {
        guard isAppActive, hasOnboarded, !isParentOpen, !isAllDone, limitMinutes > 0 else { return }
        secondsPlayed += 1
        if secondsPlayed >= limitMinutes * 60 {
            withAnimation(.easeInOut(duration: 1.5)) {
                isAllDone = true
            }
        }
    }

    func playAgain() {
        secondsPlayed = 0
        isAllDone = false
    }
}
