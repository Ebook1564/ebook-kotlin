package com.freeebookbundle.epub

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.TextToolbarStatus
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties

import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Search

class EpubTextToolbar(
    private val onColorSelected: (Int) -> Unit,
    private val onCopyRequested: () -> Unit,
    private val onSpeakRequested: () -> Unit,
    private val onDictionaryRequested: () -> Unit,
    private val onWikipediaRequested: () -> Unit,
    private val onDismissSelection: () -> Unit = {},
    private val onMenuShown: (Rect) -> Unit = {}
) : TextToolbar {
    
    private var _status = mutableStateOf(TextToolbarStatus.Hidden)
    override val status: TextToolbarStatus
        get() = _status.value

    // Latch: set true when user explicitly dismisses without picking a colour.
    // Suppresses the immediate re-show that SelectionManager triggers.
    private var isDismissedByUser = false

    private var currentRect by mutableStateOf(Rect.Zero)
    private var activeOnCopyRequested by mutableStateOf<(() -> Unit)?>(null)
    private var lastActionTime by mutableLongStateOf(0L)

    override fun hide() {
        if (_status.value != TextToolbarStatus.Hidden) {
            _status.value = TextToolbarStatus.Hidden
            activeOnCopyRequested = null
        }
    }

    override fun showMenu(
        rect: Rect,
        onCopyRequested: (() -> Unit)?,
        onPasteRequested: (() -> Unit)?,
        onCutRequested: (() -> Unit)?,
        onSelectAllRequested: (() -> Unit)?
    ) {
        // Consume the latch on ANY showMenu call — blocked or not.
        // This prevents the latch staying stuck when hideMenu() fires
        // programmatically (e.g. from SelectionContainer remount after selectionKey++).
        val blocked = isDismissedByUser
        isDismissedByUser = false

        if (blocked) {
            _status.value = TextToolbarStatus.Hidden
            return
        }

        // Prevent re-showing within 500ms of an action to let system state settle
        if (System.currentTimeMillis() - lastActionTime < 500) return

        currentRect = rect
        activeOnCopyRequested = onCopyRequested
        onMenuShown(rect)
        _status.value = TextToolbarStatus.Shown
    }

    // Call this from every action that should NOT persist the toolbar:
    // dismiss-tap, copy, speak, etc. Do NOT call it from colour/note actions
    // that already handle their own selectionKey increment.
    fun dismissedByUser() {
        isDismissedByUser = true
        _status.value = TextToolbarStatus.Hidden
    }

    // Call this when a brand-new selection gesture begins so the latch resets.
    fun resetDismissLatch() {
        isDismissedByUser = false
    }

    @Composable
    fun Content() {
        if (_status.value == TextToolbarStatus.Shown) {
            Popup(
                offset = IntOffset(
                    currentRect.left.toInt(),
                    (currentRect.top - 200).toInt().coerceAtLeast(0)
                ),
                onDismissRequest = { hide() },
                properties = PopupProperties(focusable = true)
            ) {
                Surface(
                    modifier = Modifier.wrapContentSize().padding(8.dp),
                    shape = RoundedCornerShape(28.dp),
                    tonalElevation = 12.dp,
                    shadowElevation = 16.dp,
                    color = Color(0xFF222222)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        // 1. Color Palette (Top Row)
                        Row(
                            modifier = Modifier.padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            HighlightDot(Color(0xFFFFF176)) { 
                                lastActionTime = System.currentTimeMillis()
                                onColorSelected(0xFFFFF176.toInt()); onDismissSelection(); hide() 
                            }
                            HighlightDot(Color(0xFFAED581)) { 
                                lastActionTime = System.currentTimeMillis()
                                onColorSelected(0xFFAED581.toInt()); onDismissSelection(); hide() 
                            }
                            HighlightDot(Color(0xFF81D4FA)) { 
                                lastActionTime = System.currentTimeMillis()
                                onColorSelected(0xFF81D4FA.toInt()); onDismissSelection(); hide() 
                            }
                            HighlightDot(Color(0xFFF06292)) { 
                                lastActionTime = System.currentTimeMillis()
                                onColorSelected(0xFFF06292.toInt()); onDismissSelection(); hide() 
                            }
                            }

                            HorizontalDivider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 4.dp))

                            // 2. Action Buttons (Bottom Row)

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        ) {
                            ToolbarAction(Icons.Default.ContentCopy, "Copy") { 
                                lastActionTime = System.currentTimeMillis()
                                val copy = activeOnCopyRequested
                                if (copy != null) copy() else onCopyRequested()
                                onDismissSelection(); hide() 
                            }
                            ToolbarAction(Icons.Default.VolumeUp, "Speak") { 
                                lastActionTime = System.currentTimeMillis()
                                onSpeakRequested(); onDismissSelection(); hide() 
                            }
                            ToolbarAction(Icons.Default.MenuBook, "Dict") { 
                                lastActionTime = System.currentTimeMillis()
                                onDictionaryRequested(); onDismissSelection(); hide() 
                            }
                            ToolbarAction(Icons.Default.Search, "Wiki") { 
                                lastActionTime = System.currentTimeMillis()
                                onWikipediaRequested(); onDismissSelection(); hide() 
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ToolbarAction(icon: ImageVector, label: String, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(36.dp)) {
        Icon(icon, label, tint = Color.White, modifier = Modifier.size(20.dp))
    }
}


@Composable
private fun HighlightDot(color: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .background(color, CircleShape)
            .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape)
            .clickable { onClick() }
    )
}
