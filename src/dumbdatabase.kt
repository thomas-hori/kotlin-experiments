import java.io.RandomAccessFile

/**
 * A very basic but working string-to-string dictionary stored on disk. Uses a header hashtable of
 * 256 32-bit offsets, with the hash itself being the low byte of hashCode. Entries sharing a hash
 * are single-linked lists. When keys are deleted or altered, they are unlinked from the requisite
 * linked list and/or hashtable only: all old keys and values remain in the file forever.
 *
 * This does indeed mean that the database will always expand with use until it hits 2 binary gig,
 * at which point things will break. Were this intended for production, it would have been meet to
 * do something about that.
 */
class DumbDatabase(fileName:String):MutableMap<String, String> {
    private var fileObj:RandomAccessFile = RandomAccessFile(fileName, "rwd")
    init {
        if (fileObj.length() < 1024L) {
            this.clear()
        }
    }

    override val size: Int
        get() = this.keys.size

    override fun containsKey(key:String): Boolean {
        return this[key] != null
    }

    override fun containsValue(value:String): Boolean {
        for (i in this.keys) {
            if (this[i] == value) {
                return true
            }
        }
        return false
    }

    @OptIn(ExperimentalStdlibApi::class)
    override fun get(key:String):String? {
        val hash = key.hashCode() and 0xFF
        fileObj.seek((hash * 4).toLong())
        val firstItemOffset = fileObj.readInt().toLong()
        fileObj.seek(firstItemOffset)
        while (true) {
            val valueOffset = fileObj.readInt().toLong()
            val nextOffset = fileObj.readInt().toLong()
            val size = fileObj.readInt()
            val nameRead = ByteArray(size)
            fileObj.readFully(nameRead)
            if (nameRead.decodeToString() == key) {
                fileObj.seek(valueOffset)
                val valueSize = fileObj.readInt()
                val valueRead = ByteArray(valueSize)
                fileObj.readFully(valueRead)
                return valueRead.decodeToString()
            } else {
                if (nextOffset != 0L) {
                    fileObj.seek(nextOffset)
                } else {
                    return null
                }
            }
        }
    }

    override fun isEmpty(): Boolean {
        fileObj.seek(0)
        for (i in 1..256) {
            if (fileObj.readInt() != 0) {
                return false
            }
        }
        return true
    }

    override val entries: MutableSet<MutableMap.MutableEntry<String, String>>
        get() {
            val retVal = mutableMapOf("foo" to "foo")
            retVal.clear()
            for (i in this.keys) {
                retVal[i] = this[i]!!
            }
            return retVal.entries
        }

    @OptIn(ExperimentalStdlibApi::class)
    override val keys: MutableSet<String>
        get() {
            val retVal = mutableSetOf("")
            retVal.clear()
            for (currentHash in 0..255) {
                fileObj.seek((currentHash * 4).toLong())
                var nextOffset = fileObj.readInt().toLong()
                while (nextOffset != 0L) {
                    fileObj.seek(nextOffset)
                    fileObj.readInt().toLong() // Don't need the value offset here
                    nextOffset = fileObj.readInt().toLong()
                    val size = fileObj.readInt()
                    val nameRead = ByteArray(size)
                    fileObj.readFully(nameRead)
                    retVal.add(nameRead.decodeToString())
                }
            }
            return retVal
        }
    override val values: MutableCollection<String>
        get() {
            val retVal = mutableSetOf("")
            retVal.clear()
            for (i in this.keys) {
                retVal.add(this[i]!!)
            }
            return retVal
        }

    override fun clear() {
        // Just zero out the hashtable (this also gets used when initialising a new DB file)
        fileObj.seek(0)
        fileObj.write(ByteArray(1024){0.toByte()})
    }

    @OptIn(ExperimentalStdlibApi::class)
    override fun put(key:String, value:String):String? {
        val hash = key.hashCode() and 0xFF
        fileObj.seek((hash * 4).toLong())
        var nextOffsetOffset = fileObj.filePointer
        var nextOffset:Long
        while (true) {
            fileObj.seek(nextOffsetOffset)
            nextOffset = fileObj.readInt().toLong()
            if (nextOffset != 0L) {
                fileObj.seek(nextOffset)
                val valueOffsetOffset = fileObj.filePointer
                val valueOffset = fileObj.readInt().toLong()
                nextOffsetOffset = fileObj.filePointer
                fileObj.readInt().toLong() // We'll read nextOffset when we seek back here next iteration
                val size = fileObj.readInt()
                val nameRead = ByteArray(size)
                fileObj.readFully(nameRead)
                if (nameRead.decodeToString() == key) {
                    // Get the old value for returning
                    fileObj.seek(valueOffset)
                    val oldValueSize = fileObj.readInt()
                    val oldValue = ByteArray(oldValueSize)
                    fileObj.readFully(oldValue)
                    // Replace it with the new value, assuming they're actually different
                    if (oldValue.decodeToString() != value) {
                        fileObj.seek(valueOffsetOffset)
                        fileObj.writeInt(fileObj.length().toInt())
                        fileObj.seek(fileObj.length())
                        val asBytes = value.encodeToByteArray()
                        fileObj.writeInt(asBytes.size)
                        fileObj.write(asBytes)
                    }
                    return oldValue.decodeToString()
                }
            } else {
                fileObj.seek(nextOffsetOffset)
                fileObj.writeInt(fileObj.length().toInt())
                fileObj.seek(fileObj.length())
                val keyBytes = key.encodeToByteArray()
                fileObj.writeInt(fileObj.filePointer.toInt() + 12 + keyBytes.size) // Value offset
                fileObj.writeInt(0) // Next key offset (zero since it's the last in the SLL)
                fileObj.writeInt(keyBytes.size)
                fileObj.write(keyBytes)
                val valBytes = value.encodeToByteArray()
                fileObj.writeInt(valBytes.size)
                fileObj.write(valBytes)
                return null
            }
        }
    }

    override fun putAll(from: Map<out String, String>) {
        for (i in from) {
            this[i.key] = i.value
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    override fun remove(key: String): String? {
        val hash = key.hashCode() and 0xFF
        fileObj.seek((hash * 4).toLong())
        var toOverstamp = fileObj.filePointer
        val firstItemOffset = fileObj.readInt().toLong()
        fileObj.seek(firstItemOffset)
        while (true) {
            val valueOffset = fileObj.readInt().toLong()
            val overstepCandidate = fileObj.filePointer
            val nextOffset = fileObj.readInt().toLong()
            val size = fileObj.readInt()
            val nameRead = ByteArray(size)
            fileObj.readFully(nameRead)
            if (nameRead.decodeToString() == key) {
                fileObj.seek(toOverstamp)
                fileObj.writeInt(nextOffset.toInt()) // Correct whether or not nextOffset is zero
                fileObj.seek(valueOffset)
                val valueSize = fileObj.readInt()
                val valueRead = ByteArray(valueSize)
                fileObj.readFully(valueRead)
                return valueRead.decodeToString()
            } else {
                if (nextOffset != 0L) {
                    toOverstamp = overstepCandidate
                    fileObj.seek(nextOffset)
                } else {
                    return null
                }
            }
        }
    }
}

fun main() {
    val db = DumbDatabase("aubergine.db")
    db.clear()
    db["hello"] = "world"
    db["gladly"] = "cross-eyed bear"
    db["bespoke"] = "furniture"
    db["bespoke"] = "kitchens and bathrooms"
    println(db["hello"])
    println(db["bespoke"])
    println(db["gladly"])
    // Stress test keys with the same hash
    // Strings which JVM hash to zero: "creashaks organzine", "pollinating sandboxes", "ddnqavbj", "166lr735ka3q6"
    //     -- Source: https://minecraft.gamepedia.com/Seed_(level_generation)#Seed_0 (yes, really). Note that it
    //        also lists others which don't seem to actually produce a zero hash.
    db["creashaks organzine"] = "Crœsus"
    db["pollinating sandboxes"] = "Cody Slab"
    db["166lr735ka3q6"] = "Big Taiga"
    println(db.entries)
    println(db["creashaks organzine"])
    println(db["166lr735ka3q6"])
    db.remove("pollinating sandboxes")
    println(db["creashaks organzine"])
    println(db["166lr735ka3q6"])
    db.remove("creashaks organzine")
    println(db["166lr735ka3q6"])
    println(db.entries)
}