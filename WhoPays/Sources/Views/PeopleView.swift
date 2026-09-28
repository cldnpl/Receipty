import SwiftUI

/// "Chi c'è al tavolo?": nomi scritti una volta, poi basta toccarli.
struct PeopleView: View {
    @Environment(AppModel.self) private var app
    @Bindable var draft: BillDraft
    @State private var name = ""
    @FocusState private var typing: Bool

    private var recents: [String] {
        app.store.recentNames(excluding: draft.people.map(\.name))
    }

    var body: some View {
        VStack(spacing: 0) {
            FlowHeader(title: "CHI C'È AL TAVOLO?", subtitle: "Aggiungi tutti, anche chi non ha pagato.") {
                if draft.source == .equal { app.finish() } else { app.path.removeLast() }
            }

            ScrollView {
                VStack(alignment: .leading, spacing: 0) {
                    nameField

                    VStack(spacing: 8) {
                        ForEach(draft.people) { person in
                            PersonRow(person: person) {
                                withAnimation(.snappy) { draft.remove(person) }
                            }
                            .transition(.asymmetric(insertion: .move(edge: .top).combined(with: .opacity),
                                                    removal: .opacity.combined(with: .scale(scale: 0.95))))
                        }
                    }
                    .padding(.top, 17.5)

                    if !recents.isEmpty {
                        Text("Recenti")
                            .textStyle(TextStyle(face: .bold, size: 13, tracking: -0.01))
                            .foregroundStyle(Palette.raspberry)
                            .padding(.top, 16)
                            .padding(.leading, 6)
                        FlowLayout(spacing: 10, lineSpacing: 10) {
                            ForEach(recents, id: \.self) { recent in
                                Button {
                                    add(recent)
                                } label: {
                                    Text("+ \(recent)")
                                        .textStyle(TextStyle(face: .medium, size: 15.5))
                                        .foregroundStyle(Color(hex: 0x652B41))
                                        .padding(.horizontal, 16.5)
                                        .frame(height: 39)
                                        .background(Capsule().fill(Color(hex: 0xFFDFEA)))
                                        .overlay(DashedCapsule())
                                }
                                .buttonStyle(PressableStyle(scale: 0.94))
                                .accessibilityLabel("Aggiungi \(recent)")
                            }
                        }
                        .padding(.top, 9.5)
                    }
                }
                .padding(.horizontal, 20)
                .padding(.top, 18)
                .padding(.bottom, 16)
                .animation(.snappy, value: draft.people)
            }
            .scrollIndicators(.hidden)
            .plainScrollEdges()
            .scrollDismissesKeyboard(.interactively)
            .topFade(14)
            .padding(.top, 4)
        }
        .safeAreaInset(edge: .bottom, spacing: 0) {
            BottomBar {
                Button(draft.source.isItemized ? "Assegna gli articoli" : "Chi ha pagato?") {
                    typing = false
                    app.path.append(draft.source.isItemized ? .assign : .payments)
                }
                .buttonStyle(PrimaryButtonStyle())
                .disabled(draft.people.count < 2)
                Caption(text: caption)
                    .padding(.top, 13)
                    .padding(.bottom, -2)
            }
        }
        .background(BackdropView())
        .toolbar(.hidden, for: .navigationBar)
        .onAppear {
            if draft.people.isEmpty { typing = true }
        }
    }

    private var caption: String {
        switch draft.people.count {
        case 0: return "Chi c'era a mangiare?"
        case 1: return "Aggiungi almeno un'altra persona"
        case let n: return "\(n) persone al tavolo"
        }
    }

    private var nameField: some View {
        HStack(spacing: 8) {
            TextField(text: $name, prompt: Text("Nome").foregroundStyle(Palette.placeholder)) { Text("Nome") }
                .textStyle(TextStyle(face: .regular, size: 17))
                .foregroundStyle(Palette.ink)
                .textInputAutocapitalization(.words)
                .autocorrectionDisabled()
                .submitLabel(.done)
                .focused($typing)
                .onSubmit {
                    add(name)
                    if !draft.people.isEmpty { typing = true }
                }
                .padding(.leading, 22.5)
            Button("Aggiungi") {
                if name.trimmingCharacters(in: .whitespaces).isEmpty {
                    Haptics.select()
                } else {
                    add(name)
                }
                typing = true
            }
                .textStyle(.pillLabel)
                .foregroundStyle(.white)
                .padding(.horizontal, 17.5)
                .frame(height: 47.5)
                .background(Capsule().fill(Palette.ink))
                .buttonStyle(PressableStyle(scale: 0.94))
        }
        .padding(.trailing, 6)
        .frame(height: 59)
        .background(Capsule().fill(.white).shadow(color: Palette.shadow.opacity(0.07), radius: 16, y: 7))
    }

    private func add(_ raw: String) {
        guard draft.addPerson(named: raw) != nil else { return }
        Haptics.tap()
        name = ""
    }
}

private struct PersonRow: View {
    let person: Person
    let onRemove: () -> Void

    var body: some View {
        HStack(spacing: 11.5) {
            Avatar(person: person, size: 38)
            Text(person.name)
                .textStyle(.personNameStrong)
                .foregroundStyle(Palette.ink)
                .lineLimit(1)
            Spacer()
            Button(action: onRemove) {
                Image(systemName: "xmark")
                    .font(.system(size: 15, weight: .regular))
                    .foregroundStyle(Palette.muted)
                    .frame(width: 44, height: 44)
                    .contentShape(Rectangle())
            }
            .accessibilityLabel("Togli \(person.name)")
        }
        .padding(.leading, 12)
        .padding(.trailing, 8)
        .frame(height: 60)
        .background(RoundedRectangle(cornerRadius: 24, style: .continuous).fill(Color.white.opacity(0.8)))
    }
}
