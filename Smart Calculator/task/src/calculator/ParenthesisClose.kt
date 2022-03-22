package calculator

class ParenthesisClose(rawValue: String) : Element(rawValue) {
    override fun toString(): String {
        return ")"
    }
}