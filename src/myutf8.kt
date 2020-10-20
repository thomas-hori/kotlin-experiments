/**
 * Convert a UTF-8 ByteArray to a UTF-16 String. This is implemented without the use of any of the
 * stock Kotlin/Java functions for doing that, as an exercise.
 *
 * Note: this implements standards-compliant UTF-8 (aka. UTF8mb4 or AL32UTF8), and it complies by
 * rejecting by default codes peculiar to UTF8mb3 (aka. CESU-8) and mUTF8, which are variants matching
 * UTF8mb4 for U+0001..U+FFFF but tending to use codes invalid in UTF8mb4 for the remainder.
 * The optional `soft` argument will relax this constraint.
 */
fun decodeUtf8(input:ByteArray, soft:Boolean = false):String {
    // https://encoding.spec.whatwg.org/#utf-8-decoder
    var codePoint = 0
    var bytesSeen = 0
    var bytesNeeded = 0
    var lowerBoundary = 0x80
    var upperBoundary = 0xBF
    var output = ""
    var inIndex = 0
    while (inIndex < input.size) {
        val byte = (input[inIndex].toInt() + 256) % 256
        when {
            (bytesNeeded == 0) -> {
                when (byte) {
                    in 0..0x7F -> output += String(charArrayOf(byte.toChar()))
                    0xC0 -> {
                        if (soft) {
                            bytesNeeded = 1
                        } else {
                            output += "\uFFFD"
                        }
                    }
                    in 0xC2..0xDF -> {
                        bytesNeeded = 1
                        codePoint = byte and 0x1F
                    }
                    in 0xE0..0xEF -> {
                        when (byte) {
                            0xE0 -> lowerBoundary = 0xA0
                            0xED -> upperBoundary = if (!soft) 0x9F else 0xBF
                        }
                        bytesNeeded = 2
                        codePoint = byte and 0xF
                    }
                    in 0xF0..0xF4 -> {
                        when (byte) {
                            0xF0 -> lowerBoundary = 0x90
                            0xF4 -> upperBoundary = 0x8F
                        }
                        bytesNeeded = 3
                        codePoint = byte and 0x7
                    }
                    else -> output += "\uFFFD"
                }
                inIndex += 1
            }
            (byte !in lowerBoundary..upperBoundary) -> {
                codePoint = 0
                bytesNeeded = 0
                bytesSeen = 0
                // Don't increment inIndex
                output += "\uFFFD"
            }
            else -> {
                lowerBoundary = 0x80
                upperBoundary = 0xBF
                codePoint = (codePoint shl 6) or (byte and 0x3F)
                bytesSeen += 1
                if (bytesSeen == bytesNeeded) {
                    if (codePoint in 1..0x7F) {
                        // Should only reach here in soft mode.
                        // Two-byte overlongs for ASCII characters other than NUL serve no purpose besides
                        //   getting injection sequences to bypass sanitiser code. So, U+FFFD them.
                        codePoint = 0xFFFD
                    }
                    output += when (codePoint) {
                        in 0..0xFFFF -> {
                            String(charArrayOf(codePoint.toChar()))
                        }
                        in 0x10000..0x10FFFF -> {
                            val reducedCodePoint = codePoint - 0x10000
                            val lowSurrogate = ((reducedCodePoint and 0x3FF) + 0xDC00).toChar()
                            val highSurrogate = ((reducedCodePoint shr 10) + 0xD800).toChar()
                            String(charArrayOf(highSurrogate, lowSurrogate))
                        }
                        else -> {
                            "\uFFFD"
                        }
                    }
                    codePoint = 0
                    bytesSeen = 0
                    bytesNeeded = 0
                }
                inIndex += 1
            }
        }
    }
    return output
}

/**
 * Convert a UTF-16 String to a UTF-8 ByteArray. This is implemented without the use of any of the
 * stock Kotlin/Java functions for doing that, as an exercise.
 */
fun encodeUtf8 (input:String):ByteArray {
    val output = MutableList(0){0.toByte()}
    var inIndex = 0
    while (inIndex < input.length) {
        // Decode UTF-16
        val leadWord = input[inIndex].toInt()
        inIndex += 1
        val codePoint = when (leadWord) {
            in 0xD800..0xDBFF -> {
                if (inIndex > input.lastIndex) { // Truncated lead word
                    0xFFFD
                } else {
                    when (val trailWord = input[inIndex].toInt()) {
                        in 0xDC00..0xDFFF -> { // Two-word code
                            inIndex += 1
                            (((leadWord and 0x3FF) shl 10) or (trailWord and 0x3FF)) + 0x10000
                        }
                        else -> 0xFFFD // Isolated lead word
                    }
                }
            }
            in 0xDC00..0xDFFF -> 0xFFFD // Isolated trail word
            !in 0..0x10FFFF -> 0xFFFD // Beyond Unicode
            else -> leadWord // One-word code
        }
        // Encode UTF-8: https://encoding.spec.whatwg.org/#utf-8-encoder
        if (codePoint < 0x80) {
            output.add(output.size, codePoint.toByte())
        } else {
            var count = when (codePoint) {
                in 0x80..0x7FF -> 1
                in 0x800..0xFFFF -> 2
                in 0x10000..0x10FFFF -> 3
                else -> null
            }!! // i.e. assert codePoint in 0x80..0x10FFFF
            val offset = when (codePoint) {
                in 0x80..0x7FF -> 0xC0
                in 0x800..0xFFFF -> 0xE0
                in 0x10000..0x10FFFF -> 0xF0
                else -> null
            }!!
            output.add(output.size, ((codePoint shr (6 * count)) + offset).toByte())
            while (count > 0) {
                val temp = codePoint shr (6 * (count - 1))
                output.add(output.size, ((temp and 0x3F) or 0x80).toByte())
                count -= 1
            }
        }
    }
    return output.toByteArray()
}

fun main () {
    val input = readLine() ?: ""
    println(input)
    println(encodeUtf8(input))
    println(encodeUtf8(input).toList())
    println(decodeUtf8(encodeUtf8(input)))
}