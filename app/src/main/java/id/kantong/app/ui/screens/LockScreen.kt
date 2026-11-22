package id.kantong.app.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import id.kantong.app.data.Prefs
import id.kantong.app.ui.components.CatetLogo
import id.kantong.app.ui.components.Overlay
import id.kantong.app.ui.components.SheetCard
import id.kantong.app.ui.components.pressable
import id.kantong.app.ui.components.rememberTick
import id.kantong.app.ui.theme.K
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * App lock state for the whole process. Starts locked (when a PIN is set) and locks again after the app
 * was in the background for more than 30 seconds.
 */
object AppLock {
    private const val GRACE_MS = 30_000L

    var unlocked by mutableStateOf(false)
        private set
    private var stoppedAt = 0L
    private var leaving = false

    val enabled: Boolean get() = Prefs.pinHash.isNotEmpty()
    val locked: Boolean get() = enabled && !unlocked

    fun check(pin: String): Boolean = enabled && hash(pin, Prefs.pinSalt) == Prefs.pinHash

    fun unlock(pin: String): Boolean = check(pin).also { if (it) unlocked = true }

    /** Saves a new PIN (salted SHA-256) and keeps the current session unlocked. */
    fun setPin(pin: String) {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }.joinToString("") { "%02x".format(it) }
        Prefs.pinSalt = salt
        Prefs.pinHash = hash(pin, salt)
        unlocked = true
    }

    fun clear() {
        Prefs.pinHash = ""
        Prefs.pinSalt = ""
    }

    /** Call right before opening a system screen (file picker): coming back from it does not lock the app. */
    fun allowLeave() { leaving = true }

    internal fun onStop() { stoppedAt = SystemClock.elapsedRealtime() }

    internal fun onStart() {
        val away = stoppedAt > 0 && SystemClock.elapsedRealtime() - stoppedAt > GRACE_MS
        if (away && !leaving) unlocked = false
        stoppedAt = 0
    }

    internal fun onResume() { leaving = false }

    private fun hash(pin: String, salt: String): String =
        MessageDigest.getInstance("SHA-256").digest((salt + ":" + pin).toByteArray()).joinToString("") { "%02x".format(it) }
}

/**
 * Wrap the app content: when a PIN is set and the app is locked, a full-screen lock screen covers it.
 * The content stays composed underneath, so unlocking shows the app exactly where the user left it.
 */
@Composable
fun LockGate(content: @Composable () -> Unit) {
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, e ->
            when (e) {
                Lifecycle.Event.ON_START -> AppLock.onStart()
                Lifecycle.Event.ON_RESUME -> AppLock.onResume()
                Lifecycle.Event.ON_STOP -> AppLock.onStop()
                else -> {}
            }
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    Box(Modifier.fillMaxSize()) {
        content()
        if (AppLock.locked) LockScreen()
    }
}

@Composable
private fun LockScreen() {
    val ctx = LocalContext.current
    var error by remember { mutableStateOf(false) }
    BackHandler { ctx.findActivity()?.moveTaskToBack(true) }
    Box(
        Modifier.fillMaxSize().background(K.Bg).clickable(remember { MutableInteractionSource() }, null) {},
    ) {
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(1f))
            PinEntry(
                title = "Masukkan PIN",
                message = if (error) "PIN salah, coba lagi" else "catet terkunci",
                error = error,
                big = true,
                header = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CatetLogo(46.dp)
                        Spacer(Modifier.width(10.dp))
                        Text("catet", fontSize = 36.sp, fontWeight = FontWeight.ExtraBold, color = K.Ink, letterSpacing = (-0.5).sp)
                    }
                    Spacer(Modifier.height(28.dp))
                },
            ) { pin ->
                AppLock.unlock(pin).also { error = !it }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

private fun Context.findActivity(): Activity? {
    var c: Context? = this
    while (c is ContextWrapper) {
        if (c is Activity) return c
        c = c.baseContext
    }
    return null
}

/** What the PIN sheet in Pengaturan does. */
enum class PinMode { SET, CHANGE, DISABLE }

/**
 * Bottom sheet to create (enter twice), change (current, then new twice) or remove (current) the PIN.
 * [onDone] runs after the PIN was saved or removed.
 */
@Composable
fun PinSheet(mode: PinMode, onDismiss: () -> Unit, onDone: () -> Unit) {
    // 0 = current PIN, 1 = new PIN, 2 = repeat new PIN
    var step by remember { mutableIntStateOf(if (mode == PinMode.SET) 1 else 0) }
    var first by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    Overlay(onDismiss) {
        SheetCard(scroll = false) {
            PinEntry(
                title = when (step) { 0 -> "Masukkan PIN saat ini"; 1 -> "Buat PIN baru"; else -> "Ulangi PIN baru" },
                message = error ?: when (step) {
                    0 -> if (mode == PinMode.DISABLE) "Untuk mematikan kunci aplikasi" else "Untuk mengganti PIN"
                    1 -> "4 angka untuk membuka catet"
                    else -> "Masukkan PIN yang sama sekali lagi"
                },
                error = error != null,
                big = false,
            ) { pin ->
                when (step) {
                    0 -> if (AppLock.check(pin)) {
                        error = null
                        if (mode == PinMode.DISABLE) { AppLock.clear(); onDone() } else step = 1
                        true
                    } else { error = "PIN salah"; false }
                    1 -> { first = pin; error = null; step = 2; true }
                    else -> if (pin == first) { AppLock.setPin(pin); onDone(); true } else {
                        error = "PIN tidak sama, coba lagi"; first = ""; step = 1; false
                    }
                }
            }
        }
    }
}

/**
 * Title, message, four dots and the keypad. [onComplete] gets the 4 digits and returns whether they were
 * accepted; when not, the dots shake once and clear.
 */
@Composable
private fun PinEntry(
    title: String,
    message: String,
    error: Boolean,
    big: Boolean,
    header: @Composable () -> Unit = {},
    onComplete: (String) -> Boolean,
) {
    val tick = rememberTick()
    var pin by remember { mutableStateOf("") }
    var wrong by remember { mutableIntStateOf(0) }
    val shake = remember { Animatable(0f) }
    LaunchedEffect(wrong) {
        if (wrong > 0) {
            shake.snapTo(0f)
            shake.animateTo(0f, keyframes { durationMillis = 360; -14f at 60; 14f at 120; -10f at 180; 10f at 240; -4f at 300 })
            pin = ""
        }
    }
    fun type(c: Char) {
        if (pin.length >= 4) return
        tick()
        pin += c
        if (pin.length == 4) {
            if (onComplete(pin)) pin = "" else wrong++
        }
    }
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        header()
        Text(title, fontSize = if (big) 26.sp else 20.sp, fontWeight = FontWeight.ExtraBold, color = K.Ink, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(
            message, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = if (error) K.Danger else K.Muted,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        Row(
            Modifier.offset { IntOffset(shake.value.dp.roundToPx(), 0) },
            horizontalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            repeat(4) { i ->
                Box(Modifier.size(16.dp).clip(CircleShape).background(if (i < pin.length) K.Green else K.Notch))
            }
        }
        Spacer(Modifier.height(if (big) 32.dp else 24.dp))
        PinKeypad(onDigit = { type(it) }, onDelete = { if (pin.isNotEmpty() && pin.length < 4) { tick(); pin = pin.dropLast(1) } })
    }
}

/** Flat 3x4 keypad like Tambah transaksi: plain 27sp digits, delete key as a sage circle bottom right. */
@Composable
private fun PinKeypad(onDigit: (Char) -> Unit, onDelete: () -> Unit) {
    val rows = listOf("123", "456", "789", " 0<")
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        rows.forEach { r ->
            Row(Modifier.fillMaxWidth().height(60.dp), verticalAlignment = Alignment.CenterVertically) {
                r.forEach { k ->
                    when (k) {
                        ' ' -> Spacer(Modifier.weight(1f))
                        '<' -> Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                            Box(
                                Modifier.size(48.dp).clip(CircleShape).background(K.SageCard).pressable(onDelete),
                                contentAlignment = Alignment.Center,
                            ) { Icon(Icons.AutoMirrored.Rounded.Backspace, "Hapus", tint = K.Green, modifier = Modifier.size(24.dp)) }
                        }
                        else -> Box(
                            Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(20.dp)).pressable { onDigit(k) },
                            contentAlignment = Alignment.Center,
                        ) { Text(k.toString(), fontSize = 27.sp, fontWeight = FontWeight.ExtraBold, color = K.Ink, maxLines = 1) }
                    }
                }
            }
        }
    }
}
