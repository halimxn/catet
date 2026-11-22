package id.kantong.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.kantong.app.data.Account
import id.kantong.app.data.AppData
import id.kantong.app.data.AppViewModel
import id.kantong.app.ui.Nav
import id.kantong.app.ui.components.CRoundButton
import id.kantong.app.ui.components.CTitle
import id.kantong.app.ui.components.CatIcon
import id.kantong.app.ui.components.CreamButton
import id.kantong.app.ui.components.DangerOutlineButton
import id.kantong.app.ui.components.FloatingCard
import id.kantong.app.ui.components.MeowSegmented
import id.kantong.app.ui.components.Overlay
import id.kantong.app.ui.components.RedButton
import id.kantong.app.ui.components.accountIcon
import id.kantong.app.ui.components.pressable
import id.kantong.app.ui.theme.K
import id.kantong.app.ui.theme.Tone

/** Account stickers offered in the form, each with its fixed pastel family. */
private val ACCOUNT_ICONS: List<Pair<String, Tone>> = listOf(
    "wallet" to Tone.PEACH, "cash" to Tone.PEACH, "bank" to Tone.SKY, "ewallet" to Tone.SKY, "card" to Tone.SKY,
    "coinbag" to Tone.PEACH, "safe" to Tone.PEACH, "invest" to Tone.MINT, "gold" to Tone.PEACH, "jar" to Tone.MINT,
    "phone" to Tone.SKY, "business" to Tone.MINT, "home" to Tone.PINK, "shield" to Tone.PINK, "gift" to Tone.PINK,
)

private val TYPES = listOf("cash", "bank", "ewallet")
private val TYPE_LABELS = listOf("Tunai", "Bank", "E-wallet")

/** Type suggested by a sticker (used until the user picks a type himself). */
private fun typeFor(icon: String): String = when (icon) { "bank", "card" -> "bank"; "ewallet", "phone" -> "ewallet"; else -> "cash" }

/**
 * Add (id = 0) or edit an account (R89 form): title, round icon picker, name, type, opening balance
 * (negative = liability) and remark, then Simpan; editing also offers Hapus (only without records).
 */
@Composable
fun AccountEditScreen(d: AppData, vm: AppViewModel, nav: Nav, id: Long) {
    val existing = if (id != 0L) d.account(id) else null
    var icon by rememberSaveable { mutableStateOf(existing?.let { accountIcon(it) } ?: "wallet") }
    var name by rememberSaveable { mutableStateOf(existing?.name ?: "") }
    var remark by rememberSaveable { mutableStateOf(existing?.remark ?: "") }
    var opening by rememberSaveable { mutableStateOf(existing?.openingBalance?.toString() ?: "") }
    var type by rememberSaveable { mutableStateOf(existing?.type ?: typeFor(icon)) }
    var typeTouched by rememberSaveable { mutableStateOf(existing != null) }
    var deleting by remember { mutableStateOf(false) }
    val tone = ACCOUNT_ICONS.firstOrNull { it.first == icon }?.second ?: Tone.of(existing?.tone ?: "PEACH")

    Column(Modifier.fillMaxSize().imePadding()) {
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).statusBarsPadding().padding(horizontal = 20.dp),
        ) {
            Spacer(Modifier.height(10.dp))
            CRoundButton(Icons.Rounded.ChevronLeft, "Kembali", { nav.back() }, size = 42.dp)
            Spacer(Modifier.height(14.dp))
            CTitle(if (existing == null) "Akun baru" else "Ubah akun", subtitle = "Dompet, rekening, atau e-wallet")
            Spacer(Modifier.height(20.dp))

            FieldLabel("Ikon")
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ACCOUNT_ICONS.chunked(5).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        row.forEach { (key, _) ->
                            IconChoice(key, key == icon) {
                                icon = key
                                if (!typeTouched) type = typeFor(key)
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(20.dp))

            FieldLabel("Nama akun")
            InputField(name, { name = it.take(40) }, "Contoh: Dompet, BCA, GoPay")
            Spacer(Modifier.height(16.dp))

            FieldLabel("Jenis")
            MeowSegmented(
                TYPE_LABELS, TYPES.indexOf(type), { type = TYPES[it]; typeTouched = true },
                Modifier.fillMaxWidth(), fill = true,
            )
            Spacer(Modifier.height(16.dp))

            FieldLabel("Saldo awal")
            InputField(
                opening, { v -> opening = v.filterIndexed { i, c -> c.isDigit() || (i == 0 && c == '-') }.take(14) },
                "0", KeyboardType.Number, prefix = "Rp",
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Untuk utang atau kartu kredit, tulis tanda minus (-) di depan nominal.",
                fontSize = 13.sp, fontWeight = FontWeight.Medium, color = K.Muted, lineHeight = 18.sp,
                modifier = Modifier.padding(horizontal = 2.dp),
            )
            Spacer(Modifier.height(16.dp))

            FieldLabel("Keterangan")
            InputField(remark, { remark = it.take(80) }, "Opsional")
            Spacer(Modifier.height(20.dp))
        }
        Row(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (existing != null) DangerOutlineButton("Hapus", Modifier.weight(1f)) { deleting = true }
            else CreamButton("Batal", Modifier.weight(1f)) { nav.back() }
            RedButton("Simpan", Modifier.weight(1f), enabled = name.isNotBlank()) {
                vm.saveAccount(
                    Account(
                        id = id,
                        name = name.trim(),
                        type = type,
                        openingBalance = opening.toLongOrNull() ?: 0L,
                        tone = tone.name,
                        icon = icon,
                        remark = remark.trim(),
                    )
                )
                nav.back()
            }
        }
    }

    if (deleting && existing != null) {
        val hasRecords = d.allTxns.any { it.accountId == id || it.toAccountId == id }
        Overlay({ deleting = false }) {
            FloatingCard {
                Text(if (hasRecords) "Tidak bisa dihapus" else "Hapus akun?", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = K.Ink)
                Spacer(Modifier.height(10.dp))
                Text(
                    if (hasRecords) "Akun ini masih punya catatan, jadi tidak bisa dihapus."
                    else "Akun \"${existing.name}\" akan dihapus. Tindakan ini tidak bisa dibatalkan.",
                    fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = K.Ink2, lineHeight = 21.sp,
                )
                Spacer(Modifier.height(16.dp))
                if (hasRecords) {
                    RedButton("Mengerti", Modifier.fillMaxWidth()) { deleting = false }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        CreamButton("Batal", Modifier.weight(1f)) { deleting = false }
                        DangerOutlineButton("Hapus", Modifier.weight(1f), filled = true) {
                            deleting = false
                            vm.deleteAccount(existing)
                            // leave the form and the (now empty) account detail under it
                            nav.back(); nav.back()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(text, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = K.Ink2, modifier = Modifier.padding(start = 2.dp, bottom = 8.dp))
}

/** 58dp round sticker choice; selected = yellow with a green ring (like the category row in Tambah). */
@Composable
private fun IconChoice(key: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.size(58.dp).clip(CircleShape).background(if (selected) K.Yellow else K.Card)
            .then(if (selected) Modifier.border(2.4.dp, K.Green, CircleShape) else Modifier)
            .pressable(onClick),
        contentAlignment = Alignment.Center,
    ) { CatIcon(key, 32.dp) }
}

/** Cream input field (K.Field, radius 15) with a muted placeholder and an optional small prefix. */
@Composable
private fun InputField(
    value: String, onChange: (String) -> Unit, placeholder: String,
    keyboard: KeyboardType = KeyboardType.Text, prefix: String? = null,
) {
    Row(
        Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(15.dp)).background(K.Field).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (prefix != null) {
            Text(prefix, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = K.Muted, modifier = Modifier.padding(end = 6.dp))
        }
        val style = LocalTextStyle.current.copy(fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = K.Ink)
        BasicTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            textStyle = style,
            keyboardOptions = KeyboardOptions(keyboardType = keyboard),
            cursorBrush = SolidColor(K.Green),
            modifier = Modifier.weight(1f),
            decorationBox = { inner ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (value.isEmpty()) Text(placeholder, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = K.Muted, maxLines = 1)
                    inner()
                }
            },
        )
    }
}
