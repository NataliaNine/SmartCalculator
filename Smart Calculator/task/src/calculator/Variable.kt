package calculator

class Variable(rawValue: String) : Element(rawValue) {
    val variableName: String

    init {
        variableName = rawValue
    }

    override fun toString(): String {
        return "(Variable) $variableName"
    }

    fun name(): String {
        return rawValue
    }
}