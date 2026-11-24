package id.ac.rekaptf.ui.income

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.ac.rekaptf.R
import id.ac.rekaptf.data.Directory
import id.ac.rekaptf.data.DirectoryEntry
import id.ac.rekaptf.data.Income
import id.ac.rekaptf.data.RekapRepository
import id.ac.rekaptf.data.displayDate
import id.ac.rekaptf.data.money
import id.ac.rekaptf.data.parseDate
import id.ac.rekaptf.ui.components.BadgeTone
import id.ac.rekaptf.ui.components.ChipSelector
import id.ac.rekaptf.ui.components.ChoiceField
import id.ac.rekaptf.ui.components.DeleteConfirmation
import id.ac.rekaptf.ui.components.EmptyState
import id.ac.rekaptf.ui.components.FormPage
import id.ac.rekaptf.ui.components.ForwardGlyph
import id.ac.rekaptf.ui.components.HairLine
import id.ac.rekaptf.ui.components.InfoPanel
import id.ac.rekaptf.ui.components.LabeledInput
import id.ac.rekaptf.ui.components.MetaRow
import id.ac.rekaptf.ui.components.PageTitle
import id.ac.rekaptf.ui.components.PrimaryAction
import id.ac.rekaptf.ui.components.ScreenBar
import id.ac.rekaptf.ui.components.SecondaryAction
import id.ac.rekaptf.ui.components.SectionHeader
import id.ac.rekaptf.ui.components.StatusBadge
import id.ac.rekaptf.ui.components.WorkFeedback
import id.ac.rekaptf.ui.components.WorkOutcome
import id.ac.rekaptf.ui.components.WorkOutcomeEffect
import id.ac.rekaptf.ui.components.WorkState
import id.ac.rekaptf.ui.components.WorkViewModel
import id.ac.rekaptf.ui.components.theme.RekapColor
import id.ac.rekaptf.ui.components.theme.RekapShape
import id.ac.rekaptf.ui.components.theme.RekapSize
import id.ac.rekaptf.ui.components.theme.RekapSpacing
import id.ac.rekaptf.ui.components.workModel

private enum class DraftFilter { All, NeedsReview }

@Composable
internal fun IncomeListRoute(
    repository: RekapRepository,
    drafts: Boolean,
    onExpired: () -> Unit,
    onBack: () -> Unit,
    onOpen: (String) -> Unit,
    onAddEvidence: (() -> Unit)? = null,
) {
    val model = workModel(onExpired)
    val state by model.state.collectAsStateWithLifecycle()
    var input by rememberSaveable { mutableStateOf("") }
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf(DraftFilter.All) }
    val focus = LocalFocusManager.current
    val load = { model.load(R.string.work_error_load) { repository.listIncome(drafts, query) } }
    val search = {
        focus.clearFocus()
        val term = input.trim()
        query = term
        model.load(R.string.work_error_load) { repository.listIncome(drafts, term) }
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { load() }
    val entries = (state.value as? List<Income>).orEmpty()
    val visible = entries.filter { income ->
        !drafts || filter == DraftFilter.All || income.status != "converted"
    }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier.widthIn(max = RekapSize.MaxContentWidth).fillMaxWidth(),
            contentPadding = PaddingValues(RekapSpacing.Page),
            verticalArrangement = Arrangement.spacedBy(RekapSpacing.Tight),
        ) {
            item(key = "header") {
                Column(verticalArrangement = Arrangement.spacedBy(RekapSpacing.Tight)) {
                    ScreenBar(onBack = onBack) {
                        Text(
                            stringResource(R.string.app_name),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    PageTitle(stringResource(if (drafts) R.string.work_drafts else R.string.design_income_title))
                    if (onAddEvidence != null) {
                        PrimaryAction(onClick = onAddEvidence, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.design_evidence_select), Modifier.weight(1f))
                            ForwardGlyph()
                        }
                    }
                    LabeledInput(
                        value = input,
                        onValueChange = { input = it },
                        label = stringResource(R.string.work_search),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { search() }),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(RekapSpacing.Small)) {
                        TextButton(onClick = search) { Text(stringResource(R.string.search)) }
                        if (query.isNotEmpty()) {
                            TextButton(onClick = {
                                input = ""
                                query = ""
                                load()
                            }) { Text(stringResource(R.string.income_clear_filter)) }
                        }
                    }
                    if (drafts) {
                        ChipSelector(
                            options = DraftFilter.entries,
                            selected = filter,
                            label = { option ->
                                stringResource(
                                    if (option == DraftFilter.All) R.string.income_filter_all
                                    else R.string.income_filter_review,
                                )
                            },
                            onSelect = { filter = it },
                        )
                    }
                    WorkFeedback(state, retry = load)
                    if (state.error == null && state.loading.not() && entries.isNotEmpty()) {
                        FilterSummary(visible)
                    }
                    when {
                        state.error != null -> Unit
                        state.loading -> Unit
                        visible.isEmpty() -> when {
                            query.isNotEmpty() -> EmptyState(
                                title = stringResource(R.string.search_empty),
                                body = stringResource(R.string.income_search_empty_body),
                            )
                            drafts -> EmptyState(
                                title = stringResource(R.string.work_empty_drafts),
                                body = stringResource(R.string.income_drafts_empty_body),
                                actionLabel = onAddEvidence?.let { stringResource(R.string.design_evidence_select) },
                                onAction = onAddEvidence,
                            )
                            else -> EmptyState(
                                title = stringResource(R.string.work_empty_transactions),
                                body = stringResource(R.string.income_group_empty),
                            )
                        }
                    }
                }
            }
            visible.groupBy { it.paidOn }.forEach { (date, items) ->
                item(key = "date-$date") {
                    Column(Modifier.padding(top = RekapSpacing.Tight)) {
                        Text(
                            if (date.isBlank()) stringResource(R.string.work_none) else displayDate(date),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        HairLine(Modifier.padding(top = RekapSpacing.Micro))
                    }
                }
                items(items = items, key = { it.id }, contentType = { "income" }) { income ->
                    IncomeCard(income, draft = drafts) { onOpen(income.id) }
                }
            }
        }
    }
}

@Composable
private fun FilterSummary(items: List<Income>) {
    val total = items.sumOf { it.amount }
    Row(
        Modifier.fillMaxWidth().padding(top = RekapSpacing.Micro),
        horizontalArrangement = Arrangement.spacedBy(RekapSpacing.Section),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                stringResource(R.string.income_list_total),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(money(total), style = MaterialTheme.typography.titleLarge)
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                stringResource(R.string.income_count_label),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                stringResource(R.string.income_list_count, items.size),
                style = MaterialTheme.typography.titleSmall,
            )
        }
    }
}

@Composable
internal fun IncomeCard(income: Income, draft: Boolean = false, onClick: (() -> Unit)? = null) {
    val confirmed = income.status == "converted"
    val content: @Composable () -> Unit = {
        Row(
            Modifier.fillMaxWidth()
                .heightIn(min = if (draft) RekapSize.DraftRow else RekapSize.TransactionRow)
                .padding(horizontal = RekapSpacing.Card, vertical = RekapSpacing.Tight),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(RekapSpacing.Tight),
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    income.payerName.ifBlank { stringResource(R.string.design_income_no_payer) },
                    style = MaterialTheme.typography.titleSmall,
                    color = RekapColor.LocalListInk,
                )
                Text(
                    if (income.paidOn.isBlank()) stringResource(R.string.design_income_no_amount_date)
                    else displayDate(income.paidOn),
                    style = MaterialTheme.typography.bodySmall,
                    color = RekapColor.LocalListMuted,
                )
                if (draft) {
                    StatusBadge(
                        text = stringResource(
                            if (confirmed) R.string.design_income_confirmed else R.string.design_draft_outside_total,
                        ),
                        tone = if (confirmed) BadgeTone.Success else BadgeTone.Muted,
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    if (income.amount > 0) money(income.amount) else stringResource(R.string.design_income_no_amount),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (confirmed || !draft) RekapColor.Success else RekapColor.LocalListInk,
                )
                if (draft) {
                    Text(
                        stringResource(R.string.design_draft_review),
                        style = MaterialTheme.typography.labelSmall,
                        color = RekapColor.LocalListStatus,
                    )
                }
            }
        }
    }
    val shape = if (draft) RekapShape.DraftCard else RekapShape.TransactionCard
    if (onClick == null) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = shape,
            color = MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, RekapColor.Outline),
            content = content,
        )
    } else {
        Surface(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
            shape = shape,
            color = MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, RekapColor.Outline),
            content = content,
        )
    }
}

private data class IncomeEditorData(
    val income: Income,
    val categories: List<DirectoryEntry>,
    val payers: List<DirectoryEntry>,
)

@Composable
internal fun IncomeEditorRoute(
    repository: RekapRepository,
    drafts: Boolean,
    id: String,
    onExpired: () -> Unit,
    onBack: () -> Unit,
    onConfirmed: (String) -> Unit,
) {
    val model = workModel(onExpired)
    val state by model.state.collectAsStateWithLifecycle()
    var editing by rememberSaveable(id) { mutableStateOf(false) }
    val leave = { if (editing) editing = false else onBack() }
    val load = {
        model.load(R.string.work_error_load) {
            IncomeEditorData(
                repository.getIncome(drafts, id),
                repository.list(Directory.Categories),
                repository.list(Directory.Payers),
            )
        }
    }
    LaunchedEffect(id, drafts) {
        if (state.value == null && !state.loading) load()
    }
    WorkOutcomeEffect(model) { result ->
        when (result) {
            is WorkOutcome.Confirmed -> onConfirmed(result.id)
            WorkOutcome.Back -> onBack()
            is WorkOutcome.Draft -> Unit
        }
    }
    BackHandler(enabled = state.busy || editing) { if (!state.busy) leave() }
    FormPage {
        val data = state.value as? IncomeEditorData
        if (data != null && (drafts || editing)) {
            IncomeForm(data, drafts, state, model, repository, onBack = leave)
        } else {
            ScreenBar(onBack = leave, enabled = !state.busy) {
                Text(
                    stringResource(R.string.app_name),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            PageTitle(stringResource(if (drafts) R.string.design_draft_review else R.string.design_income_detail))
            WorkFeedback(state, retry = if (data == null) load else null)
            if (data != null) {
                IncomeDetails(data, state.busy, onEdit = { editing = true }) {
                    model.act(R.string.work_error_save) {
                        repository.deleteIncome(drafts = false, id = data.income.id)
                        WorkOutcome.Back
                    }
                }
            }
        }
    }
}

@Composable
private fun IncomeDetails(data: IncomeEditorData, busy: Boolean, onEdit: () -> Unit, onDelete: () -> Unit) {
    val item = data.income
    var confirming by rememberSaveable(item.id) { mutableStateOf(false) }
    val payer = data.payers.firstOrNull { it.id == item.payerId }?.name ?: item.payerName
    val category = data.categories.firstOrNull { it.id == item.categoryId }?.name.orEmpty()
    Row(verticalAlignment = Alignment.CenterVertically) {
        StatusBadge(stringResource(R.string.design_income_confirmed), BadgeTone.Success)
        Text(
            item.id,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = RekapSpacing.Small),
        )
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RekapShape.Square,
        color = MaterialTheme.colorScheme.inverseSurface,
        contentColor = MaterialTheme.colorScheme.inverseOnSurface,
    ) {
        Column(Modifier.padding(RekapSpacing.Section), verticalArrangement = Arrangement.spacedBy(RekapSpacing.Micro)) {
            Text(stringResource(R.string.design_income_date), style = MaterialTheme.typography.bodySmall)
            Text(money(item.amount), style = MaterialTheme.typography.displaySmall, color = RekapColor.Accent)
            Text(
                if (item.paidOn.isBlank()) stringResource(R.string.design_income_no_amount_date)
                else displayDate(item.paidOn),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
    MetaRow(stringResource(R.string.work_payer), payer)
    MetaRow(stringResource(R.string.work_category), category)
    MetaRow(stringResource(R.string.income_id_label), item.id)
    MetaRow(stringResource(R.string.work_reference), item.reference)
    MetaRow(stringResource(R.string.work_note), item.note)
    InfoPanel(title = item.source, body = stringResource(R.string.income_source_note))
    PrimaryAction(onClick = onEdit, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.design_income_edit), Modifier.weight(1f))
        ForwardGlyph()
    }
    SecondaryAction(onClick = { confirming = true }, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.design_income_delete_action), color = MaterialTheme.colorScheme.error)
    }
    if (confirming) {
        AlertDialog(
            onDismissRequest = { confirming = false },
            title = { Text(stringResource(R.string.design_income_delete_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(RekapSpacing.Small)) {
                    if (payer.isNotBlank()) Text(payer, style = MaterialTheme.typography.titleMedium)
                    Text(money(item.amount), style = MaterialTheme.typography.titleLarge)
                    Text(displayDate(item.paidOn))
                    Text(stringResource(R.string.design_income_delete_effect))
                }
            },
            confirmButton = {
                TextButton(onClick = { confirming = false; onDelete() }) {
                    Text(stringResource(R.string.design_income_delete_action), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirming = false }) { Text(stringResource(R.string.design_income_delete_cancel)) }
            },
        )
    }
}

@Composable
private fun IncomeForm(
    data: IncomeEditorData,
    drafts: Boolean,
    state: WorkState,
    model: WorkViewModel,
    repository: RekapRepository,
    onBack: () -> Unit,
) {
    val item = data.income
    var amount by rememberSaveable(item.id) { mutableStateOf(item.amount.takeIf { it != 0L }?.toString().orEmpty()) }
    var date by rememberSaveable(item.id) { mutableStateOf(item.paidOn) }
    var payerName by rememberSaveable(item.id) { mutableStateOf(item.payerName) }
    var reference by rememberSaveable(item.id) { mutableStateOf(item.reference) }
    var note by rememberSaveable(item.id) { mutableStateOf(item.note) }
    var category by rememberSaveable(item.id) { mutableStateOf(item.categoryId) }
    var payer by rememberSaveable(item.id) { mutableStateOf(item.payerId) }
    var error by remember { mutableStateOf<Int?>(null) }
    var confirming by rememberSaveable { mutableStateOf(false) }
    var discarding by rememberSaveable(item.id) { mutableStateOf(false) }
    val converted = drafts && item.status == "converted"
    val enabled = !state.busy && !converted
    val initialAmount = item.amount.takeIf { it != 0L }?.toString().orEmpty()
    val dirty = !converted && (amount != initialAmount || date != item.paidOn || payerName != item.payerName ||
        reference != item.reference || note != item.note || category != item.categoryId || payer != item.payerId)
    val requestBack = { if (dirty) discarding = true else onBack() }
    BackHandler(enabled = dirty && !state.busy, onBack = requestBack)
    ScreenBar(onBack = requestBack, enabled = !state.busy) {
        if (dirty) {
            Text(
                stringResource(R.string.form_unsaved_hint),
                style = MaterialTheme.typography.labelSmall,
                color = RekapColor.MutedText,
            )
        }
    }
    PageTitle(stringResource(if (drafts) R.string.design_draft_review else R.string.design_income_edit))
    WorkFeedback(state)
    if (discarding) {
        AlertDialog(
            onDismissRequest = { discarding = false },
            title = { Text(stringResource(R.string.design_discard_title)) },
            text = { Text(stringResource(R.string.design_discard_body)) },
            confirmButton = {
                TextButton(onClick = { discarding = false; onBack() }) {
                    Text(stringResource(R.string.design_discard_action), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { discarding = false }) { Text(stringResource(R.string.design_discard_stay)) }
            },
        )
    }
    if (drafts) {
        InfoPanel(
            title = stringResource(if (converted) R.string.design_income_confirmed else R.string.design_draft_outside_total),
            body = stringResource(if (converted) R.string.work_converted_notice else R.string.draft_review_hint),
        )
    }
    SectionHeader(stringResource(R.string.form_section_amount))
    LabeledInput(
        value = amount,
        onValueChange = { amount = it.filter(Char::isDigit); error = null },
        label = stringResource(R.string.work_amount),
        modifier = Modifier.fillMaxWidth(),
        enabled = enabled,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
    )
    LabeledInput(
        value = date,
        onValueChange = { date = it.take(10); error = null },
        label = stringResource(R.string.work_paid_on),
        modifier = Modifier.fillMaxWidth(),
        enabled = enabled,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
    )
    SectionHeader(stringResource(R.string.form_section_links))
    LinkedChoice(R.string.work_payer, payer, data.payers, enabled) { payer = it }
    LinkedChoice(R.string.work_category, category, data.categories, enabled) { category = it }
    LabeledInput(
        value = payerName,
        onValueChange = { payerName = it },
        label = stringResource(R.string.work_payer_name),
        modifier = Modifier.fillMaxWidth(),
        enabled = enabled,
    )
    SectionHeader(stringResource(R.string.form_section_note))
    LabeledInput(
        value = reference,
        onValueChange = { reference = it },
        label = stringResource(R.string.work_reference),
        modifier = Modifier.fillMaxWidth(),
        enabled = enabled,
    )
    LabeledInput(
        value = note,
        onValueChange = { note = it },
        label = stringResource(R.string.work_note),
        modifier = Modifier.fillMaxWidth(),
        minLines = 2,
        enabled = enabled,
    )
    error?.let { Text(stringResource(it), color = MaterialTheme.colorScheme.error) }

    fun fields(confirm: Boolean): Income? {
        val parsedAmount = amount.toLongOrNull() ?: if (amount.isBlank() && drafts && !confirm) 0L else null
        if (parsedAmount == null || ((confirm || !drafts) && parsedAmount <= 0L)) {
            error = R.string.work_invalid_amount
            return null
        }
        if ((date.isNotBlank() || confirm || !drafts) && parseDate(date) == null) {
            error = R.string.work_invalid_date
            return null
        }
        return item.copy(
            amount = parsedAmount,
            paidOn = date,
            payerName = payerName,
            reference = reference,
            note = note,
            categoryId = category,
            payerId = payer,
        )
    }
    if (!converted) {
        if (drafts) {
            PrimaryAction(enabled = enabled, modifier = Modifier.fillMaxWidth(), onClick = {
                fields(true)?.let { income ->
                    model.act(R.string.work_error_save) { WorkOutcome.Confirmed(repository.confirmDraft(income).id) }
                }
            }) {
                Text(stringResource(R.string.work_confirm_income), Modifier.weight(1f))
                ForwardGlyph()
            }
        }
        val save: () -> Unit = {
            fields(false)?.let { income ->
                model.act(R.string.work_error_save) {
                    repository.saveIncome(drafts, income)
                    WorkOutcome.Back
                }
            }
        }
        if (drafts) {
            SecondaryAction(onClick = save, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.work_save_draft))
            }
        } else {
            PrimaryAction(onClick = save, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.save), Modifier.weight(1f))
                ForwardGlyph()
            }
        }
    }
    if (drafts && !converted) {
        TextButton(enabled = !state.busy, onClick = { confirming = true }) {
            Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
        }
    }
    if (confirming) {
        DeleteConfirmation(onDismiss = { confirming = false }, onConfirm = {
            confirming = false
            model.act(R.string.work_error_save) {
                repository.deleteIncome(drafts, item.id)
                WorkOutcome.Back
            }
        })
    }
}

@Composable
private fun LinkedChoice(
    label: Int,
    selected: String,
    entries: List<DirectoryEntry>,
    enabled: Boolean,
    onSelect: (String) -> Unit,
) {
    val none = stringResource(R.string.work_none)
    val unknown = stringResource(R.string.work_unavailable_link)
    val choices = listOf(DirectoryEntry("", none, "")) + entries
    val current = choices.firstOrNull { it.id == selected }?.name ?: unknown
    ChoiceField(stringResource(label), current, choices, enabled, { it.name }) { onSelect(it.id) }
}
