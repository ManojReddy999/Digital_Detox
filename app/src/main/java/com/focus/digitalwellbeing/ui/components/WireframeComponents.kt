package com.focus.digitalwellbeing.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Shared Wireframe UI Constants
 */
object WireframeDefaults {
    val CornerRadius = 0.dp // Sharp corners as per wireframe image
    val BorderWidth = 1.dp
    
    val Padding = 16.dp
}

/**
 * A container with a standard thin border and background from theme
 */
@Composable
fun WireframeBox(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(WireframeDefaults.CornerRadius),
    onClick: (() -> Unit)? = null,
    contentAlignment: Alignment = Alignment.TopStart,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface)
            .border(
                BorderStroke(WireframeDefaults.BorderWidth, MaterialTheme.colorScheme.outline),
                shape
            )
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = contentAlignment,
        content = content
    )
}

/**
 * Standard Wireframe Button (Outlined, rectangular)
 */
@Composable
fun WireframeButton(
    text: String,
    icon: @Composable (() -> Unit)? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(WireframeDefaults.CornerRadius))
            .border(
                BorderStroke(2.dp, MaterialTheme.colorScheme.outline),
                RoundedCornerShape(WireframeDefaults.CornerRadius)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                icon()
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/**
 * Wireframe Icon Button (Square border)
 */
@Composable
fun WireframeIconButton(
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp
) {
    WireframeBox(
        modifier = modifier.size(size),
        onClick = onClick,
        contentAlignment = Alignment.Center
    ) {
        icon()
    }
}

/**
 * Progress Bar (Thin line style)
 */
@Composable
fun WireframeProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(4.dp) // Thin line
            .background(MaterialTheme.colorScheme.surfaceVariant) // Track
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress)
                .fillMaxHeight()
                .background(color) // Use provided color or primary
        )
    }
}

/**
 * Clickable modifier with ripple effect
 */
fun Modifier.clickableWithRipple(
    onClick: () -> Unit
): Modifier = this.clickable(onClick = onClick)

/**
 * App Header with screen title
 */
@Composable
fun AppHeader(
    title: String,
    modifier: Modifier = Modifier,
    action: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 24.dp, start = 20.dp, end = 20.dp, bottom = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 40.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-1.5).sp,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        if (action != null) {
            action()
        }
    }
}

