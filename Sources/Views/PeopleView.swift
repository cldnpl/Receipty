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
            FlowHeader(title: "WHO'S AT THE TABLE?", subtitle: "Add everyone, even those who didn't pay.") {
                if draft.source == .equal { app.finish() } else { app.path.removeLast() }
            }

            ScrollViewReader { proxy in
                ScrollView {
                    VStack(alignment: .leading, spacing: 0) {
                        nameField

                        VStack(spacing: 8) {
                            ForEach(draft.people) { person in
                                PersonRow(person: person) {
                                    withAnimation(.snappy) { draft.remove(person) }
                                }
                                .id(person.id)
                                .transition(.asymmetric(insertion: .move(edge: .top).combined(with: .opacity),
                                                        removal: .opacity.combined(with: .scale(scale: 0.95))))
                            }
                        }
                        .padding(.top, 17.5)

                        if !recents.isEmpty {
                            Text("Recent")
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
                                            .foregroundStyle(Palette.chipText)
                                            .padding(.horizontal, 16.5)
                                            .frame(height: 39)
                                            .background(Capsule().fill(Palette.chipFill))
                                            .overlay(DashedCapsule())
                                    }
                                    .buttonStyle(PressableStyle(scale: 0.94))
                                    .accessibilityLabel("Add \(recent)")
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
                .onChange(of: draft.people.count) { old, new in
                    // Il nome appena aggiunto resta visibile anche con la tastiera aperta.
                    guard new > old, let last = draft.people.last else { return }
                    withAnimation(.snappy) { proxy.scrollTo(last.id, anchor: .bottom) }
                }
            }
        }
        .safeAreaInset(edge: .bottom, spacing: 0) {
            BottomBar {
                Button(draft.source.isItemized ? "Assign items" : "Who paid?") {
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
        case 0: return "Who was at dinner?"
        case 1: return "Add at least one more person"
        case let n: return "\(n) people at the table"
        }
    }

    private var nameField: some View {
        HStack(spacing: 8) {
            TextField(text: $name, prompt: Text("Name").foregroundStyle(Palette.placeholder)) { Text("Name") }
                .textStyle(TextStyle(face: .regular, size: 17))
                .foregroundStyle(Palette.ink)
                .textInputAutocapitalization(.words)
                .autocorrectionDisabled()
                .submitLabel(.done)
                .focused($typing)
                .onSubmit {
                    // Invio aggiunge il nome e la tastiera resta su per il prossimo; con il campo
                    // vuoto invece la chiude. Il fuoco va ridato al giro dopo, quando UIKit
                    // ha finito di toglierlo.
                    let hadName = !name.trimmingCharacters(in: .whitespaces).isEmpty
                    add(name)
                    if hadName { DispatchQueue.main.async { typing = true } }
                }
                .padding(.leading, 22.5)
            Button("Add") {
                if name.trimmingCharacters(in: .whitespaces).isEmpty {
                    Haptics.select()
                } else {
                    add(name)
                }
                typing = true
            }
                .textStyle(.pillLabel)
                .foregroundStyle(Palette.inverseText)
                .padding(.horizontal, 17.5)
                .frame(height: 47.5)
                .background(Capsule().fill(Palette.inverseFill))
                .buttonStyle(PressableStyle(scale: 0.94))
        }
        .padding(.trailing, 6)
        .frame(height: 59)
        .background(Capsule().fill(Palette.card).shadow(color: Palette.shadow.opacity(0.07), radius: 16, y: 7))
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
            .accessibilityLabel("Remove \(person.name)")
        }
        .padding(.leading, 12)
        .padding(.trailing, 8)
        .frame(height: 60)
        .background(RoundedRectangle(cornerRadius: 24, style: .continuous).fill(Palette.cardSoft))
    }
}
