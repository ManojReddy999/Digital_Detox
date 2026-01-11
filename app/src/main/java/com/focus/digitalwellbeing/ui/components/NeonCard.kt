package com.focus.digitalwellbeing.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.focus.digitalwellbeing.ui.theme.CardBackground
import com.focus.digitalwellbeing.ui.theme.CardBorder
import com.focus.digitalwellbeing.ui.theme.NeonGreen

@Composable
fun NeonCard(
    modifier: Modifier = Modifier,
    showLeftAccent: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .background(CardBackground)
            .border(BorderStroke(1.dp, CardBorder))
            .then(
                if (showLeftAccent) {
                    Modifier.drawBehind {
                        drawLine(
                            color = NeonGreen,
                            start = Offset(0f, 0f),
                            end = Offset(0f, size.height),
                            strokeWidth = 3.dp.toPx()
                        )
                    }
                } else {
                    Modifier
                }
            )
            .padding(start = if (showLeftAccent) 3.dp else 0.dp)
    ) {
        content()
    }
}

