package com.cldnpl.receipty.views

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cldnpl.receipty.localization.t
import com.cldnpl.receipty.models.Currency
import com.cldnpl.receipty.models.Money
import com.cldnpl.receipty.models.format
import com.cldnpl.receipty.theme.BackdropView
import com.cldnpl.receipty.theme.Face
import com.cldnpl.receipty.theme.Haptics
import com.cldnpl.receipty.theme.Initial
import com.cldnpl.receipty.theme.Palette
import com.cldnpl.receipty.theme.PrimaryButton
import com.cldnpl.receipty.theme.TightTitle
import com.cldnpl.receipty.theme.Type
import com.cldnpl.receipty.theme.card
import com.cldnpl.receipty.theme.pressable
import com.cldnpl.receipty.theme.softShadow
import com.cldnpl.receipty.theme.style
import com.cldnpl.receipty.theme.swiftSpring
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Le tre pagine della prima apertura: cosa fa l'app, come si assegnano le voci, il resto. */
@Composable
fun OnboardingView(onFinish: () -> Unit) {
    val pages = (1..3).map { t("onboarding.$it.title") to t("onboarding.$it.text") }
    val pager = rememberPagerState { pages.size }
    val scope = rememberCoroutineScope()
    val page = pager.currentPage
    val last = page == pages.lastIndex

    BackHandler(page > 0) { scope.launch { pager.animateScrollToPage(page - 1) } }

    Box(Modifier.fillMaxSize()) {
        BackdropView()
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            Row(Modifier.fillMaxWidth().height(44.dp).padding(horizontal = 26.dp), verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.weight(1f))
                val skipAlpha by animateFloatAsState(if (last) 0f else 1f, tween(200), label = "skip")
                Text(
                    t("onboarding.skip"),
                    style = style(Face.semibold, 16f),
                    color = Palette.raspberryDeep,
                    modifier = Modifier
                        .graphicsLayer { alpha = skipAlpha }
                        .pressable(0.95f, enabled = !last, onClick = onFinish),
                )
            }

            HorizontalPager(pager, Modifier.weight(1f)) { i ->
                Column(Modifier.fillMaxSize()) {
                    Illustration(i, visible = pager.settledPage == i, modifier = Modifier.padding(top = 12.dp).height(330.dp))
                    Spacer(Modifier.weight(1f).heightIn(min = 16.dp))
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 28.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        TightTitle(pages[i].first, style(Face.extraBold, 40f, -0.03f, 43f))
                        Text(pages[i].second, style = Type.lead, color = Palette.inkSoft)
                    }
                    Spacer(Modifier.weight(0.4f))
                }
            }

            Row(
                Modifier.fillMaxWidth().padding(bottom = 22.dp),
                horizontalArrangement = Arrangement.spacedBy(7.dp, Alignment.CenterHorizontally),
            ) {
                for (i in pages.indices) {
                    val on = i == page
                    val width by animateDpAsState(if (on) 24.dp else 8.dp, swiftSpring(0.35f, 0.8f), label = "dot")
                    val color by animateColorAsState(if (on) Palette.pink else Color.White, label = "dotColor")
                    Box(Modifier.width(width).height(8.dp).background(color, CircleShape))
                }
            }

            PrimaryButton(
                onClick = {
                    if (!last) {
                        scope.launch { pager.animateScrollToPage(page + 1) }
                    } else {
                        Haptics.success()
                        onFinish()
                    }
                },
                modifier = Modifier.padding(horizontal = 24.dp),
            ) { color ->
                AnimatedContent(last, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "start") {
                    Text(t(if (it) "onboarding.start" else "common.next"), style = Type.button, color = color)
                }
            }

            Text(
                t("onboarding.footer"),
                style = style(Face.medium, 13f),
                color = Palette.gray,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 14.dp, bottom = 6.dp),
            )
        }
    }
}

/** Illustrazioni fatte con i pezzi veri dell'app, così l'onboarding somiglia a quello che si userà. */
@Composable
private fun Illustration(page: Int, visible: Boolean, modifier: Modifier = Modifier) {
    val shown = remember { Animatable(0f) }
    LaunchedEffect(visible) {
        if (visible) {
            delay(100)
            shown.animateTo(1f, swiftSpring(0.6f, 0.8f))
        } else {
            shown.snapTo(0f)
        }
    }
    val s = shown.value
    val currency = Currency.current
    fun money(cents: Int) = Money.format(cents, currency)

    Box(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 28.dp)
            .clearAndSetSemantics { },
        contentAlignment = Alignment.Center,
    ) {
        when (page) {
            0 -> Box {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    MiniCard(
                        { Initial("G") }, "Giulia → Cla", t("result.pays", "Giulia", "Cla"), money(1025),
                        Modifier.graphicsLayer { rotationZ = -3f * s; translationX = -8.dp.toPx() * s },
                    )
                    MiniCard(
                        { Initial("M") }, "Marco → Cla", t("result.pays", "Marco", "Cla"), money(375),
                        Modifier.graphicsLayer { rotationZ = 2f * s; translationX = 10.dp.toPx() * s },
                    )
                    MiniCard(
                        { Initial("L") }, "Luca → Cla", t("result.pays", "Luca", "Cla"), money(375),
                        Modifier.graphicsLayer { rotationZ = -1.5f * s },
                    )
                }
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .graphicsLayer {
                            translationX = 10.dp.toPx()
                            translationY = (-34).dp.toPx()
                            val sc = 0.4f + 0.6f * s
                            scaleX = sc
                            scaleY = sc
                            alpha = s.coerceIn(0f, 1f)
                        }
                        .size(56.dp)
                        .softShadow(CircleShape, Palette.success, 0.35f, 14.dp, 6.dp)
                        .background(Palette.success, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(28.dp))
                }
            }
            1 -> Assignment(s, ::money)
            else -> Column(
                Modifier.graphicsLayer { alpha = 0.4f + 0.6f * s.coerceIn(0f, 1f) },
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                MiniCard(
                    { SymbolBadge(Icons.Outlined.Payments) }, "${t("result.change")} → Claudia",
                    t("result.keepsIt", "Claudia"), money(700),
                    Modifier.graphicsLayer { translationY = 20.dp.toPx() * (1 - s) },
                )
                MiniCard(
                    { SymbolBadge(Icons.Outlined.Payments) }, "${t("result.change")} → Marco",
                    t("result.keepsIt", "Marco"), money(100),
                    Modifier.graphicsLayer { translationY = 30.dp.toPx() * (1 - s) },
                )
                MiniCard(
                    { Initial("O") }, "Olga → Marco", t("result.pays", "Olga", "Marco"), money(1300),
                    Modifier.graphicsLayer { translationY = 40.dp.toPx() * (1 - s) },
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Assignment(s: Float, money: (Int) -> String) {
    Box {
        Column(
            Modifier
                .fillMaxWidth()
                .card()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row {
                Text(t("onboarding.sample.item"), style = Type.itemTitle, color = Palette.ink, modifier = Modifier.weight(1f))
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(money(800), style = Type.itemTitle, color = Palette.ink)
                    Text(
                        t("common.each", money(400)),
                        style = Type.perHead,
                        color = Palette.pinkText,
                        modifier = Modifier.graphicsLayer { alpha = s.coerceIn(0f, 1f) },
                    )
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Chip("C", "Cla", on = true)
                Chip("M", "Marco", on = s > 0.5f)
                Chip("G", "Giulia", on = false)
                Chip("L", "Luca", on = false)
            }
        }
        Icon(
            Icons.Filled.TouchApp,
            null,
            tint = Palette.pink,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .graphicsLayer {
                    translationX = (-40 - 110 * s).dp.toPx()
                    translationY = (40 - 60 * s).dp.toPx()
                    alpha = s.coerceIn(0f, 1f)
                }
                .size(38.dp),
        )
    }
}

@Composable
private fun Chip(initial: String, name: String, on: Boolean) {
    val fill by animateColorAsState(if (on) Palette.pink else Palette.pinkMist, label = "chip")
    Row(
        Modifier
            .height(33.5.dp)
            .background(fill, CircleShape)
            .padding(start = 6.dp, end = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Initial(initial, size = 22.dp, selected = on)
        Text(name, style = Type.chip, color = if (on) Color.White else Palette.ink)
    }
}

@Composable
private fun SymbolBadge(icon: ImageVector) {
    Box(Modifier.size(38.dp).background(Palette.pinkSoft, CircleShape), contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = Palette.pinkIcon, modifier = Modifier.size(19.dp))
    }
}

@Composable
private fun MiniCard(icon: @Composable () -> Unit, title: String, subtitle: String, amount: String, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .height(72.dp)
            .card(radius = 24.dp)
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        icon()
        Column(Modifier.weight(1f)) {
            Text(title, style = Type.transferName, color = Palette.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, style = Type.transferSub, color = Palette.gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(amount, style = Type.transferAmount, color = Palette.ink, maxLines = 1, softWrap = false)
    }
}
