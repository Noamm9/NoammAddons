import kotlin.math.*
import kotlin.random.Random

object GaussianRandomTest {
    fun gaussianRandom(min: Int, max: Int): Int {
        val u1 = 1.0 - Random.nextDouble()
        val u2 = 1.0 - Random.nextDouble()
        val gaussian = sqrt(- 2.0 * ln(u1)) * cos(2.0 * Math.PI * u2)

        val mean = min + (max - min) / 2.0
        val stdDev = (max - min) / 6.0

        val result = (gaussian * stdDev) + mean

        return max(min.toDouble(), min(max.toDouble(), result)).toInt()
    }

    @JvmStatic
    fun main(args: Array<String>) {
        println("--- Running Gaussian Random Test ---\n")

        val min = 0
        val max = 150
        val iterations = 100_000_000

        val bucketCount = 10
        val bucketSize = (max - min) / bucketCount
        val buckets = IntArray(bucketCount)

        println("Range: ${min}ms to ${max}ms | Rolls: $iterations")
        println("--------------------------------------------------")

        repeat(iterations) {
            val result = gaussianRandom(min, max) //Random.nextInt(min, max)

            var bucketIndex = (result - min) / bucketSize
            if (bucketIndex >= bucketCount) bucketIndex = bucketCount - 1

            buckets[bucketIndex] ++
        }

        val maxBucketValue = buckets.maxOrNull() ?: 1

        for (i in 0 until bucketCount) {
            val bucketStart = min + (i * bucketSize)
            val bucketEnd = bucketStart + bucketSize - 1
            val count = buckets[i]

            val barLength = ((count.toDouble() / maxBucketValue) * 40).toInt()
            val bar = "#".repeat(barLength)

            val rangeLabel = "${bucketStart}-${bucketEnd}ms".padEnd(9)
            val percentage = String.format("%.1f%%", (count.toDouble() / iterations) * 100).padEnd(6)

            println("$rangeLabel | $percentage | $bar")
        }

        println("--------------------------------------------------")
    }
}