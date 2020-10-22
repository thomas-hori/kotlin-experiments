import java.util.*
import kotlin.collections.ArrayDeque

/**
 * Given a BMP dynamic window designator (i.e. the designator byte interpreted as unsigned, as
 * an int), return the offset associated, or null for reserved designators.
 */
fun getBmpOffset(window:Int):Int? {
    return when (window) {
        in 0x01..0x67 -> window * 0x80
        in 0x68..0xA7 -> (window * 0x80) + 0xAC00
        0xF9 -> 0xC0
        0xFA -> 0x0250
        0xFB -> 0x0370
        0xFC -> 0x0530
        0xFD -> 0x3040
        0xFE -> 0x30A0
        0xFF -> 0xFF60
        else -> null
    }
}

/**
 * Given a code point in the BMP, return the BMP dynamic window designator for the window containing
 * that code point, or null if the code point is non-compressible or outside the BMP.
 */
fun getBmpWindow(offset:Int):Int? {
    return when (offset) {
        in 0xC0..0x013F -> 0xF9
        in 0x0250..0x02CF -> 0xFA
        in 0x0370..0x03EF -> 0xFB
        in 0x0530..0x05AF -> 0xFC
        in 0x3040..0x30BF -> 0xFD
        in 0x30A0..0x311F -> 0xFE
        in 0xFF60..0xFFDF -> 0xFF
        in 0x00..0x33FF -> offset / 0x80
        in 0xE000..0xFFFF -> (offset - 0xAC00) / 0x80
        else -> null
    }
}

/**
 * Given a non-BMP dynamic window designator, return the offset associated.
 */
fun getAstralOffset(window:Int):Int {
    return (window * 0x80) + 0x10000
}

/**
 * Given a non-BMP code point, return the non-BMP dynamic window designator for the window
 * containing it.
 */
fun getAstralWindow(offset:Int):Int {
    return (offset - 0x10000) / 0x80
}

/**
 * Given a byte object, obtain its unsigned value as an int.
 */
fun grokByte(input:Byte):Int {
    return (input.toInt() + 256).rem(256)
}

/**
 * Add an item to the end of the list
 */
fun<T> MutableList<T>.append(thing:T) {
    this.add(this.size, thing)
}

/**
 * Return a UTF-16 code corresponding to this numbered code point.
 */
fun Int.asCodePoint ():String = when (this) {
    in 0..0xFFFF -> {
        String(charArrayOf(this.toChar()))
    }
    in 0x10000..0x10FFFF -> {
        val reducedCodePoint = this - 0x10000
        val lowSurrogate = ((reducedCodePoint and 0x3FF) + 0xDC00).toChar()
        val highSurrogate = ((reducedCodePoint shr 10) + 0xD800).toChar()
        String(charArrayOf(highSurrogate, lowSurrogate))
    }
    else -> {
        "\uFFFD"
    }
}

/**
 * Decode some SCSU (Standard Compression Scheme for Unicode) text to UTF-16.
 */
fun decodeScsu(input:ByteArray):String {
    // https://www.unicode.org/reports/tr6/tr6-4.html
    val staticOffsets = listOf(0x00, 0x80, 0x100, 0x300, 0x2000, 0x2080, 0x2100, 0x3000)
    val dynamicOffsets = mutableListOf(0x80, 0xC0, 0x0400, 0x0600, 0x0900, 0x3040, 0x30A0, 0xFF00)
    var doubleByteMode = false
    var pointer = 0
    var activeWindow = 0
    val output = mutableListOf("")
    while (pointer < input.size) {
        when (doubleByteMode) {
            false -> {
                when (val value = grokByte(input[pointer])) {
                    0x00, 0x09, 0x0A, 0x0D, in 0x20..0x7F -> { // Verbatim ASCII subset
                        output.append(value.asCodePoint())
                    }
                    in 0x80..0xFF -> { // Single-byte refs to active dynamic window
                        val offset = dynamicOffsets[activeWindow]
                        output.append((value - 0x80 + offset).asCodePoint())
                    }
                    in 0x01..0x09 -> { // SQn
                        if (pointer > (input.lastIndex - 1)) {
                            output.append("\uFFFD")
                        } else {
                            pointer += 1
                            when (val value2 = grokByte(input[pointer])) {
                                in 0x00..0x7F -> {
                                    val offset = staticOffsets[value - 1]
                                    output.append((value2 + offset).asCodePoint())
                                }
                                in 0x80..0xFF -> {
                                    val offset = dynamicOffsets[value - 1]
                                    output.append((value2 - 0x80 + offset).asCodePoint())
                                }
                            }
                        }
                    }
                    0x0B -> { // SDX
                        if (pointer > (input.lastIndex - 2)) {
                            output.append("\uFFFD")
                        } else {
                            pointer += 1
                            val value2 = grokByte(input[pointer])
                            pointer += 1
                            val value3 = grokByte(input[pointer])
                            activeWindow = value2 shr 5
                            val offset = getAstralOffset(((value2 and 0x1F) shl 8) or value3)
                            dynamicOffsets[activeWindow] = offset
                        }
                    }
                    0x0C -> output.append("\uFFFD") // (reserved)
                    0x0E -> { // SQU
                        if (pointer > (input.lastIndex - 2)) {
                            output.append("\uFFFD")
                        } else {
                            pointer += 1
                            val value2 = grokByte(input[pointer])
                            pointer += 1
                            val value3 = grokByte(input[pointer])
                            output.append(((value2 shl 8) or value3).asCodePoint())
                        }
                    }
                    0x0F -> doubleByteMode = true // SCU
                    in 0x10..0x17 -> activeWindow = value - 0x10 // SCn
                    in 0x18..0x1F -> { // SDn
                        if (pointer > (input.lastIndex - 1)) {
                            output.append("\uFFFD")
                        } else {
                            pointer += 1
                            val value2 = grokByte(input[pointer])
                            activeWindow = value - 0x18
                            val offset = getBmpOffset(value2)
                            if (offset != null) {
                                dynamicOffsets[activeWindow] = offset
                            } else {
                                output.append("\uFFFD")
                            }
                        }
                    }
                }
            }
            true -> {
                when (val value = grokByte(input[pointer])) {
                    in 0x00..0xDF, in 0xF3..0xFF -> { // Verbatim UTF-16BE subset
                        if (pointer > (input.lastIndex - 1)) {
                            output.append("\uFFFD")
                        } else {
                            pointer += 1
                            val value2 = grokByte(input[pointer])
                            output.append(((value shl 8) or value2).asCodePoint())
                        }
                    }
                    in 0xE0..0xE7 -> { // UCn
                        activeWindow = value - 0xE0
                        doubleByteMode = false
                    }
                    in 0xE8..0xEF -> { // UDn
                        if (pointer > (input.lastIndex - 1)) {
                            output.append("\uFFFD")
                        } else {
                            pointer += 1
                            val value2 = grokByte(input[pointer])
                            activeWindow = value - 0xE8
                            doubleByteMode = false
                            val offset = getBmpOffset(value2)
                            if (offset != null) {
                                dynamicOffsets[activeWindow] = offset
                            } else {
                                output.append("\uFFFD")
                            }
                        }
                    }
                    0xF0 -> { // UQU
                        if (pointer > (input.lastIndex - 2)) {
                            output.append("\uFFFD")
                        } else {
                            pointer += 1
                            val value2 = grokByte(input[pointer])
                            pointer += 1
                            val value3 = grokByte(input[pointer])
                            output.append(((value2 shl 8) or value3).asCodePoint())
                        }
                    }
                    0xF1 -> { // UDX
                        if (pointer > (input.lastIndex - 2)) {
                            output.append("\uFFFD")
                        } else {
                            pointer += 1
                            val value2 = grokByte(input[pointer])
                            pointer += 1
                            val value3 = grokByte(input[pointer])
                            activeWindow = value2 shr 5
                            val offset = getAstralOffset(((value2 and 0x1F) shl 8) or value3)
                            dynamicOffsets[activeWindow] = offset
                            doubleByteMode = false
                        }
                    }
                    0xF2 -> output.append("\uFFFD") // (reserved)
                }
            }
        }
        pointer += 1
    }
    return output.joinToString("")
}

/**
 * Given a number and a list of offsets, return the first index into the offset list corresponding
 * to an offset where the passed number is within a 128-integer range beginning at that offset, or
 * null if it isn't for any of the offsets.
 */
fun inWindows (value:Int?, windows:List<Int>):Int? {
    var index = 0
    for (window in windows) {
        if (value in window..(window + 0x7F)) {
            return index
        }
        index += 1
    }
    return null
}

/**
 * Return whether a given code point can be accessed via a single-byte-mode window in SCSU.
 *
 * This corresponds to legacy zones A and R of the BMP, plus all non-BMP planes.
 */
fun isCompressible (value:Int?):Boolean {
    // Is it either in zones A and R of the BMP, or Astral (SCSU's semi-arbitrary "compressible" region)
    // Excluding zone I is understandable, zone S arguably isn't part of Unicode, but zone O should really
    //   be included were it designed today (especially since zones don't officially exist anymore).
    return value in 0x00..0x33FF || value in 0xE000..0x10FFFF
}

/**
 * Convert some text from UTF-16 to a list of code points.
 */
fun unUtf16 (input:String):List<Int> {
    val output = MutableList(0){0}
    var pointer = 0
    while (pointer < input.length) {
        val leadWord = input[pointer].toInt()
        pointer += 1
        output.append(when (leadWord) {
            in 0xD800..0xDBFF -> {
                if (pointer > input.lastIndex) { // Truncated lead word
                    0xFFFD
                } else {
                    when (val trailWord = input[pointer].toInt()) {
                        in 0xDC00..0xDFFF -> { // Two-word code
                            pointer += 1
                            (((leadWord and 0x3FF) shl 10) or (trailWord and 0x3FF)) + 0x10000
                        }
                        else -> 0xFFFD // Isolated lead word
                    }
                }
            }
            in 0xDC00..0xDFFF -> 0xFFFD // Isolated trail word
            !in 0..0x10FFFF -> 0xFFFD // Beyond Unicode
            else -> leadWord // One-word code
        })
    }
    return output
}

/**
 * Convert some text from UTF-16 to SCSU.
 */
@OptIn(kotlin.ExperimentalStdlibApi::class)
fun encodeScsu (inputString:String):ByteArray {
    // https://www.unicode.org/notes/tn14/UnicodeCompression.pdf#page=17
    val input = unUtf16(inputString)
    val output = MutableList(0) { 0.toByte() }
    val staticOffsets = listOf(0x00, 0x80, 0x100, 0x300, 0x2000, 0x2080, 0x2100, 0x3000)
    val dynamicOffsets = mutableListOf(0x80, 0xC0, 0x0400, 0x0600, 0x0900, 0x3040, 0x30A0, 0xFF00)
    val windowAges:ArrayDeque<Int> = ArrayDeque(listOf(7, 6, 5, 4, 3, 2, 1, 0))
    var doubleByteMode = false
    var pointer = 0
    var activeWindow = 0
    while (pointer < input.size) {
        val value = input[pointer]
        val value2 = if (pointer < input.lastIndex) input[pointer + 1] else null
        when (doubleByteMode) {
            false -> {
                when (value) {
                    0x00, 0x09, 0x0A, 0x0D, in 0x20..0x7F -> { // Verbatim ASCII subset
                        output.append(value.toByte())
                    }
                    in 0x01..0x08, 0x0B, 0x0C, in 0x0E..0x1F -> {
                        output.append(0x01.toByte()) // SQ0
                        output.append(value.toByte())
                    }
                    in 0x00..0x33FF, in 0xE000..0x10FFFF -> {
                        when (val windowIndex = inWindows(value, dynamicOffsets)) {
                            activeWindow -> {
                                output.append((value - dynamicOffsets[activeWindow] + 0x80).toByte())
                            }
                            null -> {
                                val staticWindow = inWindows(value, staticOffsets)
                                if (staticWindow != null) {
                                    output.append((0x01 + staticWindow).toByte()) // SQn
                                    output.append((value - staticOffsets[staticWindow]).toByte())
                                } else if (value <= 0xFFFF) {
                                    val newWindow = getBmpWindow(value)!!
                                    activeWindow = windowAges.removeLast()
                                    windowAges.addFirst(activeWindow)
                                    dynamicOffsets[activeWindow] = getBmpOffset(newWindow)!!
                                    output.append((0x18 + activeWindow).toByte()) // SDn
                                    output.append(newWindow.toByte())
                                    output.append((value - dynamicOffsets[activeWindow] + 0x80).toByte())
                                } else {
                                    val newWindow = getAstralWindow(value)
                                    activeWindow = windowAges.removeLast()
                                    windowAges.addFirst(activeWindow)
                                    dynamicOffsets[activeWindow] = getAstralOffset(newWindow)
                                    output.append(0x0B.toByte()) // SDX
                                    output.append(((activeWindow shl 5) or (newWindow shr 8)).toByte())
                                    output.append((newWindow and 0xFF).toByte())
                                    output.append((value - dynamicOffsets[activeWindow] + 0x80).toByte())
                                }
                            }
                            else -> {
                                windowAges.remove(windowIndex)
                                windowAges.addFirst(windowIndex)
                                when (inWindows(value2, dynamicOffsets)) {
                                    activeWindow -> {
                                        output.append((0x01 + windowIndex).toByte()) // SQn
                                        output.append((value - dynamicOffsets[windowIndex] + 0x80).toByte())
                                    }
                                    else -> {
                                        activeWindow = windowIndex
                                        output.append((0x10 + windowIndex).toByte()) // SCn
                                        output.append((value - dynamicOffsets[windowIndex] + 0x80).toByte())
                                    }
                                }
                            }
                        }
                    }
                    else -> {
                        if (value <= 0xFFFF && isCompressible(value2)) {
                            output.append(0x0E.toByte()) // SQU
                            output.append((value shr 8).toByte())
                            output.append((value and 0xFF).toByte())
                        } else {
                            output.append(0x0F.toByte()) // SCU
                            doubleByteMode = true
                            output.append((value shr 8).toByte())
                            output.append((value and 0xFF).toByte())
                        }
                    }
                }
            }
            true -> {
                if (isCompressible(value) && isCompressible(value2)) {
                    val window = inWindows(value, dynamicOffsets)
                    if (value in 0x00..0x7F) {
                        output.append((0xE0 + activeWindow).toByte()) // UCn
                        doubleByteMode = false
                        output.append(value.toByte())
                    } else if (window != null) {
                        windowAges.remove(window)
                        windowAges.addFirst(window)
                        activeWindow = window
                        output.append((0xE0 + activeWindow).toByte()) // UCn
                        doubleByteMode = false
                        output.append((value - dynamicOffsets[activeWindow] + 0x80).toByte())
                    } else if (value <= 0xFFFF) {
                        val newWindow = getBmpWindow(value)!!
                        activeWindow = windowAges.removeLast()
                        windowAges.addFirst(activeWindow)
                        dynamicOffsets[activeWindow] = getBmpOffset(newWindow)!!
                        output.append((0xE8 + activeWindow).toByte()) // UDn
                        doubleByteMode = false
                        output.append(newWindow.toByte())
                        output.append((value - dynamicOffsets[activeWindow] + 0x80).toByte())
                    } else {
                        val newWindow = getAstralWindow(value)
                        activeWindow = windowAges.removeLast()
                        windowAges.addFirst(activeWindow)
                        dynamicOffsets[activeWindow] = getAstralOffset(newWindow)
                        output.append(0xF1.toByte()) // UDX
                        doubleByteMode = false
                        output.append(((activeWindow shl 5) or (newWindow shr 8)).toByte())
                        output.append((newWindow and 0xFF).toByte())
                        output.append((value - dynamicOffsets[activeWindow] + 0x80).toByte())
                    }
                } else {
                    when (val highByte = value shr 8) {
                        in 0x00..0xDF, in 0xF3..0xFF -> {
                            output.append(highByte.toByte())
                            output.append((value and 0xFF).toByte())
                        }
                        in 0x100..0x10FF -> {
                            val utf16 = value.asCodePoint()
                            for (word in utf16) {
                                output.append((word.toInt() shr 8).toByte())
                                output.append((word.toInt() and 0xFF).toByte())
                            }
                        }
                        else -> {
                            output.append(0xF0.toByte()) // UQU
                            output.append(highByte.toByte())
                            output.append((value and 0xFF).toByte())
                        }
                    }
                }
            }
        }
        pointer += 1
    }
    return output.toByteArray()
}

@OptIn(ExperimentalStdlibApi::class)
fun main() {
    val input = readLine() ?: ""
    println(input)
    println(encodeScsu(input).size)
    println(input.encodeToByteArray().size)
    println(encodeScsu(input).toList())
    println(decodeScsu(encodeScsu(input)))
}