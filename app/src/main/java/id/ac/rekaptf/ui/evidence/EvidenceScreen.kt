package id.ac.rekaptf.ui.evidence

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.ac.rekaptf.BuildConfig
import id.ac.rekaptf.R
import id.ac.rekaptf.data.EvidenceReader
import id.ac.rekaptf.data.RekapRepository
import id.ac.rekaptf.ui.components.BadgeTone
import id.ac.rekaptf.ui.components.EmptyState
import id.ac.rekaptf.ui.components.FormPage
import id.ac.rekaptf.ui.components.ForwardGlyph
import id.ac.rekaptf.ui.components.InfoPanel
import id.ac.rekaptf.ui.components.PageTitle
import id.ac.rekaptf.ui.components.PrimaryAction
import id.ac.rekaptf.ui.components.Progress
import id.ac.rekaptf.ui.components.ScreenBar
import id.ac.rekaptf.ui.components.SecondaryAction
import id.ac.rekaptf.ui.components.SectionHeader
import id.ac.rekaptf.ui.components.StatusBadge
import id.ac.rekaptf.ui.components.WorkFeedback
import id.ac.rekaptf.ui.components.WorkOutcome
import id.ac.rekaptf.ui.components.WorkOutcomeEffect
import id.ac.rekaptf.ui.components.workModel
import id.ac.rekaptf.ui.components.theme.RekapColor
import id.ac.rekaptf.ui.components.theme.RekapShape
import id.ac.rekaptf.ui.components.theme.RekapSpacing
import kotlinx.coroutines.CancellationException

private data class EvidencePreview(val selection: Int, val bitmap: Bitmap)

@Composable
internal fun EvidenceRoute(
    repository: RekapRepository,
    onExpired: () -> Unit,
    onBack: () -> Unit,
    onDraft: (String) -> Unit,
) {
    val model = workModel(onExpired)
    val state by model.state.collectAsStateWithLifecycle()
    WorkOutcomeEffect(model) { result -> if (result is WorkOutcome.Draft) onDraft(result.id) }
    val context = LocalContext.current
    val reader = remember(context) { EvidenceReader(context.applicationContext) }
    var selected by rememberSaveable { mutableStateOf<String?>(null) }
    var selection by rememberSaveable { mutableIntStateOf(0) }
    var demo by remember { mutableStateOf<Boolean?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            model.clearError()
            selected = uri.toString()
            selection++
        }
    }
    LaunchedEffect(selected, selection) {
        val currentSelection = selection
        selected?.let { uri ->
            model.load(R.string.work_error_image) { EvidencePreview(currentSelection, reader.preview(Uri.parse(uri))) }
        }
    }
    LaunchedEffect(repository) {
        demo = try {
            repository.isDemo()
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            null
        }
    }
    BackHandler(enabled = state.busy) { model.cancel() }
    val chosen = selected
    val preview = (state.value as? EvidencePreview)?.takeIf { it.selection == selection }?.bitmap
    val unsupported = state.error?.text == R.string.evidence_unsupported_error
    val unreadable = state.error?.text == R.string.evidence_ocr_error
    val pickImage = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
    val local = BuildConfig.REKAP_LOCAL_OCR

    FormPage {
        ScreenBar(onBack = onBack, enabled = !state.busy) {
            Text(
                stringResource(R.string.app_name),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        PageTitle(
            stringResource(
                when {
                    unsupported -> R.string.evidence_unsupported_title
                    unreadable -> R.string.evidence_unreadable_title
                    state.busy -> R.string.work_evidence
                    selected != null -> R.string.design_evidence_preview
                    else -> R.string.work_evidence
                },
            ),
        )
        when {
            unsupported || unreadable -> {
                InfoPanel(
                    title = stringResource(if (unsupported) R.string.work_evidence else R.string.design_evidence_unreadable),
                    body = stringResource(if (unsupported) R.string.evidence_unsupported_error else R.string.evidence_unreadable_body),
                )
                EmptyState(
                    title = stringResource(R.string.evidence_unreadable_tip),
                    actionLabel = stringResource(R.string.evidence_unreadable_reselect),
                    onAction = pickImage,
                )
                SecondaryAction(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.evidence_unreadable_back))
                }
            }
            state.busy -> {
                // OCR dan permintaan AI berjalan dalam satu operasi, jadi status per tahap belum tersedia.
                Progress()
                Text(
                    stringResource(R.string.design_evidence_processing),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                SecondaryAction(onClick = model::cancel, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.work_cancel_process))
                }
            }
            else -> {
                if (chosen == null) {
                    EmptyState(
                        title = stringResource(R.string.work_image_empty),
                        body = stringResource(R.string.evidence_pick_hint),
                        actionLabel = stringResource(R.string.design_evidence_select),
                        onAction = pickImage,
                    )
                } else {
                    if (preview != null) {
                        val previewImage = remember(preview) { preview.asImageBitmap() }
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RekapShape.Square,
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, RekapColor.Outline),
                        ) {
                            Column(Modifier.padding(RekapSpacing.Small)) {
                                Image(
                                    bitmap = previewImage,
                                    contentDescription = stringResource(R.string.work_image_preview),
                                    modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp),
                                    contentScale = ContentScale.Fit,
                                )
                                Row(
                                    Modifier.fillMaxWidth().padding(top = RekapSpacing.Small),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(RekapSpacing.Small),
                                ) {
                                    Text(
                                        fileLabel(chosen),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.weight(1f),
                                    )
                                    StatusBadge(stringResource(R.string.design_evidence_single), BadgeTone.Info)
                                }
                            }
                        }
                    } else {
                        Text(stringResource(R.string.design_evidence_loading))
                        WorkFeedback(state)
                    }
                    SecondaryAction(onClick = pickImage, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.design_evidence_reselect))
                    }
                    SectionHeader(stringResource(R.string.evidence_local_note))
                    InfoPanel(
                        title = stringResource(
                            if (local) R.string.design_evidence_local_title else R.string.design_evidence_cloud_title,
                        ),
                        body = stringResource(if (local) R.string.design_evidence_local_body else R.string.work_ocr_baidu_disclosure),
                    )
                    if (demo == true) {
                        Text(
                            stringResource(R.string.design_demo_draft),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    WorkFeedback(state)
                    PrimaryAction(
                        enabled = !state.loading && preview != null,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            val uri = Uri.parse(chosen)
                            model.act(R.string.work_error_image) {
                                WorkOutcome.Draft(reader.extract(uri, repository).id)
                            }
                        },
                    ) {
                        Text(stringResource(R.string.work_process_image), Modifier.weight(1f))
                        ForwardGlyph()
                    }
                }
            }
        }
    }
}

private fun fileLabel(uri: String): String {
    val segment = Uri.parse(uri).lastPathSegment.orEmpty()
    return if (segment.isBlank()) uri else Uri.decode(segment)
}
