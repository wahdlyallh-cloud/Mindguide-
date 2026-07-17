package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight

// 1. Serialization Helper
fun serializeDrawing(lines: List<List<Offset>>): String {
    return lines.joinToString("_") { line ->
        line.joinToString(";") { "${it.x},${it.y}" }
    }
}

// 2. Deserialization Helper
fun deserializeDrawing(data: String): List<List<Offset>> {
    if (data.isEmpty() || data == "null") return emptyList()
    return try {
        data.split("_").map { lineStr ->
            lineStr.split(";").map { ptStr ->
                val coords = ptStr.split(",")
                Offset(coords[0].toFloat(), coords[1].toFloat())
            }
        }
    } catch (e: Exception) {
        emptyList()
    }
}

// 3. Interactive Whiteboard Drawing Component
@Composable
fun InteractiveWhiteboard(
    initialDrawingData: String?,
    modifier: Modifier = Modifier,
    onDrawingChanged: (String) -> Unit
) {
    val lines = remember { 
        mutableStateListOf<List<Offset>>().apply {
            if (!initialDrawingData.isNullOrEmpty()) {
                addAll(deserializeDrawing(initialDrawingData))
            }
        }
    }
    
    val currentLine = remember { mutableStateOf<List<Offset>>(emptyList()) }
    var selectedColor by remember { mutableStateOf(Color(0xFF3B82F6)) } // Default Primary blue
    
    val colors = listOf(
        Color(0xFF3B82F6), // Blue
        Color(0xFFEF4444), // Red
        Color(0xFF10B981), // Green
        Color(0xFF000000), // Black
        Color(0xFF8B5CF6)  // Purple
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
            .padding(12.dp),
        horizontalAlignment = Alignment.End
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(
                    onClick = {
                        if (lines.isNotEmpty()) {
                            lines.removeAt(lines.size - 1)
                            onDrawingChanged(serializeDrawing(lines))
                        }
                    },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text("تراجع ↩️", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                TextButton(
                    onClick = {
                        lines.clear()
                        currentLine.value = emptyList()
                        onDrawingChanged("")
                    },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text("مسح الكل 🧼", fontSize = 12.sp, color = Color.Red, fontWeight = FontWeight.Bold)
                }
            }
            Text(
                text = "لوحة الرسم والسبورة التفاعلية 🎨",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Drawing Area Canvas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .background(Color.White, shape = RoundedCornerShape(12.dp))
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            currentLine.value = listOf(offset)
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            currentLine.value = currentLine.value + change.position
                        },
                        onDragEnd = {
                            if (currentLine.value.isNotEmpty()) {
                                lines.add(currentLine.value)
                                currentLine.value = emptyList()
                                onDrawingChanged(serializeDrawing(lines))
                            }
                        }
                    )
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Draw historic strokes
                lines.forEach { line ->
                    if (line.size > 1) {
                        val path = Path().apply {
                            moveTo(line.first().x, line.first().y)
                            for (i in 1 until line.size) {
                                lineTo(line[i].x, line[i].y)
                            }
                        }
                        drawPath(
                            path = path,
                            color = selectedColor,
                            style = Stroke(width = 5f, cap = StrokeCap.Round)
                        )
                    }
                }
                
                // Draw current drawing stroke
                val activeLine = currentLine.value
                if (activeLine.size > 1) {
                    val path = Path().apply {
                        moveTo(activeLine.first().x, activeLine.first().y)
                        for (i in 1 until activeLine.size) {
                            lineTo(activeLine[i].x, activeLine[i].y)
                        }
                    }
                    drawPath(
                        path = path,
                        color = selectedColor,
                        style = Stroke(width = 5f, cap = StrokeCap.Round)
                    )
                }
            }
            
            if (lines.isEmpty() && currentLine.value.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "ارسم مشاعرك يدوياً هنا للتعبير البديل عن مزاجك...",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray.copy(alpha = 0.7f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Pen color selection row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                colors.forEach { color ->
                    val isSelected = selectedColor == color
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(color, shape = CircleShape)
                            .border(
                                width = if (isSelected) 3.dp else 0.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                shape = CircleShape
                            )
                            .clickable { selectedColor = color }
                    )
                }
            }
            Text(
                text = "لون قلم الرسم:",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// 4. Read-Only Whiteboard Viewer Composable
@Composable
fun ViewOnlyWhiteboard(
    drawingData: String?,
    modifier: Modifier = Modifier,
    lineColor: Color = Color(0xFF3B82F6)
) {
    val lines = remember(drawingData) { deserializeDrawing(drawingData ?: "") }
    
    Box(
        modifier = modifier
            .background(Color.White, shape = RoundedCornerShape(12.dp))
            .border(1.dp, Color.LightGray.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            lines.forEach { line ->
                if (line.size > 1) {
                    val path = Path().apply {
                        moveTo(line.first().x, line.first().y)
                        for (i in 1 until line.size) {
                            lineTo(line[i].x, line[i].y)
                        }
                    }
                    drawPath(
                        path = path,
                        color = lineColor,
                        style = Stroke(width = 4f, cap = StrokeCap.Round)
                    )
                }
            }
        }
    }
}
