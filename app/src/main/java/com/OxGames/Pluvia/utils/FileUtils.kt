package com.OxGames.Pluvia.utils

import android.util.Log
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.FileReader
import java.io.OutputStreamWriter
import java.nio.file.Files
import java.nio.file.Path
import java.util.stream.Stream
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.name

class FileUtils {
    companion object {

        fun makeDir(dirName: String) {
            val homeItemsDir = File(dirName)
            homeItemsDir.mkdirs()
        }

        fun makeFile(fileName: String, errorTag: String? = "FileUtils", errorMsg: ((Exception) -> String)? = null) {
            try {
                val file = File(fileName)
                if (!file.exists()) {
                    file.createNewFile()
                }
            } catch (e: Exception) {
                Log.e(errorTag, errorMsg?.invoke(e) ?: "Error creating file: $e")
            }
        }

        fun createPathIfNotExist(filepath: String) {
            val file = File(filepath)
            var dirs = filepath

            // if the file path is not a directory and if we're not at the root directory then get the parent directory
            if (!filepath.endsWith('/') && filepath.lastIndexOf('/') > 0) {
                dirs = file.parent!!
            }

            makeDir(dirs)
        }

        fun readFileAsString(path: String, errorTag: String = "FileUtils", errorMsg: ((Exception) -> String)? = null): String? {
            var fileData: String? = null
            try {
                val r = BufferedReader(FileReader(path))
                val total = StringBuilder()
                var line: String?

                while ((r.readLine().also { line = it }) != null) {
                    total.append(line).append('\n')
                }

                fileData = total.toString()
            } catch (e: Exception) {
                Log.e(errorTag, errorMsg?.invoke(e) ?: "Error reading file: $e")
            }

            return fileData
        }

        fun writeStringToFile(data: String, path: String, errorTag: String? = "FileUtils", errorMsg: ((Exception) -> String)? = null) {
            createPathIfNotExist(path)

            try {
                val fOut = FileOutputStream(path)
                val myOutWriter = OutputStreamWriter(fOut)
                myOutWriter.append(data)
                myOutWriter.close()
                fOut.flush()
                fOut.close()
            } catch (e: Exception) {
                Log.e(errorTag, errorMsg?.invoke(e) ?: "Error writing to file: $e")
            }
        }

        /**
         * Traverse through a directory and perform an action on each file
         *
         * @param rootPath The start path
         * @param maxDepth How deep to go in the directory tree, a value of -1 keeps going
         * @param action The action to perform on each file
         */
        fun walkThroughPath(rootPath: Path, maxDepth: Int = 0, action: (Path) -> Unit) {
            Files.list(rootPath).forEach {
                action(it)
                if (maxDepth != 0 && it.exists() && it.isDirectory()) {
                    walkThroughPath(
                        rootPath = it,
                        maxDepth = if (maxDepth > 0) maxDepth - 1 else maxDepth,
                        action = action,
                    )
                }
            }
        }

        fun findFiles(rootPath: Path, pattern: String, includeDirectories: Boolean = false): Stream<Path> {
            val patternParts = pattern.split("*").filter { it.isNotEmpty() }
            Log.d("FileUtils", "$pattern -> $patternParts")
            if (!Files.exists(rootPath)) return emptyList<Path>().stream()
            return Files.list(rootPath).filter { path ->
                if (path.isDirectory() && !includeDirectories) {
                    false
                } else {
                    val fileName = path.name
                    Log.d("FileUtils", "Checking $fileName for pattern $pattern")
                    var startIndex = 0
                    !patternParts.map {
                        val index = fileName.indexOf(it, startIndex)
                        if (index >= 0) {
                            startIndex = index + it.length
                        }
                        index
                    }.any { it < 0 }
                }
            }
        }
    }
}
