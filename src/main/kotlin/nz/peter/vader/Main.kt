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

import org.apache.commons.cli.*

import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.system.exitProcess

/**
 * main entry point and demo case for Vader
 * @param args the arguments - explained below in the code
 */
fun main(args: Array<String>) {

    // create Options object for command line parsing
    val options = Options()
    options.addOption("file", true, "input text-file (-file) to read and analyse using Vader")

    val cmdParser = DefaultParser()
    var line: CommandLine? = null
    try {
        // parse the command line arguments
        line = cmdParser.parse(options, args)
    } catch (exp: ParseException) {
        // oops, something went wrong
        println("invalid command line: ${exp.message}")
        exitProcess(-1)
    }

    // get the command line argument -file
    val inputFile = line!!.getOptionValue("file")
    if (inputFile == null) {
        help(options)
        exitProcess(0)
    }
    if (!File(inputFile).exists()) {
        println("file does not exist: $inputFile")
        exitProcess(-1)
    }

    // example use of the classes
    // read the entire input file
    val fileText = String(Files.readAllBytes(Paths.get(inputFile)))

    // setup Vader
    val vader = Vader()
    vader.init() // load vader

    // setup nlp processor
    val vaderNLP = VaderNLP()
    vaderNLP.init() // load open-nlp

    // parse the text into a set of sentences
    val sentenceList = vaderNLP.parse(fileText)

    // apply vader analysis to each sentence
    for (sentence in sentenceList) {
        val vaderScore = vader.analyseSentence(sentence)
        println("sentence: ${Token.tokenListToString(sentence)}")
        println("Vader score: $vaderScore")
    }

}

/**
 * display help for the command line
 * @param options the options file of the command line system
 */
private fun help(options: Options) {
    println(options.getOption("file").getDescription())
}
