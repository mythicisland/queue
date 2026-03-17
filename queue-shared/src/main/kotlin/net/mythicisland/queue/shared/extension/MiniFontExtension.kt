package net.mythicisland.queue.shared.extension

import kotlin.text.iterator

private val chars = mutableMapOf(
    'a' to 'ᴀ',
    'b' to 'ʙ',
    'c' to 'ᴄ',
    'd' to 'ᴅ',
    'e' to 'ᴇ',
    'f' to 'ғ',
    'g' to 'ɢ',
    'h' to 'ʜ',
    'i' to 'ɪ',
    'j' to 'ᴊ',
    'k' to 'ᴋ',
    'l' to 'ʟ',
    'm' to 'ᴍ',
    'n' to 'ɴ',
    'o' to 'ᴏ',
    'p' to 'ᴘ',
    'q' to 'ǫ',
    'r' to 'ʀ',
    's' to 'ѕ',
    't' to 'ᴛ',
    'u' to 'ᴜ',
    'v' to 'ᴠ',
    'w' to 'ᴡ',
    'x' to 'х',
    'y' to 'ʏ',
    'z' to 'ᴢ',
    '0' to '₀',
    '1' to '₁',
    '2' to '₂',
    '3' to '₃',
    '4' to '₄',
    '5' to '₅',
    '6' to '₆',
    '7' to '₇',
    '8' to '₈',
    '9' to '₉'
)

fun String.toMiniFont(): String {
    val result = StringBuilder()
    var insideTag = false

    for (char in this) {
        when {
            char == '<' -> {
                insideTag = true
                result.append(char)
            }
            char == '>' -> {
                insideTag = false
                result.append(char)
            }
            insideTag -> result.append(char)
            else -> result.append(chars[char.lowercaseChar()] ?: char)
        }
    }

    return result.toString()
}