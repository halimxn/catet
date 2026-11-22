package id.kantong.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.kantong.app.data.AppData
import id.kantong.app.data.AppViewModel
import id.kantong.app.data.Category
import id.kantong.app.ui.Nav
import id.kantong.app.ui.R
import id.kantong.app.ui.components.CCard
import id.kantong.app.ui.components.CGroupLabel
import id.kantong.app.ui.components.CList
import id.kantong.app.ui.components.CListRow
import id.kantong.app.ui.components.CRoundButton
import id.kantong.app.ui.components.CTitle
import id.kantong.app.ui.components.CatIcon
import id.kantong.app.ui.components.CatetSheet
import id.kantong.app.ui.components.CreamButton
import id.kantong.app.ui.components.DangerOutlineButton
import id.kantong.app.ui.components.MeowSegmented
import id.kantong.app.ui.components.Overlay
import id.kantong.app.ui.components.RedButton
import id.kantong.app.ui.components.pressable
import id.kantong.app.ui.theme.K

/**
 * Category management (R89): back + round "+", big title, Pengeluaran / Pemasukan segmented,
 * a yellow count card, then the main categories in a cream list. Tapping one opens a sheet with
 * its subcategories, "Tambah subkategori", Hapus and Ubah.
 */
@Composable
fun CategoriesScreen(d: AppData, vm: AppViewModel, nav: Nav) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var openId by remember { mutableStateOf<Long?>(null) }
    val kind = if (tab == 0) "out" else "in"
    val mains = d.topCategories(kind).sortedBy { it.sortOrder }
    val subCount = mains.sumOf { d.subsOf(it.id).size }
    val kindLabel = if (kind == "out") "pengeluaran" else "pemasukan"

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 20.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            CRoundButton(Icons.Rounded.ChevronLeft, "Kembali", { nav.back() }, size = 44.dp)
            Spacer(Modifier.weight(1f))
            CRoundButton(Icons.Rounded.Add, "Tambah kategori", { nav.go(R.categoryAdd(kind)) }, size = 44.dp, outlined = true)
        }
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 32.dp),
        ) {
            CTitle("Kategori", subtitle = "Atur kategori dan subkategori")
            Spacer(Modifier.height(16.dp))
            MeowSegmented(listOf("Pengeluaran", "Pemasukan"), tab, { tab = it }, Modifier.fillMaxWidth(), fill = true)
            Spacer(Modifier.height(14.dp))

            CCard(K.Yellow, radius = 28.dp, padding = PaddingValues(horizontal = 20.dp, vertical = 18.dp)) {
                Text("Kategori $kindLabel", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = K.Ink2)
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CountStat("Kategori utama", mains.size, Modifier.weight(1f))
                    Box(Modifier.padding(horizontal = 16.dp).width(1.5.dp).height(46.dp).background(K.YellowLine))
                    CountStat("Subkategori", subCount, Modifier.weight(1f))
                }
            }

            Spacer(Modifier.height(20.dp))
            CGroupLabel("Kategori utama", right = if (mains.isNotEmpty()) "Ketuk untuk detail" else null)
            Spacer(Modifier.height(8.dp))
            if (mains.isEmpty()) {
                Text(
                    "Belum ada kategori $kindLabel. Ketuk tombol + untuk menambah.",
                    fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold, color = K.Muted,
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(K.Card)
                        .pressable { nav.go(R.categoryAdd(kind)) }.padding(16.dp),
                )
            } else {
                CList {
                    mains.forEachIndexed { i, c ->
                        val subs = d.subsOf(c.id).size
                        CListRow(
                            c.icon, c.name, if (subs > 0) "$subs subkategori" else "Belum ada subkategori",
                            divider = i > 0, onClick = { openId = c.id },
                        )
                    }
                }
            }
        }
    }

    openId?.let { id -> d.category(id) }?.let { main ->
        CategorySheet(
            d, main,
            onDismiss = { openId = null },
            onOpenSub = { sub -> openId = null; nav.go(R.categoryAdd(main.kind, sub.id, main.id)) },
            onAddSub = { openId = null; nav.go(R.categoryAdd(main.kind, 0, main.id)) },
            onEdit = { openId = null; nav.go(R.categoryAdd(main.kind, main.id, 0)) },
            onDelete = { vm.deleteCategory(main); openId = null },
        )
    }
}

/** Label over a big green count in the yellow card. */
@Composable
private fun CountStat(label: String, value: Int, modifier: Modifier) {
    Column(modifier) {
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = K.Muted, maxLines = 1)
        Text(value.toString(), fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = K.Green, maxLines = 1)
    }
}

/** Bottom sheet: the main category, its subcategories, "Tambah subkategori", then Hapus (with confirm) / Ubah. */
@Composable
private fun CategorySheet(
    d: AppData, main: Category, onDismiss: () -> Unit, onOpenSub: (Category) -> Unit, onAddSub: () -> Unit,
    onEdit: () -> Unit, onDelete: () -> Unit,
) {
    var confirm by remember(main.id) { mutableStateOf(false) }
    val subs = d.subsOf(main.id)
    Overlay(onDismiss) {
        CatetSheet {
            Row(Modifier.fillMaxWidth().padding(top = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(52.dp).clip(CircleShape).background(K.Card), contentAlignment = Alignment.Center) { CatIcon(main.icon, 36.dp) }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(main.name, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = K.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        (if (main.kind == "in") "Pemasukan" else "Pengeluaran") + " · ${subs.size} subkategori",
                        fontSize = 13.sp, fontWeight = FontWeight.Medium, color = K.Muted, maxLines = 1,
                    )
                }
                Spacer(Modifier.width(10.dp))
                CRoundButton(Icons.Rounded.Close, "Tutup", onDismiss, size = 34.dp)
            }
            Spacer(Modifier.height(18.dp))
            if (confirm) {
                CCard(K.Pink, radius = 22.dp, padding = PaddingValues(16.dp)) {
                    Text(
                        "Hapus \"${main.name}\" beserta subkategorinya? Catatan lama tetap ada sebagai Tanpa kategori.",
                        fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = K.Ink, lineHeight = 21.sp,
                    )
                }
                Spacer(Modifier.height(18.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CreamButton("Batal", Modifier.weight(1f)) { confirm = false }
                    DangerOutlineButton("Hapus", Modifier.weight(1f), filled = true, onClick = onDelete)
                }
            } else {
                Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
                    CGroupLabel("Subkategori")
                    Spacer(Modifier.height(8.dp))
                    CList {
                        subs.forEachIndexed { i, sub ->
                            CListRow(sub.icon, sub.name, divider = i > 0, onClick = { onOpenSub(sub) })
                        }
                        AddSubRow(divider = subs.isNotEmpty(), onClick = onAddSub)
                    }
                    Spacer(Modifier.height(18.dp))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    DangerOutlineButton("Hapus", Modifier.weight(1f)) { confirm = true }
                    RedButton("Ubah", Modifier.weight(1f), onClick = onEdit)
                }
            }
        }
    }
}

/** Last row of the subcategory list: round "+" and a green label. */
@Composable
private fun AddSubRow(divider: Boolean, onClick: () -> Unit) {
    Column {
        if (divider) Box(Modifier.fillMaxWidth().height(1.dp).background(K.Line))
        Row(
            Modifier.fillMaxWidth().height(62.dp).pressable(onClick),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(13.dp),
        ) {
            Box(Modifier.size(32.dp).clip(CircleShape).background(K.Tile), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Add, null, tint = K.Green, modifier = Modifier.size(20.dp))
            }
            Text("Tambah subkategori", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = K.Green, maxLines = 1)
        }
    }
}
