fun getBmpOffset(window:Int):Int? {
    return when (window) {
        in 0x01..0x67 -> window * 0x80
        in 0x68..0xA7 -> (window * 0x80) + 0xAC00
        0xF9 -> 0xC0
        0xFA -> 0x250
        0xFB -> 0x370
        0xFC -> 0x530
        0xFD -> 0x3040
        0xFE -> 0x30A0
        0xFF -> 0xFF60
        else -> null
    }
}
fun getAstralOffset(window:Int):Int? {
    return (window * 0x80) + 0x10000
}
fun grokByte(input:Byte):Int {
    return (input.toInt() + 256).rem(256)
}

fun<T> MutableList<T>.append(thing:T) {
    this.add(this.size, thing)
}

/**
 * Decode some SCSU (Standard Compression Scheme for Unicode) text to UTF-16.
 */
fun decodeScsu(input:ByteArray):String {
    val staticOffsets = listOf(0x00, 0x80, 0x100, 0x300, 0x2000, 0x2080, 0x2100, 0x3000)
    val dynamicOffsets:MutableList<Int?> = mutableListOf(0x80, 0xC0, 0x0400, 0x0600, 0x0900, 0x3040, 0x30A0, 0xFF00)
    var doubleByteMode = false
    var pointer = 0
    var activeWindow = 0
    val output = mutableListOf("")
    while (pointer < input.size) {
        when (doubleByteMode) {
            false -> {
                when (val value = grokByte(input[pointer])) {
                    0x0, 0x9, 0xA, 0xD, in 0x20..0x7F -> { // Verbatim ASCII subset
                        output.append(value.toChar().toString())
                    }
                    in 0x80..0xFF -> { // Single-byte refs to active dynamic window
                        val offset = dynamicOffsets[activeWindow]
                        if (offset == null) {
                            output.append("\uFFFD")
                        } else {
                            output.append((value - 0x80 + offset).toChar().toString())
                        }
                    }
                    in 0x01..0x09 -> { // SQn
                        pointer += 1
                        when (val value2 = grokByte(input[pointer])) {
                            in 0x00..0x7F -> {
                                val offset = staticOffsets[value - 1]
                                output.append((value2 + offset).toChar().toString())
                            }
                            in 0x80..0xFF -> {
                                val offset = dynamicOffsets[value - 1]!!
                                output.append((value2 - 0x80 + offset).toChar().toString())
                            }
                        }
                    }
                    0x0B -> { // SDX
                        pointer += 1
                        val value2 = grokByte(input[pointer])
                        pointer += 1
                        val value3 = grokByte(input[pointer])
                        activeWindow = value3 shr 5
                        val offset = getAstralOffset(((value2 and 0x1F) shl 8) or value3)
                        dynamicOffsets[activeWindow] = offset
                    }
                    0x0C -> output.append("\uFFFD") // (reserved)
                    0x0E -> { // SQU
                        pointer += 1
                        val value2 = grokByte(input[pointer])
                        pointer += 1
                        val value3 = grokByte(input[pointer])
                        output.append(((value2 shl 8) or value3).toChar().toString())
                    }
                    0x0F -> doubleByteMode = true // SCU
                    in 0x10..0x17 -> activeWindow = value - 0x10 // SCn
                    in 0x18..0x1F -> { // SDn
                        pointer += 1
                        val value2 = grokByte(input[pointer])
                        activeWindow = value - 0x18
                        val offset = getBmpOffset(value2)
                        dynamicOffsets[activeWindow] = offset
                    }
                }
            }
            true -> {
                when (val value = grokByte(input[pointer])) {
                    in 0x00..0xDF, in 0xF3..0xFF -> { // Verbatim UTF-16BE subset
                        pointer += 1
                        val value2 = grokByte(input[pointer])
                        output.append(((value shl 8) or value2).toChar().toString())
                    }
                    in 0xE0..0xE7 -> { // UCn
                        activeWindow = value - 0xE0
                        doubleByteMode = false
                    }
                    in 0xE8..0xEF -> { // UDn
                        pointer += 1
                        val value2 = grokByte(input[pointer])
                        activeWindow = value - 0xE8
                        doubleByteMode = false
                        val offset = getBmpOffset(value2)
                        dynamicOffsets[activeWindow] = offset
                    }
                    0xF0 -> { // UQU
                        pointer += 1
                        val value2 = grokByte(input[pointer])
                        pointer += 1
                        val value3 = grokByte(input[pointer])
                        output.append(((value2 shl 8) or value3).toChar().toString())
                    }
                    0xF1 -> { // UDX
                        pointer += 1
                        val value2 = grokByte(input[pointer])
                        pointer += 1
                        val value3 = grokByte(input[pointer])
                        activeWindow = value3 shr 5
                        val offset = getAstralOffset(((value2 and 0x1F) shl 8) or value3)
                        dynamicOffsets[activeWindow] = offset
                        doubleByteMode = false
                    }
                    0xF2 -> output.append("\uFFFD") // (reserved)
                }
            }
        }
        pointer += 1
    }
    return output.joinToString("")
}

fun main () {
    val input = (readLine() ?: "").split(", ", ",").map{it.toLong().toByte()}.toByteArray()
    println(input.toList())
    println(decodeScsu(input))
    /*println(encodeScsu(decodeScsu(input)).toList())
    println(decodeScsu(encodeScsu(decodeScsu(input))))*/
}