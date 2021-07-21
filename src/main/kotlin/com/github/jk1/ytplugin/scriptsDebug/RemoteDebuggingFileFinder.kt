package com.github.jk1.ytplugin.scriptsDebug

import com.google.common.base.Joiner
import com.google.common.collect.BiMap
import com.google.common.collect.ImmutableBiMap
import com.intellij.javascript.debugger.*
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.impl.http.HttpVirtualFile
import com.intellij.pom.Navigatable
import com.intellij.util.Url
import com.intellij.util.Urls
import com.github.jk1.ytplugin.logger
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.DumbService

private val PREDEFINED_MAPPINGS_KEY: Key<BiMap<String, VirtualFile>> = Key.create("js.debugger.predefined.mappings")

class RemoteDebuggingFileFinder(
    private var mappings: BiMap<String, VirtualFile> = ImmutableBiMap.of(),
    private val parent: DebuggableFileFinder? = null
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

        val myExpandedPreviewFuture = ApplicationManager.getApplication().executeOnPooledThread(Runnable {
            DumbService.getInstance(project).waitForSmartMode()
        })
        myExpandedPreviewFuture.get()
        myProject = project

        return findByMappings(url, mappings)
    }

    override fun guessFile(url: Url, project: Project): VirtualFile? {
        logger.info("guess file ${url.path}")
        val myExpandedPreviewFuture = ApplicationManager.getApplication().executeOnPooledThread(Runnable {
            DumbService.getInstance(project).waitForSmartMode()
        })
        myExpandedPreviewFuture.get()

        parent?.findFile(url, project)?.let {
            return it
        }
        var predefinedMappings = project.getUserData(PREDEFINED_MAPPINGS_KEY)
        if (predefinedMappings == null) {
            predefinedMappings = createPredefinedMappings(project)
            project.putUserData(PREDEFINED_MAPPINGS_KEY, predefinedMappings)
        }

        return findByMappings(url, predefinedMappings) ?: parent?.guessFile(url, project)
    }

    override fun searchesByName(): Boolean = true

    private fun createPredefinedMappings(project: Project): BiMap<String, VirtualFile> {
        logger.info("create predefined mappings")

        val myExpandedPreviewFuture = ApplicationManager.getApplication().executeOnPooledThread(Runnable {
            DumbService.getInstance(project).waitForSmartMode()
        })
        myExpandedPreviewFuture.get()
        val projectDir = project.guessProjectDir()
        return if (projectDir != null) ImmutableBiMap.of("webpack:///.", projectDir) else ImmutableBiMap.of()
    }


    override fun getRemoteUrls(file: VirtualFile): List<Url> {
//        val myExpandedPreviewFuture = ApplicationManager.getApplication().executeOnPooledThread(Runnable {
//            DumbService.getInstance(myProject).waitForSmartMode()
//        })
//        myExpandedPreviewFuture.get()
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
}


fun findMapping(parsedUrl: Url, project: Project): VirtualFile? {

    logger.info("Find file mapping for: ${parsedUrl.path}")

    val myExpandedPreviewFuture = ApplicationManager.getApplication().executeOnPooledThread(Runnable {
        DumbService.getInstance(project).waitForSmartMode()
    })
    myExpandedPreviewFuture.get()

    val url = parsedUrl.trimParameters().toDecodedForm()
    val filename = if (url.split("/").size > 1) {
        url.split("/")[url.split("/").lastIndex - 1] + "/" + url.split("/").last()
    } else {
        url
    }
    logger.info("File mapping is found: $filename \n \n")

    // todo: not that explicit folder name
    val systemIndependentPath: String =
        FileUtil.toSystemIndependentName(project.guessProjectDir()?.findFileByRelativePath("src/@jetbrains/$filename").toString())

    val projectBaseDir: VirtualFile? = project.guessProjectDir()
    val child = if (systemIndependentPath.isEmpty()) {
        projectBaseDir
    } else projectBaseDir?.findFileByRelativePath("src/@jetbrains/$filename")

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
