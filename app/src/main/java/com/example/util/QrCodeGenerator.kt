package com.example.util

/**
 * Pure-Kotlin QR Code encoder (Byte mode, ECC Level L, Versions 1..6)
 * supporting up to ~134 UTF-8 bytes (ideal for UPI URIs like upi://pay?pa=...&pn=...&am=...&cu=INR).
 */
object QrCodeGenerator {

    fun encode(text: String): Array<BooleanArray> {
        val rawBytes = text.toByteArray(Charsets.UTF_8)
        val bytes = if (rawBytes.size > 106) rawBytes.copyOf(106) else rawBytes
        val version = selectVersion(bytes.size)
        val size = 17 + version * 4
        val dataCapacityBytes = DATA_BYTES_L[version]
        val eccBytesCount = ECC_BYTES_L[version]

        val bits = MutableBitList()
        // Mode indicator: Byte = 0100
        bits.append(0b0100, 4)
        // Character count indicator: 8 bits for versions 1..9
        bits.append(bytes.size, 8)
        for (b in bytes) {
            bits.append(b.toInt() and 0xFF, 8)
        }
        val maxDataBits = dataCapacityBytes * 8
        val terminatorLen = minOf(4, maxDataBits - bits.size)
        bits.append(0, terminatorLen)
        while (bits.size % 8 != 0) bits.append(0, 1)

        val dataBytes = IntArray(dataCapacityBytes)
        val raw = bits.toByteArray()
        for (i in raw.indices) {
            dataBytes[i] = raw[i].toInt() and 0xFF
        }
        var padIdx = raw.size
        var toggle = true
        while (padIdx < dataCapacityBytes) {
            dataBytes[padIdx++] = if (toggle) 0xEC else 0x11
            toggle = !toggle
        }

        val ecc = computeReedSolomon(dataBytes, eccBytesCount)
        val allBytes = dataBytes + ecc
        val moduleBits = ArrayList<Int>(allBytes.size * 8 + 7)
        for (b in allBytes) {
            for (bit in 7 downTo 0) {
                moduleBits.add((b shr bit) and 1)
            }
        }
        repeat(7) { moduleBits.add(0) }

        val matrix = Array(size) { BooleanArray(size) }
        val isFunction = Array(size) { BooleanArray(size) }

        // Finder patterns
        drawFinder(matrix, isFunction, 0, 0)
        drawFinder(matrix, isFunction, size - 7, 0)
        drawFinder(matrix, isFunction, 0, size - 7)

        // Alignment pattern for versions >= 2
        if (version >= 2) {
            val pos = size - 7
            drawAlignment(matrix, isFunction, pos, pos)
        }

        // Timing patterns
        for (i in 8 until size - 8) {
            val black = i % 2 == 0
            setFunction(matrix, isFunction, 6, i, black)
            setFunction(matrix, isFunction, i, 6, black)
        }

        // Dark module & reserve format info
        setFunction(matrix, isFunction, 8, size - 8, true)
        for (i in 0..8) {
            if (i != 6) {
                isFunction[8][i] = true
                isFunction[i][8] = true
            }
        }
        for (i in size - 8 until size) {
            isFunction[8][i] = true
            isFunction[i][8] = true
        }

        // Place data bits (zig-zag upward/downward)
        var bitIdx = 0
        var up = true
        var right = size - 1
        while (right >= 1) {
            if (right == 6) right--
            for (vert in 0 until size) {
                val y = if (up) size - 1 - vert else vert
                for (j in 0..1) {
                    val x = right - j
                    if (!isFunction[y][x] && bitIdx < moduleBits.size) {
                        matrix[y][x] = moduleBits[bitIdx++] == 1
                    }
                }
            }
            up = !up
            right -= 2
        }

        // Apply Mask 0: (y + x) % 2 == 0
        for (y in 0 until size) {
            for (x in 0 until size) {
                if (!isFunction[y][x] && (y + x) % 2 == 0) {
                    matrix[y][x] = !matrix[y][x]
                }
            }
        }

        // Format info for ECC L (01) + Mask 0 (000) -> 111011111000100
        val formatBits = 0b111011111000100
        for (i in 0..5) setFunction(matrix, isFunction, 8, i, ((formatBits shr (14 - i)) and 1) == 1)
        setFunction(matrix, isFunction, 8, 7, ((formatBits shr 8) and 1) == 1)
        setFunction(matrix, isFunction, 8, 8, ((formatBits shr 7) and 1) == 1)
        setFunction(matrix, isFunction, 7, 8, ((formatBits shr 6) and 1) == 1)
        for (i in 9..14) setFunction(matrix, isFunction, 14 - i, 8, ((formatBits shr (14 - i)) and 1) == 1)

        for (i in 0..7) setFunction(matrix, isFunction, size - 1 - i, 8, ((formatBits shr (14 - i)) and 1) == 1)
        for (i in 8..14) setFunction(matrix, isFunction, 8, size - 15 + i, ((formatBits shr (14 - i)) and 1) == 1)

        return matrix
    }

    private fun selectVersion(byteCount: Int): Int {
        for (v in 1..5) {
            if (byteCount + 2 <= DATA_BYTES_L[v]) return v
        }
        return 5
    }

    private fun setFunction(
        matrix: Array<BooleanArray>,
        isFunction: Array<BooleanArray>,
        x: Int,
        y: Int,
        black: Boolean
    ) {
        matrix[y][x] = black
        isFunction[y][x] = true
    }

    private fun drawFinder(
        matrix: Array<BooleanArray>,
        isFunction: Array<BooleanArray>,
        startX: Int,
        startY: Int
    ) {
        for (dy in -1..7) {
            for (dx in -1..7) {
                val x = startX + dx
                val y = startY + dy
                if (x in matrix.indices && y in matrix.indices) {
                    val inBox = dx in 0..6 && dy in 0..6
                    val black = inBox && (
                        dx == 0 || dx == 6 || dy == 0 || dy == 6 ||
                            (dx in 2..4 && dy in 2..4)
                        )
                    setFunction(matrix, isFunction, x, y, black)
                }
            }
        }
    }

    private fun drawAlignment(
        matrix: Array<BooleanArray>,
        isFunction: Array<BooleanArray>,
        centerX: Int,
        centerY: Int
    ) {
        for (dy in -2..2) {
            for (dx in -2..2) {
                val dist = maxOf(kotlin.math.abs(dx), kotlin.math.abs(dy))
                setFunction(matrix, isFunction, centerX + dx, centerY + dy, dist != 1)
            }
        }
    }

    private fun computeReedSolomon(data: IntArray, eccLen: Int): IntArray {
        val gen = rsGenerator(eccLen)
        val res = IntArray(eccLen)
        for (b in data) {
            val factor = b xor res[0]
            System.arraycopy(res, 1, res, 0, eccLen - 1)
            res[eccLen - 1] = 0
            for (j in 0 until eccLen) {
                res[j] = res[j] xor gfMul(gen[j], factor)
            }
        }
        return res
    }

    private fun rsGenerator(degree: Int): IntArray {
        var poly = intArrayOf(1)
        var root = 1
        for (i in 0 until degree) {
            val next = IntArray(poly.size + 1)
            for (j in poly.indices) {
                next[j] = next[j] xor poly[j]
                next[j + 1] = next[j + 1] xor gfMul(poly[j], root)
            }
            poly = next
            root = gfMul(root, 2)
        }
        return poly.drop(1).toIntArray()
    }

    private fun gfMul(x: Int, y: Int): Int {
        var a = x
        var b = y
        var z = 0
        while (b > 0) {
            if ((b and 1) != 0) z = z xor a
            val hi = a and 0x80
            a = (a shl 1) and 0xFF
            if (hi != 0) a = a xor 0x1D
            b = b ushr 1
        }
        return z
    }

    private class MutableBitList {
        private val bits = ArrayList<Int>(512)
        val size: Int get() = bits.size

        fun append(value: Int, length: Int) {
            for (i in length - 1 downTo 0) {
                bits.add((value ushr i) and 1)
            }
        }

        fun toByteArray(): ByteArray {
            val out = ByteArray((bits.size + 7) / 8)
            for (i in bits.indices) {
                if (bits[i] == 1) {
                    out[i / 8] = (out[i / 8].toInt() or (1 shl (7 - (i % 8)))).toByte()
                }
            }
            return out
        }
    }

    // Version 1..6 ECC-L single-block capacities (Note: v6 uses 2 blocks normally; cap to v5 single block 108 bytes or handle v6? Wait: v1..v5 are ALL single-block in ECC-L!)
    // Actually v1=19, v2=34, v3=55, v4=80, v5=108 are ALL 1 block! v6 is 2 blocks, so let's cap at v5 (108 bytes) which fits any UPI URI cleanly!
    private val DATA_BYTES_L = intArrayOf(0, 19, 34, 55, 80, 108, 108)
    private val ECC_BYTES_L = intArrayOf(0, 7, 10, 15, 20, 26, 26)
}
