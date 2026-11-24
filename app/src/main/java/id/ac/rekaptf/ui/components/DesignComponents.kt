package id.ac.rekaptf.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import id.ac.rekaptf.R
import id.ac.rekaptf.ui.components.theme.RekapColor
import id.ac.rekaptf.ui.components.theme.RekapShape
import id.ac.rekaptf.ui.components.theme.RekapSize
import id.ac.rekaptf.ui.components.theme.RekapSpacing

internal enum class BadgeTone { Success, Info, Muted, Danger }

/** Baris kembali ringkas untuk layar selain tab utama. */
@Composable
internal fun ScreenBar(onBack: (() -> Unit)?, enabled: Boolean = true, action: (@Composable () -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = RekapSize.Touch),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            TextButton(onClick = onBack, enabled = enabled) {
                Text(stringResource(R.string.back))
            }
        }
        Spacer(Modifier.weight(1f))
        action?.invoke()
    }
}

/** Judul bagian dengan aksi teks opsional di sisi kanan. */
@Composable
internal fun SectionHeader(title: String, actionLabel: String? = null, onAction: (() -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
        if (actionLabel != null && onAction != null) {
            TextButton(onClick = onAction) { Text(actionLabel) }
        }
    }
}

/** Panel informasi ringan: disclosure AI, status, atau catatan draf. */
@Composable
internal fun InfoPanel(
    title: String,
    body: String? = null,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    trailing: String? = null,
) {
    val content: @Composable () -> Unit = {
        Row(
            Modifier.fillMaxWidth().padding(RekapSpacing.Card),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(RekapSpacing.Tight),
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(RekapSpacing.Micro)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                body?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            trailing?.let { Text(it, Modifier.clearAndSetSemantics { }) }
        }
    }
    if (onClick == null) {
        Surface(
            modifier = modifier.fillMaxWidth(),
            shape = RekapShape.Square,
            color = RekapColor.InfoSurface,
            contentColor = RekapColor.Ink,
            content = content,
        )
    } else {
        Surface(
            onClick = onClick,
            modifier = modifier.fillMaxWidth().semantics { role = Role.Button },
            shape = RekapShape.Square,
            color = RekapColor.InfoSurface,
            contentColor = RekapColor.Ink,
            content = content,
        )
    }
}

/** Badge status: Terkonfirmasi, Belum masuk rekap, dan sejenisnya. */
@Composable
internal fun StatusBadge(text: String, tone: BadgeTone = BadgeTone.Info) {
    val background = when (tone) {
        BadgeTone.Success -> RekapColor.SuccessSurface
        BadgeTone.Info -> RekapColor.InfoSurface
        BadgeTone.Muted -> MaterialTheme.colorScheme.surfaceVariant
        BadgeTone.Danger -> RekapColor.DangerSurface
    }
    val foreground = when (tone) {
        BadgeTone.Success -> RekapColor.Success
        BadgeTone.Info -> RekapColor.Ink
        BadgeTone.Muted -> RekapColor.MutedText
        BadgeTone.Danger -> RekapColor.Danger
    }
    Surface(color = background, contentColor = foreground, shape = RekapShape.Badge) {
        Text(
            text,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

/** Kartu gelap dengan angka utama, dipakai Beranda dan Rekap. */
@Composable
internal fun HeroSummary(
    caption: String,
    amount: String,
    supporting: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    footnote: String? = null,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RekapShape.Square,
        color = MaterialTheme.colorScheme.inverseSurface,
        contentColor = MaterialTheme.colorScheme.inverseOnSurface,
    ) {
        Column(
            Modifier.padding(RekapSpacing.Section),
            verticalArrangement = Arrangement.spacedBy(RekapSpacing.Small),
        ) {
            Text(caption, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.inverseOnSurface)
            Text(amount, style = MaterialTheme.typography.displaySmall, color = RekapColor.Accent)
            Text(supporting, style = MaterialTheme.typography.bodyMedium)
            footnote?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.inverseOnSurface)
            }
            if (actionLabel != null && onAction != null) {
                TextButton(
                    onClick = onAction,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                ) {
                    Text(actionLabel, color = RekapColor.Accent)
                }
            }
        }
    }
}

/** Kartu komposisi kategori pada Beranda dan Rekap. */
@Composable
internal fun CompositionTile(label: String, amount: String, sharePercent: Int, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RekapShape.Square,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Column(
            Modifier.padding(RekapSpacing.Card),
            verticalArrangement = Arrangement.spacedBy(RekapSpacing.Micro),
        ) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(amount, style = MaterialTheme.typography.titleMedium)
            Text("$sharePercent%", style = MaterialTheme.typography.labelSmall, color = RekapColor.MutedText)
        }
    }
}

/** Keadaan kosong yang selalu memberi satu tindakan lanjutan. */
@Composable
internal fun EmptyState(title: String, body: String? = null, actionLabel: String? = null, onAction: (() -> Unit)? = null) {
    Column(
        Modifier.fillMaxWidth().padding(vertical = RekapSpacing.Section),
        verticalArrangement = Arrangement.spacedBy(RekapSpacing.Small),
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        body?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        if (actionLabel != null && onAction != null) {
            TextButton(onClick = onAction) { Text(actionLabel) }
        }
    }
}

/** Pemilih segmen periode atau filter. */
@Composable
internal fun <T> ChipSelector(
    options: List<T>,
    selected: T,
    enabled: Boolean = true,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(RekapSpacing.Small),
    ) {
        options.forEach { option ->
            val active = option == selected
            FilterChip(
                selected = active,
                enabled = enabled,
                onClick = { onSelect(option) },
                label = { Text(label(option), style = MaterialTheme.typography.labelLarge) },
                shape = RekapShape.Square,
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    labelColor = RekapColor.Ink,
                    selectedContainerColor = RekapColor.Accent,
                    selectedLabelColor = RekapColor.Ink,
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = enabled,
                    selected = active,
                    borderColor = MaterialTheme.colorScheme.outline,
                    selectedBorderColor = MaterialTheme.colorScheme.outline,
                ),
            )
        }
    }
}

/** Baris metadata pada layar detail. */
@Composable
internal fun MetaRow(label: String, value: String) {
    if (value.isBlank()) return
    Column(
        Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(RekapSpacing.Micro),
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleSmall)
    }
}

/** Satu tahap pemrosesan bukti dengan penanda aktif/selesai. */
@Composable
internal fun StepRow(index: Int, title: String, active: Boolean, done: Boolean) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = RekapSpacing.Small),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RekapSpacing.Tight),
    ) {
        Surface(
            modifier = Modifier.size(24.dp),
            shape = RekapShape.Square,
            color = when {
                done -> RekapColor.Ink
                active -> RekapColor.Accent
                else -> Color.Transparent
            },
            border = BorderStroke(1.dp, RekapColor.Ink.copy(alpha = if (active || done) 1f else 0.35f)),
            contentColor = if (done) RekapColor.Background else RekapColor.Ink,
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                Text(
                    if (done) "✓" else index.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.align(Alignment.CenterVertically).clearAndSetSemantics { },
                )
            }
        }
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            color = if (active || done) RekapColor.Ink else RekapColor.MutedText,
        )
    }
}

/** Panah navigasi dekoratif; makna tindakan dibawa label tombol. */
@Composable
internal fun ForwardGlyph() {
    Text("→", Modifier.clearAndSetSemantics { })
}

/** Garis pemisah tipis 1 dp sesuai token Outline. */
@Composable
internal fun HairLine(modifier: Modifier = Modifier) {
    Spacer(modifier.fillMaxWidth().heightIn(min = 1.dp).background(RekapColor.Outline))
}
