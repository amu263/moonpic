package dev.moonpic.core.filters.lut

import java.io.BufferedReader

/**
 * Lightweight .cube parser. Supports the common subset we need for v0.1:
 *   - LUT_3D_SIZE
 *   - DOMAIN_MIN / DOMAIN_MAX
 *   - TITLE
 *   - r g b triplets
 *
 * Reference: Adobe Cube LUT spec.
 */
object CubeParser {

    data class CubeLut(
        val title: String,
        val size: Int,
        val domainMin: Float,
        val domainMax: Float,
        /** size^3 * 3 floats; index = (b * size + g) * size + r. */
        val data: FloatArray,
    ) {
        fun sample(rIn: Float, gIn: Float, bIn: Float): FloatArray {
            val n = size
            val r = ((rIn - domainMin) / (domainMax - domainMin) * (n - 1)).coerceIn(0f, (n - 1).toFloat())
            val g = ((gIn - domainMin) / (domainMax - domainMin) * (n - 1)).coerceIn(0f, (n - 1).toFloat())
            val b = ((bIn - domainMin) / (domainMax - domainMin) * (n - 1)).coerceIn(0f, (n - 1).toFloat())
            val r0 = r.toInt(); val g0 = g.toInt(); val b0 = b.toInt()
            val r1 = (r0 + 1).coerceAtMost(n - 1)
            val g1 = (g0 + 1).coerceAtMost(n - 1)
            val b1 = (b0 + 1).coerceAtMost(n - 1)
            val dr = r - r0; val dg = g - g0; val db = b - b0
            val c000 = read(r0, g0, b0); val c100 = read(r1, g0, b0)
            val c010 = read(r0, g1, b0); val c110 = read(r1, g1, b0)
            val c001 = read(r0, g0, b1); val c101 = read(r1, g0, b1)
            val c011 = read(r0, g1, b1); val c111 = read(r1, g1, b1)
            val out = FloatArray(3)
            for (i in 0..2) {
                val v00 = lerp(c000[i], c100[i], dr); val v10 = lerp(c010[i], c110[i], dr)
                val v01 = lerp(c001[i], c101[i], dr); val v11 = lerp(c011[i], c111[i], dr)
                val v0 = lerp(v00, v10, dg); val v1 = lerp(v01, v11, dg)
                out[i] = lerp(v0, v1, db)
            }
            return out
        }

        private fun read(r: Int, g: Int, b: Int): FloatArray {
            val i = (b * size + g) * size + r
            return floatArrayOf(data[i * 3], data[i * 3 + 1], data[i * 3 + 2])
        }

        private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t
    }

    fun parse(reader: BufferedReader): Result<CubeLut> = runCatching {
        var title = ""
        var size = 0
        var dMin = 0f; var dMax = 1f
        val floats = ArrayList<Float>(64 * 64 * 64 * 3)

        reader.useLines { lines ->
            for (line in lines) {
                val trimmed = line.substringBefore('#').trim()
                if (trimmed.isEmpty()) continue
                val parts = trimmed.split(Regex("\\s+"))
                when (parts[0].uppercase()) {
                    "TITLE" -> if (parts.size > 1) title = parts.drop(1).joinToString(" ").trim('"')
                    "LUT_3D_SIZE" -> size = parts[1].toInt().also { require(it in 2..256) { "unsupported LUT size" } }
                    "DOMAIN_MIN" -> if (parts.size >= 4) dMin = parts[1].toFloat()
                    "DOMAIN_MAX" -> if (parts.size >= 4) dMax = parts[1].toFloat()
                    else -> {
                        if (parts.size == 3) {
                            try {
                                floats.add(parts[0].toFloat())
                                floats.add(parts[1].toFloat())
                                floats.add(parts[2].toFloat())
                            } catch (_: NumberFormatException) { /* skip */ }
                        }
                    }
                }
            }
        }
        require(size > 0) { "missing LUT_3D_SIZE" }
        val expected = size.toLong() * size * size * 3
        require(floats.size.toLong() == expected) {
            "expected $expected floats, got ${floats.size}"
        }
        CubeLut(title, size, dMin, dMax, FloatArray(floats.size) { floats[it] })
    }
}
