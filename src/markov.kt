import kotlin.random.Random
import kotlin.math.max
import kotlin.math.min

/**
 * Demonstration of generating nonsense using Markov chains.
 */
class Markov (training:String, size:Int = 3) {
    private val data = mutableMapOf("foo" to mutableMapOf("foo" to 42))
    init {
        data.clear()
        for (i in 0..training.lastIndex) {
            for (len in min(size, i) downTo 1) {
                train(i, len, training)
            }
        }
    }
    private fun train (i:Int, len:Int, training:String) {
        val sample = training.substring((i - len) until i)
        val outcome = training.substring(i..i)
        val default = mutableMapOf(outcome to 0)
        data[sample] = data.getOrDefault(sample, default)
        val ds = data[sample]!!
        ds[outcome] = ds.getOrDefault(outcome, 0) + 1
    }

    /**
     * Return a string of nonsense based on the training data
     */
    fun splurt () :String {
        val starters = data.keys.toList()
        var outcome = starters[Random.Default.nextInt(starters.size)]
        for (i in 0..Random.Default.nextInt(2000)) {
            var lenBack = 1
            var sample:String
            do {
                sample = outcome.substring(max(0, outcome.lastIndex - lenBack)..outcome.lastIndex)
                lenBack += 1
            } while (sample in data && lenBack < outcome.length)
            sample = sample.substring(1..sample.lastIndex)
            val subData = data[sample] ?: return "$outcome||leaf||"
            val total = subData.values.sum()
            val key = (Random.Default.nextDouble() * total).toInt()
            var runTotal = 0
            for (j in subData.keys) {
                if (key in runTotal until (runTotal + subData[j]!!)) {
                    outcome += j
                    break
                }
                runTotal += subData[j]!!
            }
        }
        return outcome
    }
}

fun main () {
    val mark = Markov(readLine() ?: "", 7)
    println(mark.splurt())
}