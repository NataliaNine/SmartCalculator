package calculator

val REGEX_VARIABLE = Regex("[a-zA-Z]+")
val REGEX_ALLOWED_NUMBER = Regex("-?\\d+")
val REGEX_OPERATORS = Regex("\\++|-+|\\*|/|\\^")
val REGEX_ALL_MINUS = Regex("-+")
val REGEX_ALL_PLUS = Regex("\\++")
val REGEX_WHITESPACE = Regex("\\s+")
val REGEX_FULL_SPLITTER = Regex("((?=\\+|-|\\^|=|\\s|\\(|\\))|(?<=\\+|-|\\^|=|\\s|\\(|\\)))")
var variables: MutableMap<String, Int> = mutableMapOf()
var pQueue: MutableList<Parenthesis> = mutableListOf()
var debugMode = false


fun main() {

    do {
        val equation = readLine()!!

        when {
            equation == "/exit" -> continue
            equation == "" -> continue
            equation == "/help" -> help()
            equation.startsWith("/") -> println("Unknown command")
            else -> processEquation(equation)
        }
    } while (equation != "/exit")

    println("Bye!")
}


fun processEquation(equation: String) {
    if (equation.contains(Regex("=.+="))) {
        println("Invalid Assignment")
        return
    }

    val splits = equation.split(REGEX_FULL_SPLITTER)
    if (debugMode) {
        println(splits.joinToString(":"))
    }
    //val elements: List<Element>
    val regexParserElements: List<Element>
    try {
        //elements = parseIntoElements(equation)
        regexParserElements = parseIntoElementsRegex2(splits)
        if (debugMode) {
            //println("Original Parser Results:")
            //println(elements.joinToString { "$it, " })
            println("Regex Parser Results: ")
            println(regexParserElements.joinToString { "$it, " })
            //println("Lists are equal: ${elements.map { it.toString() } == regexParserElements.map { it.toString() }}")
        }
    } catch (e: Exception) {
        if (debugMode) {
            println("Got Exception ${e.message}")
            e.printStackTrace()
        } else {
            println(e.message)
        }

        return
    }

    if (debugMode) println("\nResulting elements for input: $equation")
    if (regexParserElements.size >= 3 && regexParserElements[0] is Variable && regexParserElements[1] is Assignment) {
        val variable: Variable = regexParserElements[0] as Variable
        val valueEquation = regexParserElements.drop(2)
        try {
            val value: Int = calculateValue(valueEquation)
            if (debugMode) println("\nSetting variables[${variable.variableName}] = $value")
            variables[variable.variableName] = value
        } catch (e: Exception) {
            println(e.message)
        }


    } else {
        try {
            val value: Int? = calculateValue(regexParserElements)
            if (value != null) println(value)
        } catch (e: Exception) {
            println(e.message)
        }

    }
}

fun calculateValue(valueEquation: List<Element>): Int {
    if (debugMode) println("calling calculateValue(\"${valueEquation.joinToString(" ")}\")")

    if (valueEquation.size == 1) {
        if (isValueType((valueEquation[0]))) {
            return valueOfElement(valueEquation[0])
        } else throw Exception("Invalid Equation: ${valueEquation.joinToString(" ")}")
    }
    var flattened: MutableList<Element> = mutableListOf()
    for (element in valueEquation) {
        flattened += if (element is Parenthesis) {
            Numeric(calculateValue(element.elements))
        } else {
            element
        }
    }

    for (opOrder in 1..6) {
        val newFlat: MutableList<Element> = mutableListOf()
        var lastOp: String? = null
        for (i in 1 until flattened.size-1 step 2) {
            val operator: Operator = flattened[i] as Operator
            val element0 = if (lastOp == null) flattened[i - 1] else newFlat.last()
            val element2 = flattened[i + 1]
            if (!(isValueType(element0) && operator is Operator && isValueType(element2))) {
                throw Exception("Invalid Equation: ${flattened.joinToString(" ")}")
            }


            val nValue0 = valueOfElement(element0)
            val nValue2 = valueOfElement(element2)

            if (operator.precedence() == opOrder) {
                val result =
                    Numeric(operator.getMathFunction().invoke(nValue0, nValue2))
                if (newFlat.isEmpty()) {
                    newFlat += result
                    lastOp = operator.toCleanString()
                } else {
                    newFlat[newFlat.lastIndex] = result
                }
            } else {
                if (newFlat.isEmpty()) {
                    newFlat += element0
                }
                newFlat += operator
                newFlat += element2
            }
        }
        if (debugMode) println("Updated equation:\n${newFlat.joinToString(" ")}")
        flattened = newFlat
        if (flattened.size == 1) break
    }

    if (flattened.size == 1) {
        return valueOfElement(flattened[0])
    }

    throw Exception("Invalid expression")
}

fun valueOfElement(element: Element): Int {
    return when (element) {
        is Numeric -> element.numericValue()
        is Variable -> variableValue(element)
        is Parenthesis -> calculateValue(element.elements)
        else -> throw Exception("Unknown variable")
    }
}

fun variableValue(variable: Variable): Int {
    val name = variable.name()
    val value = variables[name]
    if (value != null) {
        return value
    } else throw Exception("Unknown Variable")
}

fun isValueType(element: Element): Boolean {
    return element is Variable || element is Numeric || element is Parenthesis
}

fun help() {
    println("The program solves basic equations in the form:")
    println("-2 + 4 - 5 + 6")
    println("OR")
    println("9 +++ 10 -- 8")
}


fun parseIntoElementsRegex2(input: List<String>): List<Element> {
    val elements = mutableListOf<Element>()
    var lastElementType = ""
    var currentList = elements
    var acc = ""

    for (c in input) {
        if (debugMode) print(c)
        when {
            acc == "" && isWhitespace(c) -> {}

            acc == "" && c in listOf("-","+") -> {
                acc = c
            }

            acc == "-" && isNumberValue(c) -> {
                when (lastElementType) {
                    "val" -> {
                        currentList += Operator(acc)
                        currentList += Numeric(c)
                        lastElementType = "val"
                        acc = ""
                    }

                    "op" -> {
                        currentList += Numeric(acc + c)
                        lastElementType = "val"
                        acc = ""
                    }

                    "assign" -> {
                        currentList += Numeric(acc + c)
                        lastElementType = "assign"
                        acc = ""
                    }
                }

            }

            isAllMinusChars(acc) && c == "-" -> {
                acc += c
            }

            isAllMinusChars(acc) && isNumberValue(c) -> {
                currentList += Operator(acc)
                currentList += Numeric(c)
                lastElementType = "val"
                acc = ""
            }

            isAllMinusChars(acc) && isVariable(c) -> {
                currentList += Operator(acc)
                currentList += Variable(c)
                lastElementType = "val"
                acc = ""
            }

            isAllPlusChars(acc) && c == "+" -> {
                acc += c
            }

            isAllPlusChars(acc) && c!= "+" -> {
                currentList += Operator(acc)
                lastElementType = "op"
                acc = ""
                if (c == "-") {
                    acc = c
                }
            }

            (isAllPlusChars(acc) || isAllMinusChars(acc)) && isWhitespace(c) -> {
                currentList += Operator(acc)
                acc = ""
                lastElementType = "op"
            }

            isOperator(c) -> {
                currentList += Operator(c)
                lastElementType = "op"
            }

            isNumberValue(c) -> {
                currentList += Numeric(c)
                lastElementType = "val"
            }

            isVariable(c) -> {
                currentList += Variable(c)
                lastElementType = "val"
            }

            isAssignment(c) -> {
                currentList += Assignment(c)
                lastElementType = "assign"
            }

            c == "(" -> {
                val pOpen = Parenthesis("(", currentList)
                pQueue += pOpen
                currentList = pOpen.elements
                lastElementType = ""
            }

            c == ")" -> {
                if (pQueue.isEmpty()) throw Exception("Invalid expression")
                val closedParen = pQueue.removeAt(pQueue.lastIndex)
                currentList = closedParen.parentList
                currentList += closedParen
                lastElementType = "val"
            }
        }

    }

    if (pQueue.isNotEmpty()) {
        pQueue = mutableListOf()
        throw Exception("Invalid Expression")
    }
    return elements
}

private fun isVariable(c: String?) = c!!.matches(REGEX_VARIABLE)

private fun isWhitespace(c: String?) = c!!.matches(REGEX_WHITESPACE)

private fun isAllMinusChars(accumulator: String) = accumulator.matches(REGEX_ALL_MINUS)

private fun isAllPlusChars(accumulator: String) = accumulator.matches(REGEX_ALL_PLUS)

private fun isAssignment(accumulator: String) = accumulator == "="

private fun isOperator(input: String) = input.matches(REGEX_OPERATORS)

private fun isNumberValue(accumulator: String?) = accumulator != null && accumulator.matches(REGEX_ALLOWED_NUMBER)