/**
 * Print the Fibonacci numbers up to a given maximum.
 */
fun fibonacci(max:Int) {
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
}

fun Int.fibo() {
    fibonacci(this)
}

fun main() {
    10000.fibo()
}