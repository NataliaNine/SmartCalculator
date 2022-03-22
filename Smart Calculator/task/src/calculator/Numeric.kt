package calculator

class Numeric(rawValue: String) : Element(rawValue) {
    override fun toString(): String {
        return "(Number) $rawValue"
    }

    fun numericValue(): Int {
        return rawValue.toInt()
    }

}