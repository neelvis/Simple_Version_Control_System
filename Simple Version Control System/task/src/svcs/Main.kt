package svcs
import java.io.File
import java.io.FileNotFoundException
import java.lang.Exception
import java.security.MessageDigest
import kotlin.math.log

class SVCS(private val args: Array <String>) {
    private enum class Commands(val description: String) {
        CONFIG("Get and set a username."),
        ADD("Add a file to the index."),
        LOG("Show commit logs."),
        COMMIT("Save changes."),
        CHECKOUT("Restore a file.");

        companion object {
            // const val resPath = "/home/pixel/IdeaProjects/Simple Version Control System/Simple Version Control System/task/src/main/resources"
            val workingDir: String = File("").absolutePath
            val configFile = File("$workingDir/vcs/config.txt")
            val indexFile = File("$workingDir/vcs/Index.txt")
            val logFile = File("$workingDir/vcs/log.txt")
            fun initFiles() {
                File("$workingDir/vcs").mkdirs()
                if (!configFile.exists()) configFile.createNewFile()
                if (!indexFile.exists()) indexFile.createNewFile()
                if (!logFile.exists()) logFile.createNewFile()
            }
            fun contains(s: String): Boolean {
                values().forEach {
                    if (it.name == s) return true
                }
                return false
            }
            fun execute(command: String, parameter: String): String {
                initFiles()
                return when(valueOf(command)) {
                    CONFIG -> executeConfig(parameter)
                    ADD -> executeAdd(parameter)
                    COMMIT -> executeCommit(parameter)
                    LOG -> executeLog(parameter)
                    CHECKOUT -> executeCheckout(parameter)
                    else -> executeDefault(command)
                }
            }
            private fun executeDefault(arg: String): String {
                return valueOf(arg).description
            }
            private fun executeConfig(arg: String): String {
                val configName = configFile.readText()
                return if (arg.isEmpty()) {
                    if (configName.isEmpty()) {
                        "Please, tell me who you are."
                    } else {
                        "The username is $configName."
                    }
                } else {
                        configFile.writeText(arg)
                        "The username is $arg."
                }
            }
            private fun executeAdd(arg: String): String {
                val indexedList = indexFile.readLines()
                return if (arg.isEmpty()) {
                     if (indexedList.isEmpty()) {
                        ADD.description
                    } else {
                        var result = "Tracked files:\n"
                        indexedList.forEach {
                            result += "$it\n"
                        }
                        result
                    }
                } else {
                    if (File("$workingDir/$arg").exists()) {
                        indexFile.appendText("$arg\n")
                        "The File '$arg' is tracked."
                    } else {
                        "Can't found '$arg'."
                    }
                }
            }
            private fun executeCommit(arg: String): String {
                if (arg == "") return "Message was not passed."
                val logFile = logFile
                val logLines = logFile.readLines()
                val indexedList = indexFile.readLines()
                val configName = configFile.readText()
                val commitID = makeCommitID()
                if (logLines.isEmpty() || logLines.isNotEmpty() && filesChanged(logLines.last().substringAfter(" "), indexedList)) {
                    logFile.appendText("\n$arg\n")
                    logFile.appendText("Author: $configName\n")
                    logFile.appendText("commit $commitID")
                    val commitPath = "$workingDir/vcs/commits/$commitID" // here
                    if (!File(commitPath).exists()) {
                        File(commitPath).mkdirs()
                    }
                    indexedList.forEach {
                        val currentFile = File("$workingDir/$it")
                        val newFile = File("$workingDir/vcs/commits/$commitID/$it")
                        try {
                            newFile.writeText(currentFile.readText())
                        } catch (e: FileNotFoundException) {
                            //println("File not found! $currentFile $newFile")
                        }
                    }
                    return "Changes are committed."
                }
                return "Nothing to commit."
            }
            private fun executeCheckout(arg: String): String {
                if (arg == "") return "Commit id was not passed."
                if (!File("$workingDir/vcs/commits/$arg").exists()) return "Commit does not exist."

                val indexedList = indexFile.readLines()
                indexedList.forEach {
                    val currentFile = File("$workingDir/$it")
                    val newFile = File("$workingDir/vcs/commits/$arg/$it")
                    try {
                        currentFile.writeText(newFile.readText())
                    } catch (e: FileNotFoundException) {
                        //println("File not found! $currentFile $newFile")
                    }
                }
                return "Switched to commit $arg."
            }
            private fun executeLog(arg: String): String {
                val logLines = logFile.readLines()
                var result = "No commits yet."
                if (logLines.isNotEmpty()) {
                    val log = mutableListOf<String>()
                    logLines.forEach {
                        log.add(it)
                    }
                    result = log.reversed().joinToString("\n")
                }
                return result
            }
            private fun filesChanged(lastCommitID: String, indexedList: List<String>): Boolean {
                indexedList.forEach {
                    try {
                        val digest1 = MessageDigest.getInstance("SHA-256")
                        val digest2 = MessageDigest.getInstance("SHA-256")
                        val currentFile = File("$workingDir/$it").readText() // here
                        val commitedFile = File("$workingDir/vcs/commits/$lastCommitID/$it").readText()
                        if (!digest1.digest(currentFile.toByteArray())
                                .contentEquals(digest2.digest(commitedFile.toByteArray()))) return true
                    } catch (e: FileNotFoundException) {

                    }
                }
                return false
            }
            private fun makeCommitID(): String {
                val digest = MessageDigest.getInstance("SHA-256")
                val time = System.currentTimeMillis().toString()
                val data = time.toByteArray(Charsets.UTF_8)
                return digest.digest(data).toUByteArray().joinToString("") { it.toString(16) }
            }
        }
    }
    private fun getHelp(): String {
        var result = "These are SVCS commands:\n"
        Commands.values().forEach {
            result += "${it.name.padEnd(10, ' ')}${it.description}\n"
        }
        return result
    }
    fun make(): String {
        val command = args[0].toUpperCase()
        val parameter = if (args.size > 1) args[1] else ""
        return when {
            command == "--HELP" -> getHelp()
            Commands.contains(command) -> Commands.execute(command, parameter)
            else -> "'${args[0]}' is not a SVCS command."
        }
    }
}
fun main(args: Array<String>) {
    val svcs = SVCS(if(args.isNotEmpty()) args else Array <String> (1) { "--help" })
    print(svcs.make())
}