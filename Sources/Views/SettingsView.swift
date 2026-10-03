import SwiftUI
import SafariServices

enum Appearance: String, CaseIterable, Identifiable {
    case system, light, dark
    var id: String { rawValue }

    var title: String { t("appearance.\(rawValue)") }

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
    case language
}

/// Privacy policy ed EULA stanno nella repo pubblica (gli stessi URL vanno in App Store Connect):
/// l'app li apre, non se li porta dentro.
enum LegalDocument: String, Identifiable {
    case privacy = "PRIVACY_POLICY.md"
    case eula = "EULA.md"

    var id: String { rawValue }
    var url: URL { URL(string: "https://github.com/cldnpl/Receipty/blob/main/\(rawValue)")! }
}

/// Impostazioni: poche e utili. Aspetto, valuta, documenti legali.
struct SettingsView: View {
    @Environment(AppModel.self) private var app
    @AppStorage("appearance") private var appearance = Appearance.system
    @AppStorage(Currency.defaultsKey) private var currencyCode = Currency.deviceDefault.code
    @Binding var path: [SettingsRoute]
    let onReplayTutorial: () -> Void
    @State private var document: LegalDocument?

    private var currency: Currency { Currency(code: currencyCode) }
    private var language: AppLanguage { I18n.shared.language }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 0) {
                Text(t("settings.title"))
                    .textStyle(.hero)
                    .foregroundStyle(Palette.ink)
                    .padding(.leading, 6)
                    .padding(.top, 8)
                    .accessibilityAddTraits(.isHeader)

                SettingsSection(title: t("settings.appearance")) {
                    AppearancePicker(selection: $appearance)
                        .padding(8)
                }
                .padding(.top, 30)

                SettingsSection(title: t("settings.language"), footer: t("settings.language.footer")) {
                    Button { path.append(.language) } label: {
                        SettingsRow(title: language.nativeName, detail: language.localizedName) {
                            SettingsIcon(systemName: "globe")
                        }
                    }
                    .buttonStyle(PressableStyle(scale: 0.98))
                }
                .padding(.top, 22)

                SettingsSection(title: t("settings.currency"), footer: t("settings.currency.footer")) {
                    Button { path.append(.currency) } label: {
                        SettingsRow(title: currency.name, detail: currency.code) {
                            CurrencyBadge(currency: currency)
                        }
                    }
                    .buttonStyle(PressableStyle(scale: 0.98))
                }
                .padding(.top, 22)

                SettingsSection(title: t("settings.about")) {
                    VStack(spacing: 0) {
                        Button { document = .privacy } label: {
                            SettingsRow(title: t("settings.privacy"), external: true) { SettingsIcon(systemName: "hand.raised") }
                        }
                        RowDivider()
                        Button { document = .eula } label: {
                            SettingsRow(title: t("settings.eula"), external: true) { SettingsIcon(systemName: "doc.text") }
                        }
                        RowDivider()
                        Button(action: onReplayTutorial) {
                            SettingsRow(title: t("settings.replayTutorial"), chevron: false) { SettingsIcon(systemName: "sparkles") }
                        }
                        RowDivider()
                        SettingsRow(title: t("settings.version"), detail: AppInfo.version, chevron: false) {
                            SettingsIcon(systemName: "info")
                        }
                    }
                    .buttonStyle(PressableStyle(scale: 0.98))
                }
                .padding(.top, 22)

                Text(t("settings.footer"))
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
            case .language: LanguagePickerView()
            }
        }
        .sheet(item: $document) { doc in
            SafariView(url: doc.url)
                .ignoresSafeArea()
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
    var external = false
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
                Image(systemName: external ? "arrow.up.right" : "chevron.right")
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

    private var searching: Bool { !query.trimmingCharacters(in: .whitespaces).isEmpty }

    private var results: [Currency] {
        let q = query.trimmingCharacters(in: .whitespaces).lowercased()
        guard !q.isEmpty else { return Currency.all }
        return Currency.all.filter {
            $0.name.lowercased().contains(q) || $0.code.lowercased().contains(q) || $0.symbol.lowercased() == q
        }
    }

    /// In cima: quella scelta, quella del telefono e le più comuni in viaggio.
    private var popular: [Currency] {
        var codes = [selection, Currency.deviceDefault.code, "EUR", "USD", "GBP", "CHF", "JPY"]
        var seen = Set<String>()
        codes = codes.filter { seen.insert($0).inserted }
        return codes.map(Currency.init(code:))
    }

    var body: some View {
        VStack(spacing: 0) {
            FlowHeader(title: t("currency.title"), subtitle: t("currency.subtitle"))

            HStack(spacing: 10) {
                Image(systemName: "magnifyingglass")
                    .font(.system(size: 16, weight: .medium))
                    .foregroundStyle(Palette.placeholder)
                TextField(text: $query, prompt: Text(t("currency.search")).foregroundStyle(Palette.placeholder)) {
                    Text(t("common.search"))
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
                VStack(alignment: .leading, spacing: 0) {
                    if !searching {
                        sectionTitle(t("currency.popular"))
                        list(popular)
                        sectionTitle(t("currency.all"))
                            .padding(.top, 22)
                    }
                    list(results)
                }
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

    private func sectionTitle(_ text: String) -> some View {
        Text(text)
            .textStyle(.section)
            .foregroundStyle(Palette.raspberry)
            .padding(.leading, 8)
            .padding(.bottom, 10)
            .accessibilityAddTraits(.isHeader)
    }

    private func list(_ currencies: [Currency]) -> some View {
        LazyVStack(spacing: 0) {
            ForEach(currencies) { currency in
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
                if currency != currencies.last {
                    RowDivider()
                }
            }
        }
        .card(radius: 26)
    }
}

// MARK: - Lingua

/// La lingua dell'interfaccia. Ogni voce è scritta nella propria lingua, così si riconosce
/// anche arrivando qui per sbaglio con l'app in una lingua che non si capisce.
struct LanguagePickerView: View {
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        VStack(spacing: 0) {
            FlowHeader(title: t("language.title"), subtitle: t("language.subtitle"))

            ScrollView {
                LazyVStack(spacing: 0) {
                    ForEach(AppLanguage.allCases) { language in
                        let on = language == I18n.shared.language
                        Button {
                            Haptics.select()
                            I18n.shared.language = language
                            dismiss()
                        } label: {
                            HStack(spacing: 14) {
                                LanguageBadge(language: language)
                                VStack(alignment: .leading, spacing: 1) {
                                    Text(language.nativeName)
                                        .textStyle(.personName)
                                        .foregroundStyle(Palette.ink)
                                    if let subtitle = language.localizedName {
                                        Text(subtitle)
                                            .textStyle(TextStyle(face: .medium, size: 13))
                                            .foregroundStyle(Palette.gray)
                                    }
                                }
                                Spacer(minLength: 8)
                                if on {
                                    Image(systemName: "checkmark.circle.fill")
                                        .font(.system(size: 21))
                                        .foregroundStyle(Palette.pink)
                                }
                            }
                            .padding(.horizontal, 16)
                            .frame(minHeight: 62)
                            .contentShape(Rectangle())
                        }
                        .buttonStyle(PressableStyle(scale: 0.98))
                        .accessibilityAddTraits(on ? .isSelected : [])
                        if language != AppLanguage.allCases.last {
                            RowDivider()
                        }
                    }
                }
                .card(radius: 26)
                .padding(.horizontal, 20)
                .padding(.top, 18)
                .padding(.bottom, 30)
            }
            .scrollIndicators(.hidden)
            .plainScrollEdges()
            .topFade(14)
        }
        .background(BackdropView())
        .toolbar(.hidden, for: .navigationBar)
    }
}

/// "IT", "EN", "日本" — oppure il globo per la lingua dell'iPhone.
private struct LanguageBadge: View {
    let language: AppLanguage

    var body: some View {
        Group {
            if language == .system {
                Image(systemName: "globe").font(.system(size: 15, weight: .medium))
            } else {
                Text(language.rawValue.prefix(2).uppercased())
                    .font(.custom(Inter.bold.rawValue, size: 13))
            }
        }
        .foregroundStyle(Palette.raspberry)
        .frame(width: 36, height: 36)
        .background(Circle().fill(Palette.pinkSoft))
        .accessibilityHidden(true)
    }
}

// MARK: - Documenti legali

/// Safari dentro l'app, per privacy policy ed EULA pubblicate nella repo.
struct SafariView: UIViewControllerRepresentable {
    let url: URL

    func makeUIViewController(context: Context) -> SFSafariViewController {
        let vc = SFSafariViewController(url: url)
        vc.preferredControlTintColor = UIColor(Palette.pink)
        return vc
    }

    func updateUIViewController(_ vc: SFSafariViewController, context: Context) {}
}
