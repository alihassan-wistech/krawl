package com.krawl

import java.io.File
import org.jsoup.Jsoup
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.BufferedReader

@Serializable
data class TermFreq(
    val term: String,
    val freq: Int
)

@Serializable
data class TermFreqPerDoc (
    val file: String,
    val termFreq: List<TermFreq> 
)

@Serializable
data class DocumentIndex (
    val tf: List<TermFreqPerDoc>,
    val idf: List<TermFreqPerDoc>
)

fun main(args: Array<String>) {

    var arg = ""
    if(args.size > 0){
        arg = args[0]
    }else{
        showValidCommands()
        return
    }

    if(arg == "index") {
        indexDocuments()
    } else if(arg == "search"){
        commandLineSearch()
    }else{
        showValidCommands()
        return
    }
}

fun indexDocuments (){
    val dirPath = "/home/ali/Documents/docs";
    var filesProcessedCount = 0
    val termFreqPerDoc = mutableMapOf<File, MutableMap<String, Int>>()    
    val documents = mutableListOf<TermFreqPerDoc>()

    try {
        val root = File(dirPath)
        root.walk()
            .filter { it.isFile }
            .forEach { file ->
                processFile(file, termFreqPerDoc)
                filesProcessedCount++
            }

        for ((file, termFreq) in termFreqPerDoc) {
            val termFreqData = mutableListOf<TermFreq>()
            for ((word, count) in  termFreq) {
                termFreqData.add(TermFreq(word, count))
            }

            val termFreqPerDocData = TermFreqPerDoc(file.absolutePath, termFreqData)
            documents.add(termFreqPerDocData)
        }

        val documentIndex = DocumentIndex(documents, documents)

        val jsonFile = File("index.json")
        jsonFile.writeText(Json.encodeToString(documentIndex))
        println("TOTAL FILES PROCESSED => $filesProcessedCount")

    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun readDirectory(dirPath: String, files: MutableList<File>): List<File> {
    val directory = File(dirPath);
    println("READING DIRECTORY => ${directory.absolutePath}")

    if (!directory.exists() || !directory.isDirectory) {
        throw Exception("Directory does not exist or isn't directory");
    }

    directory.listFiles().forEach{
        file -> files.add(file)
    };

    return files
}

fun processFile(file: File, termFreqPerDoc: MutableMap<File, MutableMap<String, Int>>) {
    println("PROCESSING FILE => ${file.absolutePath}")
    val tokens = tokenize(getTextContent(file))
    if (tokens.isEmpty()) return
    
    val fileMap = termFreqPerDoc.getOrPut(file) { mutableMapOf() }
    for (token in tokens) {
        fileMap[token] = termFreqPerDoc[file]?.getOrDefault(token, 0)!! + 1
    }
    
}

fun getTextContent(file: File) : String{
    val extension = file.name.substringAfterLast(".");
    var content = ""
    if (extension == "html") {
        content = parseHtmlFile(file)
    }

    return content
}

fun parseHtmlFile(file: File) : String {    
    var content = "";
    try {
        content = Jsoup.parse(file).body().text();
    }catch(ex: Exception){
        ex.printStackTrace()
    }

    return content
}

fun tokenize(content: String): List<String> {
    return content.uppercase()
        .split(Regex("[^a-zA-Z0-9]+")) // Split by anything that isn't a letter or number
        .filter { it.length > 1 }      // Ignore single letters like "a"
}

fun printData() {
    val file = File("index.json")
    val bufferedReader: BufferedReader = file.bufferedReader()
    val inputString = bufferedReader.use { it.readText() }
    println(Json.decodeFromString<DocumentIndex>(inputString))
}

fun loadData() : DocumentIndex {
    val file = File("index.json")
    val bufferedReader: BufferedReader = file.bufferedReader()
    val inputString = bufferedReader.use { it.readText() }
    return Json.decodeFromString<DocumentIndex>(inputString)
}

fun showValidCommands(){
    println("PLEASE PROVIDE AN ARGUMENT")
    println("VALID ARGUMENTS ARE FOLLOWING:")
    println("   - 'index' => FOR INDEXING THE DOCUMENTS")
    println("   - 'search' => FOR SEARCHING THROUGH THE INDEX")
}

fun commandLineSearch(){
    val documentIndex = loadData()
    var searchTerm = ""

    while(searchTerm != "q"){
        print("PLEASE ENTER SEARCH TERM OR 'q' TO EXIT => ")
        searchTerm = readln().trim()

        if(searchTerm == "q"){
            continue
        }

        var searchTokens = tokenize(searchTerm)
        println("RESULTS FOR => ${searchTokens}")
        
        val results = mutableListOf<MutableMap<String, Int>>()

        for (termFreqPerDoc in documentIndex.tf) {
            val filePath = termFreqPerDoc.file

            for (termFreq in termFreqPerDoc.termFreq) {
                if(searchTokens.indexOf(termFreq.term) != -1){
                    results.add(mutableMapOf<String, Int>(Pair(filePath, termFreq.freq)))
                }
            }
        }

                

        if(results.size > 0){
            val resultsAfterScore = mutableMapOf<String, Float>()
            for (file in results) {
                val filePath = file.keys.first()
                val freq = file.values.first()
                val tf = freq.toFloat() / results.size

                resultsAfterScore[filePath] = if (resultsAfterScore[filePath] != null) {
                    resultsAfterScore[filePath]!! + tf
                } else {
                    tf
                }
            }

            val sortedList = resultsAfterScore.toList().sortedByDescending { (_, value) -> value }
            for ((key, value) in sortedList.take(10)) {
                val value = resultsAfterScore[key]
                println(" => ${key} => ${value}")
            }

            println(" => ${results.size} RESULTS FOUND")
        }else{
            println("NO RESULTS FOUND")
        }
    }
}