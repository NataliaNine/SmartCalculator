package calculator

// this will hold a list of the elements

class Parenthesis (rawValue: String, val parentList: MutableList<Element>) : Element(rawValue) {
    var elements: MutableList<Element> = mutableListOf()

    override fun toString(): String {
        return "(${elements.joinToString(" ")})"
    }
}