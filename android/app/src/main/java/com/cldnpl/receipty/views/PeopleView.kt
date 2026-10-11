package com.cldnpl.receipty.views

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cldnpl.receipty.localization.t
import com.cldnpl.receipty.models.AppModel
import com.cldnpl.receipty.models.BillDraft
import com.cldnpl.receipty.models.BillSource
import com.cldnpl.receipty.models.Person
import com.cldnpl.receipty.models.Route
import com.cldnpl.receipty.theme.Avatar
import com.cldnpl.receipty.theme.BottomBar
import com.cldnpl.receipty.theme.Caption
import com.cldnpl.receipty.theme.Face
import com.cldnpl.receipty.theme.FlowHeader
import com.cldnpl.receipty.theme.FlowScaffold
import com.cldnpl.receipty.theme.Haptics
import com.cldnpl.receipty.theme.Palette
import com.cldnpl.receipty.theme.PrimaryButton
import com.cldnpl.receipty.theme.Type
import com.cldnpl.receipty.theme.dashedBorder
import com.cldnpl.receipty.theme.pressable
import com.cldnpl.receipty.theme.requestWhenReady
import com.cldnpl.receipty.theme.softShadow
import com.cldnpl.receipty.theme.style
import com.cldnpl.receipty.theme.topFade
import kotlinx.coroutines.delay

/** "Chi c'è al tavolo?": nomi scritti una volta, poi basta toccarli. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PeopleView(app: AppModel, draft: BillDraft) {
    var name by rememberSaveable { mutableStateOf("") }
    var nameFocused by remember { mutableStateOf(false) }
    val focus = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val listState = rememberLazyListState()
    val recents = app.store.recentNames(draft.people.map { it.name })

    val back: () -> Unit = {
        if (draft.source == BillSource.equal) app.finish() else app.path.removeAt(app.path.lastIndex)
    }
    BackHandler { back() }

    fun add(raw: String) {
        if (draft.addPerson(raw) == null) return
        Haptics.tap()
        name = ""
    }

    // Il nome appena aggiunto resta visibile anche con la tastiera aperta.
    var lastCount by remember { mutableIntStateOf(draft.people.size) }
    LaunchedEffect(draft.people.size) {
        if (draft.people.size > lastCount) listState.animateScrollToItem(draft.people.size)
        lastCount = draft.people.size
    }
    LaunchedEffect(Unit) {
        if (draft.people.isEmpty()) {
            delay(400)
            focus.requestWhenReady { nameFocused }
        }
    }

    val caption = when (val n = draft.people.size) {
        0 -> t("people.caption.none")
        1 -> t("people.caption.one")
        else -> t("people.caption.many", n)
    }

    FlowScaffold(
        header = { FlowHeader(t("people.title"), subtitle = t("people.subtitle"), backAction = back) },
        bottomBar = {
            BottomBar {
                PrimaryButton(
                    t(if (draft.source.isItemized) "people.assignItems" else "people.whoPaid"),
                    enabled = draft.people.size >= 2,
                    onClick = {
                        focusManager.clearFocus()
                        app.path.add(if (draft.source.isItemized) Route.Assign else Route.Payments)
                    },
                )
                Caption(caption, Modifier.padding(top = 13.dp))
            }
        },
    ) { bottomInset ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 4.dp)
                .topFade(14.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 20.dp, end = 20.dp, top = 18.dp, bottom = bottomInset + 16.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item(key = "field") {
                NameField(
                    name,
                    onChange = { name = it },
                    focus = focus,
                    onFocusChanged = { nameFocused = it },
                    onSubmit = {
                        // Invio aggiunge il nome e la tastiera resta su per il prossimo;
                        // con il campo vuoto invece la chiude.
                        if (name.isBlank()) focusManager.clearFocus() else add(name)
                    },
                    onAdd = {
                        if (name.isBlank()) Haptics.select() else add(name)
                        runCatching { focus.requestFocus() }
                    },
                    modifier = Modifier.padding(bottom = 9.5.dp),
                )
            }
            items(draft.people, key = { it.id }) { person ->
                PersonRow(person, Modifier.animateItem()) { draft.remove(person) }
            }
            if (recents.isNotEmpty()) {
                item(key = "recents") {
                    androidx.compose.foundation.layout.Column(Modifier.animateItem()) {
                        Text(
                            t("people.recent"),
                            style = style(Face.bold, 13f, -0.01f),
                            color = Palette.raspberry,
                            modifier = Modifier.padding(top = 8.dp, start = 6.dp),
                        )
                        FlowRow(
                            Modifier.padding(top = 9.5.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            for (recent in recents) {
                                val label = t("people.addRecent", recent)
                                Box(
                                    Modifier
                                        .height(39.dp)
                                        .pressable(0.94f) { add(recent) }
                                        .background(Palette.chipFill, CircleShape)
                                        .dashedBorder(Palette.dashed)
                                        .padding(horizontal = 16.5.dp)
                                        .semantics { contentDescription = label },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text("+ $recent", style = style(Face.medium, 15.5f), color = Palette.chipText, maxLines = 1)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NameField(
    name: String,
    onChange: (String) -> Unit,
    focus: FocusRequester,
    onFocusChanged: (Boolean) -> Unit,
    onSubmit: () -> Unit,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val textStyle = style(Face.regular, 17f).copy(color = Palette.ink)
    Row(
        modifier
            .fillMaxWidth()
            .height(59.dp)
            .softShadow(CircleShape, Palette.shadow, 0.07f, 16.dp, 7.dp)
            .background(Palette.card, CircleShape)
            .padding(end = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        BasicTextField(
            value = name,
            onValueChange = onChange,
            singleLine = true,
            textStyle = textStyle,
            cursorBrush = SolidColor(Palette.pink),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                autoCorrectEnabled = false,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(onDone = { onSubmit() }),
            modifier = Modifier
                .weight(1f)
                .padding(start = 22.5.dp)
                .focusRequester(focus)
                .onFocusChanged { onFocusChanged(it.isFocused) },
            decorationBox = { inner ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (name.isEmpty()) Text(t("people.name"), style = textStyle.copy(color = Palette.placeholder))
                    inner()
                }
            },
        )
        Box(
            Modifier
                .height(47.5.dp)
                .pressable(0.94f, onClick = onAdd)
                .background(Palette.inverseFill, CircleShape)
                .padding(horizontal = 17.5.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(t("common.add"), style = Type.pillLabel, color = Palette.inverseText)
        }
    }
}

@Composable
private fun PersonRow(person: Person, modifier: Modifier = Modifier, onRemove: () -> Unit) {
    val label = t("people.remove", person.name)
    Row(
        modifier
            .fillMaxWidth()
            .height(60.dp)
            .background(Palette.cardSoft, RoundedCornerShape(24.dp))
            .padding(start = 12.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(11.5.dp),
    ) {
        Avatar(person, size = 38.dp)
        Text(person.name, style = Type.personNameStrong, color = Palette.ink, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        Box(
            Modifier
                .size(44.dp)
                .pressable(0.9f) {
                    Haptics.tap()
                    onRemove()
                }
                .semantics { contentDescription = label },
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.Close, null, tint = Palette.muted, modifier = Modifier.size(19.dp))
        }
    }
}
