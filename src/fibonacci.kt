/**
 * Print the Fibonacci numbers up to a given maximum.
 */
fun fibonacci(max:Int):Double {
    var i:Int
    var j = 1
    var k = 1
    while (true) {
        println("$j")
        i = j ; j = k ; k = (i + j)
        if (k > max) {
            break
        }
    }
    return j.toDouble() / k.toDouble()
}

fun Int.fibo():Double {
    return fibonacci(this)
}

fun main() {
    val phi = 10000000.fibo()
    val invphi = 1 / phi
    println("$phi, $invphi")
}