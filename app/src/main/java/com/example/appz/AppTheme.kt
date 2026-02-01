package com.example.appz

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt




val AppBackgroundColor = Color(0xFFFEF3E2)


val AppGradientBrush = SolidColor(AppBackgroundColor)


val SliderYellow = Color(0xFFf1c523)
val SliderInactive = Color(0xFFDCD3BF)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFB92121),
    background = Color(0xFF000000),
    surface = Color(0xFF1C1B1F),

    onPrimary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White
)



@OptIn(ExperimentalMaterial3Api::class)
val AppTopBarColors: TopAppBarColors
    @Composable
    get() = TopAppBarDefaults.topAppBarColors(
        containerColor = Color.Transparent,
        titleContentColor = Color.Black,
        navigationIconContentColor = Color.Black,
        actionIconContentColor = Color.Black
    )



@Composable
fun AppzTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography(),
        content = content
    )
}



@Composable
fun StrokedText(
    text: String,
    modifier: Modifier = Modifier,
    fillColor: Color,
    strokeColor: Color,
    strokeWidth: Float,
    style: TextStyle,
    shadowColor: Color? = null,
    shadowOffset: Offset = Offset(4f, 4f),
    shadowBlurRadius: Float = 8f
) {
    val textShadow = if (shadowColor != null) {
        Shadow(
            color = shadowColor,
            offset = shadowOffset,
            blurRadius = shadowBlurRadius
        )
    } else {
        null
    }

    Box(modifier) {

        Text(
            text = text,
            color = strokeColor,
            style = style.copy(
                drawStyle = Stroke(
                    width = strokeWidth,
                    join = StrokeJoin.Round
                )
            )
        )

        Text(
            text = text,
            color = fillColor,
            style = style.copy(
                drawStyle = Fill,
                shadow = textShadow
            )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    stepSize: Float = 0f,
    modifier: Modifier = Modifier
) {
    Slider(
        value = value,
        onValueChange = { newValue ->

            if (stepSize > 0f) {
                val rounded = ((newValue - valueRange.start) / stepSize).roundToInt() * stepSize + valueRange.start
                val clamped = rounded.coerceIn(valueRange)
                onValueChange(clamped)
            } else {
                onValueChange(newValue)
            }
        },
        valueRange = valueRange,
        steps = 0,
        modifier = modifier,
        colors = SliderDefaults.colors(
            thumbColor = SliderYellow,
            activeTrackColor = SliderYellow,
            inactiveTrackColor = SliderInactive
        ),
        thumb = {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .shadow(1.dp, CircleShape)
                    .background(Color.White, CircleShape)
                    .padding(2.dp)
                    .clip(CircleShape)
                    .background(SliderYellow)
            )
        }
    )
}