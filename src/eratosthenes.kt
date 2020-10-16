/**
 * Used by eratosthenes() and primeFactors() to carry out a
 * Sieve of Eratosthenes operation.
 */
fun eratosthenesInternal(max:Int):List<Pair<Int, Boolean>> {
    val range = 0..max
    val list = (range zip List(max + 1) { true }).toMutableList()
    list[0] = Pair(0, false)
    list[1] = Pair(1, false)
    for (i in 2..list.lastIndex) {
        if (list[i].second) {
            for (multiplier in 2..(max / i)) {
                list[i * multiplier] = Pair(list[i * multiplier].first, false)
            }
        }
    }
    return list.toList()
}

/**
 * Print a list of prime numbers up to a given maximum.
 */
fun eratosthenes(max:Int) {
    val list = eratosthenesInternal(max)
    for (i in list) {
        if (i.second) {
            val j = i.first
            println("$j")
        }
    }
}

/**
 * Print the list of prime factors of the given number.
 */
fun primeFactors(of:Int) {
    val list = eratosthenesInternal(of)
    var temp = of
    val out = MutableList(0){0}
    for (i in list) {
        if (i.second) {
            while ((temp % i.first) == 0) {
                out.add(out.size, i.first)
                temp /= i.first
            }
        }
    }
    println("$out")
}

fun main() {
    eratosthenes(100)
    println("")
    primeFactors(100)
    primeFactors(360)
}