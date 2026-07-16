package com.russert.woodshed.ui.metadata

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.russert.woodshed.models.InstrumentType
import com.russert.woodshed.ui.theme.Amber
import com.russert.woodshed.ui.theme.Cream
import com.russert.woodshed.ui.theme.DarkBrown
import com.russert.woodshed.ui.theme.Theme
import com.russert.woodshed.ui.theme.WarmBrown

// Shared form UI components used by MetadataFormScreen and EditRecordingScreen.

@Composable
internal fun FormSection(content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        content()
    }
}

@Composable
internal fun FormLabel(text: String) {
    Text(
        text = text.uppercase(),
        fontFamily = FontFamily.Serif,
        fontSize = 12.sp,
        color = Cream.copy(alpha = 0.6f),
        letterSpacing = 0.8.sp,
    )
}

@Composable
internal fun FormTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "",
    imeAction: ImeAction = ImeAction.Done,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = if (placeholder.isNotEmpty()) ({
            Text(placeholder, color = Cream.copy(alpha = 0.3f), fontFamily = FontFamily.Serif, fontSize = 16.sp)
        }) else null,
        textStyle = TextStyle(fontFamily = FontFamily.Serif, fontSize = 16.sp, color = Cream),
        colors = metadataFieldColors(),
        shape = RoundedCornerShape(Theme.SmallCornerRadius),
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Words,
            imeAction = imeAction,
        ),
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
internal fun FormTextArea(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = TextStyle(fontFamily = FontFamily.Serif, fontSize = 16.sp, color = Cream),
        colors = metadataFieldColors(),
        shape = RoundedCornerShape(Theme.SmallCornerRadius),
        minLines = 3,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 80.dp),
    )
}

@Composable
internal fun InstrumentPicker(
    selected: InstrumentType,
    onSelect: (InstrumentType) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        InstrumentType.entries.forEach { instrument ->
            val isSelected = instrument == selected
            Surface(
                onClick = { onSelect(instrument) },
                shape = CircleShape,
                color = if (isSelected) Amber else WarmBrown.copy(alpha = 0.3f),
                border = BorderStroke(1.dp, if (isSelected) Amber else WarmBrown.copy(alpha = 0.5f)),
            ) {
                Text(
                    text = instrument.displayName,
                    fontFamily = FontFamily.Serif,
                    fontSize = 14.sp,
                    color = if (isSelected) DarkBrown else Cream,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun TagEditor(
    tags: List<String>,
    onAdd: (String) -> Unit,
    onRemove: (String) -> Unit,
) {
    var newTag by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (tags.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                tags.forEach { tag ->
                    InputChip(
                        selected = false,
                        onClick = { onRemove(tag) },
                        label = {
                            Text(tag, fontFamily = FontFamily.Serif, fontSize = 13.sp, color = Cream)
                        },
                        trailingIcon = {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Remove $tag",
                                tint = Cream.copy(alpha = 0.8f),
                                modifier = Modifier.size(14.dp),
                            )
                        },
                        shape = CircleShape,
                        colors = InputChipDefaults.inputChipColors(
                            containerColor = Amber.copy(alpha = 0.85f),
                        ),
                        border = BorderStroke(0.dp, Color.Transparent),
                    )
                }
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = newTag,
                onValueChange = { newTag = it },
                placeholder = {
                    Text("Add a tag", color = Cream.copy(alpha = 0.3f), fontFamily = FontFamily.Serif, fontSize = 16.sp)
                },
                textStyle = TextStyle(fontFamily = FontFamily.Serif, fontSize = 16.sp, color = Cream),
                colors = metadataFieldColors(),
                shape = RoundedCornerShape(Theme.SmallCornerRadius),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    val trimmed = newTag.trim()
                    if (trimmed.isNotEmpty() && trimmed !in tags) {
                        onAdd(trimmed)
                        newTag = ""
                    }
                }),
                modifier = Modifier.weight(1f),
            )

            Surface(
                onClick = {
                    val trimmed = newTag.trim()
                    if (trimmed.isNotEmpty() && trimmed !in tags) {
                        onAdd(trimmed)
                        newTag = ""
                    }
                },
                shape = RoundedCornerShape(Theme.SmallCornerRadius),
                color = Amber,
            ) {
                Text(
                    text = "Add",
                    fontFamily = FontFamily.Serif,
                    fontSize = 14.sp,
                    color = DarkBrown,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
                )
            }
        }
    }
}

@Composable
internal fun metadataFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Amber,
    unfocusedBorderColor = WarmBrown.copy(alpha = 0.5f),
    cursorColor = Amber,
    focusedContainerColor = WarmBrown.copy(alpha = 0.25f),
    unfocusedContainerColor = WarmBrown.copy(alpha = 0.25f),
    focusedTextColor = Cream,
    unfocusedTextColor = Cream,
)
