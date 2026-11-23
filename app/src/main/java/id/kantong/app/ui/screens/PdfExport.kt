package id.kantong.app.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import id.kantong.app.data.AppData
import id.kantong.app.data.PdfReport
import id.kantong.app.data.Prefs
import id.kantong.app.data.fmt
import id.kantong.app.data.startOfMonth
import id.kantong.app.ui.components.CPicker
import id.kantong.app.ui.components.CatetSheet
import id.kantong.app.ui.components.CreamButton
import id.kantong.app.ui.components.MeowToggle
import id.kantong.app.ui.components.Overlay
import id.kantong.app.ui.components.RedButton
import id.kantong.app.ui.components.SelOpt
import id.kantong.app.ui.components.SheetHeader
import id.kantong.app.ui.theme.K
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Calendar

/** "Ekspor PDF": month, ledger, include the transaction list, then Simpan (file picker) or Bagikan (share sheet). */
@Composable
fun PdfExportSheet(d: AppData, initialMonth: Long, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var month by remember { mutableLongStateOf(startOfMonth(initialMonth)) }
    var ledger by remember { mutableLongStateOf(Prefs.ledger) }
    var withList by remember { mutableStateOf(true) }
    var busy by remember { mutableStateOf<String?>(null) }
    val months = (0 until 12).map { i -> Calendar.getInstance().apply { timeInMillis = startOfMonth(); add(Calendar.MONTH, -i) }.timeInMillis }
    val info = remember(month, ledger, withList, d) { PdfReport.info(d, month, ledger, withList) }
    val fileName = "catet-laporan-${fmt(month, "yyyy-MM")}.pdf"

    val save = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        busy = "Membuat PDF…"
        scope.launch {
            val ok = withContext(Dispatchers.IO) { runCatching { ctx.contentResolver.openOutputStream(uri)?.use { PdfReport.write(ctx, d, month, ledger, withList, it) } }.isSuccess }
            busy = if (ok) "✓ Tersimpan" else "Gagal menyimpan PDF"
        }
    }

    Overlay(onDismiss) {
        CatetSheet {
            SheetHeader("Ekspor PDF", onDismiss, size = 21)
            Text("Laporan bulanan siap dicetak atau dibagikan", fontSize = 13.sp, color = K.Muted)
            Column(
                Modifier.padding(top = 14.dp).fillMaxWidth().clip(RoundedCornerShape(26.dp)).background(K.Yellow).padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CPicker("Periode", months.map { SelOpt(it.toString(), fmt(it, "MMMM yyyy")) }, month.toString(), title = "Pilih bulan") { month = it.toLong() }
                CPicker(
                    "Buku kas", listOf(SelOpt("0", "Semua buku")) + d.activeLedgers.map { SelOpt(it.id.toString(), it.name, icon = it.icon) },
                    ledger.toString(), title = "Pilih buku kas",
                ) { ledger = it.toLong() }
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(K.Tile).padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Sertakan daftar transaksi", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = K.Ink)
                        Text("${info.transactions} transaksi · ${info.pages} halaman", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = K.Muted)
                    }
                    Spacer(Modifier.width(10.dp))
                    MeowToggle(withList) { withList = it }
                }
            }
            busy?.let { Text(it, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = if (it.startsWith("✓")) K.ChartIn else K.OnYellow, modifier = Modifier.padding(top = 10.dp)) }
            Row(Modifier.fillMaxWidth().padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CreamButton("Simpan", Modifier.weight(1f)) { save.launch(fileName) }
                RedButton("Bagikan", Modifier.weight(1f)) {
                    busy = "Membuat PDF…"
                    scope.launch {
                        val f = File(File(ctx.externalCacheDir ?: ctx.cacheDir, "pdf").apply { mkdirs() }, fileName)
                        val ok = withContext(Dispatchers.IO) { runCatching { f.outputStream().use { PdfReport.write(ctx, d, month, ledger, withList, it) } }.isSuccess }
                        if (!ok) { busy = "Gagal membuat PDF"; return@launch }
                        busy = null
                        runCatching {
                            val uri = FileProvider.getUriForFile(ctx, "id.kantong.app.files", f)
                            ctx.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("application/pdf").putExtra(Intent.EXTRA_STREAM, uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION), "Bagikan laporan"))
                        }
                    }
                }
            }
        }
    }
}
