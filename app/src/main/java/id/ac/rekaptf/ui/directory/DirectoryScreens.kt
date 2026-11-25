package id.ac.rekaptf.ui.directory

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
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.ac.rekaptf.R
import id.ac.rekaptf.data.Directory
import id.ac.rekaptf.data.DirectoryEntry
import id.ac.rekaptf.ui.components.ErrorText
import id.ac.rekaptf.ui.components.FormPage
import id.ac.rekaptf.ui.components.ForwardGlyph
import id.ac.rekaptf.ui.components.InfoPanel
import id.ac.rekaptf.ui.components.LabeledInput
import id.ac.rekaptf.ui.components.PageTitle
import id.ac.rekaptf.ui.components.PrimaryAction
import id.ac.rekaptf.ui.components.Progress
import id.ac.rekaptf.ui.components.ScreenBar
import id.ac.rekaptf.ui.components.SecondaryAction
import id.ac.rekaptf.ui.components.theme.RekapColor
import id.ac.rekaptf.ui.components.theme.RekapShape
import id.ac.rekaptf.ui.components.theme.RekapSize
import id.ac.rekaptf.ui.components.theme.RekapSpacing

private val Directory.listTitle: Int
    get() = when (this) {
        Directory.Categories -> R.string.categories
        Directory.Payers -> R.string.design_payers
    }

private fun Directory.editorTitle(creating: Boolean): Int = when (this) {
    Directory.Categories -> if (creating) R.string.add_category else R.string.edit_category
    Directory.Payers -> if (creating) R.string.design_add_payer else R.string.design_edit_payer
}

@Composable
internal fun DirectoryListRoute(
    directory: Directory,
    model: DirectoryListViewModel,
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onOpen: (DirectoryEntry) -> Unit,
) {
    val state by model.state.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { model.load() }
    DirectoryListScreen(directory, state, onBack, onAdd, onOpen, model::load)
}

@Composable
private fun DirectoryListScreen(
    directory: Directory,
    state: DirectoryListState,
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onOpen: (DirectoryEntry) -> Unit,
    onSearch: (String) -> Unit,
) {
    var input by rememberSaveable { mutableStateOf(state.query) }
    val focus = LocalFocusManager.current
    val search = {
        focus.clearFocus()
        onSearch(input)
    }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier.widthIn(max = RekapSize.MaxContentWidth).fillMaxWidth(),
            contentPadding = PaddingValues(RekapSpacing.Page),
            verticalArrangement = Arrangement.spacedBy(RekapSpacing.Tight),
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
                    PageTitle(stringResource(directory.listTitle))
                    LabeledInput(
                        value = input,
                        onValueChange = { input = it },
                        label = stringResource(R.string.search_name),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { search() }),
                    )
                    TextButton(onClick = search) { Text(stringResource(R.string.search)) }
                    PrimaryAction(onClick = onAdd, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(directory.editorTitle(creating = true)), Modifier.weight(1f))
                        ForwardGlyph()
                    }
                    if (state.loading) Progress()
                    state.error?.let {
                        ErrorText(it)
                        TextButton(onClick = { onSearch(state.query) }) {
                            Text(stringResource(R.string.retry))
                        }
                    }
                    state.items?.let { entries ->
                        if (entries.isNotEmpty()) {
                            Text(
                                stringResource(R.string.directory_total, entries.size),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    if (!state.loading && state.error == null && state.items?.isEmpty() == true) {
                        Text(stringResource(if (state.query.isBlank()) R.string.directory_empty else R.string.search_empty))
                    }
                }
            }
            items(items = state.items.orEmpty(), key = { it.id }) { entry ->
                OutlinedCard(
                    onClick = { onOpen(entry) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = RekapSize.TransactionRow),
                    shape = RekapShape.TransactionCard,
                ) {
                    Row(
                        Modifier.padding(horizontal = RekapSpacing.Card, vertical = RekapSpacing.Tight),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(RekapSpacing.Small),
                    ) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(entry.name, style = MaterialTheme.typography.titleSmall, color = RekapColor.LocalListInk)
                            if (entry.note.isNotBlank()) {
                                Text(
                                    entry.note,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        ForwardGlyph()
                    }
                }
            }
        }
    }
}

@Composable
internal fun DirectoryEditorRoute(
    directory: Directory,
    creating: Boolean,
    model: DirectoryEditorViewModel,
    onBack: () -> Unit,
    onComplete: (Int) -> Unit,
) {
    val state by model.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.completed) { state.completed?.let(onComplete) }
    BackHandler(enabled = state.completed != null || (state.busy && (creating || state.entry != null))) {}

    FormPage {
        ScreenBar(
            onBack = onBack,
            enabled = state.completed == null && (!state.busy || (!creating && state.entry == null)),
        ) {
            Text(
                stringResource(R.string.app_name),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        PageTitle(stringResource(directory.editorTitle(creating)))
        if (state.busy) Progress()
        state.error?.let { ErrorText(it) }
        if (creating || state.entry != null) {
            DirectoryForm(
                directory = directory,
                entry = state.entry,
                enabled = !state.busy && state.completed == null,
                onEdit = model::clearError,
                onSave = model::save,
                onDelete = model::delete,
            )
        } else if (state.error != null) {
            PrimaryAction(onClick = model::load, enabled = !state.busy) { Text(stringResource(R.string.retry)) }
        }
    }
}

@Composable
private fun DirectoryForm(
    directory: Directory,
    entry: DirectoryEntry?,
    enabled: Boolean,
    onEdit: () -> Unit,
    onSave: (String, String) -> Unit,
    onDelete: () -> Unit,
) {
    var name by rememberSaveable(entry?.id) { mutableStateOf(entry?.name.orEmpty()) }
    var note by rememberSaveable(entry?.id) { mutableStateOf(entry?.note.orEmpty()) }
    var confirming by rememberSaveable { mutableStateOf(false) }
    val focus = LocalFocusManager.current

    LabeledInput(
        value = name,
        onValueChange = {
            name = it
            onEdit()
        },
        label = stringResource(if (directory == Directory.Categories) R.string.design_category_name else R.string.design_payer_name),
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        enabled = enabled,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
    )
    LabeledInput(
        value = note,
        onValueChange = {
            note = it
            onEdit()
        },
        label = stringResource(
            if (directory == Directory.Categories) R.string.entry_description else R.string.entry_note,
        ),
        modifier = Modifier.fillMaxWidth(),
        minLines = 2,
        enabled = enabled,
    )
    PrimaryAction(
        onClick = {
            focus.clearFocus()
            onSave(name, note)
        },
        modifier = Modifier.fillMaxWidth(),
        enabled = enabled,
    ) {
        Text(stringResource(R.string.save))
    }
    if (entry != null) {
        SecondaryAction(onClick = { confirming = true }, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
        }
        if (confirming) {
            AlertDialog(
                onDismissRequest = { confirming = false },
                title = { Text(stringResource(R.string.delete_title)) },
                text = { Text(stringResource(R.string.delete_entry_confirmation, entry.name)) },
                confirmButton = {
                    TextButton(onClick = {
                        confirming = false
                        onDelete()
                    }, enabled = enabled) {
                        Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { confirming = false }) { Text(stringResource(R.string.cancel)) }
                },
            )
        }
    }
}
