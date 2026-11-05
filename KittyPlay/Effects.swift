import SwiftUI

// MARK: - Sparkles

struct SparkleLayer: View {
    @Environment(GameState.self) private var state

    var body: some View {
        ZStack {
            ForEach(state.sparkles) { sparkle in
                SparkleView(sparkle: sparkle)
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .ignoresSafeArea()
        .allowsHitTesting(false)
    }
}

private struct SparkleView: View {
    let sparkle: Sparkle
    @State private var isFinished = false

    var body: some View {
        Text(sparkle.emoji)
            .font(.system(size: 44))
            .scaleEffect(isFinished ? 1.5 : 0.3)
            .opacity(isFinished ? 0 : 1)
            .offset(y: isFinished ? -50 : 0)
            .position(sparkle.point)
            .onAppear {
                withAnimation(.easeOut(duration: 0.7)) {
                    isFinished = true
                }
            }
    }
}

// MARK: - Touch and bounce helpers

/// Reacts the instant a finger lands, not when it lifts. Toddlers smear and
/// hold their fingers, and an ordinary tap would often be ignored.
private struct TouchDownModifier: ViewModifier {
    let action: () -> Void
    @GestureState private var isDown = false

    func body(content: Content) -> some View {
        content
            .contentShape(Rectangle())
            .gesture(
                DragGesture(minimumDistance: 0)
                    .updating($isDown) { _, down, _ in
                        down = true
                    }
            )
            .onChange(of: isDown) { _, newValue in
                if newValue {
                    action()
                }
            }
    }
}

/// A happy hop each time `trigger` changes.
private struct HopModifier: ViewModifier {
    let trigger: Int
    @State private var isUp = false

    func body(content: Content) -> some View {
        content
            .scaleEffect(isUp ? 1.2 : 1)
            .offset(y: isUp ? -24 : 0)
            .rotationEffect(.degrees(isUp ? -6 : 0))
            .animation(.spring(response: 0.25, dampingFraction: 0.45), value: isUp)
            .onChange(of: trigger) { _, _ in
                isUp = true
                Task {
                    try? await Task.sleep(for: .milliseconds(180))
                    isUp = false
                }
            }
    }
}

extension View {
    func onTouchDown(perform action: @escaping () -> Void) -> some View {
        modifier(TouchDownModifier(action: action))
    }

    func hop(trigger: Int) -> some View {
        modifier(HopModifier(trigger: trigger))
    }
}

// MARK: - Scenery

/// Bright sky and green grass, shared by the outdoor games.
struct MeadowBackground: View {
    var body: some View {
        GeometryReader { geo in
            ZStack {
                LinearGradient(
                    colors: [Color(red: 0.45, green: 0.82, blue: 1.0), Color(red: 0.8, green: 0.95, blue: 1.0)],
                    startPoint: .top,
                    endPoint: .bottom
                )
                Ellipse()
                    .fill(Color(red: 0.5, green: 0.85, blue: 0.35))
                    .frame(width: geo.size.width * 1.8, height: geo.size.height * 0.95)
                    .position(x: geo.size.width / 2, y: geo.size.height * 0.95)
                Text("☀️")
                    .font(.system(size: 70))
                    .position(x: geo.size.width * 0.82, y: geo.size.height * 0.1)
                Text("☁️")
                    .font(.system(size: 60))
                    .position(x: geo.size.width * 0.45, y: geo.size.height * 0.13)
            }
        }
    }
}

struct TriangleShape: Shape {
    func path(in rect: CGRect) -> Path {
        var path = Path()
        path.move(to: CGPoint(x: rect.midX, y: rect.minY))
        path.addLine(to: CGPoint(x: rect.maxX, y: rect.maxY))
        path.addLine(to: CGPoint(x: rect.minX, y: rect.maxY))
        path.closeSubpath()
        return path
    }
}

/// A single curved line, used for the cat's happy eyes and mouth.
struct CurveShape: Shape {
    /// false draws a hill, true draws a valley.
    var isValley = false

    func path(in rect: CGRect) -> Path {
        var path = Path()
        let endY = isValley ? rect.minY : rect.maxY
        let controlY = isValley ? rect.maxY + rect.height : rect.minY - rect.height
        path.move(to: CGPoint(x: rect.minX, y: endY))
        path.addQuadCurve(
            to: CGPoint(x: rect.maxX, y: endY),
            control: CGPoint(x: rect.midX, y: controlY)
        )
        return path
    }
}
