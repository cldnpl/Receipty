package com.cldnpl.receipty.views

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.cldnpl.receipty.localization.t
import com.cldnpl.receipty.models.AppModel
import com.cldnpl.receipty.theme.DivideIcon
import com.cldnpl.receipty.theme.IconBadge
import com.cldnpl.receipty.theme.Metrics
import com.cldnpl.receipty.theme.Palette
import com.cldnpl.receipty.theme.SheetSurface
import com.cldnpl.receipty.theme.Type
import com.cldnpl.receipty.theme.hex
import com.cldnpl.receipty.theme.pressable
import com.cldnpl.receipty.theme.softShadow

/** Il foglio "Nuovo conto": le due sole strade dell'app. */
@Composable
fun NewBillSheet(app: AppModel) {
    SheetSurface(onDismiss = { app.showingNewBill = false }, showHandle = false) {
        Column(Modifier.padding(start = 22.dp, end = 22.dp, bottom = 5.dp)) {
            Box(Modifier.fillMaxWidth().padding(top = 14.5.dp), contentAlignment = Alignment.Center) {
                Box(Modifier.size(40.dp, 4.dp).background(Palette.handle, CircleShape))
            }
            Text(
                t("newBill.title"),
                style = Type.sheetTitle,
                color = Palette.ink,
                modifier = Modifier
                    .padding(top = 24.dp, start = 6.dp)
                    .semantics { heading() },
            )
            Column(Modifier.padding(top = 18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OptionCard(
                    Icons.Outlined.PhotoCamera,
                    t("newBill.scan.title"),
                    t("newBill.scan.subtitle"),
                    prominent = true,
                    modifier = Modifier.coachTarget(CoachTarget.SCAN_OPTION),
                ) { app.startScan() }
                OptionCard(
                    DivideIcon,
                    t("newBill.manual.title"),
                    t("newBill.manual.subtitle"),
                    prominent = false,
                    modifier = Modifier.coachTarget(CoachTarget.MANUAL_OPTION),
                ) { app.startEqualSplit() }
            }
        }
    }
}

@Composable
private fun OptionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    prominent: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(Metrics.cardRadius)
    Row(
        modifier
            .fillMaxWidth()
            .height(90.5.dp)
            .pressable(onClick = onClick)
            .softShadow(
                shape,
                if (prominent) Palette.pink else Palette.shadow,
                if (prominent) 0.35f else 0.07f,
                if (prominent) 18.dp else 16.dp,
                8.dp,
            )
            .background(if (prominent) Palette.pink else Palette.card, shape)
            .padding(start = 18.5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(17.dp),
    ) {
        IconBadge(icon, size = 52.dp, inverted = prominent)
        Column(verticalArrangement = Arrangement.spacedBy(4.5.dp)) {
            Text(title, style = Type.optionTitle, color = if (prominent) Color.White else Palette.ink)
            Text(subtitle, style = Type.optionSub, color = if (prominent) hex(0xFFEAF6) else Palette.gray)
        }
    }
}
