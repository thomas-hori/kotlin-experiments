val mapping = mapOf(
        'a' to 'u',
        'b' to 'v',
        'c' to 's',
        'd' to 'j',
        'e' to 'o',
        'f' to 'h',
        'g' to 't',
        'h' to 'f',
        'i' to 'y',
        'j' to 'd',
        'k' to 'p',
        'l' to 'r',
        'm' to 'n',
        'n' to 'm',
        'o' to 'e',
        'p' to 'k',
        'q' to 'w',
        'r' to 'l',
        's' to 'c',
        't' to 'g',
        'u' to 'a',
        'v' to 'b',
        'w' to 'q',
        'x' to 'z',
        'y' to 'i',
        'z' to 'x'
)

fun encodeUryuomoco(input:String):String {
    /**
     * Encode to Uryuomoco, a fantasy-language-simulating cipher created by Dan Shive.
     *
     * Decoder to verify output against: https://harjit.moe/uryuomoco.html
     */
    // Stage 0: address assumptions which will be made throughout the remainder of the routine
    var temp = input.toLowerCase().replace("'", "-'")
    // Stage 1: special behaviour of Q before mapping
    temp = temp.replace("qu", "q")
    // Stage 2: mapping
    temp = temp.map{mapping[it] ?: it}.joinToString("")
    // Stage 3: special behaviour of Q, Y and C after mapping (i.e. special mapping for W, I and S)
    temp = temp.replace("q", "qu")
    temp = temp.replace("y", "yu")
    temp = temp.replace("is", "i's")
    temp = temp.replace(Regex("cc(?!f)"), "ais")
    temp = temp.replace(Regex("([bdfghjklmnpqrstvwxyz])c(?!f)"), "$1is")
    // Stage 4: special mapping for th (ch not gf), sh (us not cf), ch (se not sf), wh (quo not quf)
    temp = temp.replace("ch", "c'h")
    temp = temp.replace("gf", "ch")
    temp = temp.replace("us", "u's")
    temp = temp.replace("cf", "us")
    temp = temp.replace("se", "s'e")
    temp = temp.replace("sf", "se")
    temp = temp.replace("quo", "qu'o")
    temp = temp.replace("quf", "quo")
    // Stage 5: using tul, not tl
    temp = temp.replace("tul", "t'ul")
    temp = temp.replace("tl", "tul")
    // Stage 6: special behaviour at ends of words
    temp = temp.replace(Regex("\\Beh\\b(?!')"), "e'h")
    temp = temp.replace(Regex("\\Be\\b(?!')"), "eh")
    temp = temp.replace(Regex("ja\\b(?!')"), "j'a")
    temp = temp.replace(Regex("j\\b(?!')"), "ja")
    temp = temp.replace(Regex("ot\\b(?!')"), "o't")
    temp = temp.replace(Regex("ymt\\b(?!')"), "ot")
    temp = temp.replace(Regex("ra\\b(?!')"), "r'a")
    temp = temp.replace(Regex("rr\\b(?!')"), "ra")
    temp = temp.replace(Regex("([aiueo])yu\\b(?!')"), "$1y'u")
    temp = temp.replace(Regex("([aiueo])i\\b(?!')"), "$1yu")
    return temp
}

fun main() {
    while (true) {
        val typed = readLine() ?: ""
        println(encodeUryuomoco(typed))
    }
}