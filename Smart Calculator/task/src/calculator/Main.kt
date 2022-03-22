package calculator

val REGEX_VARIABLE = Regex("[a-zA-Z]+")
val REGEX_ALLOWED_NUMBER = Regex("-?\\d+")
val REGEX_OPERATORS = Regex("\\++|-+|\\*|/|\\^")
val REGEX_ALL_MINUS = Regex("-+")
val REGEX_ALL_PLUS = Regex("\\++")
val REGEX_WHITESPACE = Regex("\\s+")
val REGEX_FULL_SPLITTER = Regex("((?=\\+|-|\\^|=|\\s|\\(|\\))|(?<=\\+|-|\\^|=|\\s|\\(|\\)))")
var variables: MutableMap<String, Int> = mutableMapOf()
var pQueue: MutableList<Element> = mutableListOf()
var debugMode = true

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
    val splits = equation.split(REGEX_FULL_SPLITTER)
    if (debugMode) {
        println(splits.joinToString(":"))
    }
    //val elements: List<Element>
    val regexParserElements: List<Element>
    try {
        //elements = parseIntoElements(equation)
        regexParserElements = parseIntoElementsRegex(splits)
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
        val value: Int? = calculateValue(valueEquation)
        if (debugMode) if (value != null) println("\nSetting variables[${variable.variableName}] = $value")
        else println("Got null")
        if (value != null) variables[variable.variableName] = value
    } else {
        val value: Int? = calculateValue(regexParserElements)
        if (value != null) println(value)
    }
}

enum class EXPECTS {
    VALUE, OPERATOR
}

fun calculateValue(valueEquation: List<Element>): Int? {
    var expects = EXPECTS.VALUE
    var accumulator: Int? = null
    var currentOperator: Operator? = null

    try {
        for (element in valueEquation) {
            when {
                expects == EXPECTS.VALUE && isValueType(element) -> {
                    accumulator =
                        if (accumulator == null && currentOperator == null) {
                            // at the start
                            valueOfElement(element)
                        } else {
                            currentOperator!!.getMathFunction().invoke(accumulator!!, valueOfElement(element))
                        }
                    expects = EXPECTS.OPERATOR
                }

                expects == EXPECTS.OPERATOR && element is Operator -> {
                    currentOperator = element
                    expects = EXPECTS.VALUE
                }
                else -> throw Exception("Invalid order of elements in equation")
            }
        }

        if (expects == EXPECTS.VALUE && valueEquation.size > 1) throw Exception("Equation processor left hanging with Operator")
    } catch (e: Exception) {
        println(e.message)
        return null
    }

    return accumulator!!
}

fun valueOfElement(element: Element): Int {
    return when (element) {
        is Numeric -> element.numericValue()
        is Variable -> variableValue(element)
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
    return element is Variable || element is Numeric
}

fun help() {
    println("The program solves basic equations in the form:")
    println("-2 + 4 - 5 + 6")
    println("OR")
    println("9 +++ 10 -- 8")
}

fun parseIntoElements(input: String): List<Element> {
    val elements = mutableListOf<Element>()
    var accumulator = ""
    val inputChars = input.toCharArray()
    var lastElementType = ""

    for (c in inputChars) {
        if (debugMode) print(c)
        val result: Element? = accumulateCharacter(accumulator, c, lastElementType)

        if (result == null) {
            // nothing yet, add this char, if not whitespace
            if (!c.isWhitespace()) accumulator += c
        } else {
            accumulator = if (!c.isWhitespace()) {
                c.toString()
            } else {
                ""
            }
            elements += result
            if (c == '=' && elements.size != 1) throw Exception("Invalid Assignment")
            lastElementType = result::class.toString()
        }
    }

    val result: Element? = accumulateCharacter(accumulator, null, lastElementType)
    if (result != null) elements += result

    return elements.toList()
}

fun parseIntoElementsRegex2(input: List<String>): List<Element> {
    val elements = mutableListOf<Element>()
    var listQueue: List<List<Element>> = mutableListOf()
    var lastElementType = ""

    // this is used to condense multi element values into single values
    // such as "++++++" into "+" and "-----" into "-"
    // and "-" + "123" into "-123"
    var acc = ""

    for (c in input) {
        if (debugMode) print(c)
        if (acc != "") when {
            acc == "-" && isNumberValue(c) -> {
                elements += Numeric(acc + c)
                acc = ""
                continue
            }

            isOperator(acc) && !isOperator(c) -> {
                elements += Operator(acc)
            }

            isAllMinusChars(acc) && c == "-" -> {
                acc += c
                // accumulated c, done processing element
                continue
            }

            isAllMinusChars(acc) && c != "-" -> {
                elements += Operator(acc)
                acc = ""
                if (c == "+") {
                    acc = c
                    continue
                }
            }

            isAllPlusChars(acc) && c == "+" -> {
                acc += c
                continue
            }

            isAllPlusChars(acc) && c!= "+" -> {
                elements += Operator(acc)
                acc = ""
                if (c == "-") {
                    acc = c
                    continue
                }
            }
        }

        // if we got here, c has NOT been accumulated
        when {
            isOperator(c) -> elements += Operator(c)
            isNumberValue(c) -> elements += Numeric(c)
            c == "(" -> {

            }

            c == ")" -> {

            }
        }

    }
}

fun getElement(s: String): Element {
    when {
        isNumberValue(s) -> Numeric(s)
        isOperator(s) -> Operator(s)
        s == "(" -> ParenthesisOpen(s)
        s == ")" -> ParenthesisClose(s)
        s == "=" -> Assignment(s)
        else -> throw Exception("Invalid Expression")
    }
}


fun parseIntoElementsRegex(input: List<String>): List<Element> {
    val elements = mutableListOf<Element>()
    var accumulator = ""
    var lastElementType = ""

    for (c in input) {
        if (debugMode) print(c)
        val result: Element? = accumulateString(accumulator, c, lastElementType)

        if (result == null) {
            // nothing yet, add this char, if not whitespace
            if (!isWhitespace(c)) accumulator += c
        } else {
            accumulator = if (!isWhitespace(c)) {
                c.toString()
            } else {
                ""
            }
            elements += result
            if (c == "=" && elements.size != 1) throw Exception("Invalid Assignment")
            lastElementType = result::class.toString()
        }
    }

    val result: Element? = accumulateString(accumulator, null, lastElementType)
    if (result != null) elements += result

    return elements.toList()
}

private val allowedOperatorChars = listOf('-', '+', '*', '/', '^')

fun accumulateCharacter(accumulator: String, c: Char?, lastElementType: String): Element? {
    if (accumulator == "" && (c == null || c.isWhitespace())) return null

    return when (lastElementType) {
        "" -> when { // This is the first element
            isStartOfAssignment(accumulator, c.toString()) -> throw Exception("Invalid first character: $c")
            startsWithMinus(accumulator, c) -> null
            accumulator == "" -> null
            accumulator == "(" -> return ParenthesisOpen(accumulator)
            isVariable(accumulator) && c == '=' -> Variable(accumulator)
            accumulatorIsNotEmpty(accumulator) && c == '=' -> throw Exception("Found = with invalid accumulator data: $accumulator")
            isSingleMinusChar(accumulator) && c!!.isDigit() -> null
            isVariable(accumulator) && c == null -> Variable(accumulator)
            isVariable(accumulator) && c!!.isLetter() -> null
            isVariable(accumulator) && (c == null || c in allowedOperatorChars || c.isWhitespace()) -> Variable(
                accumulator
            )
            isNumberValue(accumulator) && (c == null || c.isWhitespace() || c in allowedOperatorChars) -> Numeric(
                accumulator
            )
            isNumberValue(accumulator) && c!!.isDigit() -> null
            isVariable(accumulator) && c!!.isDigit() -> throw Exception("Invalid identifier")
            else -> throw Exception("Invalid character $c for accumulator: $accumulator")
        }


        // here we just spit out a Variable, because the last new character indicated that it is time
        Variable::class.toString() ->
            // now we are tracking either an operator, an assignment, or an error
            when {
                isStartOfAssignment(accumulator, c.toString()) -> null
                isAssignment(accumulator) && c!!.isWhitespace() -> Assignment(accumulator)
                isOperator(accumulator) && c == null -> throw Exception("Ending on an operator $accumulator")
                isAssignment(accumulator) && c == null -> throw Exception("Ending on an assignment in accumulator: $accumulator")
                couldBeNewOperator(accumulator, c) -> null
                isAssignment(accumulator) && (c!!.isDigit() || c.isLetter() || c == '-') -> Assignment(accumulator)
                isAllPlusChars(accumulator) && c == '+' -> null
                isAllMinusChars(accumulator) && c == '-' -> null
                isOperator(accumulator) &&
                        (c!!.isDigit() || c.isLetter() || c.isWhitespace() || c == '-') -> Operator(accumulator)
                else -> throw Exception("Invalid character $c for accumulator: $accumulator")
            }

        // here we just spit out a number, because the last new character indicated it
        Numeric::class.toString() ->
            // now we are tracking either an operator or an error
            when {
                couldBeNewOperator(accumulator, c) -> null
                isOperator(accumulator) && c == null -> throw Exception("Ending on an operator $accumulator")
                isAllPlusChars(accumulator) && c == '+' -> null
                isAllMinusChars(accumulator) && c == '-' -> null
                isOperator(accumulator) && c == '-' -> Operator(accumulator)
                isOperator(accumulator) && (c!!.isDigit() || c.isLetter() || c.isWhitespace()) -> Operator(accumulator)
                isVariable(accumulator) && (c!!.isDigit() || c.isLetter() || c.isWhitespace()) ->
                    throw Exception("Error: Variable after number, accumulator: $accumulator, c: $c")
                else -> throw Exception("Invalid character $c for accumulator: $accumulator")
            }

        Assignment::class.toString() ->
            when {
                accumulator == "" && (c!!.isDigit() || c.isLetter() || c == '-') -> null
                isNumberValue(accumulator) && c == null -> Numeric(accumulator)
                isVariable(accumulator) && c == null -> Variable(accumulator)
                isSingleMinusChar(accumulator) && c!!.isDigit() -> null
                isNumberValue(accumulator) && c!!.isDigit() -> null
                isNumberValue(accumulator) && c in allowedOperatorChars -> Numeric(accumulator)
                isNumberValue(accumulator) && c!!.isWhitespace() -> Numeric(accumulator)
                isVariable(accumulator) && c!!.isLetter() -> null
                isVariable(accumulator) && c in allowedOperatorChars -> Variable(accumulator)
                isVariable(accumulator) && c!!.isWhitespace() -> Variable(accumulator)
                isVariable(accumulator) && c!!.isDigit() -> throw Exception("Invalid assignment")
                else -> throw Exception("Invalid character $c for accumulator: $accumulator")
            }


        // here we have just spit out an operator Element, and the accumulator is growing a new value
        Operator::class.toString() ->
            when {
                accumulator == "" && (c!!.isDigit() || c.isLetter() || c == '-') -> null
                isNumberValue(accumulator) && (c == null || c.isWhitespace()) -> Numeric(accumulator)
                isVariable(accumulator) && (c == null || c.isWhitespace()) -> Variable(accumulator)
                isSingleMinusChar(accumulator) && c!!.isDigit() -> null
                isNumberValue(accumulator) && c!!.isDigit() -> null
                isNumberValue(accumulator) && c in allowedOperatorChars -> Numeric(accumulator)
                isNumberValue(accumulator) && c!!.isWhitespace() -> Numeric(accumulator)
                isVariable(accumulator) && c!!.isLetter() -> null
                isVariable(accumulator) && c in allowedOperatorChars -> Variable(accumulator)
                isVariable(accumulator) && c!!.isWhitespace() -> Variable(accumulator)
                else -> throw Exception("Invalid character $c for accumulator: $accumulator")
            }

        else -> throw Exception("Invalid character $c for accumulator: $accumulator")
    }
}



fun accumulateString(accumulator: String, c: String?, lastElementType: String): Element? {
    if (accumulator == "" && (c == null || c.matches(REGEX_WHITESPACE))) return null

    return when (lastElementType) {
        "" -> when { // This is the first element
            isStartOfAssignment(accumulator, c) -> throw Exception("Invalid first character: $c")
            startsWithMinus(accumulator, c) -> null
            accumulator == "" -> null
            isVariable(accumulator) && c == "=" -> Variable(accumulator)
            accumulatorIsNotEmpty(accumulator) && c == "=" -> throw Exception("Found = with invalid accumulator data: $accumulator")
            isSingleMinusChar(accumulator) && c!!.matches(REGEX_ALLOWED_NUMBER) -> null
            isVariable(accumulator) && c == null -> Variable(accumulator)
            isVariable(accumulator) && isVariable(c) -> null
            isVariable(accumulator) && (c == null || isOperator(c) || c.matches(REGEX_WHITESPACE)) -> Variable(
                accumulator
            )
            isNumberValue(accumulator) && (c == null || c.matches(REGEX_WHITESPACE) || isOperator(c)) -> Numeric(
                accumulator
            )
            isNumberValue(accumulator) && c!!.matches(REGEX_ALLOWED_NUMBER) -> null
            isVariable(accumulator) && c!!.matches(REGEX_ALLOWED_NUMBER) -> throw Exception("Invalid identifier")
            else -> throw Exception("Invalid character $c for accumulator: $accumulator")
        }


        ParenthesisOpen::class.toString() -> {
            when {
                accumulator == "-" && isNumberValue(c!!) -> null
                isNumberValue(accumulator) && isOperator(c!!) -> Numeric(accumulator)
                isNumberValue(accumulator) && c == ")" -> Numeric(accumulator)
                isOperator(accumulator) && isNumberValue(c) -> Operator(accumulator)


                else -> null
            }
        }

        ParenthesisClose::class.toString() -> {
            when {
                else -> null
            }
        }

        // here we just spit out a Variable, because the last new character indicated that it is time
        Variable::class.toString() ->
            // now we are tracking either an operator, an assignment, or an error
            when {
                isStartOfAssignment(accumulator, c) -> null
                isAssignment(accumulator) && isWhitespace(c) -> Assignment(accumulator)
                isOperator(accumulator) && c == null -> throw Exception("Ending on an operator $accumulator")
                isAssignment(accumulator) && c == null -> throw Exception("Ending on an assignment in accumulator: $accumulator")
                couldBeNewOperator(accumulator, c) -> null
                isAssignment(accumulator) && c != null && (c.matches((REGEX_ALLOWED_NUMBER)) || c.matches(REGEX_VARIABLE) || c == "-") -> Assignment(
                    accumulator
                )
                isAllPlusChars(accumulator) && c!!.matches(REGEX_ALL_PLUS) -> null
                isAllMinusChars(accumulator) && c!!.matches(REGEX_ALL_MINUS) -> null
                isOperator(accumulator) &&
                    (c!!.matches(REGEX_ALLOWED_NUMBER) || c.matches(REGEX_VARIABLE)
                        || c == "("|| c.matches(REGEX_WHITESPACE) || c == "-") -> Operator(accumulator)
                else -> throw Exception("Invalid character $c for accumulator: $accumulator")
            }

        // here we just spit out a number, because the last new character indicated it
        Numeric::class.toString() ->
            // now we are tracking either an operator or an error
            when {
                couldBeNewOperator(accumulator, c) -> null
                isOperator(accumulator) && c == null -> throw Exception("Ending on an operator $accumulator")
                isAllPlusChars(accumulator) && c == "+" -> null
                isAllMinusChars(accumulator) && c == "-" -> null
                isOperator(accumulator) && c == "-" -> Operator(accumulator)
                isOperator(accumulator)
                    && (c!!.matches(REGEX_ALLOWED_NUMBER) || c.matches(REGEX_VARIABLE)
                        || c == "(" || c.matches(REGEX_WHITESPACE)) -> Operator(accumulator)
                isVariable(accumulator) && (c!!.matches(REGEX_ALLOWED_NUMBER) || c.matches(REGEX_VARIABLE) || c.matches(
                    REGEX_WHITESPACE
                )) ->
                    throw Exception("Error: Variable after number, accumulator: $accumulator, c: $c")
                else -> throw Exception("Invalid character $c for accumulator: $accumulator")
            }

        Assignment::class.toString() ->
            when {
                accumulator == "" && (c!!.matches(REGEX_ALLOWED_NUMBER) || c.matches(REGEX_VARIABLE) || c == "-") -> null
                isNumberValue(accumulator) && c == null -> Numeric(accumulator)
                isVariable(accumulator) && c == null -> Variable(accumulator)
                isSingleMinusChar(accumulator) && c!!.matches(REGEX_ALLOWED_NUMBER) -> null
                isNumberValue(accumulator) && c!!.matches(REGEX_ALLOWED_NUMBER) -> null
                isNumberValue(accumulator) && c != null && isOperator(c) -> Numeric(accumulator)
                isNumberValue(accumulator) && isWhitespace(c) -> Numeric(accumulator)
                isVariable(accumulator) && isVariable(c) -> null
                isVariable(accumulator) && c != null && isOperator(c)-> Variable(accumulator)
                isVariable(accumulator) && isWhitespace(c) -> Variable(accumulator)
                isVariable(accumulator) && c!!.matches(REGEX_ALLOWED_NUMBER) -> throw Exception("Invalid assignment")
                else -> throw Exception("Invalid character $c for accumulator: $accumulator")
            }


        // here we have just spit out an operator Element, and the accumulator is growing a new value
        Operator::class.toString() ->
            when {
                accumulator == "" && (c!!.matches(REGEX_ALLOWED_NUMBER) || c.matches(REGEX_VARIABLE) || c == "-") -> null
                isNumberValue(accumulator) && (c == null || c.matches(REGEX_WHITESPACE)) -> Numeric(accumulator)
                isVariable(accumulator) && (c == null || c.matches(REGEX_WHITESPACE)) -> Variable(accumulator)
                isSingleMinusChar(accumulator) && c!!.matches(REGEX_ALLOWED_NUMBER) -> null
                isNumberValue(accumulator) && c!!.matches(REGEX_ALLOWED_NUMBER) -> null
                isNumberValue(accumulator) && c != null && isOperator(c) -> Numeric(accumulator)
                isNumberValue(accumulator) && isWhitespace(c) -> Numeric(accumulator)
                isVariable(accumulator) && isVariable(c) -> null
                isVariable(accumulator) && c != null && isOperator(c) -> Variable(accumulator)
                isVariable(accumulator) && isWhitespace(c) -> Variable(accumulator)
                else -> throw Exception("Invalid character $c for accumulator: $accumulator")
            }

        ParenthesisOpen::class.toString() ->
            when {
                else -> throw Exception("Invalid character $c for accumulator: $accumulator")
            }

        else -> throw Exception("Invalid character $c for accumulator: $accumulator")
    }
}

private fun isVariable(c: String?) = c!!.matches(REGEX_VARIABLE)

private fun isWhitespace(c: String?) = c!!.matches(REGEX_WHITESPACE)


private fun isSingleMinusChar(accumulator: String) = accumulator == "-"

private fun accumulatorIsNotEmpty(accumulator: String) = accumulator != ""

private fun isAllMinusChars(accumulator: String) = accumulator.matches(REGEX_ALL_MINUS)

private fun isAllPlusChars(accumulator: String) = accumulator.matches(REGEX_ALL_PLUS)

private fun isAssignment(accumulator: String) = accumulator == "="

private fun isOperator(input: String) = input.matches(REGEX_OPERATORS)

private fun isNumberValue(accumulator: String) = accumulator.matches(REGEX_ALLOWED_NUMBER)

private fun startsWithMinus(accumulator: String, c: Char?) = accumulator == "" && c == '-'

private fun startsWithMinus(accumulator: String, c: String?) = accumulator == "" && c == "-"

private fun isStartOfAssignment(accumulator: String, c: String?) =
    accumulator == "" && c == "="


private fun couldBeNewOperator(accumulator: String, c: Char?) =
    accumulator == "" && c in allowedOperatorChars

private fun couldBeNewOperator(accumulator: String, c: String?) =
    accumulator == "" && c != null && c.matches(REGEX_OPERATORS)




