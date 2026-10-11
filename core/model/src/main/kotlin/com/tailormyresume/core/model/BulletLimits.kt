package com.tailormyresume.core.model

val EvidenceBullet.isTooLong: Boolean get() = text.length > ProfileLimits.MAX_BULLET_LENGTH

val ProfileEntry.hasTooLongBullet: Boolean get() = bullets.any { it.isTooLong }

val ProfileEntry.hasTooManyBullets: Boolean get() = bullets.size > ProfileLimits.MAX_BULLETS_PER_ENTRY

val ProfileEntry.exceedsLimits: Boolean get() = hasTooLongBullet || hasTooManyBullets

fun ProfileEntry.continues(previous: ProfileEntry): Boolean =
    category == previous.category &&
        title.trim() == previous.title.trim() &&
        organization.trim() == previous.organization.trim() &&
        startDate.trim() == previous.startDate.trim() &&
        endDate.trim() == previous.endDate.trim()

fun List<ProfileEntry>.countLogicalEntries(category: EntryCategory): Int = 0

fun splitBulletsForEntries(bullets: List<String>, entriesBefore: Int, entriesAfter: Int): List<List<String>> {
    val chunks = bullets.chunked(ProfileLimits.MAX_BULLETS_PER_ENTRY)
    val fits = entriesBefore + chunks.size + entriesAfter <= ProfileLimits.MAX_ENTRIES
    return if (chunks.size > 1 && fits) chunks else listOf(bullets)
}

private val whitespaceRun = Regex("\\s+")
private const val SENTENCE_ENDINGS = ".!?।"

private val abbreviations = setOf(
    "rs", "mr", "mrs", "ms", "dr", "prof", "no", "vs", "pvt", "ltd", "inc", "co", "corp", "st", "approx", "etc",
    "jan", "feb", "mar", "apr", "jun", "jul", "aug", "sep", "sept", "oct", "nov", "dec",
    "sr", "jr", "asst", "assoc", "dy", "addl", "govt", "engg", "dept", "univ", "inst", "mgr", "mgmt", "exec",
    "admin", "tech", "intl", "natl", "est", "ref", "fig", "vol", "ed", "hons", "sec", "div", "bros",
)

fun fitBulletsToLimit(texts: List<String>): List<String> {
    var roomForExtra = ProfileLimits.MAX_BULLETS_PER_ENTRY - texts.size
    return texts.flatMap { text ->
        val pieces = if (text.length > ProfileLimits.MAX_BULLET_LENGTH) packSentences(text) else emptyList()
        val extra = pieces.size - 1
        val fits = pieces.isNotEmpty() && extra <= roomForExtra &&
            (extra > 0 || pieces.single().length <= ProfileLimits.MAX_BULLET_LENGTH)
        if (fits) {
            roomForExtra -= extra
            pieces
        } else {
            listOf(text)
        }
    }
}

private fun endsWithAbbreviation(text: String, dotIndex: Int): Boolean {
    val tokenStart = text.substring(0, dotIndex).lastIndexOfAny(charArrayOf(' ', '\t', '\n', '\r', '\u00A0')) + 1
    val token = text.substring(tokenStart, dotIndex).trimStart { !it.isLetterOrDigit() }
    return token.contains('.') || token.lowercase() in abbreviations || (token.length == 1 && token[0].isUpperCase())
}

private fun sentencesOf(text: String): List<String> {
    val sentences = mutableListOf<String>()
    var start = 0
    whitespaceRun.findAll(text).forEach { gap ->
        val end = gap.range.last + 1
        if (gap.range.first == 0 || end >= text.length) return@forEach
        val previous = text[gap.range.first - 1]
        val atParagraphBreak = gap.value.count { it == '\n' } >= 2
        val atSentenceEnd = previous in SENTENCE_ENDINGS && !text[end].isLowerCase() &&
            !(previous == '.' && endsWithAbbreviation(text, gap.range.first - 1))
        if (atParagraphBreak || atSentenceEnd) {
            sentences += text.substring(start, gap.range.first)
            start = end
        }
    }
    sentences += text.substring(start)
    return sentences.map { it.trim() }.filter { it.isNotEmpty() }
}

private fun packSentences(text: String): List<String> {
    val packed = mutableListOf<String>()
    sentencesOf(text).forEach { sentence ->
        val last = packed.lastOrNull()
        if (last != null && last.length + 1 + sentence.length <= ProfileLimits.MAX_BULLET_LENGTH) {
            packed[packed.lastIndex] = "$last $sentence"
        } else {
            packed += sentence
        }
    }
    return packed.map { it.replace(whitespaceRun, " ") }
}
