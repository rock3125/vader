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

import java.text.DecimalFormat

/**
 * Created by Rock de Vocht
 *
 * a vader score element for a sentence
 *
 */
class VScore(
        var positive: Double = 0.0,
        var neutral: Double = 0.0,
        var negative: Double = 0.0,
        var compound: Double = 0.0) {

    // pretty print the scores
    override fun toString(): String {
        val df3 = DecimalFormat("#.###")
        val df4 = DecimalFormat("#.####")
        return "{'neg': " + df3.format(negative) + ", 'neu': " + df3.format(neutral) +
                ", 'pos': " + df3.format(positive) + ", 'compound': " + df4.format(compound) + "}"
    }
}
