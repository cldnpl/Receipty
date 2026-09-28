import SwiftUI

@main
struct WhoPaysApp: App {
    init() {
        // Da iOS 26 la tab bar è di vetro e fluttua: va lasciata com'è. Prima era una striscia
        // piena, che diventava bianca quando il contenuto le arrivava sotto: lì prende il colore
        // del fondo del gradiente.
        guard #unavailable(iOS 26.0) else { return }
        let bar = UITabBarAppearance()
        bar.configureWithTransparentBackground()
        bar.backgroundColor = UIColor(Backdrop.color(at: 0.985))
        bar.shadowColor = .clear
        let item = UITabBarItemAppearance()
        let font = UIFont(name: Inter.medium.rawValue, size: 10.5) ?? .systemFont(ofSize: 10.5, weight: .medium)
        item.normal.iconColor = UIColor(Palette.muted)
        item.normal.titleTextAttributes = [.foregroundColor: UIColor(Palette.muted), .font: font]
        item.selected.iconColor = UIColor(Palette.raspberry)
        item.selected.titleTextAttributes = [.foregroundColor: UIColor(Palette.raspberry), .font: font]
        bar.stackedLayoutAppearance = item
        bar.inlineLayoutAppearance = item
        bar.compactInlineLayoutAppearance = item
        UITabBar.appearance().standardAppearance = bar
        UITabBar.appearance().scrollEdgeAppearance = bar
    }

    var body: some Scene {
        WindowGroup {
            RootView()
                .dynamicTypeSize(...DynamicTypeSize.xxLarge)
        }
    }
}

/// Con la barra di navigazione nascosta UIKit spegne lo swipe per tornare indietro: lo riaccende.
extension UINavigationController: UIGestureRecognizerDelegate {
    override open func viewDidLoad() {
        super.viewDidLoad()
        interactivePopGestureRecognizer?.delegate = self
    }

    public func gestureRecognizerShouldBegin(_ gestureRecognizer: UIGestureRecognizer) -> Bool {
        viewControllers.count > 1
    }
}
