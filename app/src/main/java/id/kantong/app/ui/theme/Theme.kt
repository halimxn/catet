package id.kantong.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import id.kantong.app.R
import androidx.compose.ui.unit.sp

/** Kantong palette: pastel families with a matching "ink" for text/strokes on that tint, in a light and a dark set. */
object K {
    /** Set by [KantongTheme]; every token below reads it, so the whole UI recomposes on a theme change. */
    var dark by mutableStateOf(false)

    // catet palette (R89 reference). Light: warm cream canvas, deep green primary, pastel cards.
    // Dark ("senja redup"): warm green-black canvas, night versions of the pastel cards, mint primary.
    val Ink: Color get() = if (dark) Color(0xFFF1ECDF) else Color(0xFF24302A) // titles and body text
    val Ink2: Color get() = if (dark) Color(0xFFD8D0C0) else Color(0xFF4D453A)
    val Muted: Color get() = if (dark) Color(0xFFA39D90) else Color(0xFF847C6E)
    val OnYellow: Color get() = if (dark) Color(0xFFF6DFA0) else Color(0xFF7A6B4E) // soft labels on the yellow card
    val OnPink: Color get() = if (dark) Color(0xFFF4C9C0) else Color(0xFF8C6A64) // soft labels on the pink card
    val OnSage: Color get() = if (dark) Color(0xFFCFE0BF) else Color(0xFF5F6B58) // soft labels on the sage card
    val Bg: Color get() = if (dark) Color(0xFF1A1D1A) else Color(0xFFFDFCF5) // canvas
    val Sheet: Color get() = if (dark) Color(0xFF1F231F) else Color(0xFFFDFCF5) // full screens and sheets
    val Card: Color get() = if (dark) Color(0xFF252A25) else Color(0xFFFBF4E4) // cards that group rows
    val Field: Color get() = if (dark) Color(0xFF2C312C) else Color(0xFFF4EFE3) // inputs, keys, neutral tiles
    val Notch: Color get() = if (dark) Color(0xFF3A3F3B) else Color(0xFFE7E3D6) // sheet handle, hairline borders
    val Line: Color get() = if (dark) Color(0xFF313732) else Color(0xFFEFE4CC) // dividers
    val Red: Color get() = if (dark) Color(0xFF9FD3B0) else Color(0xFF2E5242) // primary action (save, selection, active tab)
    val RedTrack: Color get() = if (dark) Color(0xFF3A4A3F) else Color(0xFFA9B79A)
    val Green: Color get() = if (dark) Color(0xFF9FD3B0) else Color(0xFF2E5242) // primary / positive
    val OnGreen: Color get() = if (dark) Color(0xFF15221B) else Color(0xFFFFFFFF) // text and icons on Green / Red
    val Income: Color get() = if (dark) Color(0xFF8DDCAA) else Color(0xFF2E5242) // income amounts
    val Danger: Color get() = if (dark) Color(0xFFF08A7C) else Color(0xFF9A3B30) // over budget, debt, delete
    val Scrim: Color get() = if (dark) Color(0x99000000) else Color(0x6B1C2821)
    val Veil: Color get() = if (dark) Color(0x2E000000) else Color(0x73FFFFFF) // soft inset on a coloured card (buttons, icon wells)
    val Yellow: Color get() = if (dark) Color(0xFF4A3F22) else Color(0xFFFAEAC0)
    val Pink: Color get() = if (dark) Color(0xFF4A2E2B) else Color(0xFFF5DCD7)
    val SageCard: Color get() = if (dark) Color(0xFF2F3A2C) else Color(0xFFE4E9D9)
    val Tile: Color get() = if (dark) Color(0x38000000) else Color(0xFFFDFCF6) // boxes and chips on cards
    val Gold: Color get() = if (dark) Color(0xFFE8B860) else Color(0xFFE8B860)
    val SageMid: Color get() = if (dark) Color(0xFF6E7F62) else Color(0xFFA9B79A)
    val Cream: Color get() = if (dark) Color(0xFF5A4C28) else Color(0xFFF6E2AE)
    val YellowLine: Color get() = if (dark) Color(0xFF5A5238) else Color(0xFFE6D3A0)
    val ChartIn: Color get() = if (dark) Color(0xFF8DDCAA) else Color(0xFF3F8F63)
    val ChartOut: Color get() = if (dark) Color(0xFFF6A85E) else Color(0xFFD9853B)
    val Mint: Color get() = if (dark) Color(0xFF2F3A2C) else Color(0xFFE4E9D9)
    val MintStrong: Color get() = if (dark) Color(0xFF8DDCAA) else Color(0xFF3F8F63)
    val MintInk: Color get() = if (dark) Color(0xFFCFE0BF) else Color(0xFF2E5242)
    val MintText: Color get() = if (dark) Color(0xFF9FD3B0) else Color(0xFF2E5242)
    val MintTint: Color get() = if (dark) Color(0xFF262E25) else Color(0xFFEEF3E8)
    val Sky: Color get() = if (dark) Color(0xFF27323A) else Color(0xFFE2E9EE)
    val SkyStrong: Color get() = if (dark) Color(0xFF8FB2E6) else Color(0xFF5B86C5)
    val SkyInk: Color get() = if (dark) Color(0xFFB9CCE6) else Color(0xFF34557F)
    val SkyTint: Color get() = if (dark) Color(0xFF232A30) else Color(0xFFEDF2F5)
    val Lav: Color get() = if (dark) Color(0xFF342D3D) else Color(0xFFECE6F2)
    val LavStrong: Color get() = if (dark) Color(0xFFC2A6E6) else Color(0xFF9C7AC2)
    val LavInk: Color get() = if (dark) Color(0xFFD6C6EE) else Color(0xFF5D4B80)
    val LavTint: Color get() = if (dark) Color(0xFF2B2632) else Color(0xFFF3EFF7)
    val Peach: Color get() = if (dark) Color(0xFF4A3F22) else Color(0xFFFAEAC0)
    val PeachStrong: Color get() = if (dark) Color(0xFFF6A85E) else Color(0xFFD9853B)
    val PeachInk: Color get() = if (dark) Color(0xFFF6DFA0) else Color(0xFF7A5420)
    val PeachTint: Color get() = if (dark) Color(0xFF3A3220) else Color(0xFFFBF1D8)
    val Butter: Color get() = if (dark) Color(0xFF4A3F22) else Color(0xFFFAEAC0)
    val ButterStrong: Color get() = if (dark) Color(0xFFF0C46A) else Color(0xFFD9A23A)
    val ButterInk: Color get() = if (dark) Color(0xFFF6DFA0) else Color(0xFF7A5A17)
    val ButterTint: Color get() = if (dark) Color(0xFF3A3220) else Color(0xFFFBF1D8)
    val PinkT: Color get() = if (dark) Color(0xFF4A2E2B) else Color(0xFFF5DCD7)
    val PinkStrong: Color get() = if (dark) Color(0xFFF0907F) else Color(0xFFD9685A)
    val PinkInk: Color get() = if (dark) Color(0xFFF4C9C0) else Color(0xFF8E3F35)
    val PinkTint: Color get() = if (dark) Color(0xFF3A2624) else Color(0xFFF9EAE7)
    val Coral: Color get() = if (dark) Color(0xFF4A2E2B) else Color(0xFFF5DCD7)
    val CoralStrong: Color get() = if (dark) Color(0xFFF0907F) else Color(0xFFD9685A)
    val CoralInk: Color get() = if (dark) Color(0xFFF4C9C0) else Color(0xFF8E3F35)
    val CoralTint: Color get() = if (dark) Color(0xFF3A2624) else Color(0xFFF9EAE7)
    val Sage: Color get() = if (dark) Color(0xFF2F3A2C) else Color(0xFFE4E9D9)
    val SageStrong: Color get() = if (dark) Color(0xFFA9CC92) else Color(0xFF7FA36B)
    val SageInk: Color get() = if (dark) Color(0xFFCFE0BF) else Color(0xFF4F6B3E)
    val SageTint: Color get() = if (dark) Color(0xFF262E25) else Color(0xFFEEF3E8)
    /** A one-off colour with its night twin. */
    fun c(light: Long, night: Long): Color = Color(if (dark) night else light)
    /** Soft inset on a coloured card: translucent white by day, a light shade of black at night. */
    fun veil(alpha: Float): Color = if (dark) Color(0x2E000000) else Color.White.copy(alpha = alpha)

    val Expense: Color get() = Ink
    val Accent: Color get() = Green

    // validated chart colors (dataviz validator): fixed categorical order, brighter at night
    val ChartCats: List<Color> get() = if (dark) listOf(Color(0xFF8DDCAA), Color(0xFF8FB2E6), Color(0xFFF0C46A), Color(0xFFC2A6E6), Color(0xFFF0907F))
        else listOf(Color(0xFF3F8F63), Color(0xFF5B86C5), Color(0xFFD9A23A), Color(0xFF9C7AC2), Color(0xFFD9685A))
    val Heat: List<Color> get() = if (dark) listOf(Color(0xFF262B25), Color(0xFF2F4A36), Color(0xFF3E6B4B), Color(0xFF5E9E6E), Color(0xFF8DDCAA))
        else listOf(Color(0xFFEEF3E8), Color(0xFFCFE0C4), Color(0xFF9DC48F), Color(0xFF5E9E6E), Color(0xFF2E6B4A))

    val HeroMint: Brush get() = Brush.linearGradient(listOf(MintTint, SageCard))
    val HeroLav: Brush get() = Brush.linearGradient(listOf(LavTint, SageCard))
    val HeroButter: Brush get() = Brush.linearGradient(listOf(Yellow, Yellow))
    val HeroSky: Brush get() = Brush.linearGradient(listOf(SkyTint, MintTint))
    val HeroPeach: Brush get() = Brush.linearGradient(listOf(Yellow, Pink))
    val HeroMb: Brush get() = Brush.linearGradient(listOf(MintTint, Yellow))
}

/** A pastel family used for icon wells, chips and chart segments. */
/** tint = pill background (neutral on purpose: colors never clash), base = icon tile, strong = charts, ink = text. */
enum class Tone {
    MINT, SKY, LAV, PEACH, BUTTER, PINK, CORAL, SAGE;

    val tint: Color get() = K.Field
    val base: Color get() = when (this) { MINT -> K.Mint; SKY -> K.Sky; LAV -> K.Lav; PEACH -> K.Peach; BUTTER -> K.Butter; PINK -> K.PinkT; CORAL -> K.Coral; SAGE -> K.Sage }
    val strong: Color get() = when (this) { MINT -> K.MintStrong; SKY -> K.SkyStrong; LAV -> K.LavStrong; PEACH -> K.PeachStrong; BUTTER -> K.ButterStrong; PINK -> K.PinkStrong; CORAL -> K.CoralStrong; SAGE -> K.SageStrong }
    val ink: Color get() = when (this) { MINT -> K.MintInk; SKY -> K.SkyInk; LAV -> K.LavInk; PEACH -> K.PeachInk; BUTTER -> K.ButterInk; PINK -> K.PinkInk; CORAL -> K.CoralInk; SAGE -> K.SageInk }

    companion object {
        fun of(name: String): Tone = entries.firstOrNull { it.name.equals(name, true) } ?: MINT
    }
}

/**
 * Nunito (rounded variable font, OFL): friendly, cozy, very readable numbers.
 * Every requested weight is drawn heavier (Normal -> 600, SemiBold -> 800 ...) so the whole app reads
 * chunky and playful like a sticker book, without touching each screen.
 */
private val heavier = mapOf(300 to 480, 400 to 600, 500 to 680, 600 to 720, 700 to 800, 800 to 850, 900 to 920)

@OptIn(ExperimentalTextApi::class)
val Outfit = FontFamily(
    heavier.map { (w, drawn) ->
        Font(R.font.nunito, weight = FontWeight(w), variationSettings = FontVariation.Settings(FontVariation.weight(drawn)))
    }
)

private fun s(size: Int, weight: FontWeight = FontWeight.Normal, color: Color = Color.Unspecified, spacing: Float = 0f, line: Int = 0) =
    TextStyle(fontFamily = Outfit, fontSize = size.sp, fontWeight = weight, color = color, letterSpacing = spacing.sp,
        lineHeight = if (line > 0) line.sp else androidx.compose.ui.unit.TextUnit.Unspecified)

private fun typography() = Typography(
    headlineLarge = s(40, FontWeight.SemiBold, spacing = -1.4f),
    headlineSmall = s(24, FontWeight.SemiBold, spacing = -0.5f),
    titleLarge = s(20, FontWeight.SemiBold),
    titleMedium = s(18, FontWeight.SemiBold),
    titleSmall = s(15, FontWeight.Medium),
    bodyLarge = s(15),
    bodyMedium = s(14, line = 20),
    bodySmall = s(13),
    labelLarge = s(14, FontWeight.Medium),
    labelMedium = s(12, FontWeight.Medium),
    labelSmall = s(11, FontWeight.Medium),
)

/** Light, dark or follow the phone; "gelap otomatis malam" turns dark between 18.00 and 06.00. */
@Composable
fun isDarkTheme(): Boolean {
    val system = androidx.compose.foundation.isSystemInDarkTheme()
    var hour by androidx.compose.runtime.remember { androidx.compose.runtime.mutableIntStateOf(java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)) }
    // re-check the clock every minute so the scheduled switch happens while the app is open
    androidx.compose.runtime.LaunchedEffect(Unit) {
        while (true) { kotlinx.coroutines.delay(60_000); hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY) }
    }
    val night = hour >= 18 || hour < 6
    return when {
        id.kantong.app.data.Prefs.autoNight && night -> true
        id.kantong.app.data.Prefs.theme == "dark" -> true
        id.kantong.app.data.Prefs.theme == "light" -> false
        else -> system
    }
}

@Composable
fun KantongTheme(content: @Composable () -> Unit) {
    val dark = isDarkTheme()
    K.dark = dark
    val view = androidx.compose.ui.platform.LocalView.current
    androidx.compose.runtime.SideEffect {
        (view.context as? android.app.Activity)?.window?.let { w ->
            androidx.core.view.WindowCompat.getInsetsController(w, view).apply {
                isAppearanceLightStatusBars = !dark
                isAppearanceLightNavigationBars = !dark
            }
        }
    }
    val scheme = if (dark) androidx.compose.material3.darkColorScheme(
        primary = K.Green, onPrimary = K.OnGreen, primaryContainer = K.Yellow, onPrimaryContainer = K.Ink,
        secondary = K.SkyInk, background = K.Bg, surface = K.Sheet, onSurface = K.Ink, onBackground = K.Ink,
        surfaceContainerLow = K.Sheet, surfaceContainer = K.Sheet, surfaceContainerHigh = K.Sheet,
        outline = K.Line, error = K.Danger,
    ) else lightColorScheme(
        primary = K.Green, onPrimary = Color.White, primaryContainer = K.Yellow, onPrimaryContainer = K.Ink,
        secondary = K.SkyInk, background = K.Bg, surface = K.Sheet, onSurface = K.Ink, onBackground = K.Ink,
        surfaceContainerLow = K.Sheet, surfaceContainer = K.Sheet, surfaceContainerHigh = K.Sheet,
        outline = K.Line, error = K.Red,
    )
    MaterialTheme(colorScheme = scheme, typography = typography()) {
        androidx.compose.runtime.CompositionLocalProvider(androidx.compose.material3.LocalContentColor provides K.Ink, content = content)
    }
}
