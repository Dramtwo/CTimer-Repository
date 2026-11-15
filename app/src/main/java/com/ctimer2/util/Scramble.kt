package com.ctimer2.util

import kotlin.random.Random

/**
 * 三阶打乱生成器（random-move 规则，与 csTimer 的 "随机步" 打乱一致）。
 *
 * 约束：
 * 1. 连续两步不能是同一面（避免 R R）
 * 2. 连续三步不能同轴（避免 R L R / U D U 这类等价冗余）
 *
 * 输出形如：R U' F2 L' D B2 ...
 */
object Scramble {

    private val FACES = charArrayOf('R', 'L', 'U', 'D', 'F', 'B')
    private val SUFFIX = arrayOf("", "'", "2")

    /** 轴的编号：R/L=0, U/D=1, F/B=2 */
    private fun axisOf(faceIndex: Int) = faceIndex / 2

    fun generate(length: Int = 21): String {
        val moves = ArrayList<String>(length)
        var lastFace = -1
        var lastAxis = -1
        var prevAxis = -1

        while (moves.size < length) {
            val f = Random.nextInt(FACES.size)
            val axis = axisOf(f)

            if (f == lastFace) continue
            // 连续三步同轴则跳过（例如 R L R）
            if (axis == lastAxis && axis == prevAxis) continue

            moves.add(FACES[f].toString() + SUFFIX[Random.nextInt(SUFFIX.size)])
            prevAxis = lastAxis
            lastAxis = axis
            lastFace = f
        }
        return moves.joinToString(" ")
    }

    /**
     * 把字母公式翻成中文口诀，例如 "R U' F2 L' B" → "上 回 顺顺 左逆 提"
     * 对照：R=上 R'=下 R2=上上 ／ L=左 L'=左逆 L2=左左 ／ U=勾 U'=回 U2=勾勾
     *      D=底 D'=底逆 D2=底底 ／ F=顺 F'=逆 F2=顺顺 ／ B=提 B'=压 B2=提提
     */
    fun toChinese(scramble: String): String {
        val words = scramble.trim().split(" ").filter { it.isNotBlank() }.map { token ->
            val face = token[0]
            val mod = token.drop(1)
            val base = when (face) {
                'R' -> "上"
                'L' -> "左"
                'U' -> "勾"
                'D' -> "底"
                'F' -> "顺"
                'B' -> "提"
                else -> face.toString()
            }
            when (mod) {
                "'" -> when (face) {
                    'U' -> "回"
                    'F' -> "逆"
                    'B' -> "压"
                    'R' -> "下"
                    else -> base + "逆"
                }
                "2" -> base + base
                else -> base
            }
        }
        // 每 5 步断句，便于读诵
        return words.chunked(5).joinToString("，") { it.joinToString(" ") }
    }
}
