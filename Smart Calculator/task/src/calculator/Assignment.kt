package calculator

class Assignment(rawValue: String) : Element(rawValue) {
    override fun toString(): String {
        return "(Assignment) $rawValue"
    }
}