package com.github.jk1.ytplugin.scriptsDebug

import com.google.common.base.Joiner
import com.google.common.collect.BiMap
import com.google.common.collect.ImmutableBiMap
import com.intellij.javascript.debugger.*
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.impl.http.HttpVirtualFile
import com.intellij.pom.Navigatable
import com.intellij.util.Url
import com.intellij.util.Urls
import com.github.jk1.ytplugin.logger

class RemoteDebuggingFileFinder(
    private var mappings: BiMap<String, VirtualFile> = ImmutableBiMap.of(),
    private val parent: DebuggableFileFinder? = null,
    private val rootFolderName: String,
    private val instanceFolderName: String
) : DebuggableFileFinder {

    private lateinit var myProject: Project

    init {
        logger.info("Remote File Finder is initialized")
    }

    override fun findNavigatable(url: Url, project: Project): Navigatable? {
        logger.info("find navigatable file ${url.path}")

        myProject = project
        findMapping(url, project)?.let {
            return JsFileUtil.createNavigatable(project, it)
        }
        return parent?.findNavigatable(url, project)
    }

    override fun findFile(url: Url, project: Project): VirtualFile? {
        logger.info("find file ${url.path}")

        myProject = project

        return findByMappings(url, mappings)
    }

    override fun guessFile(url: Url, project: Project): VirtualFile? {
        logger.info("guess file ${url.path}")

        parent?.findFile(url, project)?.let {
            return it
        }
        val predefinedMappings = mappings

        return findByMappings(url, predefinedMappings) ?: parent?.guessFile(url, project)
    }

    override fun searchesByName(): Boolean = true

    override fun getRemoteUrls(file: VirtualFile): List<Url> {
        logger.info("Get remote urls for: ${file.name}")
        if (file !is HttpVirtualFile && !mappings.isEmpty()) {
            var current: VirtualFile? = file
            val map = mappings.inverse()
            while (current != null) {
                val url = map[current]
                if (url != null) {
                    if (current == file) {
                        return listOf(Urls.newFromIdea(url))
                    }
                    return listOf(Urls.newFromIdea("$url/${VfsUtilCore.getRelativePath(file, current, '/')}"))
                }
                current = current.parent
            }
        }
        return parent?.getRemoteUrls(file) ?: listOf(Urls.newFromVirtualFile(file))
    }

    override fun toString(): String = Joiner.on("\n ").withKeyValueSeparator("->").join(mappings)

    private fun findMapping(parsedUrl: Url, project: Project): VirtualFile? {

        logger.debug("Find file mapping for: ${parsedUrl.path}")

        val url = parsedUrl.trimParameters().toDecodedForm()
        val filename = if (url.split("/").size > 1) {
            url.split("/")[url.split("/").lastIndex - 1] + "/" + url.split("/").last()
        } else {
            url
        }
        logger.info("File mapping is found: $filename \n \n")

        val systemIndependentPath: String = FileUtil.toSystemIndependentName(project.guessProjectDir()
            ?.findFileByRelativePath("$rootFolderName/$instanceFolderName/@jetbrains/$filename").toString())

        val projectBaseDir: VirtualFile? = project.guessProjectDir()
        val child = if (systemIndependentPath.isEmpty()) {
            projectBaseDir
        } else {
            projectBaseDir?.findFileByRelativePath("$rootFolderName/$instanceFolderName/@jetbrains/$filename")
        }

        if (child != null) {
            return child
        }
        return null
    }

    private fun findByMappings(parsedUrl: Url, mappings: BiMap<String, VirtualFile>): VirtualFile? {
        if (mappings.isEmpty()) {
            return null
        }

        val url = parsedUrl.trimParameters().toDecodedForm()
        var i = url.length
        while (i != -1) {
            val prefix = url.substring(0, i)
            val file = mappings[prefix]
            if (file != null) {
                if (i == url.length) {
                    return file
                }
                if (i + 1 == url.length) {
                    // empty string, try to find index file
                    val indexFile = org.jetbrains.builtInWebServer.findIndexFile(file)
                    if (indexFile == null) {
                        break
                    } else {
                        return indexFile
                    }
                }

                val filename = url.substring(i + 1)
                val child = file.findFileByRelativePath(filename)
                if (child != null) {
                    return child
                }
                break
            }
            i = url.lastIndexOf('/', i - 1)
        }
        return null
    }

}


