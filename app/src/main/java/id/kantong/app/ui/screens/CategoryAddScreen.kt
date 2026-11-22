package id.kantong.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.kantong.app.data.AppData
import id.kantong.app.data.AppViewModel
import id.kantong.app.data.Category
import id.kantong.app.ui.Nav
import id.kantong.app.ui.components.CCard
import id.kantong.app.ui.components.CRoundButton
import id.kantong.app.ui.components.CSection
import id.kantong.app.ui.components.CTitle
import id.kantong.app.ui.components.CatIcon
import id.kantong.app.ui.components.RedButton
import id.kantong.app.ui.components.SmallChip
import id.kantong.app.ui.components.pressable
import id.kantong.app.ui.theme.K
import id.kantong.app.ui.theme.Outfit
import id.kantong.app.ui.theme.Tone

/** Icon library for categories, grouped into sticker sections. */
private val ICON_SECTIONS = listOf(
    "Makanan & Minuman" to listOf("food", "rice", "coffee", "snack", "fruit"),
    "Transportasi" to listOf("transport", "motor", "car", "train", "fuel", "parking", "travel"),
    "Belanja" to listOf("shopping", "clothes", "beauty", "gadget", "phone", "gift"),
    "Rumah & Keluarga" to listOf("home", "bill", "wifi", "bolt", "water", "laundry", "family", "baby", "pet"),
    "Kesehatan & Hiburan" to listOf("health", "medicine", "sport", "fun", "game", "music", "edu"),
    "Keuangan" to listOf("salary", "business", "wallet", "bank", "invest", "gold", "tax", "charity", "coinbag", "jar"),
    "Lainnya" to listOf("other", "shield", "ledger", "bookmark"),
)

/**
 * Add / edit a category or subcategory (R89):
 * Back button, big title, yellow card with live icon preview & name field,
 * tone color selection, grouped icons in cream cards, and a green save button at the bottom.
 */
@Composable
fun CategoryAddScreen(d: AppData, vm: AppViewModel, nav: Nav, kind: String, editId: Long, parentId: Long) {
    val existing = if (editId > 0) d.category(editId) else null
    val parentKey = existing?.parentId ?: parentId
    val parent = if (parentKey > 0) d.category(parentKey) else null

    var name by rememberSaveable { mutableStateOf(existing?.name ?: "") }
    var icon by rememberSaveable { mutableStateOf(existing?.icon ?: "other") }
    var toneName by rememberSaveable {
        mutableStateOf(existing?.let { Tone.of(it.tone).name } ?: parent?.let { Tone.of(it.tone).name } ?: Tone.SAGE.name)
    }
    val tone = Tone.of(toneName)

    fun save() {
        val k = existing?.kind ?: kind
        val siblings = d.categories.filter { it.kind == k && it.parentId == parentKey && it.id != editId }
        vm.saveCategory(
            Category(
                id = existing?.id ?: 0,
                name = name.trim(),
                icon = icon,
                tone = toneName,
                kind = k,
                budget = 0,
                sortOrder = existing?.sortOrder ?: ((siblings.maxOfOrNull { it.sortOrder } ?: -1) + 1),
                parentId = existing?.parentId ?: parentId,
            ),
        )
        nav.back()
    }

    val pageTitle = when {
        existing != null -> if (parent != null) "Ubah subkategori" else "Ubah kategori"
        parent != null -> "Subkategori baru"
        else -> "Kategori baru"
    }
    val subtitle = when {
        parent != null -> "Subkategori untuk ${parent.name}"
        (existing?.kind ?: kind) == "out" -> "Kategori pengeluaran"
        else -> "Kategori pemasukan"
    }

    Column(Modifier.fillMaxSize().background(K.Bg).imePadding()) {
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CRoundButton(Icons.Rounded.ChevronLeft, "Kembali", { nav.back() }, size = 44.dp)
        }

        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            CTitle(pageTitle, subtitle = subtitle)
            Spacer(Modifier.height(16.dp))

            // Yellow preview & name card
            CCard(K.Yellow, radius = 28.dp, padding = PaddingValues(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(58.dp).clip(RoundedCornerShape(18.dp)).background(tone.base),
                        contentAlignment = Alignment.Center,
                    ) {
                        CatIcon(icon, 40.dp)
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        if (parent != null) {
                            SmallChip(parent.name, K.Tile)
                            Spacer(Modifier.height(4.dp))
                        }
                        Box(
                            Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(14.dp)).background(K.Tile).padding(horizontal = 14.dp),
                            contentAlignment = Alignment.CenterStart,
                        ) {
                            if (name.isEmpty()) {
                                Text(
                                    if (parentKey > 0) "Nama subkategori" else "Nama kategori",
                                    fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = K.Muted, maxLines = 1,
                                )
                            }
                            BasicTextField(
                                value = name,
                                onValueChange = { name = it.take(30) },
                                singleLine = true,
                                textStyle = TextStyle(fontFamily = Outfit, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = K.Ink),
                                cursorBrush = SolidColor(K.Green),
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            // Tone / Color picker
            CSection("Warna")
            Spacer(Modifier.height(8.dp))
            CCard(K.Card, radius = 22.dp, padding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Tone.entries.forEach { t ->
                        val on = t.name == toneName
                        Box(
                            Modifier.size(34.dp).clip(CircleShape).background(t.strong)
                                .border(if (on) 2.5.dp else 0.dp, if (on) K.Green else Color.Transparent, CircleShape)
                                .pressable { toneName = t.name },
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // Icon library sections
            CSection("Ikon")
            Spacer(Modifier.height(8.dp))

            ICON_SECTIONS.forEach { (sectionTitle, keys) ->
                Text(
                    sectionTitle, fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = K.Muted,
                    modifier = Modifier.padding(start = 4.dp, top = 10.dp, bottom = 6.dp),
                )
                CCard(K.Card, radius = 22.dp, padding = PaddingValues(12.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        keys.chunked(5).forEach { row ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally)) {
                                row.forEach { key ->
                                    val on = key == icon
                                    Box(
                                        Modifier.size(54.dp).clip(RoundedCornerShape(16.dp)).background(if (on) K.Yellow else K.Tile)
                                            .border(if (on) 2.dp else 0.dp, if (on) K.Green else Color.Transparent, RoundedCornerShape(16.dp))
                                            .pressable { icon = key },
                                        contentAlignment = Alignment.Center,
                                    ) { CatIcon(key, 38.dp) }
                                }
                                repeat(5 - row.size) { Spacer(Modifier.size(54.dp)) }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
        }

        // Bottom action button
        Box(
            Modifier.fillMaxWidth().background(K.Bg).navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            RedButton("Simpan", Modifier.fillMaxWidth(), enabled = name.isNotBlank()) { save() }
        }
    }
}
