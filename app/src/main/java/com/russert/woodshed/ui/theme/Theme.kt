package com.russert.woodshed.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val WoodshedColorScheme = darkColorScheme(
    primary = Amber,
    onPrimary = DarkBrown,
    background = DarkBrown,
    onBackground = Cream,
    surface = WarmBrown,
    onSurface = Cream,
    secondary = MutedGreen,
    onSecondary = Cream,
)

object Theme {
    val Padding = 16.dp
    val SmallPadding = 8.dp
    val CornerRadius = 12.dp
    val SmallCornerRadius = 8.dp
}

@Composable
fun WoodshedTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = WoodshedColorScheme,
        typography = WoodshedTypography,
        content = content
    )
}
