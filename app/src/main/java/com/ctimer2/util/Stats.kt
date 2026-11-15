package com.ctimer2.util

import kotlin.math.abs
import kotlin.math.sqrt

/** 成绩总评结果 */
data class Summary(
    val count: Int = 0,
    val bestMs: Long? = null,
    val worstMs: Long? = null,
    val avgMs: Long? = null,
    val medianMs: Long? = null,
    val stdDevMs: Long? = null,
    val recent5AvgMs: Long? = null,
    val recent12AvgMs: Long? = null,
    val first10AvgMs: Long? = null,
    val improvementPct: Double? = null,  // 最近10条相对最早10条的提升百分比
    val consistencyScore: Int = 0       // 0-100，越稳越高
)

/**
 * 统计工具：输入毫秒列表（按最新在前排序），返回秒。
 */
object Stats {

    /** ao5：最近5条去掉一个最好一个最差，剩余3条平均。不足5条返回 null。 */
    fun ao5(timesMs: List<Long>): Double? {
        if (timesMs.size < 5) return null
        val recent = timesMs.take(5)
        val sorted = recent.sorted()
        val middle = sorted.subList(1, sorted.size - 1)
        return middle.average() / 1000.0
    }

    /** 全部成绩平均。空列表返回 null。 */
    fun avg(timesMs: List<Long>): Double? {
        if (timesMs.isEmpty()) return null
        return timesMs.average() / 1000.0
    }

    /** 格式化毫秒为显示字符串：M:SS.ss 或 SS.ss */
    fun format(ms: Long): String {
        val sec = ms / 1000
        val centis = ((ms % 1000) / 10).toInt()
        return if (sec >= 60) {
            val m = sec / 60
            val s = sec % 60
            "%d:%02d.%02d".format(m, s, centis)
        } else {
            "%d.%02d".format(sec, centis)
        }
    }

    /** 格式化统计平均值（秒，2位小数） */
    fun formatStat(seconds: Double?): String {
        if (seconds == null) return "--"
        return String.format("%.2f", seconds)
    }

    /** 差值（毫秒）→ 带符号秒，如 +0.42s / -0.31s */
    fun formatDelta(deltaMs: Long): String {
        val sec = deltaMs / 1000.0
        val sign = if (sec >= 0) "+" else "-"
        return "$sign${String.format("%.2f", abs(sec))}s"
    }

    /** 成绩总评 */
    fun summary(timesMs: List<Long>): Summary {
        if (timesMs.isEmpty()) return Summary()

        val mean = timesMs.average()
        val sorted = timesMs.sorted()
        val median = if (sorted.size % 2 == 1) {
            sorted[sorted.size / 2].toDouble()
        } else {
            (sorted[sorted.size / 2 - 1] + sorted[sorted.size / 2]) / 2.0
        }
        val variance = timesMs.sumOf { (it - mean) * (it - mean) } / timesMs.size
        val stdDev = sqrt(variance)

        // 稳定性：变异系数越低越稳，映射到 0-100
        val cv = if (mean > 0) stdDev / mean else 1.0
        val consistency = (100 - (cv * 400)).coerceIn(0.0, 100.0).toInt()

        val recent5 = timesMs.take(5).takeIf { it.size >= 3 }?.average()
        val recent12 = timesMs.take(12).takeIf { it.size >= 6 }?.average()
        val first10 = timesMs.takeLast(10).takeIf { it.size >= 5 }?.average()

        val improvement = if (recent12 != null && first10 != null && first10 > 0) {
            ((first10 - recent12) / first10) * 100.0
        } else null

        return Summary(
            count = timesMs.size,
            bestMs = timesMs.minOrNull(),
            worstMs = timesMs.maxOrNull(),
            avgMs = mean.toLong(),
            medianMs = median.toLong(),
            stdDevMs = stdDev.toLong(),
            recent5AvgMs = recent5?.toLong(),
            recent12AvgMs = recent12?.toLong(),
            first10AvgMs = first10?.toLong(),
            improvementPct = improvement,
            consistencyScore = consistency
        )
    }
}
