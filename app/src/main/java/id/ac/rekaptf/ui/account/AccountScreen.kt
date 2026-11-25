package id.ac.rekaptf.ui.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import id.ac.rekaptf.R
import id.ac.rekaptf.ui.components.ErrorText
import id.ac.rekaptf.ui.components.LabeledInput
import id.ac.rekaptf.ui.components.PageTitle
import id.ac.rekaptf.ui.components.PrimaryAction
import id.ac.rekaptf.ui.components.Progress
import id.ac.rekaptf.ui.components.theme.RekapSpacing

@Composable
internal fun AccountScreen(
    registering: Boolean,
    state: AccountState,
    onSubmit: (String, String, String?, String?) -> Unit,
    onSwitch: () -> Unit,
    onEdit: () -> Unit,
) {
    var business by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    val focus = LocalFocusManager.current
    val title = if (registering) R.string.register else R.string.sign_in
    val passwordAction = stringResource(if (showPassword) R.string.hide_password_label else R.string.show_password_label)
    val submit = {
        focus.clearFocus()
        onSubmit(email, password, business.takeIf { registering }, confirmation.takeIf { registering })
    }

    BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        val viewportHeight = maxHeight
        Column(
            modifier = Modifier.widthIn(max = 520.dp).fillMaxWidth()
                .verticalScroll(rememberScrollState()).heightIn(min = viewportHeight)
                .padding(horizontal = RekapSpacing.Page, vertical = RekapSpacing.Page),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(RekapSpacing.Auth)) {
                Text(
                    stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                PageTitle(stringResource(title))
                if (registering) {
                    LabeledInput(
                        value = business,
                        onValueChange = {
                            business = it
                            onEdit()
                        },
                        label = stringResource(R.string.business_name),
                        enabled = !state.busy,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    )
                }
                LabeledInput(
                    value = email,
                    onValueChange = {
                        email = it
                        onEdit()
                    },
                    label = stringResource(R.string.email),
                    enabled = !state.busy,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                    autofillType = if (registering) ContentType.NewUsername else ContentType.Username,
                )
                val passwordVisibility =
                    if (showPassword) VisualTransformation.None else PasswordVisualTransformation()
                LabeledInput(
                    value = password,
                    onValueChange = {
                        password = it
                        onEdit()
                    },
                    label = stringResource(R.string.password),
                    enabled = !state.busy,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = if (registering) ImeAction.Next else ImeAction.Done,
                    ),
                    keyboardActions = if (registering) KeyboardActions.Default else KeyboardActions(onDone = { submit() }),
                    visualTransformation = passwordVisibility,
                    autofillType = if (registering) ContentType.NewPassword else ContentType.Password,
                    supportingText = if (registering) ({ Text(stringResource(R.string.password_hint)) }) else null,
                    trailingIcon = {
                        TextButton(
                            onClick = { showPassword = !showPassword },
                            enabled = !state.busy,
                            modifier = Modifier.semantics { contentDescription = passwordAction },
                        ) {
                            Text(stringResource(if (showPassword) R.string.hide_password else R.string.show_password))
                        }
                    },
                )
                if (registering) {
                    LabeledInput(
                        value = confirmation,
                        onValueChange = {
                            confirmation = it
                            onEdit()
                        },
                        label = stringResource(R.string.confirm_password),
                        enabled = !state.busy,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { submit() }),
                        visualTransformation = passwordVisibility,
                        autofillType = ContentType.NewPassword,
                    )
                }
                if (state.busy) Progress()
                state.error?.let { ErrorText(it) }
            }
            Column(Modifier.padding(top = RekapSpacing.Page), horizontalAlignment = Alignment.CenterHorizontally) {
                PrimaryAction(onClick = submit, modifier = Modifier.fillMaxWidth(), enabled = !state.busy) {
                    Text(stringResource(title), Modifier.weight(1f))
                    Text("→", modifier = Modifier.clearAndSetSemantics {})
                }
                TextButton(onClick = onSwitch, enabled = !state.busy) {
                    Text(stringResource(if (registering) R.string.back_to_sign_in else R.string.create_account))
                }
            }
        }
    }
}
