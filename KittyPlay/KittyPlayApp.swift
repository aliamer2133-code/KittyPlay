import SwiftUI

@main
struct KittyPlayApp: App {
    @State private var state = GameState()

    var body: some Scene {
        WindowGroup {
            ContentView()
                .environment(state)
        }
    }
}
