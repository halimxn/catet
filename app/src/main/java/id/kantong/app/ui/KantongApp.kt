package id.kantong.app.ui

import id.kantong.app.ui.screens.HoldingScreen
import id.kantong.app.ui.screens.DebtsScreen
import id.kantong.app.ui.screens.ZakatScreen
import id.kantong.app.ui.screens.DebtDetailScreen
import androidx.activity.compose.BackHandler
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import id.kantong.app.data.AppViewModel
import id.kantong.app.ui.components.CatIcon
import androidx.compose.runtime.CompositionLocalProvider
import id.kantong.app.ui.components.LocalOverlayHost
import id.kantong.app.ui.components.OverlayHost
import id.kantong.app.ui.components.OverlayHostView
import id.kantong.app.ui.components.MeowFab
import id.kantong.app.ui.components.pressable
import id.kantong.app.ui.screens.AccountDetailScreen
import id.kantong.app.ui.screens.AccountEditScreen
import id.kantong.app.ui.screens.AddTransactionScreen
import id.kantong.app.ui.screens.AllTypesScreen
import id.kantong.app.ui.screens.AssetsScreen
import id.kantong.app.ui.screens.BookmarksScreen
import id.kantong.app.ui.screens.CategoriesScreen
import id.kantong.app.ui.screens.CategoryAddScreen
import id.kantong.app.ui.screens.GoalsScreen
import id.kantong.app.ui.screens.HomeScreen
import id.kantong.app.ui.screens.LedgerDetailScreen
import id.kantong.app.ui.screens.LedgersScreen
import id.kantong.app.ui.screens.RecurringScreen
import id.kantong.app.ui.screens.ReimburseScreen
import id.kantong.app.ui.screens.ReportsScreen
import id.kantong.app.ui.screens.SearchScreen
import id.kantong.app.ui.screens.SettingsScreen
import id.kantong.app.ui.theme.K

/**
 * Routes. The four tabs (Catatan, Laporan, Aset, Saya) live on the base layer; everything else is pushed on top.
 * Use the helper functions for routes with arguments, e.g. nav.go(R.add()) or nav.go(R.account(id)).
 */
object R {
    // tabs
    const val HOME = "home"; const val REPORTS = "reports"; const val ASSETS = "assets"; const val ME = "me"
    // pushed screens
    const val SEARCH = "search"; const val CATEGORIES = "categories"; const val LEDGERS = "ledgers"
    const val RECURRING = "recurring"; const val REIMBURSE = "reimburse"; const val BOOKMARKS = "bookmarks"; const val GOALS = "goals"
    fun add(editId: Long = 0) = "add/$editId"
    fun categoryAdd(kind: String, editId: Long = 0, parentId: Long = 0) = "category_add/$kind/$editId/$parentId"
    fun ledger(id: Long) = "ledger/$id"
    fun account(id: Long) = "account/$id"
    fun holding(id: Long) = "holding/$id"
    fun debt(id: Long) = "debt/$id"
    const val DEBTS = "debts"
    const val ZAKAT = "zakat"
    fun accountEdit(id: Long = 0) = "account_edit/$id"
    fun types(kind: String) = "types/$kind"
}

/** Route of the NavHost's base layer: the tabs live there, sub-screens are pushed on top of it. */
private const val MAIN = "main"

class Nav(private val c: NavHostController, private val selectTab: (String) -> Unit) {
    fun go(route: String) = c.navigate(route) { launchSingleTop = true }
    fun tab(route: String) {
        selectTab(route)
        if (c.currentDestination?.route != MAIN) c.popBackStack(MAIN, inclusive = false)
    }
    fun back() { c.popBackStack() }
}

private val tabs = listOf(R.HOME, R.REPORTS, R.ASSETS, R.ME)

@Composable
fun KantongApp() {
    val vm: AppViewModel = viewModel()
    val data by vm.data.collectAsStateWithLifecycle()
    val controller = rememberNavController()
    var tab by rememberSaveable { mutableStateOf(R.HOME) }
    val nav = remember(controller) { Nav(controller) { tab = it } }
    val entry by controller.currentBackStackEntryAsState()
    val onMain = (entry?.destination?.route ?: MAIN) == MAIN
    val overlays = remember { OverlayHost() }

    // The system splash stays up until data is loaded (MainActivity), so there is no second splash here.
    if (!data.loaded) { Box(Modifier.fillMaxSize().background(K.Bg)); return }
    if (!id.kantong.app.data.Prefs.onboarded) { id.kantong.app.ui.screens.OnboardingScreen(data, vm); return }
    CompositionLocalProvider(LocalOverlayHost provides overlays) { Box(Modifier.fillMaxSize().background(K.Bg)) {
        // Tabs stay composed once visited, so switching tabs only shows what is already built.
        ShowIf(onMain) {
            TabsHost(tabs.indexOf(tab)) { i ->
                when (i) {
                    0 -> HomeScreen(data, vm, nav)
                    1 -> ReportsScreen(data, vm, nav)
                    2 -> AssetsScreen(data, vm, nav)
                    else -> SettingsScreen(data, vm, nav)
                }
            }
        }
        NavHost(controller, startDestination = MAIN,
            enterTransition = { EnterTransition.None }, exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None }, popExitTransition = { ExitTransition.None }) {
            composable(MAIN) { /* transparent: the tabs above show through */ }
            composable("add/{edit}", listOf(navArgument("edit") { type = NavType.LongType })) { e ->
                Screen { AddTransactionScreen(data, vm, nav, e.arguments?.getLong("edit") ?: 0L) }
            }
            composable(R.SEARCH) { Screen { SearchScreen(data, vm, nav) } }
            composable(R.CATEGORIES) { Screen { CategoriesScreen(data, vm, nav) } }
            composable("category_add/{kind}/{edit}/{parent}", listOf(
                navArgument("kind") { type = NavType.StringType },
                navArgument("edit") { type = NavType.LongType },
                navArgument("parent") { type = NavType.LongType },
            )) { e ->
                Screen { CategoryAddScreen(data, vm, nav, e.arguments?.getString("kind") ?: "out", e.arguments?.getLong("edit") ?: 0L, e.arguments?.getLong("parent") ?: 0L) }
            }
            composable(R.LEDGERS) { Screen { LedgersScreen(data, vm, nav) } }
            composable("ledger/{id}", listOf(navArgument("id") { type = NavType.LongType })) { e ->
                Screen { LedgerDetailScreen(data, vm, nav, e.arguments?.getLong("id") ?: 0L) }
            }
            composable(R.DEBTS) { Screen { DebtsScreen(data, vm, nav) } }
            composable(R.ZAKAT) { Screen { ZakatScreen(data, vm, nav) } }
            composable("debt/{id}", listOf(navArgument("id") { type = NavType.LongType })) { e ->
                Screen { DebtDetailScreen(data, vm, nav, e.arguments?.getLong("id") ?: 0L) }
            }
            composable("holding/{id}", listOf(navArgument("id") { type = NavType.LongType })) { e ->
                Screen { HoldingScreen(data, vm, nav, e.arguments?.getLong("id") ?: 0L) }
            }
            composable("account/{id}", listOf(navArgument("id") { type = NavType.LongType })) { e ->
                Screen { AccountDetailScreen(data, vm, nav, e.arguments?.getLong("id") ?: 0L) }
            }
            composable("account_edit/{id}", listOf(navArgument("id") { type = NavType.LongType })) { e ->
                Screen { AccountEditScreen(data, vm, nav, e.arguments?.getLong("id") ?: 0L) }
            }
            composable("types/{kind}", listOf(navArgument("kind") { type = NavType.StringType })) { e ->
                Screen { AllTypesScreen(data, vm, nav, e.arguments?.getString("kind") ?: "out") }
            }
            composable(R.RECURRING) { Screen { RecurringScreen(data, vm, nav) } }
            composable(R.REIMBURSE) { Screen { ReimburseScreen(data, vm, nav) } }
            composable(R.BOOKMARKS) { Screen { BookmarksScreen(data, vm, nav) } }
            composable(R.GOALS) { Screen { GoalsScreen(data, vm, nav) } }
        }
        if (onMain) BottomBar(tab, nav, Modifier.align(Alignment.BottomCenter))
        // Back from another tab returns to Beranda first.
        BackHandler(enabled = onMain && tab != R.HOME) { tab = R.HOME }
        OverlayHostView(overlays)
    } }
}

/** Opaque full-size surface for pushed sub-screens. */
@Composable
private fun Screen(content: @Composable () -> Unit) = Box(Modifier.fillMaxSize().background(K.Sheet)) { content() }

/**
 * Keeps [content] composed, measured and recorded in its own layer; when not [visible] it is parked
 * off-screen (not drawn, no touches), so bringing it back is just a layer move.
 */
@Composable
private fun ShowIf(visible: Boolean, content: @Composable () -> Unit) {
    Layout(content) { measurables, constraints ->
        val placeables = measurables.map { it.measure(constraints) }
        layout(constraints.maxWidth, constraints.maxHeight) {
            placeables.forEach { it.placeWithLayer(if (visible) 0 else constraints.maxWidth * 2, 0) }
        }
    }
}

/** Composes each tab the first time it is opened and keeps it; the current one is on screen, the rest parked off-screen. */
@Composable
private fun TabsHost(current: Int, content: @Composable (Int) -> Unit) {
    val visited = remember { mutableStateListOf<Int>() }
    if (current !in visited) visited.add(current)
    Layout({ visited.forEach { i -> key(i) { Box(Modifier.layoutId(i)) { content(i) } } } }) { measurables, constraints ->
        val placeables = measurables.map { it.layoutId to it.measure(constraints) }
        layout(constraints.maxWidth, constraints.maxHeight) {
            placeables.forEach { (id, p) -> p.placeWithLayer(if (id == current) 0 else constraints.maxWidth * 2, 0) }
        }
    }
}

/** catet bottom bar (R89 style): cream bar with a hairline, two-tone icons; the active tab is filled gold. */
@Composable
private fun BottomBar(tab: String, nav: Nav, modifier: Modifier) {
    // Floating capsule instead of a full-width board: slim, rounded, a soft fade of the canvas behind it.
    Box(modifier.fillMaxWidth().background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(K.Bg.copy(alpha = 0f), K.Bg, K.Bg))).navigationBarsPadding().padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 10.dp)) {
        Row(
            Modifier.fillMaxWidth().height(64.dp).shadow(10.dp, RoundedCornerShape(32.dp), ambientColor = Color(0x223A2A14), spotColor = Color(0x223A2A14))
                .clip(RoundedCornerShape(32.dp)).background(K.Tile).padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NavItem("Beranda", 0, tab == R.HOME) { nav.tab(R.HOME) }
            NavItem("Laporan", 1, tab == R.REPORTS) { nav.tab(R.REPORTS) }
            NavItem("Aset", 2, tab == R.ASSETS) { nav.tab(R.ASSETS) }
            NavItem("Saya", 3, tab == R.ME) { nav.tab(R.ME) }
        }
    }
}

@Composable
private fun RowScope.NavItem(label: String, icon: Int, on: Boolean, onClick: () -> Unit) {
    // Active tab: soft yellow pill behind the filled icon + green bold label; others stay quiet grey.
    Column(Modifier.weight(1f).pressable(onClick), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.size(width = 54.dp, height = 30.dp).clip(RoundedCornerShape(15.dp)).background(if (on) K.Yellow else Color.Transparent),
            contentAlignment = Alignment.Center,
        ) { NavGlyph(icon, on) }
        Spacer(Modifier.height(1.dp))
        Text(label, fontSize = 12.sp, fontWeight = if (on) FontWeight.ExtraBold else FontWeight.SemiBold, color = if (on) K.Green else Color(0xFF9C968A), maxLines = 1)
    }
}

@Composable
private fun NavGlyph(icon: Int, on: Boolean) {
    androidx.compose.foundation.Canvas(Modifier.size(23.dp)) {
        val u = size.width / 36f
        val line = if (on) Color(0xFF2F2F2F) else Color(0xFFA39D90)
        val st = androidx.compose.ui.graphics.drawscope.Stroke(2.2f * u, cap = androidx.compose.ui.graphics.StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round)
        fun p(block: androidx.compose.ui.graphics.Path.() -> Unit) = androidx.compose.ui.graphics.Path().apply(block)
        fun fillStroke(path: androidx.compose.ui.graphics.Path, fill: Color) { if (on) drawPath(path, fill); drawPath(path, line, style = st) }
        when (icon) {
            0 -> {
                fillStroke(p { moveTo(5 * u, 16 * u); lineTo(18 * u, 5 * u); lineTo(31 * u, 16 * u); lineTo(31 * u, 30.5f * u); quadraticTo(31 * u, 33 * u, 28.5f * u, 33 * u); lineTo(7.5f * u, 33 * u); quadraticTo(5 * u, 33 * u, 5 * u, 30.5f * u); close() }, K.Gold)
                fillStroke(p { addRoundRect(androidx.compose.ui.geometry.RoundRect(14.5f * u, 21 * u, 21.5f * u, 33 * u, 1.5f * u, 1.5f * u)) }, K.Tile)
            }
            1 -> {
                fillStroke(p { addOval(androidx.compose.ui.geometry.Rect(5 * u, 5 * u, 31 * u, 31 * u)) }, K.Gold)
                drawPath(p { moveTo(18 * u, 5 * u); lineTo(18 * u, 18 * u); lineTo(31 * u, 18 * u) }, line, style = st)
            }
            2 -> {
                fillStroke(p { addRoundRect(androidx.compose.ui.geometry.RoundRect(4 * u, 9 * u, 32 * u, 30 * u, 4 * u, 4 * u)) }, K.Gold)
                fillStroke(p { addRoundRect(androidx.compose.ui.geometry.RoundRect(21 * u, 16 * u, 32 * u, 23 * u, 3.5f * u, 3.5f * u)) }, K.Tile)
            }
            else -> {
                fillStroke(p { addOval(androidx.compose.ui.geometry.Rect(12 * u, 7 * u, 24 * u, 19 * u)) }, K.Gold)
                drawPath(p { moveTo(7 * u, 31 * u); cubicTo(8.8f * u, 25.7f * u, 13 * u, 23 * u, 18 * u, 23 * u); cubicTo(23 * u, 23 * u, 27.2f * u, 25.7f * u, 29 * u, 31 * u) }, line, style = st)
            }
        }
    }
}
