package com.gymora.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.gymora.ui.theme.GymoraShapes
import com.gymora.ui.theme.GymoraThemeTokens
import com.gymora.ui.theme.Sizes

// Night volt component layer. These wrappers share the names of their Material 3
// counterparts so a screen adopts the design by swapping its import only.

private const val MOTION_MS = 150
private const val PRESSED_SCALE = 0.97f
private val ButtonPadding = PaddingValues(horizontal = 24.dp, vertical = 0.dp)

@Composable
private fun pressScale(source: MutableInteractionSource): Modifier {
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) PRESSED_SCALE else 1f,
        animationSpec = tween(MOTION_MS, easing = FastOutSlowInEasing),
        label = "pressScale",
    )
    return Modifier.scale(scale)
}

/** Primary button: lime fill, dark text, 52dp tall; pressed = scale 0.97 and darker fill. */
@Composable
fun Button(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = GymoraShapes.button,
    colors: ButtonColors = primaryButtonColors(),
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit,
) {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val base = colors
    val effective = if (pressed && base.containerColor == MaterialTheme.colorScheme.primary) {
        base.copy(containerColor = GymoraThemeTokens.extraColors.primaryPressed)
    } else {
        base
    }
    androidx.compose.material3.Button(
        onClick = onClick,
        modifier = modifier.defaultMinSize(minHeight = Sizes.button).then(pressScale(source)),
        enabled = enabled,
        shape = shape,
        colors = effective,
        border = border,
        contentPadding = contentPadding,
        interactionSource = source,
        content = content,
    )
}

@Composable
fun primaryButtonColors(): ButtonColors = ButtonDefaults.buttonColors(
    containerColor = MaterialTheme.colorScheme.primary,
    contentColor = MaterialTheme.colorScheme.onPrimary,
    disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
    disabledContentColor = GymoraThemeTokens.extraColors.textDisabled,
)

/** Secondary button: transparent with a 1dp outline and light text. */
@Composable
fun OutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = GymoraShapes.button,
    colors: ButtonColors = ButtonDefaults.outlinedButtonColors(
        contentColor = MaterialTheme.colorScheme.onSurface,
        disabledContentColor = GymoraThemeTokens.extraColors.textDisabled,
    ),
    border: BorderStroke? = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    contentPadding: PaddingValues = ButtonPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit,
) {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    androidx.compose.material3.OutlinedButton(
        onClick = onClick,
        modifier = modifier.defaultMinSize(minHeight = Sizes.button).then(pressScale(source)),
        enabled = enabled,
        shape = shape,
        colors = colors,
        border = border,
        contentPadding = contentPadding,
        interactionSource = source,
        content = content,
    )
}

/** Text button: lime text, no background. */
@Composable
fun TextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = GymoraShapes.button,
    colors: ButtonColors = ButtonDefaults.textButtonColors(
        contentColor = MaterialTheme.colorScheme.primary,
        disabledContentColor = GymoraThemeTokens.extraColors.textDisabled,
    ),
    contentPadding: PaddingValues = ButtonDefaults.TextButtonContentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit,
) {
    androidx.compose.material3.TextButton(
        onClick = onClick,
        modifier = modifier.defaultMinSize(minHeight = Sizes.touchTarget),
        enabled = enabled,
        shape = shape,
        colors = colors,
        contentPadding = contentPadding,
        interactionSource = interactionSource,
        content = content,
    )
}

/** Text field: highest-surface fill, no underline, 12dp radius, 1.5dp lime outline when focused. */
@Composable
fun OutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle = MaterialTheme.typography.bodyLarge,
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    prefix: @Composable (() -> Unit)? = null,
    suffix: @Composable (() -> Unit)? = null,
    supportingText: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = false,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    minLines: Int = 1,
    interactionSource: MutableInteractionSource? = null,
    shape: Shape = GymoraShapes.input,
    colors: TextFieldColors = gymoraTextFieldColors(),
) {
    androidx.compose.material3.OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.defaultMinSize(minHeight = Sizes.touchTarget),
        enabled = enabled,
        readOnly = readOnly,
        textStyle = textStyle,
        label = label,
        placeholder = placeholder,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        prefix = prefix,
        suffix = suffix,
        supportingText = supportingText,
        isError = isError,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        singleLine = singleLine,
        maxLines = maxLines,
        minLines = minLines,
        interactionSource = interactionSource,
        shape = shape,
        colors = colors,
    )
}

@Composable
fun gymoraTextFieldColors(): TextFieldColors {
    val scheme = MaterialTheme.colorScheme
    val extra = GymoraThemeTokens.extraColors
    return OutlinedTextFieldDefaults.colors(
        focusedContainerColor = scheme.surfaceContainerHighest,
        unfocusedContainerColor = scheme.surfaceContainerHighest,
        disabledContainerColor = scheme.surfaceContainerHighest,
        errorContainerColor = scheme.surfaceContainerHighest,
        focusedBorderColor = scheme.primary,
        unfocusedBorderColor = Color.Transparent,
        disabledBorderColor = Color.Transparent,
        errorBorderColor = scheme.error,
        focusedTextColor = scheme.onSurface,
        unfocusedTextColor = scheme.onSurface,
        disabledTextColor = extra.textDisabled,
        focusedLabelColor = scheme.primary,
        unfocusedLabelColor = scheme.onSurfaceVariant,
        disabledLabelColor = extra.textDisabled,
        focusedPlaceholderColor = scheme.onSurfaceVariant,
        unfocusedPlaceholderColor = scheme.onSurfaceVariant,
        cursorColor = scheme.primary,
        focusedSupportingTextColor = scheme.onSurfaceVariant,
        unfocusedSupportingTextColor = scheme.onSurfaceVariant,
    )
}

/** Top app bar: background color, no elevation, Title large. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopAppBar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    colors: TopAppBarColors = gymoraTopAppBarColors(),
    scrollBehavior: TopAppBarScrollBehavior? = null,
) {
    androidx.compose.material3.TopAppBar(
        title = title,
        modifier = modifier,
        navigationIcon = navigationIcon,
        actions = actions,
        colors = colors,
        scrollBehavior = scrollBehavior,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun gymoraTopAppBarColors(): TopAppBarColors {
    val scheme = MaterialTheme.colorScheme
    return TopAppBarDefaults.topAppBarColors(
        containerColor = scheme.background,
        scrolledContainerColor = scheme.background,
        navigationIconContentColor = scheme.onSurfaceVariant,
        titleContentColor = scheme.onSurface,
        actionIconContentColor = scheme.onSurfaceVariant,
    )
}

/** Card colors: surface fill, no border or shadow. */
@Composable
fun gymoraCardColors(raised: Boolean = false): CardColors {
    val scheme = MaterialTheme.colorScheme
    return CardDefaults.cardColors(
        containerColor = if (raised) scheme.surfaceContainerHigh else scheme.surfaceContainer,
        contentColor = scheme.onSurface,
        disabledContainerColor = scheme.surfaceContainer,
        disabledContentColor = GymoraThemeTokens.extraColors.textDisabled,
    )
}

/** Flat card: 16dp radius, surface color, no elevation. */
@Composable
fun Card(
    modifier: Modifier = Modifier,
    shape: Shape = GymoraShapes.card,
    colors: CardColors = gymoraCardColors(),
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    androidx.compose.material3.Card(
        modifier = modifier,
        shape = shape,
        colors = colors,
        elevation = CardDefaults.cardElevation(0.dp, 0.dp, 0.dp, 0.dp, 0.dp, 0.dp),
        content = content,
    )
}

/** Clickable flat card; pressed state steps up to the raised surface. */
@Composable
fun Card(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = GymoraShapes.card,
    colors: CardColors = gymoraCardColors(),
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    androidx.compose.material3.Card(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = shape,
        colors = colors,
        elevation = CardDefaults.cardElevation(0.dp, 0.dp, 0.dp, 0.dp, 0.dp, 0.dp),
        content = content,
    )
}

/** Chip colors: unselected = surface with outline; selected = lime fill with dark text. */
@Composable
fun gymoraFilterChipColors() = FilterChipDefaults.filterChipColors(
    containerColor = MaterialTheme.colorScheme.surfaceContainer,
    labelColor = MaterialTheme.colorScheme.onSurface,
    iconColor = MaterialTheme.colorScheme.onSurfaceVariant,
    selectedContainerColor = MaterialTheme.colorScheme.primary,
    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
    selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary,
    selectedTrailingIconColor = MaterialTheme.colorScheme.onPrimary,
)

@Composable
fun gymoraFilterChipBorder(selected: Boolean, enabled: Boolean = true) =
    FilterChipDefaults.filterChipBorder(
        enabled = enabled,
        selected = selected,
        borderColor = MaterialTheme.colorScheme.outline,
        selectedBorderColor = Color.Transparent,
        borderWidth = 1.dp,
        selectedBorderWidth = 0.dp,
    )

/** Bottom navigation item colors: selected is lime icon and label, no pill. */
@Composable
fun gymoraNavigationBarItemColors() = NavigationBarItemDefaults.colors(
    selectedIconColor = MaterialTheme.colorScheme.primary,
    selectedTextColor = MaterialTheme.colorScheme.primary,
    indicatorColor = androidx.compose.ui.graphics.Color.Transparent,
    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
)

/** Circular progress in lime on a highest-surface track with rounded caps. */
@Composable
fun GymoraProgressRing(
    progress: () -> Float,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 8.dp,
) {
    CircularProgressIndicator(
        progress = progress,
        modifier = modifier,
        color = MaterialTheme.colorScheme.primary,
        trackColor = GymoraThemeTokens.extraColors.track,
        strokeWidth = strokeWidth,
        strokeCap = androidx.compose.ui.graphics.StrokeCap.Round,
    )
}

/** Round-capped linear track colors shared by progress bars. */
@Composable
fun gymoraProgressColors(): Pair<Color, Color> =
    MaterialTheme.colorScheme.primary to GymoraThemeTokens.extraColors.track

/** Default indeterminate spinner, lime on a highest-surface track. */
@Composable
fun GymoraLoading(modifier: Modifier = Modifier) {
    CircularProgressIndicator(
        modifier = modifier,
        color = MaterialTheme.colorScheme.primary,
        trackColor = GymoraThemeTokens.extraColors.track,
        strokeCap = androidx.compose.ui.graphics.StrokeCap.Round,
    )
}

/** Snackbar: highest surface with light text and a lime action. */
@Composable
fun GymoraSnackbar(data: SnackbarData, modifier: Modifier = Modifier) {
    androidx.compose.material3.Snackbar(
        snackbarData = data,
        modifier = modifier,
        shape = GymoraShapes.input,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        contentColor = MaterialTheme.colorScheme.onSurface,
        actionColor = MaterialTheme.colorScheme.primary,
        dismissActionContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/** Snackbar host that renders Night volt snackbars. */
@Composable
fun SnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    androidx.compose.material3.SnackbarHost(
        hostState = hostState,
        modifier = modifier,
        snackbar = { GymoraSnackbar(it) },
    )
}

/** Filter chip: unselected = surface with outline; selected = lime fill with dark text. */
@Composable
fun FilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: @Composable (() -> Unit)? = null,
) {
    androidx.compose.material3.FilterChip(
        selected = selected,
        onClick = onClick,
        label = label,
        modifier = modifier.defaultMinSize(minHeight = Sizes.touchTarget),
        enabled = enabled,
        leadingIcon = leadingIcon,
        shape = GymoraShapes.chip,
        colors = gymoraFilterChipColors(),
        border = gymoraFilterChipBorder(selected, enabled),
    )
}
