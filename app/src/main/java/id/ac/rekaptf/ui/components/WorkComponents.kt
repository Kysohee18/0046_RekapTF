package id.ac.rekaptf.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import id.ac.rekaptf.R

@Composable
internal fun workModel(onExpired: () -> Unit): WorkViewModel =
    viewModel(factory = viewModelFactory { initializer { WorkViewModel(onExpired) } })

@Composable
internal fun WorkOutcomeEffect(model: WorkViewModel, handle: (WorkOutcome) -> Unit) {
    val outcome by model.outcome.collectAsStateWithLifecycle()
    LaunchedEffect(outcome) {
        val result = outcome ?: return@LaunchedEffect
        if (model.consumeOutcome(result)) handle(result)
    }
}

@Composable
internal fun WorkFeedback(state: WorkState, retry: (() -> Unit)? = null) {
    if (state.loading || state.busy) Progress()
    state.error?.let {
        ErrorText(it)
        if (retry != null) {
            TextButton(onClick = retry) { Text(stringResource(R.string.retry)) }
        }
    }
}

@Composable
internal fun <T> ChoiceField(
    label: String,
    selected: String,
    options: List<T>,
    enabled: Boolean = true,
    text: @Composable (T) -> String,
    onSelect: (T) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        Box(Modifier.fillMaxWidth()) {
            SecondaryAction(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth(),
                enabled = enabled,
            ) {
                Text(selected, Modifier.weight(1f))
                Text("⌄", Modifier.clearAndSetSemantics {})
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { option ->
                    DropdownMenuItem(text = { Text(text(option)) }, onClick = {
                        expanded = false
                        onSelect(option)
                    })
                }
            }
        }
    }
}

@Composable
internal fun DeleteConfirmation(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.work_delete_title)) },
        text = { Text(stringResource(R.string.work_delete_body)) },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
            ) {
                Text(stringResource(R.string.delete))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
