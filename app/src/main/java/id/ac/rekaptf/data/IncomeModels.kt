package id.ac.rekaptf.data

import id.ac.rekaptf.R
import org.json.JSONArray
import org.json.JSONObject
import java.text.NumberFormat
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale

private val Indonesia = Locale.forLanguageTag("id-ID")
private val LongDateFormat = DateTimeFormatter.ofPattern("d MMM yyyy", Indonesia)

internal fun today(): LocalDate = LocalDate.now(ZoneId.of("Asia/Jakarta"))

internal fun parseDate(value: String): LocalDate? = if (value.length != 10) {
    null
} else {
    runCatching { LocalDate.parse(value) }.getOrNull()?.takeIf { it.year in 1..9999 }
}

internal fun displayDate(value: String): String =
    parseDate(value)?.format(LongDateFormat) ?: value

internal fun money(amount: Number): String =
    "Rp " + NumberFormat.getIntegerInstance(Indonesia).format(amount)

internal fun JSONObject.optionalText(key: String): String = if (isNull(key)) "" else optString(key)

internal fun <T> JSONObject.mapObjects(key: String, transform: (JSONObject) -> T): List<T> {
    val array = getJSONArray(key)
    return List(array.length()) { index -> transform(array.getJSONObject(index)) }
}

internal const val MandiriSource = "Livin’ by Mandiri"

internal data class Income(
    val id: String = "",
    val amount: Long = 0,
    val paidOn: String = "",
    val payerName: String = "",
    val reference: String = "",
    val note: String = "",
    val source: String = MandiriSource,
    val status: String = "",
    val categoryId: String = "",
    val payerId: String = ""
) {
    constructor(json: JSONObject) : this(
        id = json.optionalText("id"),
        amount = json.optLong("amount"),
        paidOn = json.optionalText("paidOn"),
        payerName = json.optionalText("payerName"),
        reference = json.optionalText("reference"),
        note = json.optionalText("note"),
        source = json.optionalText("source").ifBlank { MandiriSource },
        status = json.optionalText("status"),
        categoryId = json.optionalText("categoryId"),
        payerId = json.optionalText("payerId")
    )

    fun fields(): JSONObject = JSONObject()
        .put("amount", amount)
        .put("paidOn", paidOn)
        .put("payerName", payerName.trim())
        .put("reference", reference.trim())
        .put("note", note.trim())
        .put("source", source)
        .put("categoryId", categoryId)
        .put("payerId", payerId)
}

internal enum class Period(val key: String, val label: Int) {
    DAY("day", R.string.period_day),
    WEEK("week", R.string.period_week),
    MONTH("month", R.string.period_month),
    CUSTOM("custom", R.string.period_custom);

    fun range(date: LocalDate = today()): DateRange = when (this) {
        DAY, CUSTOM -> DateRange(date.toString(), date.toString())
        WEEK -> {
            val monday = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            DateRange(monday.toString(), monday.plusDays(6).toString())
        }
        MONTH -> DateRange(date.withDayOfMonth(1).toString(), date.withDayOfMonth(date.lengthOfMonth()).toString())
    }

    companion object {
        fun fromKey(key: String): Period = entries.firstOrNull { it.key == key } ?: CUSTOM
    }
}

internal data class DateRange(val from: String, val to: String) {
    val valid: Boolean
        get() {
            val start = parseDate(from) ?: return false
            val end = parseDate(to) ?: return false
            return !end.isBefore(start)
        }
}

internal data class SavedRecap(val id: String, val name: String, val period: Period, val range: DateRange) {
    constructor(json: JSONObject) : this(
        json.optionalText("id"), json.optionalText("name"),
        Period.fromKey(json.optionalText("period")),
        DateRange(json.optionalText("from"), json.optionalText("to"))
    )

    fun fields(): JSONObject = JSONObject()
        .put("name", name.trim())
        .put("period", period.key)
        .put("from", range.from)
        .put("to", range.to)
}

internal data class Recap(val range: DateRange, val total: Long, val count: Int, val items: List<Income>) {
    constructor(json: JSONObject) : this(
        DateRange(json.getString("from"), json.getString("to")),
        json.getLong("total"), json.getInt("count"), json.mapObjects("items", ::Income)
    )

    fun toDocument(): JSONObject = JSONObject()
        .put("format", "RekapTF")
        .put("version", 1)
        .put("from", range.from)
        .put("to", range.to)
        .put("total", total)
        .put("count", count)
        .put("items", JSONArray().apply {
            items.forEach { income ->
                put(
                    income.fields().removeWorkspaceLinks()
                )
            }
        })

    companion object {
        fun fromDocument(json: JSONObject): Recap {
            require(json.getString("format") == "RekapTF" && json.getInt("version") == 1)
            val recap = Recap(json)
            require(recap.range.valid && recap.count == recap.items.size)
            var total = 0L
            recap.items.forEach { item ->
                require(item.amount > 0 && parseDate(item.paidOn) != null)
                require(item.paidOn >= recap.range.from && item.paidOn <= recap.range.to)
                require(Long.MAX_VALUE - total >= item.amount)
                total += item.amount
            }
            require(total == recap.total)
            return recap
        }
    }
}

private fun JSONObject.removeWorkspaceLinks(): JSONObject = apply {
    remove("categoryId")
    remove("payerId")
}
