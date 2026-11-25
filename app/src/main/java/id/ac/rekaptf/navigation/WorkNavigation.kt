package id.ac.rekaptf.navigation

import android.net.Uri
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import id.ac.rekaptf.ui.account.AccountViewModel
import id.ac.rekaptf.ui.evidence.EvidenceRoute
import id.ac.rekaptf.ui.income.IncomeEditorRoute
import id.ac.rekaptf.ui.income.IncomeListRoute
import id.ac.rekaptf.ui.recap.RecapRoute
import id.ac.rekaptf.ui.recap.SavedRecapEditorRoute
import id.ac.rekaptf.ui.recap.SavedRecapListRoute
import id.ac.rekaptf.ui.recap.SnapshotRoute

internal const val EvidencePath = "evidence"
internal const val DraftsPath = "drafts"
internal const val TransactionsPath = "transactions"
internal const val RecapPath = "recap"

internal const val SavedPath = "saved-recaps"
internal const val SnapshotPath = "snapshot"
private const val IdArg = "id"
private const val NewRecap = "new"

internal fun NavGraphBuilder.workRoutes(nav: NavHostController, account: AccountViewModel, home: String) {
    val repository = account.repository
    val expired = account::sessionExpired
    val back: () -> Unit = { nav.popBackStack() }
    val openIncome: (String) -> Unit = { nav.navigate(itemPath(TransactionsPath, it)) { launchSingleTop = true } }
    val openSaved: () -> Unit = { nav.navigate(SavedPath) { launchSingleTop = true } }
    val openFile: () -> Unit = { nav.navigate(SnapshotPath) { launchSingleTop = true } }

    composable(EvidencePath) {
        EvidenceRoute(repository, expired, back) { id ->
            nav.navigate(DraftsPath) {
                popUpTo(EvidencePath) { inclusive = true }
                launchSingleTop = true
            }
            nav.navigate(itemPath(DraftsPath, id)) { launchSingleTop = true }
        }
    }
    for (path in listOf(DraftsPath, TransactionsPath)) {
        val drafts = path == DraftsPath
        composable(path) {
            IncomeListRoute(
                repository, drafts, expired, back,
                onOpen = { id -> nav.navigate(itemPath(path, id)) { launchSingleTop = true } },
                onAddEvidence = if (drafts) ({ nav.navigate(EvidencePath) { launchSingleTop = true } }) else null,
            )
        }
        composable("$path/{$IdArg}") { entry ->
            val id = entry.arguments?.getString(IdArg) ?: return@composable
            IncomeEditorRoute(repository, drafts, id, expired, back) { transactionId ->
                nav.navigate(TransactionsPath) {
                    popUpTo(home)
                    launchSingleTop = true
                }
                openIncome(transactionId)
            }
        }
    }
    composable(RecapPath) {
        RecapRoute(repository, expired, back, openSaved, openFile, openIncome)
    }
    composable("$RecapPath/$SavedPath/{$IdArg}") { entry ->
        val id = entry.arguments?.getString(IdArg) ?: return@composable
        RecapRoute(repository, expired, back, openSaved, openFile, openIncome, savedId = id)
    }
    composable(SavedPath) {
        SavedRecapListRoute(
            repository, expired, back,
            onEdit = { id -> nav.navigate(itemPath(SavedPath, id ?: NewRecap)) { launchSingleTop = true } },
            onOpen = { savedId -> nav.navigate(itemPath("$RecapPath/$SavedPath", savedId)) { launchSingleTop = true } },
        )
    }
    composable("$SavedPath/{$IdArg}") { entry ->
        val id = entry.arguments?.getString(IdArg) ?: return@composable
        SavedRecapEditorRoute(repository, id.takeUnless { it == NewRecap }, expired, back) { savedId ->
            nav.navigate(itemPath("$RecapPath/$SavedPath", savedId)) { launchSingleTop = true }
        }
    }
    composable(SnapshotPath) {
        SnapshotRoute(back)
    }
}

private fun itemPath(collection: String, id: String): String = "$collection/${Uri.encode(id)}"

/** Rute detail transaksi, dipakai Beranda dan Rekap. */
internal fun incomePath(id: String): String = itemPath(TransactionsPath, id)
