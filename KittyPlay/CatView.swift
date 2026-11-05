import SwiftUI

/// The big cartoon cat, drawn entirely in code so it can blink.
/// All measurements are on a 100 x 100 grid, scaled by `u`.
struct CatView: View {
    var size: CGFloat = 300
    var eyesClosed = false
    var isHappy = false

    private let fur = Color(red: 1.0, green: 0.65, blue: 0.24)
    private let stripe = Color(red: 0.9, green: 0.45, blue: 0.1)
    private let cream = Color(red: 1.0, green: 0.92, blue: 0.78)
    private let pink = Color(red: 1.0, green: 0.55, blue: 0.65)
    private let ink = Color(red: 0.25, green: 0.15, blue: 0.1)
    private let sides: [CGFloat] = [-1, 1]

    private var u: CGFloat { size / 100 }

    var body: some View {
        ZStack {
            tail
            torso
            ForEach(sides, id: \.self) { side in
                ear(side: side)
            }
            head
            ForEach(sides, id: \.self) { side in
                eye(side: side)
                cheek(side: side)
                whiskers(side: side)
            }
            mouth
        }
        .frame(width: size, height: size)
    }

    private var tail: some View {
        Capsule()
            .fill(stripe)
            .frame(width: 13 * u, height: 50 * u)
            .rotationEffect(.degrees(28))
            .offset(x: 36 * u, y: 20 * u)
    }

    private var torso: some View {
        ZStack {
            Ellipse()
                .fill(fur)
                .frame(width: 58 * u, height: 54 * u)
                .offset(y: 23 * u)
            Ellipse()
                .fill(cream)
                .frame(width: 34 * u, height: 36 * u)
                .offset(y: 27 * u)
            ForEach(sides, id: \.self) { side in
                Capsule()
                    .fill(fur)
                    .frame(width: 18 * u, height: 12 * u)
                    .offset(x: side * 16 * u, y: 46 * u)
            }
        }
    }

    private func ear(side: CGFloat) -> some View {
        ZStack {
            TriangleShape()
                .fill(fur)
                .frame(width: 26 * u, height: 28 * u)
            TriangleShape()
                .fill(pink)
                .frame(width: 13 * u, height: 15 * u)
                .offset(y: 5 * u)
        }
        .rotationEffect(.degrees(Double(side) * 14))
        .offset(x: side * 25 * u, y: -38 * u)
    }

    private var head: some View {
        ZStack {
            Ellipse()
                .fill(fur)
                .frame(width: 74 * u, height: 60 * u)
            ForEach([-9, 0, 9] as [CGFloat], id: \.self) { x in
                Capsule()
                    .fill(stripe)
                    .frame(width: 4 * u, height: 11 * u)
                    .offset(x: x * u, y: -22 * u)
            }
        }
        .offset(y: -16 * u)
    }

    @ViewBuilder
    private func eye(side: CGFloat) -> some View {
        Group {
            if eyesClosed {
                CurveShape()
                    .stroke(ink, style: StrokeStyle(lineWidth: 2.6 * u, lineCap: .round))
                    .frame(width: 13 * u, height: 4 * u)
            } else {
                ZStack {
                    Circle()
                        .fill(ink)
                        .frame(width: 16 * u, height: 16 * u)
                    Circle()
                        .fill(Color.white)
                        .frame(width: 5.5 * u, height: 5.5 * u)
                        .offset(x: 3 * u, y: -3 * u)
                }
            }
        }
        .offset(x: side * 16 * u, y: -21 * u)
    }

    private func cheek(side: CGFloat) -> some View {
        Ellipse()
            .fill(pink.opacity(0.55))
            .frame(width: 11 * u, height: 7 * u)
            .offset(x: side * 25 * u, y: -9 * u)
    }

    private func whiskers(side: CGFloat) -> some View {
        ForEach([-5, 3] as [CGFloat], id: \.self) { height in
            Capsule()
                .fill(ink)
                .frame(width: 20 * u, height: 1.3 * u)
                .rotationEffect(.degrees(Double(side * height) * 1.6))
                .offset(x: side * 37 * u, y: (height - 8) * u)
        }
    }

    private var mouth: some View {
        ZStack {
            TriangleShape()
                .fill(pink)
                .frame(width: 7 * u, height: 5 * u)
                .rotationEffect(.degrees(180))
                .offset(y: -12 * u)
            if isHappy {
                Ellipse()
                    .fill(pink)
                    .frame(width: 8 * u, height: 8 * u)
                    .offset(y: -3 * u)
            }
            ForEach(sides, id: \.self) { side in
                CurveShape(isValley: true)
                    .stroke(ink, style: StrokeStyle(lineWidth: 1.8 * u, lineCap: .round))
                    .frame(width: 8 * u, height: 2.5 * u)
                    .offset(x: side * 4 * u, y: -8 * u)
            }
        }
    }
}
