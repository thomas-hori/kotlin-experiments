/**
 * Given a BF string and an offset immediately following an opening
 * bracket, return the offset immediately following the corresponding
 * closing bracket
 */
fun seekPastNextBracket (bf:String, initOffset:Int):Int {
    var curOffset = initOffset
    var balance = 1
    while (curOffset < bf.length) {
        when (bf[curOffset]) {
            '[' -> balance += 1
            ']' -> balance -= 1
        }
        if (balance == 0) {
            return curOffset + 1
        }
        curOffset += 1
    }
    return bf.length
}

/**
 * Given a BF string and an offset immediately preceding a closing
 * bracket, return the offset immediately following the corresponding
 * opening bracket
 */
fun seekAgainstLastBracket (bf:String, initOffset:Int):Int {
    var curOffset = initOffset
    var balance = -1
    while (curOffset > 0) {
        when (bf[curOffset]) {
            '[' -> balance += 1
            ']' -> balance -= 1
        }
        if (balance == 0) {
            return curOffset + 1
        }
        curOffset -= 1
    }
    return 0
}

/**
 * Execute a BF program. Memory and the initial pointer within that memory can
 * be supplied to override the defaults.
 */
fun executeBF (bf:String, memory:ByteArray? = null, initialPointer:Int = 0) {
    val memoryReal = memory ?: ByteArray(60000){0.toByte()}
    var memoryPointer = initialPointer
    var codePointer = 0
    while (codePointer < bf.length) {
        codePointer += 1
        when (bf[codePointer - 1]) {
            '>' -> memoryPointer += 1
            '<' -> memoryPointer -= 1
            '+' -> memoryReal[memoryPointer] = (memoryReal[memoryPointer] + 1).toByte()
            '-' -> memoryReal[memoryPointer] = (memoryReal[memoryPointer] - 1).toByte()
            '.' -> print(String(byteArrayOf(memoryReal[memoryPointer])))
            ',' -> memoryReal[memoryPointer] = System.`in`.read().toByte()
            '[' -> if (memoryReal[memoryPointer].toInt() == 0) {
                codePointer = seekPastNextBracket(bf, codePointer)
            }
            ']' -> if (memoryReal[memoryPointer].toInt() != 0) {
                codePointer = seekAgainstLastBracket(bf, codePointer - 2)
            }
        }
    }
}

fun main () {
    executeBF(readLine() ?: "")
}