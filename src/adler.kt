/**
 * Calculate an adler32 checksum.
 */
@OptIn(ExperimentalUnsignedTypes::class)
fun adler32 (data:ByteArray):UInt {
    var a = 1.toUShort()
    var b = data.size.rem(65521).toUShort()
    var n = data.size
    for (i in data) {
        val ib = (i.toInt() + 256).rem(256)
        val j = (ib.toUInt() * n.toUInt()).toInt()
        a = (a.toInt() + ib).rem(65521).toUShort()
        b = (b.toInt() + j).rem(65521).toUShort()
        n -= 1
    }
    return (b.toUInt() shl 16) or a.toUInt()
}

@OptIn(ExperimentalStdlibApi::class)
fun main () {
    println(adler32((readLine() ?: "").encodeToByteArray()))
}