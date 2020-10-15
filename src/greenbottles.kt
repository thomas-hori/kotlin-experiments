infix fun Int.bottlesDownTo(end:Int) {
    /**
     * Prints "Ten Green Bottles" between a specified initial and final number of suspended bottles.
     */
    for (i in this downTo end) {
        val j = i - 1
        println("$i green bottles hanging on the wall,")
        println("$i green bottles hanging on the wall")
        println("And if one green bottle should accidentally fall")
        when {
            (i > end) && (j != 1) -> println("There'll be $j green bottles hanging on the wall\n")
            (i > end) && (j == 1) -> println("There'll be one green bottle hanging on the wall\n")
            else -> println("There'll be $this green bottles at the bottom of the wall")
        }
    }
}

fun main() {
    10 bottlesDownTo 1
}

