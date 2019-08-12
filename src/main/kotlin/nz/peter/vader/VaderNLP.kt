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

import opennlp.tools.postag.POSModel
import opennlp.tools.postag.POSTaggerME
import opennlp.tools.sentdetect.SentenceDetector
import opennlp.tools.sentdetect.SentenceDetectorME
import opennlp.tools.sentdetect.SentenceModel
import opennlp.tools.tokenize.Tokenizer
import opennlp.tools.tokenize.TokenizerME
import opennlp.tools.tokenize.TokenizerModel

import java.io.IOException
import java.util.ArrayList

/**
 * Created by Rock de Vocht
 *
 * simple tokenizer and pos tagger using OpenNLP
 *
 */
class VaderNLP {

    // the apache-nlp sentence boundary detector
    private var sentenceDetector: SentenceDetector? = null

    // the apache-nlp penn-tree tagger
    private var posTagger: POSTaggerME? = null

    // apache-nlp tokenizer
    private var tokenizer: Tokenizer? = null

    /**
     * convert a piece of text to a list of parsed tokens with POS tags
     * @param text the text to parse
     * @return a list of sentences (each sentence a list of tokens itself) that is the entire text
     * @throws IOException if things don't go as planned
     */
    fun parse(text: String): ArrayList<ArrayList<Token>> {

        val sentenceList = ArrayList<ArrayList<Token>>()

        // this is how it works boys and girls - apache-opennlp
        val sentenceArray = getSentences(text)
        for (sentenceStr in sentenceArray) {

            val sentence = ArrayList<Token>()

            // get the results of the syntactic parse
            val words = getTokens(sentenceStr)
            val posTags = getTags(words)

            // the number of tags should always match the number of words - a little primitive
            // how open-nlp treats it
            if (words.size != posTags.size) {
                throw IOException("unmatched words / posTags in nlp-parser")
            }

            // add this sentence - the first word in the sentence gets the "is a sentence start" marker
            for (i in words.indices) {
                sentence.add(Token(words[i], posTags[i]))
            }

            sentenceList.add(sentence)
        }

        return sentenceList
    }

    /**
     * invoke the OpenNLP sentence detector to split text into sentences
     * @param text the text to split
     * @return a set of string representing nlp sentences
     */
    private fun getSentences(text: String): Array<String> {
        return sentenceDetector!!.sentDetect(text)
    }

    /**
     * turn a sentence into a set of tokens (split words and punctuation etc)
     * @param sentence a string that is a sentence
     * @return a set of tokens from that sentence in order
     */
    private fun getTokens(sentence: String): Array<String> {
        return tokenizer!!.tokenize(sentence)
    }

    /**
     * use a pos-tagger to get the set of penn-tree tags for a given set of tokens
     * that form a sentence
     * @param tokens a sentence split into tokens
     * @return a set of penn-tags
     */
    private fun getTags(tokens: Array<String>): Array<String> {
        return posTagger!!.tag(tokens)
    }

    /**
     * initialise the parser and its constituents - called from spring init
     * @throws IOException
     */
    fun init() {

        println("VaderNLP: init()")

        // setup the sentence splitter
        println("VaderNLP: loading en-sent.bin")
        VaderNLP::class.java.getResourceAsStream("en-sent.bin").use { modelIn ->
            if (modelIn == null) {
                throw IOException("resource en-sent.bin not found in classpath")
            }
            val sentenceModel = SentenceModel(modelIn)
            sentenceDetector = SentenceDetectorME(sentenceModel)
        }

        // setup the max-ent tokenizer
        println("VaderNLP: loading en-token.bin")
        VaderNLP::class.java.getResourceAsStream("en-token.bin").use { modelIn ->
            if (modelIn == null) {
                throw IOException("resource en-sent.bin not found in classpath")
            }
            val tokenizerModel = TokenizerModel(modelIn)
            tokenizer = TokenizerME(tokenizerModel)
        }

        // setup the pos tagger
        println("VaderNLP: loading en-pos-maxent.bin")
        VaderNLP::class.java.getResourceAsStream("en-pos-maxent.bin").use { modelIn ->
            if (modelIn == null) {
                throw IOException("resource en-sent.bin not found in classpath")
            }
            val posModel = POSModel(modelIn)
            posTagger = POSTaggerME(posModel)
        }

    }


}

