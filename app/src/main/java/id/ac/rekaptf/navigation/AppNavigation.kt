package id.ac.rekaptf.navigation

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.annotation.StringRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import id.ac.rekaptf.R
import id.ac.rekaptf.data.Directory
import id.ac.rekaptf.ui.account.AccountScreen
import id.ac.rekaptf.ui.account.AccountViewModel
import id.ac.rekaptf.ui.components.workModel
import id.ac.rekaptf.ui.directory.DirectoryEditorRoute
import id.ac.rekaptf.ui.directory.DirectoryEditorViewModel
import id.ac.rekaptf.ui.directory.DirectoryListRoute
import id.ac.rekaptf.ui.directory.DirectoryListViewModel
import id.ac.rekaptf.ui.home.DataAccountScreen
import id.ac.rekaptf.ui.home.HomeScreen
import id.ac.rekaptf.ui.home.loadHome
import kotlinx.coroutines.launch

private const val SignIn = "sign-in"
private const val Register = "register"
private const val Home = "home"
private const val DataAccount = "data-account"
private const val RecordId = "recordId"
private const val NewRecord = "new"

private enum class RootTab(val path: String, @StringRes val label: Int) {
    HomeTab(Home, R.string.home),
    EvidenceTab(DraftsPath, R.string.shell_tab_evidence),
    TransactionsTab(TransactionsPath, R.string.shell_tab_transactions),
    RecapTab(RecapPath, R.string.shell_tab_recap),
}

@Composable
internal fun RekapApp(account: AccountViewModel = viewModel()) {
    val state by account.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val business = state.businessName
    if (business == null) {
        SystemNavigationContrast(darkBackground = false)
        Scaffold(
            modifier = Modifier.imePadding(),
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { SnackbarHost(snackbar) },
        ) { padding ->
            AccountNavigation(Modifier.padding(padding).consumeWindowInsets(padding), account)
        }
    } else {
        HomeNavigation(business, account, snackbar)
    }
}

@Composable
private fun AccountNavigation(modifier: Modifier, account: AccountViewModel) {
    val nav = rememberNavController()
    val state by account.state.collectAsStateWithLifecycle()
    val entry by nav.currentBackStackEntryAsState()
    val showRegister = entry?.destination?.route == Register
    val back: () -> Unit = remember(nav, account) {
        {
            account.clearError()
            nav.popBackStack()
        }
    }
    NavHost(nav, startDestination = SignIn, modifier = modifier) {
        composable(SignIn) {
            val screenState by account.state.collectAsStateWithLifecycle()
            AccountScreen(false, screenState, account::signIn, {
                account.clearError()
                nav.navigate(Register) { launchSingleTop = true }
            }, account::clearError)
        }
        composable(Register) {
            val screenState by account.state.collectAsStateWithLifecycle()
            AccountScreen(true, screenState, account::signIn, back, account::clearError)
        }
    }
    BackHandler(enabled = state.busy || showRegister) {
        if (!state.busy) back()
    }
}

@Composable
private fun HomeNavigation(
    business: String,
    account: AccountViewModel,
    snackbar: SnackbarHostState,
) {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val active = RootTab.entries.firstOrNull { it.path == entry?.destination?.route }
    val keyboardOpen = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    val showTabs = active != null && !keyboardOpen
    SystemNavigationContrast(darkBackground = showTabs)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    Scaffold(
        modifier = Modifier.imePadding(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (active != null && showTabs) RootTabs(active, nav::openRoot)
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        NavHost(nav, startDestination = Home, modifier = Modifier.padding(padding).consumeWindowInsets(padding)) {
            composable(Home) {
                val summaryModel = workModel(account::sessionExpired)
                val summary by summaryModel.state.collectAsStateWithLifecycle()
                val uncategorized = stringResource(R.string.home_uncategorized)
                val load = {
                    summaryModel.load(R.string.shell_summary_error) {
                        loadHome(account.repository, uncategorized)
                    }
                }
                LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { load() }
                HomeScreen(
                    business = business,
                    summary = summary,
                    onRetrySummary = load,
                    onEvidence = { nav.navigate(EvidencePath) { launchSingleTop = true } },
                    onDrafts = { nav.openRoot(RootTab.EvidenceTab) },
                    onRecap = { nav.openRoot(RootTab.RecapTab) },
                    onTransactions = { nav.openRoot(RootTab.TransactionsTab) },
                    onOpenIncome = { id -> nav.navigate(incomePath(id)) { launchSingleTop = true } },
                    onManage = { nav.navigate(DataAccount) { launchSingleTop = true } },
                )
            }
            composable(DataAccount) {
                val state by account.state.collectAsStateWithLifecycle()
                DataAccountScreen(
                    state = state,
                    business = business,
                    onBack = { nav.popBackStack() },
                    onCategories = { nav.navigate(Directory.Categories.path) { launchSingleTop = true } },
                    onPayers = { nav.navigate(Directory.Payers.path) { launchSingleTop = true } },
                    onSavedRecaps = { nav.navigate(SavedPath) { launchSingleTop = true } },
                    onSnapshot = { nav.navigate(SnapshotPath) { launchSingleTop = true } },
                    onSignOut = account::signOut,
                )
            }
            Directory.entries.forEach { directory ->
                directoryRoutes(nav, directory, account) { message ->
                    scope.launch { snackbar.showSnackbar(context.getString(message)) }
                }
            }
            workRoutes(nav, account, home = Home)
        }
    }
}

@Composable
private fun SystemNavigationContrast(darkBackground: Boolean) {
    val activity = LocalActivity.current ?: return
    val view = LocalView.current
    val darkTheme = isSystemInDarkTheme()
    SideEffect {
        WindowCompat.getInsetsController(activity.window, view).isAppearanceLightNavigationBars =
            !darkBackground && !darkTheme
    }
}

private fun NavHostController.openRoot(tab: RootTab) {
    if (currentDestination?.route == tab.path) return
    navigate(tab.path) {
        popUpTo(Home) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
private fun RootTabs(active: RootTab, onSelect: (RootTab) -> Unit) {
    val colors = MaterialTheme.colorScheme
    NavigationBar(
        containerColor = colors.inverseSurface,
        contentColor = colors.inverseOnSurface,
        tonalElevation = 0.dp,
        windowInsets = NavigationBarDefaults.windowInsets,
    ) {
        RootTab.entries.forEach { tab ->
            NavigationBarItem(
                selected = tab == active,
                onClick = { onSelect(tab) },
                icon = { TabGlyph(tab) },
                label = { Text(stringResource(tab.label), style = MaterialTheme.typography.labelSmall) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = colors.primaryContainer,
                    selectedTextColor = colors.primaryContainer,
                    indicatorColor = Color.Transparent,
                    unselectedIconColor = colors.inverseOnSurface.copy(alpha = 0.72f),
                    unselectedTextColor = colors.inverseOnSurface.copy(alpha = 0.72f),
                ),
            )
        }
    }
}

@Composable
private fun TabGlyph(tab: RootTab) {
    val tint = LocalContentColor.current
    Canvas(Modifier.size(22.dp)) {
        val scale = size.width / 24f
        fun line(x1: Int, y1: Int, x2: Int, y2: Int) {
            drawLine(
                tint, Offset(x1 * scale, y1 * scale), Offset(x2 * scale, y2 * scale),
                strokeWidth = 1.7.dp.toPx(), cap = StrokeCap.Round,
            )
        }
        when (tab) {
            RootTab.HomeTab -> {
                line(3, 11, 12, 4)
                line(12, 4, 21, 11)
                line(6, 10, 6, 20)
                line(6, 20, 18, 20)
                line(18, 20, 18, 10)
                line(12, 15, 12, 20)
            }
            RootTab.EvidenceTab -> {
                line(6, 4, 18, 4)
                line(18, 4, 18, 20)
                line(18, 20, 6, 20)
                line(6, 20, 6, 4)
                line(9, 9, 15, 9)
                line(9, 13, 15, 13)
            }
            RootTab.TransactionsTab -> {
                line(4, 8, 20, 8)
                line(16, 4, 20, 8)
                line(16, 12, 20, 8)
                line(20, 16, 4, 16)
                line(8, 12, 4, 16)
                line(8, 20, 4, 16)
            }
            RootTab.RecapTab -> {
                line(4, 20, 20, 20)
                line(7, 17, 7, 12)
                line(12, 17, 12, 7)
                line(17, 17, 17, 4)
            }
        }
    }
}

private fun NavGraphBuilder.directoryRoutes(
    nav: NavHostController,
    directory: Directory,
    account: AccountViewModel,
    notify: (Int) -> Unit,
) {
    val listPath = directory.path
    composable(listPath) {
        val model: DirectoryListViewModel = viewModel(factory = viewModelFactory {
            initializer { DirectoryListViewModel(account.repository, directory, account::sessionExpired) }
        })
        DirectoryListRoute(
            directory = directory,
            model = model,
            onBack = { nav.popBackStack() },
            onAdd = { nav.navigate("$listPath/$NewRecord") { launchSingleTop = true } },
            onOpen = { entry -> nav.navigate("$listPath/${Uri.encode(entry.id)}") { launchSingleTop = true } },
        )
    }
    composable("$listPath/{$RecordId}") { backStackEntry ->
        val id = backStackEntry.arguments?.getString(RecordId)?.takeUnless { it == NewRecord }
        val model: DirectoryEditorViewModel = viewModel(factory = viewModelFactory {
            initializer { DirectoryEditorViewModel(account.repository, directory, id, account::sessionExpired) }
        })
        DirectoryEditorRoute(
            directory = directory,
            creating = id == null,
            model = model,
            onBack = { nav.popBackStack() },
            onComplete = { message ->
                if (nav.currentBackStackEntry == backStackEntry && nav.popBackStack()) notify(message)
            },
        )
    }
}
