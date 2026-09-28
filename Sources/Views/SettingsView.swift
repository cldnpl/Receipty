import SwiftUI

enum Appearance: String, CaseIterable, Identifiable {
    case system, light, dark
    var id: String { rawValue }

    var title: String {
        switch self {
        case .system: "System"
        case .light: "Light"
        case .dark: "Dark"
        }
    }

    var colorScheme: ColorScheme? {
        switch self {
        case .system: nil
        case .light: .light
        case .dark: .dark
        }
    }
}

enum SettingsRoute: Hashable {
    case currency
    case legal(LegalDocument)
}

enum LegalDocument: String, Hashable {
    case privacy = "privacy-policy"
    case terms = "terms-of-use"
}

/// Impostazioni: poche e utili. Aspetto, valuta, documenti legali.
struct SettingsView: View {
    @Environment(AppModel.self) private var app
    @AppStorage("appearance") private var appearance = Appearance.system
    @AppStorage(Currency.defaultsKey) private var currencyCode = Currency.deviceDefault.code
    @Binding var path: [SettingsRoute]
    let onReplayTutorial: () -> Void

    private var currency: Currency { Currency(code: currencyCode) }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 0) {
                Text("Settings")
                    .textStyle(.hero)
                    .foregroundStyle(Palette.ink)
                    .padding(.leading, 6)
                    .padding(.top, 8)
                    .accessibilityAddTraits(.isHeader)

                SettingsSection(title: "Appearance") {
                    AppearancePicker(selection: $appearance)
                        .padding(8)
                }
                .padding(.top, 30)

                SettingsSection(title: "Currency",
                                footer: "Used for new bills. Bills you already split keep their own currency.") {
                    Button { path.append(.currency) } label: {
                        SettingsRow(title: currency.name, detail: currency.code) {
                            CurrencyBadge(currency: currency)
                        }
                    }
                    .buttonStyle(PressableStyle(scale: 0.98))
                }
                .padding(.top, 22)

                SettingsSection(title: "About") {
                    VStack(spacing: 0) {
                        Button { path.append(.legal(.privacy)) } label: {
                            SettingsRow(title: "Privacy Policy") { SettingsIcon(systemName: "hand.raised") }
                        }
                        RowDivider()
                        Button { path.append(.legal(.terms)) } label: {
                            SettingsRow(title: "Terms of Use (EULA)") { SettingsIcon(systemName: "doc.text") }
                        }
                        RowDivider()
                        Button(action: onReplayTutorial) {
                            SettingsRow(title: "Replay tutorial", chevron: false) { SettingsIcon(systemName: "sparkles") }
                        }
                        RowDivider()
                        SettingsRow(title: "Version", detail: AppInfo.version, chevron: false) {
                            SettingsIcon(systemName: "info")
                        }
                    }
                    .buttonStyle(PressableStyle(scale: 0.98))
                }
                .padding(.top, 22)

                Text("No account, no ads, no tracking.\nYour bills stay on this iPhone.")
                    .textStyle(TextStyle(face: .medium, size: 13.5, lineHeight: 19))
                    .foregroundStyle(Palette.gray)
                    .multilineTextAlignment(.center)
                    .frame(maxWidth: .infinity)
                    .padding(.top, 26)
            }
            .padding(.horizontal, 22)
            .padding(.bottom, 30)
        }
        .scrollIndicators(.hidden)
        .plainScrollEdges()
        .topFade(10)
        .background(BackdropView())
        .toolbar(.hidden, for: .navigationBar)
        .navigationDestination(for: SettingsRoute.self) { route in
            switch route {
            case .currency: CurrencyPickerView(selection: $currencyCode)
            case .legal(let doc): LegalView(document: doc)
            }
        }
    }
}

enum AppInfo {
    static var version: String {
        let v = Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "1.0"
        let b = Bundle.main.infoDictionary?["CFBundleVersion"] as? String ?? "1"
        return "\(v) (\(b))"
    }
}

// MARK: - Pezzi

private struct SettingsSection<Content: View>: View {
    let title: String
    var footer: String?
    @ViewBuilder var content: Content

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            Text(title)
                .textStyle(.section)
                .foregroundStyle(Palette.raspberry)
                .padding(.leading, 8)
                .padding(.bottom, 10)
                .accessibilityAddTraits(.isHeader)
            content
                .frame(maxWidth: .infinity)
                .card(radius: 26)
            if let footer {
                Text(footer)
                    .textStyle(TextStyle(face: .regular, size: 13, lineHeight: 18))
                    .foregroundStyle(Palette.gray)
                    .fixedSize(horizontal: false, vertical: true)
                    .padding(.horizontal, 8)
                    .padding(.top, 9)
            }
        }
    }
}

private struct SettingsRow<Icon: View>: View {
    let title: String
    var detail: String?
    var chevron = true
    @ViewBuilder var icon: Icon

    var body: some View {
        HStack(spacing: 14) {
            icon
            Text(title)
                .textStyle(.personName)
                .foregroundStyle(Palette.ink)
            Spacer(minLength: 8)
            if let detail {
                Text(detail)
                    .textStyle(TextStyle(face: .medium, size: 15))
                    .foregroundStyle(Palette.gray)
            }
            if chevron {
                Image(systemName: "chevron.right")
                    .font(.system(size: 13, weight: .semibold))
                    .foregroundStyle(Palette.muted)
            }
        }
        .padding(.horizontal, 16)
        .frame(minHeight: 62)
        .contentShape(Rectangle())
    }
}

private struct SettingsIcon: View {
    let systemName: String

    var body: some View {
        Image(systemName: systemName)
            .font(.system(size: 15, weight: .medium))
            .foregroundStyle(Palette.pinkIcon)
            .frame(width: 36, height: 36)
            .background(Circle().fill(Palette.pinkSoft))
    }
}

private struct RowDivider: View {
    var body: some View {
        Rectangle().fill(Palette.hairline).frame(height: 1).padding(.leading, 66)
    }
}

struct CurrencyBadge: View {
    let currency: Currency
    var size: CGFloat = 36

    var body: some View {
        Text(currency.symbol)
            .font(.custom(Inter.bold.rawValue, size: currency.symbol.count > 2 ? 11 : 15))
            .minimumScaleFactor(0.6)
            .lineLimit(1)
            .foregroundStyle(Palette.raspberry)
            .padding(.horizontal, 3)
            .frame(width: size, height: size)
            .background(Circle().fill(Palette.pinkSoft))
    }
}

/// Sistema / Chiaro / Scuro, a pillole come le chip dell'app.
private struct AppearancePicker: View {
    @Binding var selection: Appearance
    @Namespace private var pill

    var body: some View {
        HStack(spacing: 4) {
            ForEach(Appearance.allCases) { option in
                let on = option == selection
                Button {
                    Haptics.select()
                    withAnimation(.spring(response: 0.35, dampingFraction: 0.8)) { selection = option }
                } label: {
                    HStack(spacing: 7) {
                        Image(systemName: icon(option)).font(.system(size: 14, weight: .medium))
                        Text(option.title).textStyle(TextStyle(face: .semibold, size: 15))
                    }
                    .foregroundStyle(on ? Color.white : Palette.ink)
                    .frame(maxWidth: .infinity)
                    .frame(height: 44)
                    .background {
                        if on { Capsule().fill(Palette.pink).matchedGeometryEffect(id: "pill", in: pill) }
                    }
                    .contentShape(Capsule())
                }
                .buttonStyle(.plain)
                .accessibilityAddTraits(on ? .isSelected : [])
            }
        }
        .padding(4)
        .background(Capsule().fill(Palette.pinkMist))
    }

    private func icon(_ a: Appearance) -> String {
        switch a {
        case .system: "circle.lefthalf.filled"
        case .light: "sun.max"
        case .dark: "moon"
        }
    }
}

// MARK: - Valuta

struct CurrencyPickerView: View {
    @Environment(\.dismiss) private var dismiss
    @Binding var selection: String
    @State private var query = ""

    private var results: [Currency] {
        let q = query.trimmingCharacters(in: .whitespaces).lowercased()
        guard !q.isEmpty else { return Currency.all }
        return Currency.all.filter {
            $0.name.lowercased().contains(q) || $0.code.lowercased().contains(q) || $0.symbol.lowercased() == q
        }
    }

    var body: some View {
        VStack(spacing: 0) {
            FlowHeader(title: "CURRENCY", subtitle: "Pick the currency on your receipts.")

            HStack(spacing: 10) {
                Image(systemName: "magnifyingglass")
                    .font(.system(size: 16, weight: .medium))
                    .foregroundStyle(Palette.placeholder)
                TextField(text: $query, prompt: Text("Search currency or code").foregroundStyle(Palette.placeholder)) {
                    Text("Search")
                }
                .textStyle(TextStyle(face: .regular, size: 17))
                .foregroundStyle(Palette.ink)
                .autocorrectionDisabled()
                .textInputAutocapitalization(.never)
            }
            .padding(.horizontal, 20)
            .frame(height: 54)
            .background(Capsule().fill(Palette.card).shadow(color: Palette.shadow.opacity(0.07), radius: 16, y: 7))
            .padding(.horizontal, 20)
            .padding(.top, 18)

            ScrollView {
                LazyVStack(spacing: 0) {
                    ForEach(results) { currency in
                        Button {
                            Haptics.select()
                            selection = currency.code
                            dismiss()
                        } label: {
                            HStack(spacing: 14) {
                                CurrencyBadge(currency: currency)
                                VStack(alignment: .leading, spacing: 1) {
                                    Text(currency.name)
                                        .textStyle(.personName)
                                        .foregroundStyle(Palette.ink)
                                    Text(currency.code)
                                        .textStyle(TextStyle(face: .medium, size: 13))
                                        .foregroundStyle(Palette.gray)
                                }
                                Spacer()
                                if currency.code == selection {
                                    Image(systemName: "checkmark.circle.fill")
                                        .font(.system(size: 21))
                                        .foregroundStyle(Palette.pink)
                                }
                            }
                            .padding(.horizontal, 16)
                            .frame(height: 62)
                            .contentShape(Rectangle())
                        }
                        .buttonStyle(PressableStyle(scale: 0.98))
                        .accessibilityAddTraits(currency.code == selection ? .isSelected : [])
                        if currency != results.last {
                            RowDivider()
                        }
                    }
                }
                .card(radius: 26)
                .padding(.horizontal, 20)
                .padding(.top, 16)
                .padding(.bottom, 30)
            }
            .scrollIndicators(.hidden)
            .plainScrollEdges()
            .scrollDismissesKeyboard(.interactively)
            .topFade(14)
        }
        .background(BackdropView())
        .toolbar(.hidden, for: .navigationBar)
    }
}

// MARK: - Documenti legali

/// Mostra i Markdown in Resources/Legal: gli stessi file si pubblicano per l'App Store.
struct LegalView: View {
    let document: LegalDocument

    private var blocks: [LegalBlock] { LegalBlock.load(document) }

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            BackButton()
                .padding(.top, 4)
                .padding(.leading, 24)

            ScrollView {
                VStack(alignment: .leading, spacing: 0) {
                    ForEach(Array(blocks.enumerated()), id: \.offset) { _, block in
                        block.view
                    }
                    if document == .terms {
                        Link(destination: URL(string: "https://www.apple.com/legal/internet-services/itunes/dev/stdeula/")!) {
                            HStack(spacing: 8) {
                                Text("Read Apple's Standard EULA")
                                Image(systemName: "arrow.up.right").font(.system(size: 13, weight: .semibold))
                            }
                            .textStyle(.link)
                            .foregroundStyle(Palette.raspberryDeep)
                        }
                        .padding(.top, 24)
                    }
                }
                .padding(.horizontal, 24)
                .padding(.top, 14)
                .padding(.bottom, 40)
            }
            .scrollIndicators(.hidden)
            .plainScrollEdges()
            .topFade()
        }
        .background(BackdropView())
        .toolbar(.hidden, for: .navigationBar)
    }
}

private enum LegalBlock {
    case title(String)
    case heading(String)
    case paragraph(AttributedString)
    case bullet(AttributedString)

    static func load(_ doc: LegalDocument) -> [LegalBlock] {
        guard let url = Bundle.main.url(forResource: doc.rawValue, withExtension: "md"),
              let text = try? String(contentsOf: url, encoding: .utf8) else { return [] }
        func inline(_ s: String) -> AttributedString {
            (try? AttributedString(markdown: s)) ?? AttributedString(s)
        }
        return text.components(separatedBy: "\n").compactMap { raw in
            let line = raw.trimmingCharacters(in: .whitespaces)
            if line.isEmpty { return nil }
            if line.hasPrefix("## ") { return .heading(String(line.dropFirst(3))) }
            if line.hasPrefix("# ") { return .title(String(line.dropFirst(2))) }
            if line.hasPrefix("- ") { return .bullet(inline(String(line.dropFirst(2)))) }
            return .paragraph(inline(line))
        }
    }

    @ViewBuilder
    var view: some View {
        switch self {
        case .title(let t):
            Text(t)
                .textStyle(TextStyle(face: .extraBold, size: 34, tracking: -0.02))
                .foregroundStyle(Palette.ink)
                .fixedSize(horizontal: false, vertical: true)
                .padding(.top, 10)
                .accessibilityAddTraits(.isHeader)
        case .heading(let t):
            Text(t)
                .textStyle(TextStyle(face: .bold, size: 18.5, tracking: -0.01))
                .foregroundStyle(Palette.ink)
                .padding(.top, 24)
                .accessibilityAddTraits(.isHeader)
        case .paragraph(let t):
            Text(t)
                .textStyle(TextStyle(face: .regular, size: 16, lineHeight: 23.5))
                .foregroundStyle(Palette.inkSoft)
                .fixedSize(horizontal: false, vertical: true)
                .padding(.top, 10)
        case .bullet(let t):
            HStack(alignment: .firstTextBaseline, spacing: 10) {
                Circle().fill(Palette.pink).frame(width: 6, height: 6).alignmentGuide(.firstTextBaseline) { $0[.bottom] + 2 }
                Text(t)
                    .textStyle(TextStyle(face: .regular, size: 16, lineHeight: 23.5))
                    .foregroundStyle(Palette.inkSoft)
                    .fixedSize(horizontal: false, vertical: true)
            }
            .padding(.top, 9)
        }
    }
}
