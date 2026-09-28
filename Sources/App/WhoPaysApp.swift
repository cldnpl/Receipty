import SwiftUI

@main
struct WhoPaysApp: App {
    var body: some Scene {
        WindowGroup {
            RootView()
                .preferredColorScheme(.light)
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
