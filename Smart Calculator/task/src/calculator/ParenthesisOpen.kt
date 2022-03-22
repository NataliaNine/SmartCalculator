package calculator

// this will hold a list of the elements

class ParenthesisOpen (rawValue: String) : Element(rawValue) {
    var elements: MutableList<Element> = mutableListOf()

    override fun toString(): String {
        return "(Parenthesis) $rawValue"
    }
}