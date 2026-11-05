import SwiftUI

// MARK: - PIN pad

struct PinPad: View {
    let title: String
    var message: String = ""
    var onCancel: (() -> Void)?
    /// Called with the 4 digits once they are all entered.
    let onComplete: (String) -> Void

    @State private var digits = ""

    private let rows = [["1", "2", "3"], ["4", "5", "6"], ["7", "8", "9"]]

    var body: some View {
        VStack(spacing: 18) {
            Text(title)
                .font(.title2.bold())
                .multilineTextAlignment(.center)
            Text(message)
                .font(.subheadline)
                .foregroundStyle(.red)
            HStack(spacing: 14) {
                ForEach(0..<4, id: \.self) { index in
                    Circle()
                        .fill(index < digits.count ? Color.primary : Color.secondary.opacity(0.25))
                        .frame(width: 16, height: 16)
                }
            }
            ForEach(rows, id: \.self) { row in
                HStack(spacing: 18) {
                    ForEach(row, id: \.self) { digit in
                        digitButton(digit)
                    }
                }
            }
            HStack(spacing: 18) {
                Button("Cancel") {
                    onCancel?()
                }
                .frame(width: 72, height: 72)
                .opacity(onCancel == nil ? 0 : 1)
                .disabled(onCancel == nil)

                digitButton("0")

                Button {
                    if !digits.isEmpty {
                        digits.removeLast()
                    }
                } label: {
                    Image(systemName: "delete.left")
                        .font(.title2)
                        .frame(width: 72, height: 72)
                }
            }
        }
        .padding()
    }

    private func digitButton(_ digit: String) -> some View {
        Button {
            enter(digit)
        } label: {
            Text(digit)
                .font(.title)
                .frame(width: 72, height: 72)
                .background(Color.secondary.opacity(0.15), in: Circle())
        }
        .buttonStyle(.plain)
    }

    private func enter(_ digit: String) {
        guard digits.count < 4 else { return }
        digits.append(digit)
        if digits.count == 4 {
            let entered = digits
            digits = ""
            onComplete(entered)
        }
    }
}

// MARK: - Gate and menu

struct ParentGateView: View {
    @Environment(GameState.self) private var state
    @State private var isUnlocked = false
    @State private var message = ""

    var body: some View {
        ZStack {
            Color(.systemBackground)
                .ignoresSafeArea()
            if isUnlocked {
                ParentMenuView()
            } else {
                PinPad(
                    title: "Grown-ups only",
                    message: message,
                    onCancel: { state.isParentOpen = false },
                    onComplete: { entered in checkPin(entered) }
                )
            }
        }
    }

    private func checkPin(_ entered: String) {
        if entered == state.pin {
            isUnlocked = true
        } else {
            message = "That PIN didn't match."
        }
    }
}

struct ParentMenuView: View {
    @Environment(GameState.self) private var state

    private let limitChoices = [0, 5, 10, 15, 20, 30, 45, 60]

    var body: some View {
        @Bindable var state = state

        NavigationStack {
            Form {
                if state.isAllDone {
                    Section {
                        Button("Play Again") {
                            closeAndPlay()
                        }
                    } footer: {
                        Text("The sleepy cat screen stays until you tap this.")
                    }
                }

                Section("Sound") {
                    Toggle("Sound on", isOn: $state.soundOn)
                }

                Section("Mini-games") {
                    ForEach(MiniGame.allCases) { game in
                        gameToggle(for: game)
                    }
                }

                Section {
                    Picker("Play-time limit", selection: $state.limitMinutes) {
                        ForEach(limitChoices, id: \.self) { minutes in
                            Text(minutes == 0 ? "No limit" : "\(minutes) minutes").tag(minutes)
                        }
                    }
                    Button("Restart the play clock") {
                        state.secondsPlayed = 0
                    }
                } header: {
                    Text("Play time")
                } footer: {
                    Text("Played so far: \(state.secondsPlayed / 60) min. The clock restarts when the app is closed and reopened.")
                }

                Section("Locking the phone") {
                    NavigationLink("How to set up Guided Access") {
                        ScrollView {
                            GuidedAccessGuide()
                                .padding()
                        }
                        .navigationTitle("Guided Access")
                    }
                    NavigationLink("Change PIN") {
                        ChangePinView()
                    }
                }
            }
            .navigationTitle("Parent Menu")
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button("Done") {
                        state.isParentOpen = false
                    }
                }
            }
        }
    }

    private func gameToggle(for game: MiniGame) -> some View {
        let isOn = state.enabledGameIDs.contains(game.rawValue)
        let isOnlyGame = isOn && state.enabledGameIDs.count == 1
        return Toggle(game.parentLabel, isOn: Binding(
            get: { state.enabledGameIDs.contains(game.rawValue) },
            set: { newValue in
                if newValue {
                    state.enabledGameIDs.insert(game.rawValue)
                } else {
                    state.enabledGameIDs.remove(game.rawValue)
                }
                state.pageIndex = 0
            }
        ))
        // At least one game always stays on, so she never reaches an empty screen.
        .disabled(isOnlyGame)
    }

    private func closeAndPlay() {
        state.playAgain()
        state.isParentOpen = false
    }
}

struct ChangePinView: View {
    @Environment(GameState.self) private var state
    @Environment(\.dismiss) private var dismiss
    @State private var firstEntry: String?
    @State private var message = ""

    var body: some View {
        PinPad(
            title: firstEntry == nil ? "Enter a new PIN" : "Enter it once more",
            message: message,
            onComplete: { entered in handle(entered) }
        )
    }

    private func handle(_ entered: String) {
        guard let first = firstEntry else {
            firstEntry = entered
            message = ""
            return
        }
        if first == entered {
            state.pin = entered
            dismiss()
        } else {
            firstEntry = nil
            message = "Those didn't match. Try again."
        }
    }
}

// MARK: - First launch

struct OnboardingView: View {
    @Environment(GameState.self) private var state
    @State private var firstEntry: String?
    @State private var chosenPin: String?
    @State private var message = ""

    var body: some View {
        if let chosenPin {
            VStack(spacing: 0) {
                ScrollView {
                    GuidedAccessGuide()
                        .padding()
                }
                Button {
                    state.pin = chosenPin
                    state.hasOnboarded = true
                } label: {
                    Text("Got it, start playing")
                        .font(.headline)
                        .frame(maxWidth: .infinity)
                        .padding(8)
                }
                .buttonStyle(.borderedProminent)
                .padding()
            }
        } else {
            VStack(spacing: 8) {
                Text("Hold the top-left corner for 3 seconds during play to open the parent menu. It asks for this PIN.")
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal)
                PinPad(
                    title: firstEntry == nil ? "Choose a parent PIN" : "Enter it once more",
                    message: message,
                    onComplete: { entered in handle(entered) }
                )
            }
        }
    }

    private func handle(_ entered: String) {
        guard let first = firstEntry else {
            firstEntry = entered
            message = ""
            return
        }
        if first == entered {
            chosenPin = entered
        } else {
            firstEntry = nil
            message = "Those didn't match. Try again."
        }
    }
}

struct GuidedAccessGuide: View {
    private let steps = [
        "Open the Settings app, then Accessibility, then Guided Access, and turn it on.",
        "Tap Passcode Settings, then Set Guided Access Passcode. Pick a code she doesn't know, and don't forget it.",
        "In the same Guided Access screen, set Display Auto-Lock to Never if your iPhone offers it.",
        "Come back to this game. Triple-click the side button (the Home button on older iPhones), then tap Start.",
        "To finish, triple-click the side button, enter the passcode, and tap End."
    ]

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text("Lock the phone to this game")
                .font(.title2.bold())
            Text("Apps can't lock an iPhone by themselves, but Guided Access can. While it's on, she can't leave the game.")
            ForEach(Array(steps.enumerated()), id: \.offset) { index, step in
                HStack(alignment: .top, spacing: 10) {
                    Text("\(index + 1).")
                        .bold()
                    Text(step)
                }
            }
            Text("Tip: turn on Airplane Mode first, so calls and messages can't interrupt.")
                .foregroundStyle(.secondary)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}
