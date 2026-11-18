package ca.justinmo.countdown.emojis

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import ca.justinmo.countdown.R
import ca.justinmo.countdown.editor.dpToSp
import kotlinx.coroutines.launch

/** Number of emojis shown per row. Each item is therefore 1/6 of the screen width. */
private const val EmojisPerRow = 6

/** How strongly an item background is tinted while it is pressed or while its variants are shown. */
private const val PressedHighlightAlpha = 0.12f

/** Size of the corner triangle that marks emoji with variants, and its gap from the cell corner. */
private val VariantIndicatorSize = 5.dp
private val VariantIndicatorInset = 3.dp

/** Icon of the "Suggested icons" tab. */
private const val SuggestedTabEmoji = "✨"

val emojis by
lazy(LazyThreadSafetyMode.NONE) {
    listOf(
        "🏠",
        "🦋",
        "✈️",
        "🌴",
        "🎶",
        "👶",
        "🚗",
        "🐶",
        "😸",
        "🎂",
        "🎄",
        "💌",
        "🎃",
        "🎶",
        "🎵",
        "🎼",
        "⚜️",
        "🚸",
        "🚭",
        "⚱️",
        "⚰️",
        "🪦",
        "💍",
        "🪖",
        "🎟️",
        "🎫",
        "🎭",
        "🎃",
        "🎄",
        "🎂",
        "🎈",
        "🎁",
        "🎉",
        "🎆",
        "🎇",
        "🎉",
        "🧳",
        "⛱️",
        "🏡",
        "🏠",
        "🛬",
        "🛫",
        "✈️",
        "🚢",
        "🛳️",
        "⚓",
        "🧭",
        "🍿",
        "🏖️",
        "🏝️",
        "🌊",
        "🧑‍🍼",
        "🤱",
        "❤️‍🔥",
        "💖",
        "💘",
        "💗",
        "💕",
        "❤️",
        "💑",
        "👩‍❤️‍👨",
        "👨‍❤️‍👨",
        "👩‍❤️‍👩",
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmojiBottomSheet(
    onDismissRequest: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(),
    onEmojiSelected: (String) -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismissRequest, sheetState = sheetState) {
        EmojiPicker(onEmojiSelected)
    }
}

/**
 * A Gboard style emoji picker: a "Suggested icons" section followed by the emoji categories, with a
 * category bar for quick jumps and a variants bubble on long press.
 */
@Composable
fun EmojiPicker(onEmojiSelected: (String) -> Unit) {
    val context = LocalContext.current
    val dataSet = remember(context) { loadEmojiData(context) }
    val content = remember(dataSet) { buildPickerContent(dataSet) }
    val gridState = rememberLazyGridState()
    val tabListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var variantTarget by remember { mutableStateOf<VariantTarget?>(null) }

    val selectedTab by
        remember(gridState, content) {
            derivedStateOf {
                val firstVisible = gridState.firstVisibleItemIndex
                content.tabs.indexOfLast { it.itemIndex <= firstVisible }.coerceAtLeast(0)
            }
        }

    LaunchedEffect(selectedTab) { tabListState.animateScrollToItem(selectedTab) }

    LaunchedEffect(gridState.isScrollInProgress) {
        if (gridState.isScrollInProgress) variantTarget = null
    }

    EmojiTabs(
        tabs = content.tabs,
        selectedIndex = selectedTab,
        listState = tabListState,
        onTabClick = { index ->
            variantTarget = null
            coroutineScope.launch { gridState.animateScrollToItem(content.tabs[index].itemIndex) }
        },
    )

    LazyVerticalGrid(
        columns = GridCells.Fixed(EmojisPerRow),
        state = gridState,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(bottom = 16.dp),
    ) {
        items(
            count = content.items.size,
            key = { index -> index },
            span = { index ->
                if (content.items[index] is PickerItem.Header) GridItemSpan(maxLineSpan)
                else GridItemSpan(1)
            },
        ) { index ->
            when (val item = content.items[index]) {
                is PickerItem.Header -> SectionHeader(item.titleRes)
                is PickerItem.Cell ->
                    EmojiItem(
                        entry = item.entry,
                        showVariants = variantTarget?.itemIndex == index,
                        onSelected = onEmojiSelected,
                        onShowVariants = { variantTarget = VariantTarget(index, item.entry) },
                        onDismissVariants = { variantTarget = null },
                    )
            }
        }
    }
}

/** One selectable emoji in the grid: either a section title or an emoji cell. */
private sealed interface PickerItem {
    data class Header(val titleRes: Int) : PickerItem

    data class Cell(val entry: EmojiEntry) : PickerItem
}

/** A category shortcut in the category bar. [itemIndex] is the grid item it scrolls to. */
private data class EmojiTab(val titleRes: Int, val icon: String, val itemIndex: Int)

private class PickerContent(val items: List<PickerItem>, val tabs: List<EmojiTab>)

/** The emoji whose variants bubble is currently open, and the grid item that shows it. */
private data class VariantTarget(val itemIndex: Int, val entry: EmojiEntry)

private fun buildPickerContent(dataSet: EmojiDataSet): PickerContent {
    val items = ArrayList<PickerItem>(2048)
    val tabs = ArrayList<EmojiTab>(dataSet.categories.size + 1)

    fun addSection(titleRes: Int, icon: String, entries: List<EmojiEntry>) {
        tabs += EmojiTab(titleRes, icon, items.size)
        items += PickerItem.Header(titleRes)
        entries.forEach { entry -> items += PickerItem.Cell(entry) }
    }

    addSection(
        titleRes = R.string.suggested_icons,
        icon = SuggestedTabEmoji,
        entries = emojis.map { emoji -> EmojiEntry(emoji, dataSet.variantsOf(emoji)) },
    )
    dataSet.categories.forEach { category -> addSection(category.titleRes, category.tabEmoji, category.entries) }

    return PickerContent(items, tabs)
}

@Composable
private fun SectionHeader(titleRes: Int) {
    Text(
        text = stringResource(titleRes),
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Medium,
        modifier = Modifier
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
    )
}

@Composable
private fun EmojiTabs(
    tabs: List<EmojiTab>,
    selectedIndex: Int,
    listState: LazyListState,
    onTabClick: (Int) -> Unit,
) {
    LazyRow(
        state = listState,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        itemsIndexed(tabs) { index, tab ->
            EmojiTabItem(
                tab = tab,
                selected = index == selectedIndex,
                onClick = { onTabClick(index) },
            )
        }
    }
}

@Composable
private fun EmojiTabItem(tab: EmojiTab, selected: Boolean, onClick: () -> Unit) {
    val title = stringResource(tab.titleRes)
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val background by
        animateColorAsState(
            targetValue =
                when {
                    selected -> MaterialTheme.colorScheme.secondaryContainer
                    isPressed -> MaterialTheme.colorScheme.onSurface.copy(alpha = PressedHighlightAlpha)
                    else -> Color.Transparent
                },
            label = "emojiTabBackground",
        )

    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(background)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .semantics(mergeDescendants = true) { contentDescription = title },
        contentAlignment = Alignment.Center,
    ) {
        Text(text = tab.icon, fontSize = dpToSp(22.dp), maxLines = 1)
    }
}

/**
 * A single square emoji cell. The cell is as wide as one sixth of the screen, has no background at
 * rest, and shows a tinted background while it is pressed or while its variants are open. Long
 * pressing an emoji that has variants opens the variants bubble.
 */
@Composable
private fun EmojiItem(
    entry: EmojiEntry,
    showVariants: Boolean,
    onSelected: (String) -> Unit,
    onShowVariants: () -> Unit,
    onDismissVariants: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val haptics = LocalHapticFeedback.current
    val hasVariants = entry.variants.size > 1
    val backgroundColor by
        animateColorAsState(
            targetValue =
                if (isPressed || showVariants) {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = PressedHighlightAlpha)
                } else {
                    Color.Transparent
                },
            label = "emojiPressedBackground",
        )
    val onLongClick: (() -> Unit)? =
        if (hasVariants) {
            {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onShowVariants()
            }
        } else {
            null
        }
    val variantsLabel = stringResource(R.string.emoji_variants)

    BoxWithConstraints(
        modifier =
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .combinedClickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onLongClickLabel = if (hasVariants) variantsLabel else null,
                    onLongClick = onLongClick,
                    onClick = { onSelected(entry.emoji) },
                ),
        contentAlignment = Alignment.Center,
    ) {
        // Scale the glyph with the cell so the grid looks consistent on any screen width.
        val emojiFontSize = dpToSp(maxWidth * 0.5f)
        Box(
            modifier = Modifier
                .padding(2.dp)
                .fillMaxSize()
                .clip(RoundedCornerShape(8.dp))
                .background(backgroundColor),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = entry.emoji, fontSize = emojiFontSize, maxLines = 1)
            if (hasVariants) {
                VariantIndicator(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = VariantIndicatorInset, bottom = VariantIndicatorInset),
                )
            }
        }

        if (showVariants && hasVariants) {
            EmojiVariantPopup(
                variants = entry.variants,
                onDismiss = onDismissVariants,
                onSelected = onSelected,
            )
        }
    }
}

/**
 * Draws the Gboard style corner triangle that tells the user an emoji offers variants on long
 * press.
 */
@Composable
private fun VariantIndicator(modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
    Canvas(modifier = modifier.size(VariantIndicatorSize)) {
        val corner =
            Path().apply {
                moveTo(size.width, 0f)
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
                close()
            }
        drawPath(path = corner, color = color)
    }
}

/**
 * Gboard style bubble with the selectable variants of an emoji. It floats above the emoji that was
 * long pressed, flips below it when there is no room, and scrolls horizontally for long lists.
 */
@Composable
private fun EmojiVariantPopup(
    variants: List<String>,
    onDismiss: () -> Unit,
    onSelected: (String) -> Unit,
) {
    val gapPx = with(LocalDensity.current) { 8.dp.roundToPx() }
    Popup(
        popupPositionProvider = remember(gapPx) { EmojiVariantPositionProvider(gapPx) },
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true),
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurface,
            shadowElevation = 8.dp,
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 4.dp, vertical = 2.dp)
                    .widthIn(max = 320.dp)
                    .horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                variants.forEach { variant ->
                    VariantItem(emoji = variant, onClick = { onSelected(variant) })
                }
            }
        }
    }
}

@Composable
private fun VariantItem(emoji: String, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val background by
        animateColorAsState(
            targetValue =
                if (isPressed) {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = PressedHighlightAlpha)
                } else {
                    Color.Transparent
                },
            label = "emojiVariantBackground",
        )

    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(background)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = emoji, fontSize = dpToSp(28.dp), maxLines = 1)
    }
}

/** Places the variants bubble above the pressed emoji, clamped to the window. */
private class EmojiVariantPositionProvider(private val gap: Int) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val maxX = (windowSize.width - popupContentSize.width - gap).coerceAtLeast(gap)
        val x = (anchorBounds.center.x - popupContentSize.width / 2).coerceIn(gap, maxX)

        val above = anchorBounds.top - popupContentSize.height - gap
        val below = anchorBounds.bottom + gap
        val y =
            when {
                above >= gap -> above
                below + popupContentSize.height <= windowSize.height - gap -> below
                else -> (windowSize.height - popupContentSize.height - gap).coerceAtLeast(gap)
            }
        return IntOffset(x, y)
    }
}
