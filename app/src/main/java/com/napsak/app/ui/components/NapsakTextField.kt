package com.napsak.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/**
 * The app's single text input: a soft tinted field with rounded corners, no harsh outline, a floating
 * label, an optional leading icon, a clear button while typing, a character counter, helper/error text
 * and an animated focus ring. Works in light and dark themes and at large font sizes.
 */
@Composable
fun NapsakTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier.fillMaxWidth(),
    placeholder: String? = null,
    leadingIcon: ImageVector? = null,
    trailing: (@Composable () -> Unit)? = null,
    singleLine: Boolean = true,
    minLines: Int = 1,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    maxLength: Int? = null,
    helperText: String? = null,
    errorText: String? = null,
    enabled: Boolean = true,
    clearable: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val scheme = MaterialTheme.colorScheme
    val isError = errorText != null
    val shape = RoundedCornerShape(16.dp)

    val ring by animateColorAsState(
        targetValue = when {
            isError -> scheme.error
            focused -> scheme.primary
            else -> Color.Transparent
        },
        animationSpec = tween(150),
        label = "focusRing"
    )
    // a faint tint of the text colour: soft grey on light surfaces, a lighter veil on dark ones
    val container = scheme.onSurface.copy(alpha = 0.06f)
    val hint = scheme.onSurface.copy(alpha = 0.45f)

    Column(modifier) {
        TextField(
            value = value,
            onValueChange = { input ->
                onValueChange(if (maxLength != null && input.length > maxLength) input.take(maxLength) else input)
            },
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, ring, shape)
                .semantics { if (errorText != null) error(errorText) },
            enabled = enabled,
            label = { Text(label) },
            placeholder = placeholder?.let { text -> { Text(text) } },
            leadingIcon = leadingIcon?.let { icon ->
                {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (focused) scheme.primary else scheme.onSurface.copy(alpha = 0.55f)
                    )
                }
            },
            trailingIcon = when {
                trailing != null -> trailing
                clearable && enabled && focused && value.isNotEmpty() -> {
                    {
                        IconButton(onClick = { onValueChange("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Temizle")
                        }
                    }
                }
                else -> null
            },
            isError = isError,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            singleLine = singleLine,
            minLines = minLines,
            maxLines = maxLines,
            interactionSource = interaction,
            shape = shape,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = container,
                unfocusedContainerColor = container,
                disabledContainerColor = container.copy(alpha = 0.03f),
                errorContainerColor = scheme.error.copy(alpha = 0.06f),
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                errorIndicatorColor = Color.Transparent,
                cursorColor = scheme.primary,
                errorCursorColor = scheme.error,
                focusedLabelColor = scheme.primary,
                unfocusedLabelColor = scheme.onSurface.copy(alpha = 0.7f),
                errorLabelColor = scheme.error,
                focusedTextColor = scheme.onSurface,
                unfocusedTextColor = scheme.onSurface,
                focusedPlaceholderColor = hint,
                unfocusedPlaceholderColor = hint
            )
        )

        val supporting = errorText ?: helperText
        val counter = maxLength?.let { "${value.length}/$it" }
        if (supporting != null || counter != null) {
            Row(modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 6.dp)) {
                Text(
                    text = supporting.orEmpty(),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isError) scheme.error else scheme.onSurface.copy(alpha = 0.65f)
                )
                if (counter != null) {
                    Text(
                        text = counter,
                        style = MaterialTheme.typography.labelMedium,
                        color = scheme.onSurface.copy(alpha = 0.55f)
                    )
                }
            }
        }
    }
}
