package ca.justinmo.countdown.emojis

import android.content.Context
import android.os.Build
import android.util.Log
import ca.justinmo.countdown.R
import java.io.BufferedReader
import java.io.IOException

/** A single emoji in the picker plus every variant that can be picked in its place. */
data class EmojiEntry(val emoji: String, val variants: List<String>)

/** A titled block of emoji, e.g. "Food & Drink". */
data class EmojiCategory(
    val id: String,
    val titleRes: Int,
    val tabEmoji: String,
    val entries: List<EmojiEntry>,
)

/** Everything the picker shows: the suggested section and the emoji categories. */
data class EmojiDataSet(val categories: List<EmojiCategory>) {

    private val variantsByEmoji: Map<String, List<String>> =
        categories
            .asSequence()
            .flatMap { it.entries.asSequence() }
            .flatMap { entry -> entry.variants.asSequence().map { variant -> variant to entry.variants } }
            .toMap()

    /**
     * Variants that can be selected instead of [emoji], including [emoji] itself. Emoji that are
     * themselves variants (for example the emoji used in the suggested section) resolve to the
     * variants of their family, so every suggestion can offer its variants too.
     */
    fun variantsOf(emoji: String): List<String> = variantsByEmoji[emoji] ?: listOf(emoji)
}

private const val AssetPath = "emoji/emoji_data.tsv"
private const val LogTag = "EmojiData"

private data class CategoryDescriptor(val id: String, val titleRes: Int, val tabEmoji: String)

private val CategoryDescriptors =
    listOf(
        CategoryDescriptor("people", R.string.emoji_category_people, "😀"),
        CategoryDescriptor("animals", R.string.emoji_category_animals, "🐻"),
        CategoryDescriptor("food", R.string.emoji_category_food, "🍔"),
        CategoryDescriptor("travel", R.string.emoji_category_travel, "✈️"),
        CategoryDescriptor("activities", R.string.emoji_category_activities, "⚽"),
        CategoryDescriptor("objects", R.string.emoji_category_objects, "💡"),
        CategoryDescriptor("symbols", R.string.emoji_category_symbols, "❤️"),
        CategoryDescriptor("flags", R.string.emoji_category_flags, "🏁"),
    )

@Volatile private var cachedDataSet: EmojiDataSet? = null

/**
 * Reads the bundled emoji data set (generated from Unicode's emoji-test.txt). The asset is small, so
 * it is parsed synchronously once and then reused.
 */
fun loadEmojiData(context: Context): EmojiDataSet {
    cachedDataSet?.let { return it }
    val categories =
        try {
            context.assets.open(AssetPath).bufferedReader().use { reader -> parseCategories(reader) }
        } catch (e: IOException) {
            Log.w(LogTag, "Unable to read $AssetPath", e)
            emptyList()
        }
    return EmojiDataSet(categories).also { cachedDataSet = it }
}

private fun parseCategories(reader: BufferedReader): List<EmojiCategory> {
    val entriesByCategory = LinkedHashMap<String, MutableList<EmojiEntry>>()
    var current: MutableList<EmojiEntry>? = null

    reader.forEachLine { line ->
        when {
            // Comments are written as "# ", which keeps the "#️⃣" keycap emoji out of this branch.
            line.isEmpty() || line.startsWith("# ") -> Unit
            line.startsWith("C\t") ->
                current = entriesByCategory.getOrPut(line.substring(2).trim()) { mutableListOf() }
            else -> {
                val separator = line.indexOf('\t')
                val baseToken = if (separator < 0) line else line.substring(0, separator)
                val base = parseEmojiToken(baseToken) ?: return@forEachLine
                val variants =
                    if (separator < 0) {
                        listOf(base)
                    } else {
                        line.substring(separator + 1)
                            .split(' ')
                            .mapNotNull { token -> parseEmojiToken(token) }
                            .ifEmpty { listOf(base) }
                    }
                current?.add(EmojiEntry(base, variants))
            }
        }
    }

    return CategoryDescriptors.mapNotNull { descriptor ->
        val entries = entriesByCategory[descriptor.id] ?: return@mapNotNull null
        EmojiCategory(
            id = descriptor.id,
            titleRes = descriptor.titleRes,
            tabEmoji = descriptor.tabEmoji,
            entries = entries,
        )
    }
}

/**
 * Parses one "<emoji>:<first supported API level>" token, returning null when the device's emoji font
 * is too old to draw it (which would otherwise show up as a tofu box in the picker).
 */
private fun parseEmojiToken(token: String): String? {
    val separator = token.lastIndexOf(':')
    if (separator < 0) return token.ifEmpty { null }
    val minApi = token.substring(separator + 1).toIntOrNull() ?: return token.substring(0, separator)
    return if (Build.VERSION.SDK_INT >= minApi) token.substring(0, separator) else null
}
