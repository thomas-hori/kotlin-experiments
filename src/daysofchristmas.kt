import kotlin.math.absoluteValue

val gifts = arrayOf(null,
        // https://archive.org/stream/monthlychronicl02unkngoog#page/n55/mode/1up
        // Other sources may vary, especially in the last four (9..12).
        "partridge on a pear tree",
        "turtle doves",
        "French hens",
        "colly birds",
        "gold rings",
        "geese a-laying",
        "swans a-swimming",
        "maids a-milking",
        "drummers drumming",
        "pipers playing",
        "ladies dancing",
        "lords a-leaping",
        // Additional attested gifts in no particular order for when a number > 12 is passed
        // https://books.google.nl/books?id=efIHAAAAQAAJ&pg=PA145
        "hares a-running",
        "badgers baiting",
        "bells a-ringing"
)

/**
 * Get the appropriate ordinal suffix for a given number
 */
fun ordinal (number:Int):String {
    return if (number.absoluteValue in 10..19) {
        "th"
    } else {
        when (number.absoluteValue % 10) {
            1 -> "st"
            2 -> "nd"
            3 -> "rd"
            else -> "th"
        }
    }
}

/**
 * Print the Twelve Days of Christmas and return the total number of gifts given.
 */
fun daysOfChristmas(days:Int = 12):Int {
    var total = 0
    for (day in 1..days) {
        val ordinal = ordinal(day)
        println("On the ${day}${ordinal} day of Christmas, my true love sent to me")
        for (gift in day downTo 1) {
            total += gift
            val giftType = gifts.getOrElse(gift){"thneeds a-thneeding"}!!
            if (gift != 1) {
                println("$gift ${giftType},")
            } else {
                println("and a ${giftType}.")
            }
        }
        println("")
    }
    return total
}

fun main() {
    val days = 12
    val total = daysOfChristmas(days)
    println("A total of $total gifts were delivered over $days days of Christmas")
}