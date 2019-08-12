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

import org.apache.commons.io.IOUtils

import java.io.IOException
import java.util.*
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Created by Rock de Vocht on 18/03/16 for Booktrack.com
 *
 * the vader emotional analysis system
 *
 */
class Vader {

    // set of vectors keyed on word + tag e {n,v,a}
    private var moodSet = HashMap<String, Double>()
    private var negatedSet = HashSet<String>()

    /**
     * Analyse a sentence using Vader's algorithm and return a score for that sentence
     * @param sentence the sentence to analyse
     * @return the vader score
     */
    fun analyseSentence(sentence: List<Token>): VScore {
        val isCapsDifferential = isAllCAPDifferential(sentence)
        var sentiments = ArrayList<Double>()
        var i = 0
        val snt = filterPunctuation(sentence)
        for (item in snt) {

            var v = 0.0
            val itemLowercase = item.value.toLowerCase()

            // skip "kind of" and any value already in the booster dictionary
            if (i + 1 < snt.size && itemLowercase == "kind" && wordInSentenceEquals(snt, i + 1, "of") || boosterMap.containsKey(itemLowercase)) {
                sentiments.add(v)
                i++ // next index
                continue
            }

            if (moodSet.containsKey(itemLowercase)) {

                // get sentiment value
                v = moodSet[itemLowercase]!!

                // check if sentiment laden word is in ALLCAPS (while others aren't)
                if (isCapsDifferential && isUpper(item.value)) {
                    if (v > 0.0) {
                        v += c_INCR
                    } else {
                        v -= c_INCR
                    }
                }

                val nScalar = -0.74 // negative scalar

                if (i > 0 && !moodSetContainsSentenceIndex(snt, i - 1)) {
                    val s1 = scalarIncDec(snt[i - 1].value, v, isCapsDifferential)
                    v += s1
                }

                if (i > 1 && !moodSetContainsSentenceIndex(snt, i - 2)) {

                    val s2 = scalarIncDec(snt[i - 2].value, v, isCapsDifferential)
                    v += s2 * 0.95

                    // check for special use of 'never' as valence modifier instead of negation
                    if (wordInSentenceEquals(snt, i - 2, "never") && (wordInSentenceEquals(snt, i - 1, "so") || wordInSentenceEquals(snt, i - 1, "this"))) {

                        v *= 1.5

                    } else if (negated(snt, i - 2)) { //  otherwise, check for negation/nullification
                        v *= nScalar
                    }
                }

                if (i > 2 && !moodSetContainsSentenceIndex(snt, i - 3)) {

                    val s3 = scalarIncDec(snt[i - 3].value, v, isCapsDifferential)
                    v += s3 * 0.9

                    // check for special use of 'never' as valence modifier instead of negation
                    if (wordInSentenceEquals(snt, i - 3, "never") && (wordInSentenceEquals(snt, i - 2, "so") || wordInSentenceEquals(snt, i - 2, "this") || wordInSentenceEquals(snt, i - 1, "so") || wordInSentenceEquals(snt, i - 1, "this"))) {
                        v *= 1.25
                    } else if (negated(snt, i - 3)) {

                        v *= nScalar

                    }

                    // test the special case idioms
                    val idiom = StringBuilder()
                    var index = 0
                    while (index < idiomMaxSize && index < snt.size) {
                        idiom.append(getLcaseWordAt(snt, index + i))
                        val idiomStr = idiom.toString()
                        if (idiomMap.containsKey(idiomStr)) {
                            v = idiomMap[idiomStr]!!
                        }
                        if (boosterMap.containsKey(idiomStr)) {
                            v += B_DECR
                        }
                        idiom.append(" ")
                        index++
                    }

                }

                // check for negation case using "least"
                if (i > 1 && !moodSetContainsSentenceIndex(snt, i - 1) &&
                        wordInSentenceEquals(snt, i - 1, "least")) {
                    if (!wordInSentenceEquals(snt, i - 2, "at") && !wordInSentenceEquals(snt, i - 2, "very")) {
                        v *= nScalar
                    }
                } else if (i > 0 && !moodSetContainsSentenceIndex(snt, i - 1) &&
                        wordInSentenceEquals(snt, i - 1, "least")) {
                    v *= nScalar
                }

            } // if moodSet contains word

            sentiments.add(v)
            i++ // next index

        } // for each item in snt

        // set the sentiment on the tokens
        if (snt.size == sentiments.size) {
            for (j in 0 until snt.size) {
                snt[j].wordScore = sentiments[j]
            }
        }

        // find but in the sentence
        var butIndex = -1
        for (j in 0 until sentence.size) {
            val t = sentence[j]
            if (t.value == "but" || t.value == "BUT") {
                butIndex = j
                break
            }
        }
        if (butIndex >= 0) {
            val newSentiments = ArrayList<Double>()
            for (j in 0 until sentiments.size) {
                if (j < butIndex) {
                    newSentiments.add(sentiments[j] * 0.5)
                } else if (j > butIndex) {
                    newSentiments.add(sentiments[j] * 1.5)
                } else {
                    newSentiments.add(sentiments[j])
                }
            }
            sentiments = newSentiments
        }

        // do the sum of the total
        var sum = 0.0
        for (value in sentiments) {
            sum += value
        }

        // count the number of exclamation marks
        var epCount = 0
        for (t in sentence) {
            if (t.value == "!") {
                epCount += 1
            }
        }
        if (epCount > 4) {
            epCount = 4
        }
        val emAmplifier = epCount.toDouble() * 0.292 // empirically derived mean sentiment intensity rating increase for exclamation points

        if (sum > 0.0) {
            sum += emAmplifier
        } else if (sum < 0.0) {
            sum -= emAmplifier
        }

        // count the number of question marks
        var qmCount = 0
        for (t in sentence) {
            if (t.value == "?") {
                qmCount += 1
            }
        }

        // check for added emphasis resulting from question marks (2 or 3+)
        var qmAmplifier = 0.0
        if (qmCount > 1) {
            if (qmCount <= 3) {
                qmAmplifier = qmCount.toDouble() * 0.18
            } else {
                qmAmplifier = 0.96
            }
            if (sum > 0.0) {
                sum += qmAmplifier
            } else if (sum < 0.0) {
                sum -= qmAmplifier
            }
        }

        val compound = normalize(sum)

        var posSum = 0.0
        var negSum = 0.0
        var neutralCount = 0.0
        for (sentimentScore in sentiments) {
            if (sentimentScore > 0.0) {
                posSum += sentimentScore + 1.0 // compensates for neutral words that are counted as 1
            }
            if (sentimentScore < 0.0) {
                negSum += sentimentScore - 1.0 // when used with math.fabs(), compensates for neutrals
            }
            if (sentimentScore == 0.0) {
                neutralCount += 1
            }
        }

        // adjust amplifiers
        if (posSum > abs(negSum)) {
            posSum += (qmAmplifier + emAmplifier)
        } else if (posSum < abs(negSum)) {
            negSum -= (qmAmplifier + emAmplifier)
        }

        val total = posSum + abs(negSum) + neutralCount
        if (total > 0.0) { // make sure values are valid
            posSum = abs(posSum / total)
            negSum = abs(negSum / total)
            neutralCount = abs(neutralCount / total)
        } else {
            posSum = 0.0
            negSum = 0.0
            neutralCount = 0.0
        }
        return VScore(posSum, neutralCount, negSum, compound)
    }

    /**
     * check if the word in sentence @ index is in the moodSet or not
     * @param sentence the sentence to check
     * @param index the index
     * @return true if the word is in the mood-set
     */
    private fun moodSetContainsSentenceIndex(sentence: List<Token>, index: Int): Boolean {
        if (index >= 0 && index < sentence.size) {
            val t = sentence[index]
            return moodSet.containsKey(t.value.toLowerCase())
        }
        return false
    }

    /**
     * return true if the word in sentence @ index equals wordStr (case insensitive)
     * @param sentence the sentence to check
     * @param index the index of the word
     * @param wordStr the word to check for
     * @return true if the word is there
     */
    private fun wordInSentenceEquals(sentence: List<Token>, index: Int, wordStr: String): Boolean {
        if (index >= 0 && index < sentence.size) {
            val t = sentence[index]
            return t.value.compareTo(wordStr, ignoreCase = true) == 0
        }
        return false
    }

    /**
     * return a word at index lowercased
     * @param sentence the sentence to get it from
     * @param index the index of the word
     * @return the word at index, lower-cased, or empty string if not a word
     */
    private fun getLcaseWordAt(sentence: List<Token>, index: Int): String {
        if (index >= 0 && index < sentence.size) {
            val t = sentence[index]
            return t.value.toLowerCase()
        }
        return ""
    }

    /**
     * load vader from class-path
     * @throws IOException
     */
    fun init() {

        println("Vader: init lexicon(vader_sentiment_lexicon.txt)")
        moodSet = HashMap()
        Vader::class.java.getResourceAsStream("vader_sentiment_lexicon.txt").use { vaderIn ->
            if (vaderIn == null) {
                throw IOException("vader_sentiment_lexicon.txt not found on class-path")
            }
            val vaderLexicon = String(IOUtils.toByteArray(vaderIn))
            if (vaderLexicon.isNotEmpty()) {
                for (line in vaderLexicon.split("\n")) {
                    val items = line.split("\t")
                    if (items.size > 2) {
                        moodSet[items[0].trim()] = items[1].trim().toDouble()
                    } else {
                        println("skipping invalid Vader line: $line")
                    }
                }
            }
        }

        // setup booster dict
        for (incr in BoosterIncrementList) {
            boosterMap[incr] = B_INCR
        }
        for (decr in BoosterDecreaseList) {
            boosterMap[decr] = B_DECR
        }

        // add the special case idioms
        println("Vader: init idioms(vader_idioms.txt)")
        Vader::class.java.getResourceAsStream("vader_idioms.txt").use { vaderIdiomsIn ->
            if (vaderIdiomsIn == null) {
                throw IOException("vader_idioms.txt not found on class-path")
            }
            val vaderIdiomContent = String(IOUtils.toByteArray(vaderIdiomsIn))
            if (vaderIdiomContent.isNotEmpty()) {
                for (line in vaderIdiomContent.split("\n")) {
                    val items = line.split(",")
                    if (items.size == 2) {
                        idiomMap[items[0].trim()] = items[1].trim().toDouble()
                    }
                }
            }
        }

        negatedSet = HashSet()
        for (str in NEGATE) {
            negatedSet.add(str)
        }
    }

    /**
     * return true if the sentence has a negation in it
     * @param sentence the sentence to check
     * @return true if negated
     */
    private fun negated(sentence: List<Token>, index: Int): Boolean {
        val t = sentence[index]
        val lCaseWord = t.value.toLowerCase()

        // anything in the negatedSet is a negator
        if (negatedSet.contains(lCaseWord)) {

            // exceptions for don't and dont "know", or "like"
            // can't take/feel
            if (index + 1 < sentence.size) {
                val lCaseWord2 = sentence[index + 1].value.toLowerCase()
                if (lCaseWord2 == "know" || lCaseWord2 == "take" || lCaseWord2 == "feel" || lCaseWord2 == "like" ||
                        lCaseWord2 == "want" || lCaseWord2 == "wanna") {
                    return false
                }
            }
            return true
        }
        // any "couldn't" modal is a negator
        if (lCaseWord.contains("n't")) {
            return true
        }
        // "at least" is a negator
        if (lCaseWord == "least" && index > 0) {
            if (wordInSentenceEquals(sentence, index - 1, "at")) {
                return true
            }
        }
        return false
    }

    /**
     * normalize a score using an alpha magic value
     * @param score the score to normalize
     * @return the normalized score
     */
    private fun normalize(score: Double): Double {
        return score / sqrt(score * score + ALPHA)
    }

    /**
     * return true if str does not contain any lower case characters a..z
     * @param str the string to check
     * @return true if there are no lower case characters in this string
     */
    private fun isUpper(str: String?): Boolean {
        if (str != null) {
            for (ch in str.toCharArray()) {
                if (ch in 'a'..'z') {
                    return false
                }
            }
            return true
        }
        return false
    }

    /**
     * return a differential of all the caps, meaning that if the sentence is all
     * caps it doesn't count, but if any one word (or n-1 words) are caps, return true
     * @param sentence the sentence to check
     * @return true if one or more, but not all words are caps
     */
    private fun isAllCAPDifferential(sentence: List<Token>): Boolean {
        var countAllCaps = 0
        for (t in sentence) {
            if (isUpper(t.value)) {
                countAllCaps += 1
            }
        }
        val capsDifferential = sentence.size - countAllCaps
        return capsDifferential > 0 && capsDifferential < sentence.size
    }

    /**
     * word out an individual word's scalar given a valance and a the sentence's isCaps diff
     * @param word the word to check
     * @param valence its valence value
     * @param isCapsDifferential the is diff of the sentence
     * @return an emotional scalar value for this word
     */
    private fun scalarIncDec(word: String, valence: Double, isCapsDifferential: Boolean): Double {
        var scalar = 0.0
        val lcase = word.toLowerCase()
        if (boosterMap.containsKey(lcase)) {
            scalar = boosterMap[lcase]!!
            if (valence < 0) {
                scalar *= -1.0
            }
            // check if booster/dampener word is in ALLCAPS (while others aren't)
            if (isUpper(word) && isCapsDifferential) {
                if (valence > 0.0) {
                    scalar += c_INCR
                } else {
                    scalar -= c_INCR
                }
            }
        }
        return scalar
    }

    /**
     * return a sentence without any punctuation in it - assume that all
     * punctuation are characters of length 1, not entirely correct but it
     * helps filter out all the little niggly noise words like "a" and "i" too
     * @param sentence the sentence to check
     * @return a sentence without any of the punctuation marks in it
     */
    private fun filterPunctuation(sentence: List<Token>): List<Token> {
        val newSentence = ArrayList<Token>()
        for (t in sentence) {
            if (t.value.trim().isNotEmpty()) {
                newSentence.add(t)
            }
        }
        return newSentence
    }

    companion object {

        // empirically derived mean sentiment intensity rating increase for booster words
        private const val B_INCR = 0.293
        private const val B_DECR = -0.293

        // alpha normalization value
        private const val ALPHA = 15.0

        // empirically derived mean sentiment intensity rating increase for using ALLCAPs to emphasize a word
        private const val c_INCR = 0.733

        // the maximum number of words in an idiom
        private const val idiomMaxSize = 5

        // negations of mood
        private val NEGATE = arrayOf("aint", "arent", "cannot", "cant", "couldnt", "darent", "didnt", "doesnt", "ain't", "aren't", "can't", "couldn't", "daren't", "didn't", "doesn't", "dont", "hadnt", "hasnt", "havent", "mightnt", "mustnt", "neither", "don't", "hadn't", "hasn't", "haven't", "isn't", "isnt", "mightn't", "mustn't", "neednt", "needn't", "never", "none", "nope", "nor", "not", "nothing", "nowhere", "oughtnt", "shant", "shouldnt", "uhuh", "wasnt", "werent", "oughtn't", "shan't", "shouldn't", "uh-uh", "wasn't", "weren't", "without", "wont", "wouldnt", "won't", "wouldn't", "rarely", "seldom", "despite")

        // items in the boosterMap that need an increment in sentiment when seen
        private val BoosterIncrementList = arrayOf("absolutely", "amazingly", "awfully", "completely", "considerably", "decidedly", "deeply", "effing", "enormously", "entirely", "especially", "exceptionally", "extremely", "fabulously", "flipping", "flippin", "fricking", "frickin", "frigging", "friggin", "fully", "fucking", "greatly", "hella", "highly", "hugely", "incredibly", "intensely", "majorly", "more", "most", "particularly", "purely", "quite", "really", "remarkably", "so", "substantially", "thoroughly", "totally", "tremendously", "uber", "unbelievably", "unusually", "utterly", "very")

        // items in the boosterMap that need an decrement in sentiment when seen
        private val BoosterDecreaseList = arrayOf("almost", "barely", "hardly", "just enough", "kind of", "kinda", "kindof", "kind-of", "less", "little", "marginally", "occasionally", "partly", "scarcely", "slightly", "somewhat", "sort of", "sorta", "sortof", "sort-of")

        private val boosterMap = HashMap<String, Double>()

        // check for special case idioms using a sentiment-laden keyword known to SAGE
        private val idiomMap = HashMap<String, Double>()
    }


}

