import SwiftUI

/// Le tre pagine della prima apertura: cosa fa l'app, come si assegnano le voci, il resto.
struct OnboardingView: View {
    let onFinish: () -> Void
    @State private var page = 0

    private var pages: [(title: String, text: String)] {
        (1...3).map { (t("onboarding.\($0).title"), t("onboarding.\($0).text")) }
    }

    var body: some View {
        VStack(spacing: 0) {
            HStack {
                Spacer()
                Button(t("onboarding.skip"), action: onFinish)
                    .textStyle(TextStyle(face: .semibold, size: 16))
                    .foregroundStyle(Palette.raspberryDeep)
                    .opacity(page == pages.count - 1 ? 0 : 1)
                    .animation(.easeOut(duration: 0.2), value: page)
            }
            .padding(.horizontal, 26)
            .frame(height: 44)

            TabView(selection: $page) {
                ForEach(pages.indices, id: \.self) { i in
                    VStack(spacing: 0) {
                        Illustration(page: i)
                            .frame(height: 330)
                            .padding(.top, 12)
                        Spacer(minLength: 16)
                        VStack(alignment: .leading, spacing: 12) {
                            TightTitle(text: pages[i].title, style: TextStyle(face: .extraBold, size: 40, tracking: -0.03, lineHeight: 43))
                            Text(pages[i].text)
                                .textStyle(.lead)
                                .foregroundStyle(Palette.inkSoft)
                                .fixedSize(horizontal: false, vertical: true)
                        }
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(.horizontal, 28)
                        Spacer(minLength: 0)
                    }
                    .tag(i)
                }
            }
            .tabViewStyle(.page(indexDisplayMode: .never))

            HStack(spacing: 7) {
                ForEach(pages.indices, id: \.self) { i in
                    Capsule()
                        .fill(i == page ? Palette.pink : Color.white)
                        .frame(width: i == page ? 24 : 8, height: 8)
                }
            }
            .animation(.spring(response: 0.35, dampingFraction: 0.8), value: page)
            .padding(.bottom, 22)

            Button {
                if page < pages.count - 1 {
                    withAnimation(.snappy) { page += 1 }
                } else {
                    Haptics.success()
                    onFinish()
                }
            } label: {
                Text(t(page < pages.count - 1 ? "common.next" : "onboarding.start"))
                    .contentTransition(.opacity)
            }
            .buttonStyle(PrimaryButtonStyle())
            .padding(.horizontal, 24)

            Text(t("onboarding.footer"))
                .textStyle(TextStyle(face: .medium, size: 13))
                .foregroundStyle(Palette.gray)
                .padding(.top, 14)
                .padding(.bottom, 6)
        }
        .background(BackdropView())
    }
}

/// Illustrazioni fatte con i pezzi veri dell'app, così l'onboarding somiglia a quello che si userà.
private struct Illustration: View {
    let page: Int
    @State private var shown = false
    private var currency: Currency { .current }

    private func money(_ cents: Int) -> String { Money.format(cents, currency) }

    var body: some View {
        ZStack {
            switch page {
            case 0: transfers
            case 1: assignment
            default: change
            }
        }
        .padding(.horizontal, 28)
        .onAppear { withAnimation(.spring(response: 0.6, dampingFraction: 0.8).delay(0.1)) { shown = true } }
        .onDisappear { shown = false }
    }

    private var transfers: some View {
        VStack(spacing: 12) {
            MiniCard(icon: .person("G"), title: "Giulia → Cla", subtitle: t("result.pays", "Giulia", "Cla"), amount: money(1025))
                .rotationEffect(.degrees(shown ? -3 : 0))
                .offset(x: shown ? -8 : 0)
            MiniCard(icon: .person("M"), title: "Marco → Cla", subtitle: t("result.pays", "Marco", "Cla"), amount: money(375))
                .rotationEffect(.degrees(shown ? 2 : 0))
                .offset(x: shown ? 10 : 0)
            MiniCard(icon: .person("L"), title: "Luca → Cla", subtitle: t("result.pays", "Luca", "Cla"), amount: money(375))
                .rotationEffect(.degrees(shown ? -1.5 : 0))
        }
        .overlay(alignment: .topTrailing) {
            Image(systemName: "checkmark")
                .font(.system(size: 22, weight: .bold))
                .foregroundStyle(.white)
                .frame(width: 56, height: 56)
                .background(Circle().fill(Palette.success).shadow(color: Palette.success.opacity(0.35), radius: 14, y: 6))
                .offset(x: 10, y: -34)
                .scaleEffect(shown ? 1 : 0.4)
                .opacity(shown ? 1 : 0)
        }
    }

    private var assignment: some View {
        VStack(alignment: .leading, spacing: 14) {
            HStack(alignment: .firstTextBaseline) {
                Text(t("onboarding.sample.item")).textStyle(.itemTitle).foregroundStyle(Palette.ink)
                Spacer()
                VStack(alignment: .trailing, spacing: 3) {
                    Text(money(800)).textStyle(.itemTitle).foregroundStyle(Palette.ink)
                    Text(t("common.each", money(400))).textStyle(.perHead).foregroundStyle(Palette.pinkText)
                        .opacity(shown ? 1 : 0)
                }
            }
            FlowLayout(spacing: 8, lineSpacing: 9) {
                chip("C", "Cla", on: true)
                chip("M", "Marco", on: shown)
                chip("G", "Giulia", on: false)
                chip("L", "Luca", on: false)
            }
        }
        .padding(18)
        .card()
        .overlay(alignment: .bottomTrailing) {
            Image(systemName: "hand.tap.fill")
                .font(.system(size: 34))
                .foregroundStyle(Palette.pink)
                .shadow(color: .black.opacity(0.15), radius: 6, y: 3)
                .offset(x: shown ? -150 : -40, y: shown ? -20 : 40)
                .opacity(shown ? 1 : 0)
        }
    }

    private var change: some View {
        VStack(spacing: 12) {
            MiniCard(icon: .symbol("banknote"), title: "\(t("result.change")) → Claudia",
                     subtitle: t("result.keepsIt", "Claudia"), amount: money(700))
                .offset(y: shown ? 0 : 20)
            MiniCard(icon: .symbol("banknote"), title: "\(t("result.change")) → Marco",
                     subtitle: t("result.keepsIt", "Marco"), amount: money(100))
                .offset(y: shown ? 0 : 30)
            MiniCard(icon: .person("O"), title: "Olga → Marco", subtitle: t("result.pays", "Olga", "Marco"), amount: money(1300))
                .offset(y: shown ? 0 : 40)
        }
        .opacity(shown ? 1 : 0.4)
    }

    private func chip(_ initial: String, _ name: String, on: Bool) -> some View {
        HStack(spacing: 8) {
            Text(initial)
                .font(.custom(Inter.semibold.rawValue, size: 10.5))
                .foregroundStyle(on ? Color.white : Palette.raspberry)
                .frame(width: 22, height: 22)
                .background(Circle().fill(on ? Palette.pinkLight : Palette.pinkSoft))
            Text(name).textStyle(.chip).foregroundStyle(on ? Color.white : Palette.ink)
        }
        .padding(.leading, 6)
        .padding(.trailing, 13)
        .frame(height: 33.5)
        .background(Capsule().fill(on ? Palette.pink : Palette.pinkMist))
    }
}

private struct MiniCard: View {
    enum Icon {
        case person(String)
        case symbol(String)
    }

    let icon: Icon
    let title: String
    let subtitle: String
    let amount: String

    var body: some View {
        HStack(spacing: 11) {
            Group {
                switch icon {
                case .person(let initial):
                    Text(initial).font(.custom(Inter.semibold.rawValue, size: 17.5)).foregroundStyle(Palette.raspberry)
                case .symbol(let name):
                    Image(systemName: name).font(.system(size: 15, weight: .medium)).foregroundStyle(Palette.pinkIcon)
                }
            }
            .frame(width: 38, height: 38)
            .background(Circle().fill(Palette.pinkSoft))
            VStack(alignment: .leading, spacing: 0) {
                Text(title).textStyle(.transferName).foregroundStyle(Palette.ink)
                Text(subtitle).textStyle(.transferSub).foregroundStyle(Palette.gray)
            }
            .lineLimit(1)
            .minimumScaleFactor(0.8)
            Spacer(minLength: 8)
            Text(amount).textStyle(.transferAmount).foregroundStyle(Palette.ink).fixedSize()
        }
        .padding(.horizontal, 18)
        .frame(height: 72)
        .card(radius: 24)
        .accessibilityHidden(true)
    }
}
