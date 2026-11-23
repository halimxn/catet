package id.kantong.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.kantong.app.data.AppData
import id.kantong.app.data.AppViewModel
import id.kantong.app.data.Goal
import id.kantong.app.data.fmt
import id.kantong.app.data.grouped
import id.kantong.app.data.rupiah
import id.kantong.app.ui.Nav
import id.kantong.app.ui.components.CCard
import id.kantong.app.ui.components.CRoundButton
import id.kantong.app.ui.components.CTitle
import id.kantong.app.ui.components.CalendarCard
import id.kantong.app.ui.components.CatIcon
import id.kantong.app.ui.components.CatetSheet
import id.kantong.app.ui.components.CreamButton
import id.kantong.app.ui.components.DangerOutlineButton
import id.kantong.app.ui.components.FloatingCard
import id.kantong.app.ui.components.FormRow
import id.kantong.app.ui.components.Money
import id.kantong.app.ui.components.Overlay
import id.kantong.app.ui.components.RedButton
import id.kantong.app.ui.components.SheetHeader
import id.kantong.app.ui.components.SmallChip
import id.kantong.app.ui.components.pressable
import id.kantong.app.ui.theme.K
import id.kantong.app.ui.theme.Tone

private val GOAL_ICONS = listOf("jar", "coinbag", "motor", "car", "travel", "home", "shield", "edu", "gift", "gadget")
private val QUICK = listOf(50_000L to "+50 rb", 100_000L to "+100 rb", 250_000L to "+250 rb", 500_000L to "+500 rb")
private const val MAX_AMOUNT = 9_999_999_999_999L

private fun Goal.frac(): Float = if (target > 0) (saved.toFloat() / target).coerceIn(0f, 1f) else 0f
private fun Goal.pct(): Long = if (target > 0) (saved * 100 / target).coerceIn(0L, 999L) else 0L
private fun Goal.reached(): Boolean = target > 0 && saved >= target

/**
 * Savings goals (R89):
 * Back + round "+", big title, yellow card showing total saved vs targets,
 * list of goal cards with progress bar & quick deposit/withdraw,
 * no mascot, no pigs.
 */
@Composable
fun GoalsScreen(d: AppData, vm: AppViewModel, nav: Nav) {
    var adding by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Goal?>(null) }
    var moving by remember { mutableStateOf<Pair<Goal, Boolean>?>(null) }

    val saved = d.savedInGoals
    val target = d.goals.sumOf { it.target }
    val allPct = if (target > 0) (saved * 100 / target).coerceIn(0L, 999L) else 0L

    Column(Modifier.fillMaxSize().background(K.Bg)) {
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CRoundButton(Icons.Rounded.ChevronLeft, "Kembali", { nav.back() }, size = 44.dp)
            Spacer(Modifier.weight(1f))
            CRoundButton(Icons.Rounded.Add, "Tambah target", { adding = true }, size = 44.dp, outlined = true)
        }

        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 32.dp),
        ) {
            CTitle("Target Tabungan", subtitle = "Rencana tabungan dan impianmu")
            Spacer(Modifier.height(16.dp))

            // Yellow Summary Card
            CCard(K.Yellow, radius = 28.dp, padding = PaddingValues(horizontal = 20.dp, vertical = 18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Total Terkumpul", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = K.Ink2)
                        Spacer(Modifier.height(4.dp))
                        Money(saved, 36.sp, color = K.Green)
                    }
                    Spacer(Modifier.width(12.dp))
                    CatIcon("jar", 50.dp)
                }
                Spacer(Modifier.height(14.dp))
                Spacer(Modifier.fillMaxWidth().height(1.5.dp).background(K.YellowLine))
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Total Target", fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, color = K.Muted)
                        Spacer(Modifier.height(2.dp))
                        Text(target.rupiah(), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = K.Ink)
                    }
                    Box(Modifier.width(1.5.dp).height(36.dp).background(K.YellowLine))
                    Column(Modifier.weight(1f).padding(start = 20.dp)) {
                        Text("Tercapai", fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, color = K.Muted)
                        Spacer(Modifier.height(2.dp))
                        Text("$allPct%", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = K.Income)
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            if (d.goals.isEmpty()) {
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(K.Card)
                        .padding(horizontal = 20.dp, vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CatIcon("jar", 52.dp)
                    Spacer(Modifier.height(12.dp))
                    Text("Belum ada target tabungan", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = K.Ink)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Mulai menabung untuk dana darurat, liburan, atau barang impian.",
                        fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium, color = K.Muted, textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(16.dp))
                    RedButton("Buat Target Pertama", onClick = { adding = true })
                }
            } else {
                Text("Daftar Target (${d.goals.size})", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = K.Ink)
                Spacer(Modifier.height(10.dp))

                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    d.goals.forEach { g ->
                        GoalCardR89(
                            g = g,
                            onEdit = { editing = g },
                            onDeposit = { moving = g to true },
                            onWithdraw = { moving = g to false },
                        )
                    }
                }
            }
        }
    }

    if (adding || editing != null) {
        GoalSheet(editing, onDismiss = { adding = false; editing = null }) { g ->
            vm.saveGoal(g); adding = false; editing = null
        }
    }

    moving?.let { (g, deposit) ->
        val cur = d.goals.firstOrNull { it.id == g.id } ?: g
        AmountSheet(cur, deposit, onDismiss = { moving = null }) { amount ->
            vm.deposit(cur, if (deposit) amount else -amount); moving = null
        }
    }
}

/** R89 Goal Card: clean cream card with progress bar, amounts, milestone badge, and deposit/withdraw pills. */
@Composable
private fun GoalCardR89(g: Goal, onEdit: () -> Unit, onDeposit: () -> Unit, onWithdraw: () -> Unit) {
    val frac = g.frac()
    val reached = g.reached()

    CCard(K.Card, radius = 24.dp, padding = PaddingValues(16.dp)) {
        Row(Modifier.fillMaxWidth().pressable(onEdit), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(46.dp).clip(RoundedCornerShape(14.dp)).background(K.Tile),
                contentAlignment = Alignment.Center,
            ) {
                CatIcon(g.icon.ifBlank { "jar" }, 30.dp)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(g.name, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = K.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(2.dp))
                Text(
                    g.deadline?.let { "Tenggat: ${fmt(it, "d MMM yyyy")}" } ?: "Tanpa tenggat",
                    fontSize = 12.5.sp, fontWeight = FontWeight.Medium, color = K.Muted, maxLines = 1,
                )
            }
            Spacer(Modifier.width(8.dp))
            Text("${g.pct()}%", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = if (reached) K.Green else K.Ink)
        }

        Spacer(Modifier.height(14.dp))

        // Progress Track
        Box(
            Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)).background(K.Field),
        ) {
            if (frac > 0f) {
                Box(Modifier.fillMaxWidth(frac).fillMaxHeight().clip(RoundedCornerShape(5.dp)).background(if (reached) K.Green else K.Gold))
            }
        }

        Spacer(Modifier.height(8.dp))

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(g.saved.rupiah(), fontSize = 14.5.sp, fontWeight = FontWeight.Bold, color = K.Ink)
            Spacer(Modifier.width(6.dp))
            Text("dari ${g.target.rupiah()}", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = K.Muted, modifier = Modifier.weight(1f))
            if (reached) {
                SmallChip("Tercapai! 🎉", K.SageCard)
            }
        }

        Spacer(Modifier.height(14.dp))
        Spacer(Modifier.fillMaxWidth().height(1.dp).background(K.Line))
        Spacer(Modifier.height(10.dp))

        // Actions
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.height(34.dp).clip(RoundedCornerShape(12.dp)).background(K.Green).pressable(onDeposit)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("+ Setor", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = K.OnGreen)
            }
            if (g.saved > 0) {
                Spacer(Modifier.width(8.dp))
                Box(
                    Modifier.height(34.dp).clip(RoundedCornerShape(12.dp)).background(K.Tile)
                        .border(1.dp, K.Line, RoundedCornerShape(12.dp)).pressable(onWithdraw)
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("− Tarik", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = K.Ink)
                }
            }
            Spacer(Modifier.weight(1f))
            Text(
                "Ubah ›", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = K.Muted,
                modifier = Modifier.pressable(onEdit).padding(vertical = 4.dp, horizontal = 6.dp),
            )
        }
    }
}

/** Setor / Tarik bottom sheet. */
@Composable
private fun AmountSheet(g: Goal, deposit: Boolean, onDismiss: () -> Unit, onConfirm: (Long) -> Unit) {
    var digits by remember { mutableStateOf("") }
    val amount = digits.toLongOrNull() ?: 0L
    val focus = remember { FocusRequester() }

    Overlay(onDismiss) {
        CatetSheet {
            LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
            SheetHeader(if (deposit) "Setor Tabungan" else "Tarik Tabungan", onDismiss)
            Spacer(Modifier.height(8.dp))

            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
                Text(
                    "Target \"${g.name}\" · Terkumpul ${g.saved.rupiah()} dari ${g.target.rupiah()}",
                    fontSize = 13.sp, fontWeight = FontWeight.Medium, color = K.Muted,
                )
                Spacer(Modifier.height(16.dp))

                // Big grouped amount field
                Box(
                    Modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(16.dp)).background(K.Field)
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    val display = if (digits.isNotEmpty()) digits.toLongOrNull()?.grouped() ?: digits else ""
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Rp", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = K.Ink2)
                        Spacer(Modifier.width(8.dp))
                        if (display.isEmpty()) {
                            Text("0", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = K.Muted)
                        }
                        BasicTextField(
                            value = TextFieldValue(display, TextRange(display.length)),
                            onValueChange = { v -> digits = v.text.filter { it.isDigit() }.trimStart('0').take(12) },
                            singleLine = true,
                            textStyle = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = K.Ink),
                            cursorBrush = SolidColor(K.Green),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth().focusRequester(focus),
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Quick add chips
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    QUICK.forEach { (value, label) ->
                        Box(
                            Modifier.weight(1f).height(36.dp).clip(RoundedCornerShape(12.dp)).background(K.Tile)
                                .border(1.dp, K.Line, RoundedCornerShape(12.dp))
                                .pressable { digits = (amount + value).coerceAtMost(MAX_AMOUNT).toString() },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(label, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = K.Ink, maxLines = 1)
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CreamButton("Batal", Modifier.weight(1f), onClick = onDismiss)
                    RedButton("Simpan", Modifier.weight(1f), enabled = amount > 0) { onConfirm(amount) }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

/** Add or edit goal sheet. */
@Composable
private fun GoalSheet(goal: Goal?, onDismiss: () -> Unit, onSave: (Goal) -> Unit) {
    var name by remember { mutableStateOf(goal?.name ?: "") }
    var targetDigits by remember { mutableStateOf(goal?.target?.takeIf { it > 0 }?.toString() ?: "") }
    var icon by remember { mutableStateOf(goal?.icon ?: "jar") }
    var toneName by remember { mutableStateOf(goal?.tone ?: Tone.MINT.name) }
    var deadline by remember { mutableStateOf(goal?.deadline) }
    var picking by remember { mutableStateOf(false) }
    val targetValue = targetDigits.toLongOrNull() ?: 0L

    Overlay(onDismiss) {
        CatetSheet {
            SheetHeader(if (goal == null) "Target Baru" else "Ubah Target", onDismiss)
            Spacer(Modifier.height(12.dp))

            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Name Field
                Column {
                    Text("Nama Target", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = K.Muted, modifier = Modifier.padding(start = 4.dp, bottom = 4.dp))
                    Box(
                        Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(16.dp)).background(K.Field).padding(horizontal = 14.dp),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        if (name.isEmpty()) Text("Misal: Dana Darurat, Laptop", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = K.Muted)
                        BasicTextField(
                            value = name,
                            onValueChange = { name = it.take(40) },
                            singleLine = true,
                            textStyle = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold, color = K.Ink),
                            cursorBrush = SolidColor(K.Green),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                // Target Amount Field
                Column {
                    Text("Jumlah Target", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = K.Muted, modifier = Modifier.padding(start = 4.dp, bottom = 4.dp))
                    Box(
                        Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(16.dp)).background(K.Field).padding(horizontal = 14.dp),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        val display = if (targetDigits.isNotEmpty()) targetDigits.toLongOrNull()?.grouped() ?: targetDigits else ""
                        if (display.isEmpty()) Text("0", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = K.Muted)
                        BasicTextField(
                            value = TextFieldValue(display, TextRange(display.length)),
                            onValueChange = { v -> targetDigits = v.text.filter { it.isDigit() }.trimStart('0').take(12) },
                            singleLine = true,
                            textStyle = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = K.Ink),
                            cursorBrush = SolidColor(K.Green),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                // Deadline Row
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(K.Field)
                        .pressable { picking = true }.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Tenggat Waktu", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = K.Muted)
                        Spacer(Modifier.height(2.dp))
                        Text(deadline?.let { fmt(it, "d MMMM yyyy") } ?: "Tanpa tenggat", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = K.Ink)
                    }
                    if (deadline != null) {
                        Text(
                            "Hapus", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = K.Danger,
                            modifier = Modifier.pressable { deadline = null }.padding(end = 8.dp),
                        )
                    }
                    Text("Pilih ›", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = K.Green)
                }

                // Icon Picker
                Column {
                    Text("Pilih Ikon", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = K.Muted, modifier = Modifier.padding(start = 4.dp, bottom = 4.dp))
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GOAL_ICONS.forEach { key ->
                            val on = key == icon
                            Box(
                                Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(if (on) K.Yellow else K.Field)
                                    .border(if (on) 2.dp else 0.dp, if (on) K.Green else Color.Transparent, RoundedCornerShape(14.dp))
                                    .pressable { icon = key },
                                contentAlignment = Alignment.Center,
                            ) {
                                CatIcon(key, 30.dp)
                            }
                        }
                    }
                }

                // Actions
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CreamButton("Batal", Modifier.weight(1f), onClick = onDismiss)
                    RedButton("Simpan", Modifier.weight(1f), enabled = name.isNotBlank() && targetValue > 0) {
                        onSave(
                            Goal(
                                id = goal?.id ?: 0,
                                name = name.trim(),
                                icon = icon,
                                tone = toneName,
                                target = targetValue,
                                saved = goal?.saved ?: 0,
                                deadline = deadline,
                            ),
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    if (picking) {
        CalendarCard(deadline ?: System.currentTimeMillis(), onDismiss = { picking = false }, withTime = false) {
            deadline = it; picking = false
        }
    }
}
