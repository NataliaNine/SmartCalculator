package calculator

class Numeric(rawValue: String) : Element(rawValue) {
    constructor(rawValue: Int?) : this(rawValue.toString())

    override fun toString(): String {
        return "$rawValue"
    }

    fun numericValue(): Int {
        return rawValue.toInt()
    }

}