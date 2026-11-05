import SwiftUI

/// The calm end of play time. No sparkles, no sounds on touch, nothing to re-excite.
/// It stays here until a grown-up opens the parent menu and taps Play Again.
struct SleepyCatView: View {
    @State private var isBreathingIn = false

    var body: some View {
        GeometryReader { geo in
            ZStack {
                LinearGradient(
                    colors: [Color(red: 0.1, green: 0.13, blue: 0.35), Color(red: 0.3, green: 0.25, blue: 0.5)],
                    startPoint: .top,
                    endPoint: .bottom
                )
                Text("🌙")
                    .font(.system(size: 80))
                    .position(x: geo.size.width * 0.78, y: geo.size.height * 0.14)
                Text("⭐️")
                    .font(.system(size: 26))
                    .position(x: geo.size.width * 0.2, y: geo.size.height * 0.16)
                Text("⭐️")
                    .font(.system(size: 18))
                    .position(x: geo.size.width * 0.42, y: geo.size.height * 0.26)
                CatView(size: min(geo.size.width, geo.size.height) * 0.8, eyesClosed: true)
                    .scaleEffect(isBreathingIn ? 1.03 : 0.98)
                    .position(x: geo.size.width / 2, y: geo.size.height * 0.56)
                Text("💤")
                    .font(.system(size: 60))
                    .opacity(isBreathingIn ? 1 : 0.35)
                    .offset(y: isBreathingIn ? -10 : 8)
                    .position(x: geo.size.width * 0.74, y: geo.size.height * 0.33)
            }
        }
        .ignoresSafeArea()
        .onAppear {
            withAnimation(.easeInOut(duration: 2.5).repeatForever(autoreverses: true)) {
                isBreathingIn = true
            }
            Sound.shared.stopAll()
            Sound.shared.sayAllDone()
        }
    }
}
