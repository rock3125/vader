/*
    The MIT License (MIT)

    Copyright (c) 2014 cjhutto
    Copyright (c) 2019 Rock de Vocht

    Permission is hereby granted, free of charge, to any person obtaining a copy
    of this software and associated documentation files (the "Software"), to deal
    in the Software without restriction, including without limitation the rights
    to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
    copies of the Software, and to permit persons to whom the Software is
    furnished to do so, subject to the following conditions:

    The above copyright notice and this permission notice shall be included in all
    copies or substantial portions of the Software.

    THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
    IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
    FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
    AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
    LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
    OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
    SOFTWARE.

 */
package nz.peter.vader

/**
 * Created by Rock de Vocht
 *
 * a token, usually a single word, with POS (part of speech) tag
 *
 */
class Token(var value: String, private var posTag: String) {

    var wordScore = 0.0 // the vader individual word score

    // pretty print
    override fun toString(): String {
        return "$value:$posTag"
    }

    companion object {

        /**
         * convert a list of tokens to a readable string / very crude
         * for demo purposes
         * @param tokenList the list to convert
         * @return a string for the list
         */
        fun tokenListToString(tokenList: List<Token>): String {
            val sb = StringBuilder()
            for (token in tokenList) {
                sb.append(token.value).append(" ")
            }
            return sb.toString()
        }
    }

}

