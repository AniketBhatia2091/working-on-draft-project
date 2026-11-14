package com.daykhata.app.util

object CurrencyUtils {

    /**
     * Formats an amount using Indian currency notation (e.g. ₹14,770, ₹1,05,000).
     */
    fun formatIndianRupees(amount: Int): String {
        val isNegative = amount < 0
        val absAmount = kotlin.math.abs(amount).toLong()
        val formattedNumber = formatIndianNumber(absAmount)
        return if (isNegative) "₹-$formattedNumber" else "₹$formattedNumber"
    }

    /**
     * Formats an integer to Indian numbering format (rightmost 3 digits, then groups of 2).
     */
    fun formatIndianNumber(num: Long): String {
        val str = num.toString()
        if (str.length <= 3) return str

        val lastThree = str.substring(str.length - 3)
        val remaining = str.substring(0, str.length - 3)
        val sb = StringBuilder()
        var count = 0

        for (i in remaining.length - 1 downTo 0) {
            sb.append(remaining[i])
            count++
            if (count % 2 == 0 && i > 0) {
                sb.append(',')
            }
        }

        return sb.reverse().toString() + "," + lastThree
    }
}
