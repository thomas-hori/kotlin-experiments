import kotlin.math.min

fun<T:Comparable<T>> mergeSortInPlace(sequence:MutableList<T>) {
    /**
     * Carry out an in-place merge sort on the provided mutable list.
     */
    var span = 1
    while (span < sequence.size) {
        for (firstOffset in 0 .. sequence.size step (span * 2)) {
            val secondOffset = firstOffset + span
            if (secondOffset > sequence.lastIndex) { break }
            var pointer = firstOffset
            while (pointer < secondOffset) {
                if (sequence[pointer] > sequence[secondOffset]) {
                    var destinationPointer = secondOffset
                    val register = sequence[pointer]
                    sequence[pointer] = sequence[destinationPointer]
                    while ((destinationPointer < min(sequence.lastIndex, secondOffset + span - 1)) &&
                            (sequence[destinationPointer + 1] < register)) {
                        sequence[destinationPointer] = sequence[destinationPointer + 1]
                        destinationPointer += 1
                    }
                    sequence[destinationPointer] = register
                } else {
                    pointer += 1
                }
            }
        }
        span *= 2
    }
}

fun main() {
    val numbers = (readLine() ?: "").split(", ", ",").map{it.toBigInteger(10)}.toMutableList()
    mergeSortInPlace(numbers)
    println("$numbers")
}