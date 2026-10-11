package com.cldnpl.receipty.views

import android.app.Application
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cldnpl.receipty.app.DemoSeed
import com.cldnpl.receipty.localization.t
import com.cldnpl.receipty.models.AppModel
import com.cldnpl.receipty.models.AppSettings
import com.cldnpl.receipty.models.BillDraft
import com.cldnpl.receipty.models.BillStore
import com.cldnpl.receipty.models.LocalCurrency
import com.cldnpl.receipty.models.Route
import com.cldnpl.receipty.theme.Backdrop
import com.cldnpl.receipty.theme.BackdropView
import com.cldnpl.receipty.theme.Face
import com.cldnpl.receipty.theme.Haptics
import com.cldnpl.receipty.theme.LocalBack
import com.cldnpl.receipty.theme.LocalDark
import com.cldnpl.receipty.theme.LocalScreenHeight
import com.cldnpl.receipty.theme.Palette
import com.cldnpl.receipty.theme.style
import com.cldnpl.receipty.theme.swiftSpring
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

enum class AppTab { BILLS, SETTINGS }

enum class SettingsRoute { CURRENCY, LANGUAGE }

/** Lo stato che sopravvive a rotazioni e cambi di tema: percorso, conto in corso, tutorial. */
class RootViewModel(application: Application) : AndroidViewModel(application) {
    val app: AppModel = AppModel(
        DemoSeed.makeStore(application) ?: BillStore(File(application.filesDir, "bills"))
    ).also { DemoSeed.apply(it) }
    val coach = Coach()
    var tab by mutableStateOf(AppTab.BILLS)
    val settingsPath = mutableStateListOf<SettingsRoute>()

    // Negli screenshot (`screen`) lo splash si salta, tranne quando è lui lo screenshot.
    var splashDone by mutableStateOf(DemoSeed.screen != null && DemoSeed.screen != "splash")
}

/** Una schermata della pila: il conto in corso viaggia con lei, così esce animata anche a percorso finito. */
private data class StackEntry(val depth: Int, val route: Route?, val draft: BillDraft?)

@Composable
fun RootView(vm: RootViewModel = viewModel()) {
    val app = vm.app
    val coach = vm.coach
    val scope = rememberCoroutineScope()

    fun startTutorial() {
        vm.tab = AppTab.BILLS
        app.path.clear()
        app.showingNewBill = false
        coach.wide = true
        coach.step = Coach.Step.PLUS
        scope.launch {
            // Prima la patina copre lo schermo, poi si stringe attorno al +.
            delay(50)
            coach.wide = false
        }
    }

    fun replayTutorial() {
        vm.settingsPath.clear()
        startTutorial()
    }

    fun endTutorial() {
        app.showingNewBill = false
        coach.wide = true
        scope.launch {
            delay(450)
            coach.step = null
        }
    }

    fun next(step: Coach.Step) {
        Haptics.tap()
        when (step) {
            Coach.Step.PLUS -> app.showingNewBill = true
            Coach.Step.SCAN -> coach.step = Coach.Step.MANUAL
            Coach.Step.MANUAL -> endTutorial()
        }
    }

    // Al primo passo si tocca il + vero: quando il foglio si apre, si passa alle due opzioni.
    LaunchedEffect(app.showingNewBill) {
        if (app.showingNewBill && coach.step == Coach.Step.PLUS) {
            delay(450)
            coach.step = Coach.Step.SCAN
        }
    }

    LaunchedEffect(Unit) {
        when (DemoSeed.screen) {
            "tutorial" -> startTutorial()
            "settings" -> vm.tab = AppTab.SETTINGS
            "currency" -> { vm.tab = AppTab.SETTINGS; vm.settingsPath.add(SettingsRoute.CURRENCY) }
            "language" -> { vm.tab = AppTab.SETTINGS; vm.settingsPath.add(SettingsRoute.LANGUAGE) }
        }
    }

    val showOnboarding = vm.splashDone && when (val screen = DemoSeed.screen) {
        null -> !AppSettings.didOnboard
        else -> screen == "onboarding" && !AppSettings.didOnboard
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val screenHeight = constraints.maxHeight.toFloat()
        CompositionLocalProvider(
            LocalScreenHeight provides screenHeight,
            LocalCoach provides coach,
        ) {
            Box(Modifier.fillMaxSize()) {
                val holder = rememberSaveableStateHolder()
                holder.SaveableStateProvider(vm.tab) {
                    when (vm.tab) {
                        AppTab.BILLS -> BillsStack(app, holder, onTab = { vm.tab = it })
                        AppTab.SETTINGS -> SettingsStack(vm, onReplayTutorial = ::replayTutorial)
                    }
                }

                AnimatedVisibility(app.showingNewBill, enter = fadeIn(), exit = fadeOut()) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(Palette.scrim.copy(alpha = 0.32f))
                            .pointerInput(Unit) { detectTapGestures { app.showingNewBill = false } }
                    )
                }
                BackHandler(app.showingNewBill && !coach.isActive) { app.showingNewBill = false }
                AnimatedVisibility(
                    app.showingNewBill,
                    modifier = Modifier.align(Alignment.BottomCenter).zIndex(1f),
                    enter = slideInVertically(swiftSpring(0.42f, 0.88f, androidx.compose.ui.unit.IntOffset(1, 1))) { it },
                    exit = slideOutVertically(swiftSpring(0.42f, 0.88f, androidx.compose.ui.unit.IntOffset(1, 1))) { it },
                ) {
                    NewBillSheet(app)
                }

                BackHandler(coach.isActive) { endTutorial() }
                AnimatedVisibility(
                    coach.isActive,
                    modifier = Modifier.zIndex(2f),
                    enter = fadeIn(tween(0)),
                    exit = fadeOut(tween(250)),
                ) {
                    CoachOverlay(coach, onNext = ::next, onSkip = ::endTutorial)
                }

                // Uscendo si allarga e sfuma. Resta davanti a tutto finché non se n'è andato:
                // l'onboarding sale di sotto nello stesso momento, così tra lo splash e la prima
                // schermata non si intravede la home.
                AnimatedVisibility(
                    !vm.splashDone,
                    modifier = Modifier.zIndex(3f),
                    enter = fadeIn(tween(0)),
                    exit = fadeOut(tween(380)) + scaleOut(tween(380), targetScale = 1.07f),
                ) {
                    SplashView { vm.splashDone = true }
                }

                AnimatedVisibility(
                    showOnboarding,
                    modifier = Modifier.zIndex(4f),
                    enter = slideInVertically(tween(450)) { it },
                    exit = slideOutVertically(tween(350)) { it },
                ) {
                    OnboardingView {
                        AppSettings.updateDidOnboard(true)
                        scope.launch {
                            delay(600)
                            startTutorial()
                        }
                    }
                }
            }
        }
    }
}

// Pila dei conti

@Composable
private fun BillsStack(app: AppModel, holder: androidx.compose.runtime.saveable.SaveableStateHolder, onTab: (AppTab) -> Unit) {
    val pop: () -> Unit = { if (app.path.isNotEmpty()) app.path.removeAt(app.path.lastIndex) }
    BackHandler(app.path.isNotEmpty()) { pop() }

    val entry = StackEntry(app.path.size, app.path.lastOrNull(), app.draft)
    AnimatedContent(
        entry,
        transitionSpec = { pushPop(initialState.depth, targetState.depth) },
        contentKey = { it.depth to it.route },
        label = "bills",
    ) { e ->
        holder.SaveableStateProvider("bills-${e.depth}-${e.route}") {
            CompositionLocalProvider(LocalBack provides pop) {
                Box(Modifier.fillMaxSize()) {
                    if (e.route == null) {
                        BackdropView()
                        Column(Modifier.fillMaxSize()) {
                            Box(Modifier.weight(1f)) { HomeView(app) }
                            TabBar(AppTab.BILLS, onTab)
                        }
                    } else {
                        Destination(app, e.route, e.draft)
                    }
                }
            }
        }
    }
}

/** Come la pila di navigazione di iOS: la nuova schermata entra da destra sopra la vecchia. */
private fun pushPop(from: Int, to: Int): ContentTransform {
    val spec = tween<androidx.compose.ui.unit.IntOffset>(380, easing = androidx.compose.animation.core.FastOutSlowInEasing)
    return if (to >= from) {
        (slideInHorizontally(spec) { it } togetherWith slideOutHorizontally(spec) { -it / 4 })
    } else {
        (slideInHorizontally(spec) { -it / 4 } togetherWith slideOutHorizontally(spec) { it })
    }.apply { targetContentZIndex = to.toFloat() }
}

@Composable
private fun Destination(app: AppModel, route: Route, draft: BillDraft?) {
    when (route) {
        is Route.Saved -> {
            val bill = app.store.bill(route.id) ?: return
            CompositionLocalProvider(LocalCurrency provides bill.settlement.currency) {
                ResultView(bill.settlement, bill.date, onDone = { app.path.clear() })
            }
        }
        else -> {
            draft ?: return
            CompositionLocalProvider(LocalCurrency provides draft.currency) {
                when (route) {
                    Route.Scan -> ScanView(app, draft)
                    Route.Review -> ReviewView(app, draft)
                    Route.People -> PeopleView(app, draft)
                    Route.Assign -> AssignView(app, draft)
                    Route.Payments -> PaymentsView(app, draft)
                    Route.Result -> DraftResultView(app, draft)
                    is Route.Saved -> Unit
                }
            }
        }
    }
}

// Impostazioni

@Composable
private fun SettingsStack(vm: RootViewModel, onReplayTutorial: () -> Unit) {
    val path = vm.settingsPath
    val pop: () -> Unit = { if (path.isNotEmpty()) path.removeAt(path.lastIndex) }
    BackHandler(path.isNotEmpty()) { pop() }
    Box(Modifier.fillMaxSize()) {
        BackdropView()
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f)) {
                AnimatedContent(
                    path.size to path.lastOrNull(),
                    transitionSpec = { pushPop(initialState.first, targetState.first) },
                    label = "settings",
                ) { (_, route) ->
                    CompositionLocalProvider(LocalBack provides pop) {
                        Box(Modifier.fillMaxSize()) {
                            BackdropView()
                            when (route) {
                                null -> SettingsView(onOpen = { path.add(it) }, onReplayTutorial = onReplayTutorial)
                                SettingsRoute.CURRENCY -> CurrencyPickerView(onDone = pop)
                                SettingsRoute.LANGUAGE -> LanguagePickerView(onDone = pop)
                            }
                        }
                    }
                }
            }
            TabBar(AppTab.SETTINGS) { vm.tab = it }
        }
    }
}

// Barra delle schede

@Composable
private fun TabBar(selected: AppTab, onSelect: (AppTab) -> Unit) {
    val dark = LocalDark.current
    Row(
        Modifier
            .fillMaxWidth()
            .background(Backdrop.color(0.985f, dark))
            .navigationBarsPadding()
            .height(56.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TabItem(Icons.Outlined.Receipt, t("tab.bills"), selected == AppTab.BILLS) { onSelect(AppTab.BILLS) }
        TabItem(Icons.Outlined.Settings, t("tab.settings"), selected == AppTab.SETTINGS) { onSelect(AppTab.SETTINGS) }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.TabItem(icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
    val color = if (selected) Palette.raspberry else Palette.muted
    Column(
        Modifier
            .weight(1f)
            .selectable(
                selected = selected,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Tab,
                onClick = onClick,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(25.dp))
        Text(label, style = style(Face.medium, 10.5f), color = color, maxLines = 1)
    }
}
