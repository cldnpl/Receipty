package com.cldnpl.receipty.views

import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Contrast
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.PanTool
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.NorthEast
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cldnpl.receipty.localization.AppLanguage
import com.cldnpl.receipty.localization.I18n
import com.cldnpl.receipty.localization.t
import com.cldnpl.receipty.models.AppSettings
import com.cldnpl.receipty.models.Appearance
import com.cldnpl.receipty.models.Currency
import com.cldnpl.receipty.theme.Face
import com.cldnpl.receipty.theme.FlowHeader
import com.cldnpl.receipty.theme.Haptics
import com.cldnpl.receipty.theme.Palette
import com.cldnpl.receipty.theme.Type
import com.cldnpl.receipty.theme.card
import com.cldnpl.receipty.theme.fixedStyle
import com.cldnpl.receipty.theme.pressable
import com.cldnpl.receipty.theme.softShadow
import com.cldnpl.receipty.theme.style
import com.cldnpl.receipty.theme.swiftSpring
import com.cldnpl.receipty.theme.topFade

/**
 * Privacy policy ed EULA stanno nella repo pubblica (gli stessi URL vanno negli store):
 * l'app li apre, non se li porta dentro.
 */
enum class LegalDocument(val file: String) {
    PRIVACY("PRIVACY_POLICY.md"),
    EULA("EULA.md");

    val url: String get() = "https://github.com/cldnpl/Receipty/blob/main/$file"
}

object AppInfo {
    fun version(context: Context): String = runCatching {
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        val build = if (Build.VERSION.SDK_INT >= 28) info.longVersionCode else @Suppress("DEPRECATION") info.versionCode.toLong()
        "${info.versionName} ($build)"
    }.getOrDefault("1.0 (1)")
}

/** Safari dentro l'app su iOS, una Custom Tab qui: privacy policy ed EULA pubblicate nella repo. */
fun openLegal(context: Context, document: LegalDocument, color: Color) {
    val params = CustomTabColorSchemeParams.Builder().setToolbarColor(color.toArgb()).build()
    runCatching {
        CustomTabsIntent.Builder()
            .setDefaultColorSchemeParams(params)
            .setShowTitle(true)
            .build()
            .launchUrl(context, Uri.parse(document.url))
    }
}

/** Impostazioni: poche e utili. Aspetto, lingua, valuta, documenti legali. */
@Composable
fun SettingsView(onOpen: (SettingsRoute) -> Unit, onReplayTutorial: () -> Unit) {
    val context = LocalContext.current
    val currency = Currency(AppSettings.currencyCode ?: Currency.deviceDefault.code)
    val language = I18n.language
    val sheetTop = Palette.sheetTop

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .topFade(10.dp)
            .verticalScroll(rememberScrollState())
            .padding(start = 22.dp, end = 22.dp, bottom = 30.dp)
    ) {
        Text(
            t("settings.title"),
            style = Type.hero,
            color = Palette.ink,
            modifier = Modifier
                .padding(start = 6.dp, top = 8.dp)
                .semantics { heading() },
        )

        SettingsSection(t("settings.appearance"), Modifier.padding(top = 30.dp)) {
            AppearancePicker(AppSettings.appearance, Modifier.padding(8.dp)) { AppSettings.updateAppearance(it) }
        }

        SettingsSection(t("settings.language"), Modifier.padding(top = 22.dp), footer = t("settings.language.footer")) {
            SettingsRow(language.nativeName, detail = language.localizedName, onClick = { onOpen(SettingsRoute.LANGUAGE) }) {
                SettingsIcon(Icons.Outlined.Language)
            }
        }

        SettingsSection(t("settings.currency"), Modifier.padding(top = 22.dp), footer = t("settings.currency.footer")) {
            SettingsRow(currency.name, detail = currency.code, onClick = { onOpen(SettingsRoute.CURRENCY) }) {
                CurrencyBadge(currency)
            }
        }

        SettingsSection(t("settings.about"), Modifier.padding(top = 22.dp)) {
            SettingsRow(t("settings.privacy"), external = true, onClick = { openLegal(context, LegalDocument.PRIVACY, sheetTop) }) {
                SettingsIcon(Icons.Outlined.PanTool)
            }
            RowDivider()
            SettingsRow(t("settings.eula"), external = true, onClick = { openLegal(context, LegalDocument.EULA, sheetTop) }) {
                SettingsIcon(Icons.Outlined.Description)
            }
            RowDivider()
            SettingsRow(t("settings.replayTutorial"), chevron = false, onClick = onReplayTutorial) {
                SettingsIcon(Icons.Outlined.AutoAwesome)
            }
            RowDivider()
            SettingsRow(t("settings.version"), detail = AppInfo.version(context), chevron = false) {
                SettingsIcon(Icons.Outlined.Info)
            }
        }

        Text(
            t("settings.footer"),
            style = style(Face.medium, 13.5f, lineHeight = 19f),
            color = Palette.gray,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 26.dp),
        )
    }
}

// Pezzi

@Composable
private fun SettingsSection(
    title: String,
    modifier: Modifier = Modifier,
    footer: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier) {
        SectionTitle(title)
        Column(
            Modifier
                .fillMaxWidth()
                .card(radius = 26.dp),
            content = content,
        )
        if (footer != null) {
            Text(
                footer,
                style = style(Face.regular, 13f, lineHeight = 18f),
                color = Palette.gray,
                modifier = Modifier.padding(horizontal = 8.dp).padding(top = 9.dp),
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = Type.section,
        color = Palette.raspberry,
        modifier = modifier
            .padding(start = 8.dp, bottom = 10.dp)
            .semantics { heading() },
    )
}

@Composable
private fun SettingsRow(
    title: String,
    detail: String? = null,
    chevron: Boolean = true,
    external: Boolean = false,
    onClick: (() -> Unit)? = null,
    icon: @Composable () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.pressable(0.98f, onClick = onClick) else Modifier)
            .heightIn(min = 62.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        icon()
        Text(title, style = Type.personName, color = Palette.ink, modifier = Modifier.weight(1f))
        if (detail != null) {
            Text(detail, style = style(Face.medium, 15f), color = Palette.gray, maxLines = 1)
        }
        if (chevron) {
            Icon(
                if (external) Icons.Rounded.NorthEast else Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                null,
                tint = Palette.muted,
                modifier = Modifier.size(if (external) 16.dp else 20.dp),
            )
        }
    }
}

@Composable
private fun SettingsIcon(icon: ImageVector) {
    Box(Modifier.size(36.dp).background(Palette.pinkSoft, CircleShape), contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = Palette.pinkIcon, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun RowDivider() {
    Box(
        Modifier
            .padding(start = 66.dp)
            .fillMaxWidth()
            .height(1.dp)
            .background(Palette.hairline)
    )
}

@Composable
fun CurrencyBadge(currency: Currency, size: Dp = 36.dp) {
    val symbol = currency.symbol
    Box(
        Modifier
            .size(size)
            .background(Palette.pinkSoft, CircleShape)
            .padding(horizontal = 3.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            symbol,
            style = fixedStyle(Face.bold, if (symbol.length > 2) 11f else 15f),
            color = Palette.raspberry,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Clip,
        )
    }
}

/** Sistema / Chiaro / Scuro, a pillole come le chip dell'app. */
@Composable
private fun AppearancePicker(selection: Appearance, modifier: Modifier = Modifier, onSelect: (Appearance) -> Unit) {
    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .background(Palette.pinkMist, CircleShape)
            .padding(4.dp)
    ) {
        val gap = 4.dp
        val segment = (maxWidth - gap * 2) / 3
        val index = Appearance.entries.indexOf(selection)
        val x by animateDpAsState((segment + gap) * index, swiftSpring(0.35f, 0.8f), label = "pill")
        Box(
            Modifier
                .offset(x = x)
                .width(segment)
                .height(44.dp)
                .background(Palette.pink, CircleShape)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
            for (option in Appearance.entries) {
                val on = option == selection
                Row(
                    Modifier
                        .width(segment)
                        .height(44.dp)
                        .selectable(on, role = Role.RadioButton, interactionSource = null, indication = null) {
                            Haptics.select()
                            onSelect(option)
                        },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    val color = if (on) Color.White else Palette.ink
                    Icon(
                        when (option) {
                            Appearance.SYSTEM -> Icons.Outlined.Contrast
                            Appearance.LIGHT -> Icons.Outlined.LightMode
                            Appearance.DARK -> Icons.Outlined.DarkMode
                        },
                        null,
                        tint = color,
                        modifier = Modifier.size(17.dp),
                    )
                    Spacer(Modifier.width(7.dp))
                    Text(option.title, style = style(Face.semibold, 15f), color = color, maxLines = 1)
                }
            }
        }
    }
}

// Valuta

@Composable
fun CurrencyPickerView(onDone: () -> Unit) {
    val focusManager = LocalFocusManager.current
    var query by rememberSaveable { mutableStateOf("") }
    val selection = AppSettings.currencyCode ?: Currency.deviceDefault.code
    val q = query.trim().lowercase()
    val searching = q.isNotEmpty()
    val results = if (!searching) Currency.all else Currency.all.filter {
        it.name.lowercase().contains(q) || it.code.lowercase().contains(q) || it.symbol.lowercase() == q
    }
    // In cima: quella scelta, quella del telefono e le più comuni in viaggio.
    val popular = listOf(selection, Currency.deviceDefault.code, "EUR", "USD", "GBP", "CHF", "JPY").distinct().map(::Currency)

    fun choose(currency: Currency) {
        Haptics.select()
        AppSettings.updateCurrency(currency.code)
        focusManager.clearFocus()
        onDone()
    }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        FlowHeader(t("currency.title"), subtitle = t("currency.subtitle"))

        val textStyle = style(Face.regular, 17f).copy(color = Palette.ink)
        Row(
            Modifier
                .padding(horizontal = 20.dp)
                .padding(top = 18.dp)
                .fillMaxWidth()
                .height(54.dp)
                .softShadow(CircleShape, Palette.shadow, 0.07f, 16.dp, 7.dp)
                .background(Palette.card, CircleShape)
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(Icons.Rounded.Search, null, tint = Palette.placeholder, modifier = Modifier.size(20.dp))
            BasicTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                textStyle = textStyle,
                cursorBrush = SolidColor(Palette.pink),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None, autoCorrectEnabled = false, imeAction = ImeAction.Search),
                modifier = Modifier.weight(1f),
                decorationBox = { inner ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (query.isEmpty()) Text(t("currency.search"), style = textStyle.copy(color = Palette.placeholder), maxLines = 1)
                        inner()
                    }
                },
            )
        }

        LazyColumn(
            Modifier
                .fillMaxSize()
                .topFade(14.dp),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 30.dp),
        ) {
            if (!searching) {
                item(key = "popular-title") { SectionTitle(t("currency.popular")) }
                item(key = "popular") { CurrencyList(popular, selection, ::choose) }
                item(key = "all-title") { SectionTitle(t("currency.all"), Modifier.padding(top = 22.dp)) }
            }
            item(key = "all") { CurrencyList(results, selection, ::choose) }
        }
    }
}

@Composable
private fun CurrencyList(currencies: List<Currency>, selection: String, onChoose: (Currency) -> Unit) {
    if (currencies.isEmpty()) return
    Column(Modifier.fillMaxWidth().card(radius = 26.dp)) {
        currencies.forEachIndexed { i, currency ->
            val on = currency.code == selection
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(62.dp)
                    .pressable(0.98f, role = Role.RadioButton) { onChoose(currency) }
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                CurrencyBadge(currency)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text(currency.name, style = Type.personName, color = Palette.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(currency.code, style = style(Face.medium, 13f), color = Palette.gray)
                }
                if (on) Icon(Icons.Rounded.CheckCircle, null, tint = Palette.pink, modifier = Modifier.size(24.dp))
            }
            if (i < currencies.lastIndex) RowDivider()
        }
    }
}

// Lingua

/**
 * La lingua dell'interfaccia. Ogni voce è scritta nella propria lingua, così si riconosce
 * anche arrivando qui per sbaglio con l'app in una lingua che non si capisce.
 */
@Composable
fun LanguagePickerView(onDone: () -> Unit) {
    val current = I18n.language
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        FlowHeader(t("language.title"), subtitle = t("language.subtitle"))
        LazyColumn(
            Modifier
                .fillMaxSize()
                .topFade(14.dp),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 30.dp),
        ) {
            item {
                Column(Modifier.fillMaxWidth().card(radius = 26.dp)) {
                    AppLanguage.entries.forEachIndexed { i, language ->
                        val on = language == current
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .pressable(0.98f, role = Role.RadioButton) {
                                    Haptics.select()
                                    I18n.select(language)
                                    onDone()
                                }
                                .heightIn(min = 62.dp)
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            LanguageBadge(language)
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                                Text(language.nativeName, style = Type.personName, color = Palette.ink)
                                val subtitle = language.localizedName
                                if (subtitle != null) Text(subtitle, style = style(Face.medium, 13f), color = Palette.gray)
                            }
                            if (on) Icon(Icons.Rounded.CheckCircle, null, tint = Palette.pink, modifier = Modifier.size(24.dp))
                        }
                        if (i < AppLanguage.entries.lastIndex) RowDivider()
                    }
                }
            }
        }
    }
}

/** "IT", "EN", "ZH" — oppure il globo per la lingua del telefono. */
@Composable
private fun LanguageBadge(language: AppLanguage) {
    Box(Modifier.size(36.dp).background(Palette.pinkSoft, CircleShape), contentAlignment = Alignment.Center) {
        if (language == AppLanguage.SYSTEM) {
            Icon(Icons.Outlined.Language, null, tint = Palette.raspberry, modifier = Modifier.size(18.dp))
        } else {
            Text(
                language.raw.take(2).uppercase(),
                style = fixedStyle(Face.bold, 13f),
                color = Palette.raspberry,
            )
        }
    }
}
