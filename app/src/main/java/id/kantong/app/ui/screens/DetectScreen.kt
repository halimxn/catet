package id.kantong.app.ui.screens

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import id.kantong.app.data.AppData
import id.kantong.app.data.AppViewModel
import id.kantong.app.data.DetectApp
import id.kantong.app.data.DetectApps
import id.kantong.app.data.Detected
import id.kantong.app.data.Detector
import id.kantong.app.data.Prefs
import id.kantong.app.data.TxnListener
import id.kantong.app.data.fmt
import id.kantong.app.data.grouped
import id.kantong.app.data.startOfMonth
import id.kantong.app.ui.Nav
import id.kantong.app.ui.components.CList
import id.kantong.app.ui.components.CPicker
import id.kantong.app.ui.components.CRoundButton
import id.kantong.app.ui.components.CTitle
import id.kantong.app.ui.components.CatIcon
import id.kantong.app.ui.components.CatetSheet
import id.kantong.app.ui.components.CreamButton
import id.kantong.app.ui.components.MeowToggle
import id.kantong.app.ui.components.Overlay
import id.kantong.app.ui.components.PillMenu
import id.kantong.app.ui.components.PillOption
import id.kantong.app.ui.components.RedButton
import id.kantong.app.ui.components.SelOpt
import id.kantong.app.ui.components.SheetHeader
import id.kantong.app.ui.components.accountIcon
import id.kantong.app.ui.components.anchor
import id.kantong.app.ui.components.pressable
import id.kantong.app.ui.components.rememberAnchor
import id.kantong.app.ui.theme.K
import id.kantong.app.ui.theme.Tone

/** Pastel badge colours for app initials, picked by position so neighbours differ. */
private val badgeTones = listOf(K.SageCard to K.OnSage, K.Pink to K.OnPink, K.Yellow to K.OnYellow)

@Composable
internal fun AppBadge(app: DetectApp?, label: String, size: Int = 38) {
    val i = DetectApps.all.indexOf(app).coerceAtLeast(0) % badgeTones.size
    val (bg, fg) = badgeTones[i]
    Box(Modifier.size(size.dp).clip(RoundedCornerShape((size * 0.34f).dp)).background(bg), contentAlignment = Alignment.Center) {
        Text(app?.short ?: label.take(2).uppercase(), fontSize = (size * 0.3f).sp, fontWeight = FontWeight.Black, color = fg, maxLines = 1)
    }
}

/** "Deteksi otomatis": switch on, grant notification access, pick which apps and accounts. */
@Composable
fun DetectScreen(d: AppData, vm: AppViewModel, nav: Nav) {
    val ctx = LocalContext.current
    var access by remember { mutableStateOf(Detector.hasAccess(ctx)) }
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val obs = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_RESUME) access = Detector.hasAccess(ctx) }
        owner.lifecycle.addObserver(obs)
        onDispose { owner.lifecycle.removeObserver(obs) }
    }
    fun openAccess() {
        val detail = if (Build.VERSION.SDK_INT >= 30) Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS)
            .putExtra(Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME, ComponentName(ctx, TxnListener::class.java).flattenToString()) else null
        runCatching { ctx.startActivity(detail ?: Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }
            .onFailure { runCatching { ctx.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) } }
    }
    val notifPerm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { if (!access) openAccess() }
    fun turnOn() {
        Prefs.detectOn = true
        val needPost = Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        if (needPost) notifPerm.launch(Manifest.permission.POST_NOTIFICATIONS) else if (!access) openAccess()
    }

    val pm = ctx.packageManager
    val installed = remember { DetectApps.all.filter { runCatching { pm.getPackageInfo(it.pkg, 0) }.isSuccess } }
    val others = DetectApps.all.filter { it !in installed }.map { it.name }.distinct()
    val on = Prefs.detectOn
    val active = on && access
    val monthStart = startOfMonth()
    val month = d.detected.filter { it.time >= monthStart }

    Column(Modifier.fillMaxSize().background(K.Bg).verticalScroll(rememberScrollState()).statusBarsPadding().padding(horizontal = 20.dp)) {
        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp)) { CRoundButton(Icons.Rounded.ChevronLeft, "Kembali", { nav.back() }, size = 44.dp) }
        CTitle("Deteksi otomatis", Modifier.padding(top = 8.dp), subtitle = "Dari notifikasi bank & e-wallet")

        // switch card
        Column(Modifier.padding(top = 16.dp).fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(K.Yellow).padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(48.dp).clip(RoundedCornerShape(16.dp)).background(K.Tile), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.NotificationsActive, null, tint = K.ChartOut, modifier = Modifier.size(26.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Baca notifikasi", fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = K.Ink)
                    Text(
                        when { active -> "Aktif · transaksi jadi draf"; on -> "Izin akses belum diberikan"; else -> "Bank & e-wallet jadi draf transaksi" },
                        fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = K.OnYellow,
                    )
                }
                MeowToggle(on) { if (it) turnOn() else Prefs.detectOn = false }
            }
            if (on && !access) {
                Column(Modifier.padding(top = 12.dp).fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(K.Tile).padding(12.dp)) {
                    Text("Nyalakan Catet di halaman Akses notifikasi Android.", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = K.Ink)
                    Text(
                        "Kalau tombolnya terkunci: Info aplikasi › ⋮ › Izinkan setelan terbatas.",
                        fontSize = 12.sp, fontWeight = FontWeight.Medium, color = K.Muted, modifier = Modifier.padding(top = 2.dp),
                    )
                    RedButton("Buka pengaturan", Modifier.padding(top = 10.dp).fillMaxWidth()) { openAccess() }
                }
            } else {
                Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatBox("${month.size}", "terdeteksi bulan ini", Modifier.weight(1f))
                    StatBox("${month.count { it.status == 1 }}", "disimpan", Modifier.weight(1f))
                }
            }
        }

        Text("Aplikasi dipantau", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = K.Muted, modifier = Modifier.padding(start = 2.dp, top = 20.dp, bottom = 8.dp))
        if (installed.isEmpty()) {
            Text(
                "Belum ada aplikasi bank atau e-wallet yang didukung di HP ini.", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = K.Muted,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(K.Card).padding(16.dp),
            )
        } else {
            CList {
                installed.forEachIndexed { i, app -> DetectAppRow(d, app, divider = i > 0) }
            }
        }
        if (others.isNotEmpty()) Text(
            "Juga didukung: " + others.joinToString(", "), fontSize = 12.5.sp, fontWeight = FontWeight.Medium, color = K.Muted,
            modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 8.dp),
        )

        // privacy
        Row(Modifier.padding(top = 16.dp).fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(K.SageCard).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            CatIcon("shield", 30.dp)
            Spacer(Modifier.width(12.dp))
            Text(
                "Diproses di HP saja, tanpa internet. Kode OTP dan promo dilewati, teks notifikasi tidak disimpan.",
                fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = K.OnSage, lineHeight = 18.sp,
            )
        }
        Spacer(Modifier.height(120.dp))
    }
}

@Composable
private fun StatBox(value: String, label: String, modifier: Modifier) {
    Column(modifier.clip(RoundedCornerShape(16.dp)).background(K.Tile).padding(horizontal = 12.dp, vertical = 9.dp)) {
        Text(value, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold, color = K.Ink)
        Text(label, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = K.OnYellow)
    }
}

@Composable
private fun DetectAppRow(d: AppData, app: DetectApp, divider: Boolean) {
    var enabled by remember { mutableStateOf(Prefs.detectEnabled(app.pkg)) }
    var accId by remember { mutableLongStateOf(Prefs.detectAccount(app.pkg)) }
    var menu by remember { mutableStateOf(false) }
    val anchor = rememberAnchor()
    val acc = d.account(accId)
    Column {
        if (divider) Box(Modifier.fillMaxWidth().height(1.dp).background(K.Line))
        Row(Modifier.fillMaxWidth().height(64.dp), verticalAlignment = Alignment.CenterVertically) {
            AppBadge(app, app.name, 36)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f).anchor(anchor).let { if (enabled) it.pressable { menu = true } else it }) {
                Text(app.name, fontSize = 15.5.sp, fontWeight = FontWeight.Bold, color = K.Ink, maxLines = 1)
                Text(
                    if (!enabled) "Tidak dipantau" else "→ " + (acc?.name ?: "Tebak otomatis") + "  ›",
                    fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = if (enabled) K.Green else K.Muted, maxLines = 1,
                )
            }
            MeowToggle(enabled) { enabled = it; Prefs.setDetectEnabled(app.pkg, it) }
        }
    }
    if (menu) {
        PillMenu(anchor.rect, onDismiss = { menu = false }, title = "Masuk ke rekening") {
            PillOption("Tebak otomatis", Tone.PEACH, accId == 0L, { accId = 0L; Prefs.setDetectAccount(app.pkg, 0L); menu = false }, icon = "wallet")
            d.accounts.forEach { a ->
                PillOption(a.name, Tone.of(a.tone), accId == a.id, { accId = a.id; Prefs.setDetectAccount(app.pkg, a.id); menu = false }, icon = accountIcon(a))
            }
        }
    }
}

/** Waiting detections: check category and account, then save or skip. The first one is open for editing. */
@Composable
fun DetectReviewSheet(d: AppData, vm: AppViewModel, onDismiss: () -> Unit) {
    val items = d.pendingDetected
    var open by remember { mutableLongStateOf(items.firstOrNull()?.id ?: 0L) }
    val cat = remember { mutableStateMapOf<Long, Long?>() }
    val acc = remember { mutableStateMapOf<Long, Long>() }
    fun catOf(x: Detected) = if (cat.containsKey(x.id)) cat[x.id] else x.categoryId
    fun accOf(x: Detected) = acc[x.id] ?: x.accountId
    if (items.isEmpty()) { androidx.compose.runtime.LaunchedEffect(Unit) { onDismiss() }; return }
    if (items.none { it.id == open }) open = items.first().id

    Overlay(onDismiss) {
        CatetSheet {
            SheetHeader("Tinjau deteksi", onDismiss, size = 21)
            Text("Periksa kategori, lalu simpan", fontSize = 13.sp, color = K.Muted)
            Column(
                Modifier.padding(top = 12.dp).heightIn(max = 470.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items.forEach { x ->
                    val expanded = x.id == open
                    val app = DetectApps.of(x.pkg)
                    val twin = d.allTxns.firstOrNull { it.amount == x.amount && it.kind == x.kind && kotlin.math.abs(it.time - x.time) < 2 * 86_400_000L }
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(if (expanded) K.Yellow else K.Card).pressable { open = x.id }.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AppBadge(app, x.app, 34)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(x.merchant, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = K.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("${x.app} · ${whenText(x.time)}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = if (expanded) K.OnYellow else K.Muted, maxLines = 1)
                            }
                            Text(
                                (if (x.kind == "in") "+" else "−") + x.amount.grouped(), fontSize = 16.sp, fontWeight = FontWeight.ExtraBold,
                                color = if (x.kind == "in") K.ChartIn else K.Ink, maxLines = 1,
                            )
                        }
                        if (twin != null) Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.ContentCopy, null, tint = K.OnPink, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(5.dp))
                            Text(
                                "Mirip catatan \"${twin.merchant.ifBlank { d.category(twin.categoryId)?.name ?: "tanpa nama" }}\" ${fmt(twin.time, "d MMM")}",
                                fontSize = 12.sp, fontWeight = FontWeight.Bold, color = K.OnPink, maxLines = 1, overflow = TextOverflow.Ellipsis,
                            )
                        }
                        if (expanded) {
                            Column(Modifier.padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                CPicker(
                                    "Kategori", d.categories.filter { it.kind == x.kind }.map { SelOpt(it.id.toString(), it.name, icon = it.icon) },
                                    catOf(x)?.toString() ?: "", title = "Pilih kategori",
                                ) { cat[x.id] = it.toLongOrNull() }
                                CPicker(
                                    "Rekening", d.accounts.map { SelOpt(it.id.toString(), it.name, icon = accountIcon(it)) },
                                    accOf(x).toString(), title = "Pilih rekening",
                                ) { acc[x.id] = it.toLong() }
                            }
                            Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                SmallButton("Lewati", K.Tile, K.OnYellow, Modifier.weight(1f)) { vm.skipDetected(listOf(x.id)) }
                                SmallButton("Simpan", K.Green, Color.White, Modifier.weight(1.4f)) { vm.saveDetected(x, catOf(x), accOf(x)) }
                            }
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CreamButton("Lewati semua", Modifier.weight(1f)) { vm.skipDetected(items.map { it.id }); onDismiss() }
                RedButton(if (items.size > 1) "Simpan semua (${items.size})" else "Simpan", Modifier.weight(1.3f)) {
                    items.forEach { vm.saveDetected(it, catOf(it), accOf(it)) }; onDismiss()
                }
            }
        }
    }
}

@Composable
private fun SmallButton(label: String, bg: Color, fg: Color, modifier: Modifier, onClick: () -> Unit) {
    Box(modifier.height(42.dp).clip(RoundedCornerShape(14.dp)).background(bg).pressable(onClick), contentAlignment = Alignment.Center) {
        Text(label, fontSize = 14.5.sp, fontWeight = FontWeight.Bold, color = fg)
    }
}

/** "hari ini 08.12", "kemarin 19.40", or "21 Nov 10.05". */
internal fun whenText(t: Long): String {
    val day = 86_400_000L
    val today = id.kantong.app.data.startOfDay()
    return when {
        t >= today -> "hari ini " + fmt(t, "HH.mm")
        t >= today - day -> "kemarin " + fmt(t, "HH.mm")
        else -> fmt(t, "d MMM HH.mm")
    }
}
