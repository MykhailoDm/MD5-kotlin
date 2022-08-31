import java.io.File
import kotlin.math.abs
import kotlin.math.sin

fun main() {

    val md5 = MD5MessageDigest();

    val testStrings = arrayOf(
        "",
        "a",
        "abc",
        "message digest",
        "abcdefghijklmnopqrstuvwxyz",
        "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789",
        "12345678901234567890123456789012345678901234567890123456789012345678901234567890"
    )

    for (s in testStrings) {
        println(getHashValueOutput(md5, s))
    }

    File("testResult.txt").printWriter().use { out ->
        testStrings.forEach { s ->
            out.println(getHashValueOutput(md5, s))
        }
    }


    println("Do you want to hash string? (alternative is to input the name of the file later)")
    val hashString = readLine()?.toBoolean()

    var inputString: String? = null
    var inputFilename: String? = null

    hashString?.let { hs ->
        if (hs) {
            println("Please input your string")
            inputString = readLine()
        } else {
            println("Please input your file name")
            inputFilename = readLine()
        }
    }

    inputString?.let {
        val hashPrettyString = getHashValueOutput(md5, it)
        println("Hash: $hashPrettyString")
        File("resultString.txt").printWriter().use { out ->
            out.println(hashPrettyString)
        }
    }

    inputFilename?.let {
        val hash = md5.compute(File(it).readBytes())
        val hashPrettyString = "0x${hash.toHexString()}"
        println("Hash File: $hashPrettyString")
        File("resultFile.txt").printWriter().use { out ->
            out.println(hashPrettyString)
        }

        println("Check: ${md5.isValidChecksum(File(it).readBytes(), hash)}")
    }
}

class MD5MessageDigest(
    private val initialA: Int = 0x67452301, private val initialB: Int = 0xEFCDAB89L.toInt(),
    private val initialC: Int = 0x98BADCFEL.toInt(), private val initialD: Int = 0x10325476,
    private val shift: IntArray = intArrayOf(
        7, 12, 17, 22,
        5, 9, 14, 20,
        4, 11, 16, 23,
        6, 10, 15, 21
    ), private val tableT: IntArray = IntArray(64) {
        ((1L shl 32) * abs(sin(it + 1.0))).toLong().toInt()
    }
) {


    fun compute(message: ByteArray): ByteArray {
        val messageLenBytes = message.size
        val numBlocks = ((messageLenBytes + 8) ushr 6) + 1
        val totalLen = numBlocks shl 6
        val paddingBytes = ByteArray(totalLen - messageLenBytes)
        paddingBytes[0] = 0x80.toByte()
        var messageLenBits = (messageLenBytes shl 3).toLong()
        for (i in 0..7) {
            paddingBytes[paddingBytes.size - 8 + i] = messageLenBits.toByte()
            messageLenBits = messageLenBits ushr 8
        }
        var a = initialA
        var b = initialB
        var c = initialC
        var d = initialD
        val buffer = IntArray(16)
        for (i in 0 until numBlocks) {
            var index = i shl 6
            for (j in 0..63) {
                val temp = if (index < messageLenBytes) message[index] else
                    paddingBytes[index - messageLenBytes]
                buffer[j ushr 2] = (temp.toInt() shl 24) or (buffer[j ushr 2] ushr 8)
                index++
            }
            val originalA = a
            val originalB = b
            val originalC = c
            val originalD = d
            for (j in 0..63) {
                val div16 = j ushr 4
                var f = 0
                var bufferIndex = j
                when (div16) {
                    0 -> {
                        f = (b and c) or (b.inv() and d)
                    }
                    1 -> {
                        f = (b and d) or (c and d.inv())
                        bufferIndex = (bufferIndex * 5 + 1) and 0x0F
                    }

                    2 -> {
                        f = b xor c xor d
                        bufferIndex = (bufferIndex * 3 + 5) and 0x0F
                    }
                    3 -> {
                        f = c xor (b or d.inv())
                        bufferIndex = (bufferIndex * 7) and 0x0F
                    }
                }
                val temp = b + Integer.rotateLeft(
                    a + f + buffer[bufferIndex] +
                            tableT[j], shift[(div16 shl 2) or (j and 3)]
                )
                a = d
                d = c
                c = b
                b = temp
            }
            a += originalA
            b += originalB
            c += originalC
            d += originalD
        }
        val md5 = ByteArray(16)
        var count = 0
        for (i in 0..3) {
            var n = if (i == 0) a else (if (i == 1) b else (if (i == 2) c else d))
            for (j in 0..3) {
                md5[count++] = n.toByte()
                n = n ushr 8
            }
        }
        return md5
    }

    fun isValidChecksum(fileByteArray: ByteArray, md5Hash: ByteArray): Boolean {
        return md5Hash.contentEquals(compute(fileByteArray))
    }

}

fun getHashValueOutput(md5: MD5MessageDigest, value: String): String {
    return "0x${md5.compute(value.toByteArray()).toHexString()} <== \"$value\""
}

fun ByteArray.toHexString(): String {

    val sb = StringBuilder()
    for (b in this) sb.append(String.format("%02x", b.toInt() and 0xFF).uppercase())
    return sb.toString()
}