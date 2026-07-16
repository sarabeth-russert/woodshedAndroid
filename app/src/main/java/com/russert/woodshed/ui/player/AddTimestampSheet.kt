package com.russert.woodshed.ui.player

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.russert.woodshed.ui.theme.Amber
import com.russert.woodshed.ui.theme.Cream
import com.russert.woodshed.ui.theme.DarkBrown
import com.russert.woodshed.ui.theme.Theme
import com.russert.woodshed.ui.theme.WarmBrown
import com.russert.woodshed.util.TimeFormatter
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTimestampSheet(
    currentTime: Double,
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var label by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = {
            label = ""
            onDismiss()
        },
        sheetState = sheetState,
        containerColor = DarkBrown,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Theme.Padding)
                .padding(bottom = 32.dp),
        ) {
            Text(
                text = "Add Chapter",
                style = TextStyle(fontFamily = FontFamily.Serif, fontSize = 20.sp),
                color = Cream,
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "at ${TimeFormatter.formatDetailed(currentTime)}",
                fontFamily = FontFamily.Monospace,
                fontSize = 16.sp,
                color = Amber,
            )

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = label,
                onValueChange = { label = it },
                placeholder = {
                    Text("Chapter name", color = Cream.copy(alpha = 0.4f), fontFamily = FontFamily.Serif)
                },
                textStyle = TextStyle(fontFamily = FontFamily.Serif, fontSize = 16.sp, color = Cream),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Amber,
                    unfocusedBorderColor = WarmBrown.copy(alpha = 0.5f),
                    cursorColor = Amber,
                    focusedContainerColor = WarmBrown.copy(alpha = 0.2f),
                    unfocusedContainerColor = WarmBrown.copy(alpha = 0.2f),
                ),
                shape = RoundedCornerShape(Theme.SmallCornerRadius),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(20.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = {
                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                            label = ""
                            onDismiss()
                        }
                    },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Cream.copy(alpha = 0.6f)),
                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                ) {
                    Text("Cancel")
                }

                Button(
                    onClick = {
                        val trimmed = label.trim()
                        if (trimmed.isNotEmpty()) {
                            onSave(trimmed)
                            scope.launch { sheetState.hide() }.invokeOnCompletion {
                                label = ""
                                onDismiss()
                            }
                        }
                    },
                    enabled = label.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = Amber, contentColor = DarkBrown),
                    modifier = Modifier.weight(1f).padding(start = 8.dp),
                ) {
                    Text("Save")
                }
            }
        }
    }
}
