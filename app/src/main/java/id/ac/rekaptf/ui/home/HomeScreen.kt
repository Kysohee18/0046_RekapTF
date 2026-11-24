package id.ac.rekaptf.ui.home

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import id.ac.rekaptf.R
import id.ac.rekaptf.data.Directory
import id.ac.rekaptf.data.Income
import id.ac.rekaptf.data.Period
import id.ac.rekaptf.data.Recap
import id.ac.rekaptf.data.RekapRepository
import id.ac.rekaptf.data.money
import id.ac.rekaptf.ui.account.AccountState
import id.ac.rekaptf.ui.components.CompositionTile
import id.ac.rekaptf.ui.components.EmptyState
import id.ac.rekaptf.ui.components.ErrorText
import id.ac.rekaptf.ui.components.FormPage
import id.ac.rekaptf.ui.components.HeroSummary
import id.ac.rekaptf.ui.components.InfoPanel
import id.ac.rekaptf.ui.components.PageTitle
import id.ac.rekaptf.ui.components.PrimaryAction
import id.ac.rekaptf.ui.components.Progress
import id.ac.rekaptf.ui.components.ScreenBar
import id.ac.rekaptf.ui.components.SectionHeader
import id.ac.rekaptf.ui.components.SecondaryAction
import id.ac.rekaptf.ui.components.WorkState
import id.ac.rekaptf.ui.components.theme.RekapSpacing
import id.ac.rekaptf.ui.income.IncomeCard
import kotlin.math.roundToInt

internal data class HomeCategoryTotal(val label: String, val amount: Long, val share: Int)

internal data class HomeData(
    val recap: Recap,
    val categories: List<HomeCategoryTotal>,
    val recent: List<Income>,
    val draftsToReview: Int,
)

/**
 * Satu permintaan untuk Beranda: rekap bulan aktif, komposisi kategori, dan draf
 * yang belum dikonfirmasi. Hanya transaksi terkonfirmasi yang dihitung.
 */
internal suspend fun loadHome(repository: RekapRepository, uncategorized: String): HomeData {
    val recap = repository.recap(Period.MONTH.range())
    val names = repository.list(Directory.Categories).associate { it.id to it.name }
    val pending = repository.listIncome(drafts = true).count { it.status != "converted" }
    val categories = recap.items
        .groupBy { income -> names[income.categoryId] ?: uncategorized }
        .map { (label, items) ->
            val amount = items.sumOf { it.amount }
            HomeCategoryTotal(
                label = label,
                amount = amount,
                share = if (recap.total > 0) (amount * 100.0 / recap.total).roundToInt() else 0,
            )
        }
        .sortedByDescending { it.amount }
        .take(2)
    return HomeData(
        recap = recap,
        categories = categories,
        recent = recap.items.sortedByDescending { it.paidOn }.take(3),
        draftsToReview = pending,
    )
}

@Composable
internal fun HomeScreen(
    business: String,
    summary: WorkState,
    onRetrySummary: () -> Unit,
    onEvidence: () -> Unit,
    onDrafts: () -> Unit,
    onRecap: () -> Unit,
    onTransactions: () -> Unit,
    onOpenIncome: (String) -> Unit,
    onManage: () -> Unit,
) {
    FormPage {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            PageTitle(stringResource(R.string.home), Modifier.weight(1f))
            TextButton(onClick = onManage) {
                Text(stringResource(R.string.shell_data_account))
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(RekapSpacing.Micro)) {
            Text(
                stringResource(R.string.home_welcome),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(business, style = MaterialTheme.typography.titleMedium)
        }
        MonthlySummary(summary, onRetrySummary, onRecap)
        CategoryComposition(summary, onRecap)
        PrimaryAction(onClick = onEvidence, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.home_add_evidence), Modifier.weight(1f))
            Text("→", Modifier.clearAndSetSemantics { })
        }
        DraftsPanel(summary, onDrafts)
        val data = summary.value as? HomeData
        SectionHeader(
            title = stringResource(R.string.home_latest),
            actionLabel = stringResource(R.string.home_all),
            onAction = onTransactions,
        )
        when {
            summary.error != null -> Unit
            data == null -> Unit
            data.recent.isEmpty() -> EmptyState(
                title = stringResource(R.string.home_latest_empty),
                body = stringResource(R.string.home_latest_empty_body),
                actionLabel = stringResource(R.string.home_add_evidence),
                onAction = onEvidence,
            )
            else -> data.recent.forEach { income ->
                IncomeCard(income) { onOpenIncome(income.id) }
            }
        }
    }
}

@Composable
private fun MonthlySummary(summary: WorkState, onRetry: () -> Unit, onRecap: () -> Unit) {
    when (val data = summary.value as? HomeData) {
        null -> if (summary.error != null) {
            SurfaceError(onRetry)
        } else {
            LinearProgressIndicator(Modifier.fillMaxWidth())
        }
        else -> HeroSummary(
            caption = stringResource(R.string.shell_month_total),
            amount = money(data.recap.total),
            supporting = stringResource(R.string.work_count, data.recap.count),
            actionLabel = stringResource(R.string.home_recap),
            onAction = onRecap,
        )
    }
}

@Composable
private fun SurfaceError(onRetry: () -> Unit) {
    InfoPanel(
        title = stringResource(R.string.shell_summary_error),
        body = null,
        trailing = stringResource(R.string.retry),
        onClick = onRetry,
    )
}

@Composable
private fun CategoryComposition(summary: WorkState, onRecap: () -> Unit) {
    val data = summary.value as? HomeData ?: return
    if (data.categories.isEmpty()) return
    SectionHeader(
        title = stringResource(R.string.home_composition),
        actionLabel = stringResource(R.string.home_recap),
        onAction = onRecap,
    )
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(RekapSpacing.Tight),
    ) {
        data.categories.forEach { category ->
            CompositionTile(
                label = category.label,
                amount = money(category.amount),
                sharePercent = category.share,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun DraftsPanel(summary: WorkState, onDrafts: () -> Unit) {
    val data = summary.value as? HomeData ?: return
    val pending = data.draftsToReview
    InfoPanel(
        title = stringResource(R.string.home_drafts),
        body = if (pending > 0) {
            stringResource(R.string.home_drafts_pending, pending)
        } else {
            stringResource(R.string.home_drafts_clear)
        },
        trailing = "→",
        onClick = onDrafts,
    )
}

@Composable
internal fun DataAccountScreen(
    state: AccountState,
    business: String,
    onBack: () -> Unit,
    onCategories: () -> Unit,
    onPayers: () -> Unit,
    onSavedRecaps: () -> Unit,
    onSnapshot: () -> Unit,
    onSignOut: () -> Unit,
) {
    FormPage {
        ScreenBar(onBack = onBack, enabled = !state.busy) {
            Text(
                stringResource(R.string.app_name),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        PageTitle(stringResource(R.string.shell_data_account))
        Column(verticalArrangement = Arrangement.spacedBy(RekapSpacing.Micro)) {
            Text(business, style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(R.string.account_role_line),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        InfoPanel(
            title = stringResource(R.string.account_single_role),
            body = stringResource(R.string.account_single_role_body),
        )
        AccountShortcut(R.string.categories, !state.busy, onCategories)
        AccountShortcut(R.string.design_payers, !state.busy, onPayers)
        AccountShortcut(R.string.work_saved_recaps, !state.busy, onSavedRecaps)
        AccountShortcut(R.string.work_open_snapshot, !state.busy, onSnapshot)
        if (state.busy) Progress()
        state.error?.let { ErrorText(it) }
        SecondaryAction(onClick = onSignOut, modifier = Modifier.fillMaxWidth(), enabled = !state.busy) {
            Text(stringResource(R.string.sign_out), color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun AccountShortcut(@StringRes label: Int, enabled: Boolean, onClick: () -> Unit) {
    SecondaryAction(onClick = onClick, modifier = Modifier.fillMaxWidth(), enabled = enabled) {
        Text(stringResource(label), Modifier.weight(1f))
        Text("→", Modifier.clearAndSetSemantics { })
    }
}
