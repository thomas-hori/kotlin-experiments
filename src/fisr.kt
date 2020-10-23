import kotlin.math.pow
import kotlin.math.sqrt // For comparison

/**
 * Fast inverse square root, as an application of toBits/fromBits for reinterpret casting.
 */
fun fastInverseSquareRoot (value:Double):Double {
    val doubleConst = 0x5FE6EB50C7B537A9
    val firstApprox = Double.fromBits(doubleConst - (value.toBits() shr 1))
    return firstApprox * (1.5 - (value * 0.5 * firstApprox.pow(2)))
}

fun main () {
    println("%f".format(fastInverseSquareRoot(999.0)))
    println("%f".format(1 / sqrt(999.0)))
}