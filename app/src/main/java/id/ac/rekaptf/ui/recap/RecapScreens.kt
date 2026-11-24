package id.ac.rekaptf.ui.recap

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import id.ac.rekaptf.R
import id.ac.rekaptf.data.DateRange
import id.ac.rekaptf.data.Period
import id.ac.rekaptf.data.Recap
import id.ac.rekaptf.data.RekapRepository
import id.ac.rekaptf.data.SavedRecap
import id.ac.rekaptf.data.SnapshotFiles
import id.ac.rekaptf.data.displayDate
import id.ac.rekaptf.data.money
import id.ac.rekaptf.data.parseDate
import id.ac.rekaptf.ui.components.DeleteConfirmation
import id.ac.rekaptf.ui.components.EmptyState
import id.ac.rekaptf.ui.components.ErrorText
import id.ac.rekaptf.ui.components.FormPage
import id.ac.rekaptf.ui.components.ForwardGlyph
import id.ac.rekaptf.ui.components.HeroSummary
import id.ac.rekaptf.ui.components.InfoPanel
import id.ac.rekaptf.ui.components.LabeledInput
import id.ac.rekaptf.ui.components.Message
import id.ac.rekaptf.ui.components.PageTitle
import id.ac.rekaptf.ui.components.PrimaryAction
import id.ac.rekaptf.ui.components.Progress
import id.ac.rekaptf.ui.components.ScreenBar
import id.ac.rekaptf.ui.components.SecondaryAction
import id.ac.rekaptf.ui.components.SectionHeader
import id.ac.rekaptf.ui.components.WorkFeedback
import id.ac.rekaptf.ui.components.WorkOutcome
import id.ac.rekaptf.ui.components.WorkOutcomeEffect
import id.ac.rekaptf.ui.components.WorkState
import id.ac.rekaptf.ui.components.WorkViewModel
import id.ac.rekaptf.ui.components.toMessage
import id.ac.rekaptf.ui.components.workModel
import id.ac.rekaptf.ui.components.theme.RekapColor
import id.ac.rekaptf.ui.components.theme.RekapShape
import id.ac.rekaptf.ui.components.theme.RekapSize
import id.ac.rekaptf.ui.components.theme.RekapSpacing
import id.ac.rekaptf.ui.income.IncomeCard
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal data class SnapshotExportState(
    val pending: Boolean = false,
    val saving: Boolean = false,
    val saved: Boolean = false,
    val error: Message? = null,
)

internal class SnapshotExportViewModel : ViewModel() {
    private var selected: Recap? = null
    private val status = MutableStateFlow(SnapshotExportState())
    val state = status.asStateFlow()

    fun prepare(recap: Recap) {
        selected = recap
        status.value = SnapshotExportState(pending = true)
    }

    fun receive(uri: Uri?, files: SnapshotFiles) {
        val recap = selected
        selected = null
        if (uri == null) {
            status.value = SnapshotExportState()
            return
        }
        if (recap == null) {
            status.value = SnapshotExportState(error = Message(R.string.work_snapshot_lost_context))
            return
        }
        status.value = SnapshotExportState(saving = true)
        viewModelScope.launch {
            status.value = try {
                files.save(uri, recap)
                SnapshotExportState(saved = true)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                SnapshotExportState(error = error.toMessage(R.string.work_error_snapshot))
            }
        }
    }
}

@Composable
internal fun RecapRoute(
    repository: RekapRepository,
    onExpired: () -> Unit,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    onOpenFile: () -> Unit,
    onOpenIncome: (String) -> Unit,
    savedId: String? = null,
) {
    val model = workModel(onExpired)
    val state by model.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snapshotFiles = remember(context) { SnapshotFiles(context.applicationContext) }
    val export: SnapshotExportViewModel = viewModel()
    val exportState by export.state.collectAsStateWithLifecycle()
    var initial by remember(savedId) { mutableStateOf<SavedRecap?>(null) }
    var periodKey by rememberSaveable(savedId) { mutableStateOf(Period.MONTH.key) }
    var from by rememberSaveable(savedId) { mutableStateOf(Period.MONTH.range().from) }
    var to by rememberSaveable(savedId) { mutableStateOf(Period.MONTH.range().to) }
    var appliedFrom by rememberSaveable(savedId) { mutableStateOf(from) }
    var appliedTo by rememberSaveable(savedId) { mutableStateOf(to) }
    var revision by rememberSaveable(savedId) { mutableIntStateOf(0) }
    var validation by remember { mutableStateOf(false) }
    val load = {
        model.load(R.string.work_error_load) {
            if (savedId == null) repository.recap(DateRange(appliedFrom, appliedTo))
            else {
                val filter = repository.savedRecap(savedId)
                initial = filter
                periodKey = filter.period.key
                from = filter.range.from
                to = filter.range.to
                repository.recap(filter.range)
            }
        }
    }
    LaunchedEffect(savedId, revision) { load() }
    val create = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        export.receive(uri, snapshotFiles)
    }
    BackHandler(enabled = exportState.saving) {}
    val recap = state.value as? Recap
    RecapPage {
        item(key = "controls") {
            Column(verticalArrangement = Arrangement.spacedBy(RekapSpacing.Tight)) {
                ScreenBar(onBack = onBack, enabled = !exportState.saving) {
                    Text(
                        stringResource(R.string.app_name),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                PageTitle(stringResource(R.string.work_recap))
                initial?.let { Text(it.name, style = MaterialTheme.typography.titleMedium) }
                if (savedId == null) {
                    val period = Period.fromKey(periodKey)
                    RangeFields(period, DateRange(from, to), !state.busy, {
                        periodKey = it.key
                        if (it != Period.CUSTOM) {
                            val range = it.range(parseDate(from) ?: id.ac.rekaptf.data.today())
                            from = range.from
                            to = range.to
                        }
                        validation = false
                    }) {
                        from = it.from
                        to = it.to
                        validation = false
                    }
                    if (validation) {
                        Text(stringResource(R.string.work_range_error), color = MaterialTheme.colorScheme.error)
                    }
                    PrimaryAction(enabled = !state.busy, modifier = Modifier.fillMaxWidth(), onClick = {
                        validation = !DateRange(from, to).valid
                        if (!validation) {
                            appliedFrom = from
                            appliedTo = to
                            revision++
                        }
                    }) { Text(stringResource(R.string.work_show_recap)) }
                }
                WorkFeedback(state, retry = load)
            }
        }
        if (recap != null) recapSummary(recap, onOpenIncome)
        if (recap != null || exportState.saving || exportState.saved || exportState.error != null || savedId == null) {
            item(key = "actions") {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    if (recap != null) {
                        PrimaryAction(enabled = !exportState.pending && !exportState.saving, modifier = Modifier.fillMaxWidth(), onClick = {
                            export.prepare(recap)
                            create.launch("RekapTF-${recap.range.from}-${recap.range.to}.json")
                        }) { Text(stringResource(R.string.design_save_snapshot)) }
                    }
                    if (exportState.saving) Progress()
                    if (exportState.saved) Text(stringResource(R.string.work_snapshot_saved))
                    exportState.error?.let { ErrorText(it) }
                    if (savedId == null) {
                        InfoPanel(
                            title = stringResource(R.string.work_saved_recaps),
                            body = stringResource(R.string.recap_saved_hint),
                        )
                        SecondaryAction(enabled = !exportState.saving, onClick = onSaved, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.work_saved_recaps), Modifier.weight(1f))
                            ForwardGlyph()
                        }
                        SecondaryAction(enabled = !exportState.saving, onClick = onOpenFile, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.work_open_snapshot), Modifier.weight(1f))
                            ForwardGlyph()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecapPage(content: LazyListScope.() -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            content = content,
        )
    }
}

private fun LazyListScope.recapSummary(recap: Recap, onOpenIncome: ((String) -> Unit)? = null) {
    item(key = "summary") {
        Column(verticalArrangement = Arrangement.spacedBy(RekapSpacing.Tight)) {
            HeroSummary(
                caption = stringResource(R.string.work_total),
                amount = money(recap.total),
                supporting = stringResource(R.string.work_count, recap.count),
                footnote = stringResource(
                    R.string.work_range,
                    displayDate(recap.range.from),
                    displayDate(recap.range.to),
                ),
            )
            if (recap.items.isEmpty()) {
                EmptyState(title = stringResource(R.string.work_empty_recap))
            } else {
                SectionHeader(stringResource(R.string.design_recap_transactions))
            }
        }
    }
    itemsIndexed(
        items = recap.items,
        key = { index, income ->
            // Snapshot rows have no stable record ID.
            if (onOpenIncome == null || income.id.isEmpty()) "row:$index" else "income:${income.id}"
        },
        contentType = { _, _ -> "income" },
    ) { _, income ->
        val open = onOpenIncome?.takeIf { income.id.isNotEmpty() }
        IncomeCard(income, onClick = if (open == null) null else ({ open(income.id) }))
    }
}

@Composable
internal fun SnapshotRoute(onBack: () -> Unit) {
    val model = workModel(onExpired = {})
    val state by model.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val files = remember(context) { SnapshotFiles(context.applicationContext) }
    val open = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) model.load(R.string.work_error_snapshot) { files.open(uri) }
    }
    RecapPage {
        item(key = "controls") {
            Column(verticalArrangement = Arrangement.spacedBy(RekapSpacing.Tight)) {
                ScreenBar(onBack = onBack) {
                    Text(
                        stringResource(R.string.app_name),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                PageTitle(stringResource(R.string.design_snapshot_title))
                InfoPanel(
                    title = stringResource(R.string.design_snapshot_read_only),
                    body = stringResource(R.string.recap_snapshot_note),
                )
                SecondaryAction(enabled = !state.loading, modifier = Modifier.fillMaxWidth(), onClick = {
                    open.launch(arrayOf("application/json", "application/octet-stream", "text/plain"))
                }) { Text(stringResource(R.string.work_snapshot_choose)) }
                WorkFeedback(state)
            }
        }
        (state.value as? Recap)?.let { recapSummary(it) }
        item(key = "close-snapshot") {
            SecondaryAction(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.design_snapshot_close))
            }
        }
    }
}

@Composable
internal fun SavedRecapListRoute(
    repository: RekapRepository,
    onExpired: () -> Unit,
    onBack: () -> Unit,
    onEdit: (String?) -> Unit,
    onOpen: ((String) -> Unit)? = null,
) {
    val model = workModel(onExpired)
    val state by model.state.collectAsStateWithLifecycle()
    var input by rememberSaveable { mutableStateOf("") }
    var query by rememberSaveable { mutableStateOf("") }
    val load = { model.load(R.string.work_error_load) { repository.savedRecaps(query) } }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { load() }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(RekapSpacing.Tight)) {
                    ScreenBar(onBack = onBack) {
                        Text(
                            stringResource(R.string.app_name),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    PageTitle(stringResource(R.string.work_saved_recaps))
                    InfoPanel(
                        title = stringResource(R.string.work_saved_recaps),
                        body = stringResource(R.string.design_saved_recap_note),
                    )
                    LabeledInput(
                        value = input, onValueChange = { input = it },
                        label = stringResource(R.string.work_recap_search),
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                    )
                    TextButton(onClick = {
                        val term = input.trim()
                        query = term
                        model.load(R.string.work_error_load) { repository.savedRecaps(term) }
                    }) { Text(stringResource(R.string.search)) }
                    PrimaryAction(onClick = { onEdit(null) }, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.work_add_recap), Modifier.weight(1f))
                        ForwardGlyph()
                    }
                    WorkFeedback(state, retry = load)
                    @Suppress("UNCHECKED_CAST")
                    val entries = state.value as? List<SavedRecap>
                    if (entries?.isEmpty() == true) {
                        Text(stringResource(if (query.isBlank()) R.string.work_recap_empty else R.string.search_empty))
                    }
                }
            }
            @Suppress("UNCHECKED_CAST")
            val entries = state.value as? List<SavedRecap> ?: emptyList()
            items(entries, key = { it.id }) { saved ->
                OutlinedCard(modifier = Modifier.fillMaxWidth(), shape = RectangleShape) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(saved.name, style = MaterialTheme.typography.titleMedium)
                        Text(stringResource(R.string.work_range, displayDate(saved.range.from), displayDate(saved.range.to)))
                        if (onOpen != null) {
                            PrimaryAction(onClick = { onOpen(saved.id) }, modifier = Modifier.fillMaxWidth()) {
                                Text(stringResource(R.string.work_open_recap), Modifier.weight(1f))
                                ForwardGlyph()
                            }
                        }
                        SecondaryAction(onClick = { onEdit(saved.id) }, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.work_edit_recap))
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun SavedRecapEditorRoute(
    repository: RekapRepository,
    id: String?,
    onExpired: () -> Unit,
    onBack: () -> Unit,
    onOpenRecap: (String) -> Unit,
) {
    val model = workModel(onExpired)
    val state by model.state.collectAsStateWithLifecycle()
    val load = {
        if (id == null) model.load(R.string.work_error_load) {
            SavedRecap("", "", Period.MONTH, Period.MONTH.range())
        } else model.load(R.string.work_error_load) { repository.savedRecap(id) }
    }
    LaunchedEffect(id) {
        if (state.value == null && !state.loading) load()
    }
    WorkOutcomeEffect(model) { if (it == WorkOutcome.Back) onBack() }
    BackHandler(enabled = state.busy) {}
    FormPage {
        ScreenBar(onBack = onBack, enabled = !state.busy) {
            Text(
                stringResource(R.string.app_name),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        PageTitle(stringResource(if (id == null) R.string.work_add_recap else R.string.work_edit_recap))
        Text(stringResource(R.string.design_saved_recap_note), color = MaterialTheme.colorScheme.onSurfaceVariant)
        WorkFeedback(state, retry = if (state.value == null) load else null)
        (state.value as? SavedRecap)?.let { saved ->
            SavedRecapForm(saved, state, model, repository, onOpenRecap)
        }
    }
}

@Composable
private fun SavedRecapForm(
    saved: SavedRecap,
    state: WorkState,
    model: WorkViewModel,
    repository: RekapRepository,
    onOpenRecap: (String) -> Unit,
) {
    var name by rememberSaveable(saved.id) { mutableStateOf(saved.name) }
    var periodKey by rememberSaveable(saved.id) { mutableStateOf(saved.period.key) }
    var from by rememberSaveable(saved.id) { mutableStateOf(saved.range.from) }
    var to by rememberSaveable(saved.id) { mutableStateOf(saved.range.to) }
    var error by remember { mutableStateOf<Int?>(null) }
    var confirming by rememberSaveable { mutableStateOf(false) }
    val period = Period.fromKey(periodKey)
    LabeledInput(value = name, onValueChange = { name = it; error = null },
        label = stringResource(R.string.work_recap_name), modifier = Modifier.fillMaxWidth(),
        enabled = !state.busy, singleLine = true)
    RangeFields(period, DateRange(from, to), !state.busy, {
        periodKey = it.key
        if (it != Period.CUSTOM) {
            val range = it.range(parseDate(from) ?: id.ac.rekaptf.data.today())
            from = range.from
            to = range.to
        }
        error = null
    }) {
        from = it.from
        to = it.to
        error = null
    }
    error?.let { Text(stringResource(it), color = MaterialTheme.colorScheme.error) }
    PrimaryAction(enabled = !state.busy, modifier = Modifier.fillMaxWidth(), onClick = {
        error = when {
            name.isBlank() -> R.string.error_entry_name
            !DateRange(from, to).valid -> R.string.work_range_error
            else -> null
        }
        if (error == null) {
            val updated = SavedRecap(saved.id, name.trim(), period, DateRange(from, to))
            model.act(R.string.work_error_save) {
                repository.saveRecap(updated)
                WorkOutcome.Back
            }
        }
    }) { Text(stringResource(R.string.save)) }
    if (saved.id.isNotEmpty()) {
        SecondaryAction(enabled = !state.busy, onClick = { onOpenRecap(saved.id) }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.work_open_recap))
        }
        TextButton(enabled = !state.busy, onClick = { confirming = true }) {
            Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
        }
        if (confirming) {
            DeleteConfirmation(onDismiss = { confirming = false }, onConfirm = {
                confirming = false
                model.act(R.string.work_error_save) {
                    repository.deleteRecap(saved.id)
                    WorkOutcome.Back
                }
            })
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun RangeFields(
    period: Period,
    range: DateRange,
    enabled: Boolean,
    onPeriod: (Period) -> Unit,
    onRange: (DateRange) -> Unit,
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(RekapSpacing.Small), verticalArrangement = Arrangement.spacedBy(RekapSpacing.Small)) {
        for (option in Period.entries) {
            FilterChip(
                selected = option == period,
                onClick = { onPeriod(option) },
                enabled = enabled,
                modifier = Modifier.heightIn(min = RekapSize.Touch),
                label = { Text(stringResource(option.label)) },
                shape = RekapShape.Square,
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    labelColor = RekapColor.Ink,
                    selectedContainerColor = RekapColor.Accent,
                    selectedLabelColor = RekapColor.Ink,
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = enabled,
                    selected = option == period,
                    borderColor = MaterialTheme.colorScheme.outline,
                    selectedBorderColor = MaterialTheme.colorScheme.outline,
                ),
            )
        }
    }
    if (period == Period.CUSTOM) {
        DateInput(R.string.work_date_from, range.from, enabled) { onRange(range.copy(from = it)) }
        DateInput(R.string.work_date_to, range.to, enabled) { onRange(range.copy(to = it)) }
    } else {
        var anchor by rememberSaveable(period.key) { mutableStateOf(range.from) }
        val shown = if (parseDate(anchor)?.let(period::range) == range || parseDate(anchor) == null) anchor else range.from
        DateInput(R.string.work_anchor_date, shown, enabled) { value ->
            anchor = value
            val parsed = parseDate(value)
            onRange(if (parsed == null) DateRange(value, value) else period.range(parsed))
        }
    }
}

@Composable
private fun DateInput(label: Int, value: String, enabled: Boolean, onChange: (String) -> Unit) {
    LabeledInput(
        value = value,
        onValueChange = { onChange(it.take(10)) },
        label = stringResource(label),
        modifier = Modifier.fillMaxWidth(),
        enabled = enabled,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
    )
}
