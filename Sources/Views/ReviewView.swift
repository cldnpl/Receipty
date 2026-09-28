import SwiftUI

/// "È tutto giusto?": le voci lette dallo scontrino, da confermare prima di andare avanti.
struct ReviewView: View {
    @Environment(AppModel.self) private var app
    @Bindable var draft: BillDraft
    @State private var editing: EditorTarget?

    private var typed: Bool { draft.source == .typed }

    var body: some View {
        VStack(spacing: 0) {
            FlowHeader(title: typed ? "WHAT DID\nYOU ORDER?" : "LOOKS RIGHT?",
                       subtitle: typed ? "Add the items on the receipt, one per line." : "Tap an item to fix it.")

            ScrollView {
                itemsCard
                    .padding(.horizontal, 20)
                    .padding(.top, 15.5)
                    .padding(.bottom, 12)
            }
            .scrollIndicators(.hidden)
            .plainScrollEdges()
            .topFade()
            .padding(.top, 4)
        }
        .safeAreaInset(edge: .bottom, spacing: 0) {
            BottomBar {
                Button("Continue") { app.path.append(.people) }
                    .buttonStyle(PrimaryButtonStyle())
                    .disabled(draft.items.isEmpty || draft.itemsToCheck > 0)
                Caption(text: caption)
                    .padding(.top, 13)
                    .padding(.bottom, -2)
            }
        }
        .background(BackdropView())
        .toolbar(.hidden, for: .navigationBar)
        .sheet(item: $editing) { target in
            ItemEditor(original: target.item) { saved in
                withAnimation(.snappy) {
                    if let i = draft.items.firstIndex(where: { $0.id == saved.id }) {
                        draft.items[i] = saved
                    } else {
                        draft.items.append(saved)
                    }
                }
            } onDelete: { id in
                withAnimation(.snappy) { draft.items.removeAll { $0.id == id } }
            }
        }
    }

    private var caption: String {
        if draft.items.isEmpty { return "Add at least one item" }
        switch draft.itemsToCheck {
        case 0: return draft.items.count == 1 ? "1 item" : "\(draft.items.count) items"
        case 1: return "Check the highlighted item"
        case let n: return "Check the \(n) highlighted items"
        }
    }

    private var itemsCard: some View {
        VStack(spacing: 0) {
            VStack(spacing: 0) {
                ForEach(draft.items) { item in
                    Button { editing = EditorTarget(item: item) } label: {
                        ItemRow(item: item)
                    }
                    .buttonStyle(PressableStyle(scale: 0.985))
                    .transition(.opacity.combined(with: .move(edge: .top)))
                }
            }

            Button { editing = EditorTarget(item: nil) } label: {
                HStack(spacing: 10) {
                    Image(systemName: "plus").font(.system(size: 17, weight: .semibold))
                    Text("Add item").textStyle(TextStyle(face: .semibold, size: 16))
                    Spacer()
                }
                .foregroundStyle(Color(hex: 0xE388A7))
                .padding(.horizontal, 13)
                .frame(height: 50)
                .contentShape(Rectangle())
            }
            .buttonStyle(PressableStyle(scale: 0.98))

            Rectangle()
                .fill(Palette.hairline)
                .frame(height: 1)
                .padding(.horizontal, 12)
                .padding(.top, 3)

            HStack(alignment: .firstTextBaseline) {
                Text(typed ? "Total" : "Recognized total")
                    .textStyle(.totalLabel)
                    .foregroundStyle(Palette.gray)
                Spacer()
                Text(Money.format(draft.itemsTotal))
                    .textStyle(.totalValue)
                    .foregroundStyle(Palette.ink)
                    .contentTransition(.numericText())
            }
            .padding(.horizontal, 13)
            .padding(.top, 15)

            if let printed = draft.printedTotal, printed != draft.itemsTotal {
                HStack(spacing: 6) {
                    Image(systemName: "exclamationmark.circle").font(.system(size: 13, weight: .medium))
                    Text("The receipt says \(Money.format(printed))")
                        .textStyle(.rowWarning)
                    Spacer()
                }
                .foregroundStyle(Palette.warning)
                .padding(.horizontal, 13)
                .padding(.top, 8)
            }
        }
        .padding(.horizontal, 8)
        .padding(.top, 10)
        .padding(.bottom, 18)
        .card()
        .animation(.snappy, value: draft.items)
    }
}

private struct ItemRow: View {
    let item: BillItem

    var body: some View {
        HStack(alignment: .center, spacing: 12) {
            VStack(alignment: .leading, spacing: 3) {
                HStack(spacing: 0) {
                    if item.quantity > 1 {
                        Text("\(item.quantity)× ").foregroundStyle(Palette.gray)
                    }
                    Text(item.name).foregroundStyle(Palette.ink)
                }
                .textStyle(.rowName)
                .lineLimit(2)
                if let issue = item.issue {
                    HStack(spacing: 5) {
                        Image(systemName: "exclamationmark.circle").font(.system(size: 13.5, weight: .medium))
                        Text(issue == .name ? "Name hard to read" : "Check the price")
                            .textStyle(.rowWarning)
                    }
                    .foregroundStyle(Palette.warning)
                }
            }
            Spacer(minLength: 8)
            Text(Money.format(item.cents))
                .textStyle(.rowPrice)
                .foregroundStyle(Palette.ink)
        }
        .padding(.horizontal, 13)
        .frame(minHeight: item.issue == nil ? 49.5 : 63)
        .background(
            RoundedRectangle(cornerRadius: 18, style: .continuous)
                .fill(item.issue == nil ? Color.clear : Palette.flag)
        )
        .padding(.vertical, item.issue == nil ? 0 : 2)
        .contentShape(Rectangle())
        .accessibilityElement(children: .combine)
        .accessibilityHint("Tap to edit")
    }
}

struct EditorTarget: Identifiable {
    let id = UUID()
    let item: BillItem?
}

/// Correggere o aggiungere una voce: nome, quantità, prezzo.
struct ItemEditor: View {
    @Environment(\.dismiss) private var dismiss
    let original: BillItem?
    let onSave: (BillItem) -> Void
    let onDelete: (BillItem.ID) -> Void

    @State private var name: String
    @State private var quantity: Int
    @State private var price: String
    @FocusState private var focus: Field?

    enum Field { case name, price }

    init(original: BillItem?, onSave: @escaping (BillItem) -> Void, onDelete: @escaping (BillItem.ID) -> Void) {
        self.original = original
        self.onSave = onSave
        self.onDelete = onDelete
        _name = State(initialValue: original?.name ?? "")
        _quantity = State(initialValue: original?.quantity ?? 1)
        _price = State(initialValue: original.map { Money.editable($0.cents) } ?? "")
    }

    private var cents: Int? { Money.parse(price) }
    private var canSave: Bool {
        !name.trimmingCharacters(in: .whitespaces).isEmpty && cents != nil
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            Text(original == nil ? "New item" : "Edit item")
                .textStyle(.sheetTitle)
                .foregroundStyle(Palette.ink)
                .padding(.top, 28)

            if let issue = original?.issue {
                HStack(spacing: 6) {
                    Image(systemName: "exclamationmark.circle").font(.system(size: 13.5, weight: .medium))
                    Text(issue == .name ? "The name was hard to read: check it." : "The price was hard to read: compare it with the receipt.")
                        .textStyle(.rowWarning)
                }
                .foregroundStyle(Palette.warning)
                .padding(.top, 8)
            }

            Text("Name").editorLabel().padding(.top, 20)
            TextField(text: $name, prompt: Text("E.g. Margherita pizza").foregroundStyle(Palette.placeholder)) { Text("Name") }
                .textStyle(TextStyle(face: .medium, size: 17))
                .foregroundStyle(Palette.ink)
                .focused($focus, equals: .name)
                .submitLabel(.next)
                .onSubmit { focus = .price }
                .padding(.horizontal, 20)
                .frame(height: 54)
                .background(Capsule().fill(.white).shadow(color: Palette.shadow.opacity(0.06), radius: 10, y: 4))

            HStack(alignment: .bottom, spacing: 12) {
                VStack(alignment: .leading, spacing: 0) {
                    Text("Quantity").editorLabel()
                    HStack(spacing: 0) {
                        stepperButton("minus") { quantity = max(1, quantity - 1) }
                            .disabled(quantity <= 1)
                        Text("\(quantity)")
                            .textStyle(TextStyle(face: .bold, size: 18))
                            .foregroundStyle(Palette.ink)
                            .contentTransition(.numericText())
                            .frame(minWidth: 30)
                        stepperButton("plus") { quantity = min(99, quantity + 1) }
                    }
                    .padding(.horizontal, 6)
                    .frame(height: 54)
                    .background(Capsule().fill(.white).shadow(color: Palette.shadow.opacity(0.06), radius: 10, y: 4))
                    .animation(.snappy, value: quantity)
                }
                VStack(alignment: .leading, spacing: 0) {
                    Text("Line total").editorLabel()
                    HStack(spacing: 6) {
                        Text("€")
                            .font(.custom(Inter.semibold.rawValue, size: 18))
                            .foregroundStyle(Palette.raspberry)
                        TextField(text: $price, prompt: Text("0.00").foregroundStyle(Palette.fieldPlaceholder)) { Text("Price") }
                            .textStyle(TextStyle(face: .bold, size: 19))
                            .foregroundStyle(Palette.ink)
                            .keyboardType(.decimalPad)
                            .focused($focus, equals: .price)
                            .onChange(of: price) { _, new in
                                let negative = new.hasPrefix("-")
                                let clean = (negative ? "-" : "") + Money.sanitizeInput(new)
                                if clean != new { price = clean }
                            }
                    }
                    .padding(.horizontal, 20)
                    .frame(height: 54)
                    .background(Capsule().fill(.white).shadow(color: Palette.shadow.opacity(0.06), radius: 10, y: 4))
                }
            }
            .padding(.top, 16)

            if quantity > 1, let c = cents {
                Text("\(Money.format(c / quantity)) each")
                    .textStyle(.micro)
                    .foregroundStyle(Palette.gray)
                    .frame(maxWidth: .infinity, alignment: .trailing)
                    .padding(.top, 7)
                    .padding(.trailing, 20)
            }

            Spacer(minLength: 20)

            HStack(spacing: 11) {
                if let original {
                    Button {
                        onDelete(original.id)
                        dismiss()
                    } label: {
                        Text("Delete")
                            .textStyle(TextStyle(face: .semibold, size: 17))
                            .foregroundStyle(Palette.warning)
                            .frame(maxWidth: .infinity)
                            .frame(height: Metrics.buttonHeight)
                            .background(Capsule().fill(Color(hex: 0xFFF8FA)).shadow(color: Palette.shadow.opacity(0.08), radius: 12, y: 5))
                    }
                    .buttonStyle(PressableStyle())
                    .frame(width: 130)
                }
                Button(original == nil ? "Add" : "Confirm") {
                    guard let cents else { return }
                    var item = original ?? BillItem(name: "", cents: 0)
                    item.name = name.trimmingCharacters(in: .whitespaces)
                    item.quantity = quantity
                    item.cents = cents
                    item.issue = nil
                    Haptics.tap()
                    onSave(item)
                    dismiss()
                }
                .buttonStyle(PrimaryButtonStyle())
                .disabled(!canSave)
            }
            .padding(.bottom, 8)
        }
        .padding(.horizontal, 22)
        .presentationDetents([.height(original?.issue == nil ? 420 : 446)])
        .presentationCornerRadius(38)
        .presentationDragIndicator(.visible)
        .presentationBackground {
            LinearGradient(colors: [Palette.sheetTop, Palette.sheetBottom], startPoint: .top, endPoint: .bottom)
        }
        .onAppear {
            if original == nil { focus = .name }
            else if original?.issue == .price { focus = .price }
            else if original?.issue == .name { focus = .name }
        }
    }

    private func stepperButton(_ icon: String, action: @escaping () -> Void) -> some View {
        Button {
            Haptics.select()
            action()
        } label: {
            Image(systemName: icon)
                .font(.system(size: 15, weight: .bold))
                .foregroundStyle(Palette.pink)
                .frame(width: 42, height: 42)
                .background(Circle().fill(Palette.pinkMist))
        }
        .buttonStyle(PressableStyle(scale: 0.9))
    }
}

private extension Text {
    func editorLabel() -> some View {
        self.textStyle(TextStyle(face: .semibold, size: 13))
            .foregroundStyle(Palette.raspberry)
            .padding(.leading, 6)
            .padding(.bottom, 8)
    }
}
