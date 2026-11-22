package id.kantong.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
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

/** Kantong palette: pastel families with a matching "ink" for text/strokes on that tint (see the design canvas). */
object K {
    // catet palette (R89 reference): warm cream canvas, warm ink, deep green primary, pastel cards.
    val Ink = Color(0xFF24302A)   // R89: green-black titles
    val Ink2 = Color(0xFF4D453A)
    val Muted = Color(0xFF847C6E)
    val OnYellow = Color(0xFF7A6B4E) // soft labels on the yellow card
    val OnPink = Color(0xFF8C6A64)   // soft labels on the pink card
    val OnSage = Color(0xFF5F6B58)   // soft labels on the sage card
    val Bg = Color(0xFFFDFCF5)      // canvas
    val Sheet = Color(0xFFFDFCF5)   // full screens and sheets
    val Card = Color(0xFFFBF4E4)    // cream cards that group rows
    val Field = Color(0xFFF4EFE3)   // inputs, keys, neutral tiles
    val Notch = Color(0xFFE7E3D6)   // sheet handle, hairline borders
    val Line = Color(0xFFEFE4CC)    // dividers
    val Red = Color(0xFF2E5242)     // primary action (save, selection, active tab) - deep green
    val RedTrack = Color(0xFFA9B79A)
    val Green = Color(0xFF2E5242)
    val Expense = Ink               // expense amounts read in ink
    val Income = Color(0xFF2E5242)  // income amounts in green
    val Danger = Color(0xFF9A3B30)  // over budget, debt, delete
    val Accent = Green
    val Scrim = Color(0x6B1C2821)

    // R89 card colors
    val Yellow = Color(0xFFFAEAC0); val Pink = Color(0xFFF5DCD7); val SageCard = Color(0xFFE4E9D9); val Tile = Color(0xFFFDFCF6)
    val Gold = Color(0xFFE8B860); val SageMid = Color(0xFFA9B79A); val Cream = Color(0xFFF6E2AE); val YellowLine = Color(0xFFE6D3A0)
    // validated chart colors (dataviz validator): income/expense pair + fixed categorical order
    val ChartIn = Color(0xFF3F8F63); val ChartOut = Color(0xFFD9853B)
    val ChartCats = listOf(Color(0xFF3F8F63), Color(0xFF5B86C5), Color(0xFFD9A23A), Color(0xFF9C7AC2), Color(0xFFD9685A))
    val Heat = listOf(Color(0xFFEEF3E8), Color(0xFFCFE0C4), Color(0xFF9DC48F), Color(0xFF5E9E6E), Color(0xFF2E6B4A))

    // Pastel families kept for tones: base = soft tile, Strong = chart, Ink = text on tint.
    val Mint = Color(0xFFE4E9D9); val MintStrong = Color(0xFF3F8F63); val MintInk = Color(0xFF2E5242); val MintText = Color(0xFF2E5242); val MintTint = Color(0xFFEEF3E8)
    val Sky = Color(0xFFE2E9EE); val SkyStrong = Color(0xFF5B86C5); val SkyInk = Color(0xFF34557F); val SkyTint = Color(0xFFEDF2F5)
    val Lav = Color(0xFFECE6F2); val LavStrong = Color(0xFF9C7AC2); val LavInk = Color(0xFF5D4B80); val LavTint = Color(0xFFF3EFF7)
    val Peach = Color(0xFFFAEAC0); val PeachStrong = Color(0xFFD9853B); val PeachInk = Color(0xFF7A5420); val PeachTint = Color(0xFFFBF1D8)
    val Butter = Color(0xFFFAEAC0); val ButterStrong = Color(0xFFD9A23A); val ButterInk = Color(0xFF7A5A17); val ButterTint = Color(0xFFFBF1D8)
    val PinkT = Color(0xFFF5DCD7); val PinkStrong = Color(0xFFD9685A); val PinkInk = Color(0xFF8E3F35); val PinkTint = Color(0xFFF9EAE7)
    val Coral = Color(0xFFF5DCD7); val CoralStrong = Color(0xFFD9685A); val CoralInk = Color(0xFF8E3F35); val CoralTint = Color(0xFFF9EAE7)
    val Sage = Color(0xFFE4E9D9); val SageStrong = Color(0xFF7FA36B); val SageInk = Color(0xFF4F6B3E); val SageTint = Color(0xFFEEF3E8)

    val HeroMint = Brush.linearGradient(listOf(MintTint, SageCard))
    val HeroLav = Brush.linearGradient(listOf(LavTint, SageCard))
    val HeroButter = Brush.linearGradient(listOf(Yellow, Yellow))
    val HeroSky = Brush.linearGradient(listOf(SkyTint, MintTint))
    val HeroPeach = Brush.linearGradient(listOf(Yellow, Pink))
    val HeroMb = Brush.linearGradient(listOf(MintTint, Yellow))
}

/** A pastel family used for icon wells, chips and chart segments. */
/** tint = pill background (neutral on purpose: colors never clash), base = icon tile, strong = charts, ink = text. */
enum class Tone(val tint: Color, val base: Color, val strong: Color, val ink: Color) {
    MINT(K.Field, K.Mint, K.MintStrong, K.MintInk),
    SKY(K.Field, K.Sky, K.SkyStrong, K.SkyInk),
    LAV(K.Field, K.Lav, K.LavStrong, K.LavInk),
    PEACH(K.Field, K.Peach, K.PeachStrong, K.PeachInk),
    BUTTER(K.Field, K.Butter, K.ButterStrong, K.ButterInk),
    PINK(K.Field, K.PinkT, K.PinkStrong, K.PinkInk),
    CORAL(K.Field, K.Coral, K.CoralStrong, K.CoralInk),
    SAGE(K.Field, K.Sage, K.SageStrong, K.SageInk);

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

private fun s(size: Int, weight: FontWeight = FontWeight.Normal, color: Color = K.Ink, spacing: Float = 0f, line: Int = 0) =
    TextStyle(fontFamily = Outfit, fontSize = size.sp, fontWeight = weight, color = color, letterSpacing = spacing.sp,
        lineHeight = if (line > 0) line.sp else androidx.compose.ui.unit.TextUnit.Unspecified)

private val typography = Typography(
    headlineLarge = s(40, FontWeight.SemiBold, spacing = -1.4f),
    headlineSmall = s(24, FontWeight.SemiBold, spacing = -0.5f),
    titleLarge = s(20, FontWeight.SemiBold),
    titleMedium = s(18, FontWeight.SemiBold),
    titleSmall = s(15, FontWeight.Medium),
    bodyLarge = s(15),
    bodyMedium = s(14, line = 20),
    bodySmall = s(13, color = K.Muted),
    labelLarge = s(14, FontWeight.Medium),
    labelMedium = s(12, FontWeight.Medium),
    labelSmall = s(11, FontWeight.Medium),
)

@Composable
fun KantongTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = K.Green, onPrimary = Color.White, primaryContainer = K.Yellow, onPrimaryContainer = K.Ink,
            secondary = K.SkyInk, background = K.Bg, surface = K.Sheet, onSurface = K.Ink, onBackground = K.Ink,
            surfaceContainerLow = K.Sheet, surfaceContainer = K.Sheet, surfaceContainerHigh = K.Sheet,
            outline = K.Line, error = K.Red,
        ),
        typography = typography,
        content = content,
    )
}
