package com.github.jk1.ytplugin.workflowsDebugConfiguration

import com.google.common.base.Joiner
import com.google.common.collect.BiMap
import com.google.common.collect.HashBiMap
import com.google.common.collect.ImmutableBiMap
import com.intellij.javascript.debugger.*
import com.intellij.javascript.debugger.execution.RemoteUrlMappingBean
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.IndexNotReadyException
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.impl.http.HttpVirtualFile
import com.intellij.pom.Navigatable
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.ProjectScope
import com.intellij.util.PathUtilRt
import com.intellij.util.SmartList
import com.intellij.util.Url
import com.intellij.util.Urls
import org.jetbrains.io.LocalFileFinder
import kotlin.reflect.jvm.internal.impl.load.java.lazy.ContextKt.child




private val PREDEFINED_MAPPINGS_KEY: Key<BiMap<String, VirtualFile>> = Key.create("js.debugger.predefined.mappings")

class RemoteDebuggingFileFinder(mappings: BiMap<String, VirtualFile> = ImmutableBiMap.of(),
                                internal val parent: DebuggableFileFinder? = null)
    : DebuggableFileFinder {

    internal var mappings = mappings
        private set

    @Deprecated("Use constructor with DebuggableFileFinder")
    constructor(mappings: BiMap<String, VirtualFile>, parent: JSFileFinderBase) : this(mappings, null)

    override fun findNavigatable(url: Url, project: Project): Navigatable? {
        findMapping(url, project)?.let {
            return JsFileUtil.createNavigatable(project, it)
        }
        return parent?.findNavigatable(url, project)
    }

    override fun findFile(url: Url, project: Project): VirtualFile? {
        return findByMappings(url, mappings)
    }

    override fun guessFile(url: Url, project: Project): VirtualFile? {
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
        val projectDir = project.guessProjectDir()
        return if (projectDir != null) ImmutableBiMap.of("webpack:///.", projectDir) else ImmutableBiMap.of()
    }

    fun isDebuggable(file: VirtualFile, project: Project): Boolean {
        if (file is HttpVirtualFile) {
            return true
        }

        if (!mappings.isEmpty()) {
            var current: VirtualFile? = file
            while (current != null) {
                if (mappings.containsValue(current)) {
                    return true
                }
                current = current.parent
            }
        }
        if (parent is DebuggableFileFinderImpl && parent.isDebuggable(file, project)) {
            return true
        } else {
            return findByName(file.name, project, null) != null
        }
    }

    override fun canSetRemoteUrl(file: VirtualFile, project: Project): Boolean = ProjectRootManager.getInstance(project).fileIndex.isInContent(file)

    override fun getRemoteUrls(file: VirtualFile): List<Url> {
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

    fun updateRemoteUrlMapping(mappings: List<RemoteUrlMappingBean>): Boolean {
        val newMap = createUrlToLocalMap(mappings)
        if (newMap != this.mappings) {
            this.mappings = newMap
            return true
        }
        return false
    }

    override fun toString(): String = Joiner.on("\n ").withKeyValueSeparator("->").join(mappings)
}

fun createUrlToLocalMap(mappings: List<RemoteUrlMappingBean>): BiMap<String, VirtualFile> {
    if (mappings.isEmpty()) {
        return ImmutableBiMap.of()
    }

    val map = HashBiMap.create<String, VirtualFile>(mappings.size)
    for (mapping in mappings) {
        val file = LocalFileFinder.findFile(mapping.localFilePath)
        if (file != null) {
            map.forcePut(mapping.remoteUrl, file)
        }
    }
    return map
}

fun findByName(url: Url, project: Project) = findByName(if (url.path.endsWith('/')) "" else PathUtilRt.getFileName(url.path), project, url)

private fun findByName(filename: String, project: Project, url: Url?): VirtualFile? {
    val files = findByName(filename, project)
    if (files.isEmpty()) {
        return null
    } else if (files.size == 1) {
        return files[0]
    }

    // lets canonicalize
    val canonicalFile = files[0].canonicalFile ?: return null

    var i = 1
    val size = files.size
    while (i < size) {
        if (canonicalFile != files[i].canonicalFile) {
            return null
        }
        i++
    }

    if (url == null) {
        return files[0]
    }

    // ok, all files are the same, select more matching
    val path = url.path
    var offset = 0
    while (true) {
        for (file in files) {
            val filePath = file.path
            val length = path.length - offset
            if (filePath.regionMatches(filePath.length - length, path, offset, length, true)) {
                return file
            }
        }

        offset = path.indexOf('/', offset + 1)
        if (offset < 0) {
            break
        }
    }

    return null
}

internal fun findByName(filename: String, project: Project): List<VirtualFile> = ReadAction.compute<List<VirtualFile>, Throwable> {
    try {
        val scope = ProjectScope.getContentScope(project)
        var files = FilenameIndex.getVirtualFilesByName(project, if (filename.isEmpty()) "index.html" else filename, scope)
        if (files.isEmpty() && filename.isEmpty()) {
            files = FilenameIndex.getVirtualFilesByName(project, "index.xhtml", scope)
        }

        if (files.isEmpty()) {
            return@compute emptyList()
        }

        val projectFileIndex = ProjectRootManager.getInstance(project).fileIndex
        val result = SmartList<VirtualFile>()
        for (file in files) {
            if (!projectFileIndex.isInLibrary(file)) {
                result.add(file)
            }
        }
        return@compute result
    } catch (e: IndexNotReadyException) {
        DumbService.getInstance(project).showDumbModeNotification(JSDebuggerBundle.message("js.file.mapping.indexing"))
        return@compute emptyList()
    }
}

private fun findMapping(parsedUrl: Url, project: Project): VirtualFile? {

    val url = parsedUrl.trimParameters().toDecodedForm()
    val filename = url.split("/")[url.split("/").size - 1]

    val systemIndependentPath: String = FileUtil.toSystemIndependentName("src/$filename")
    val projectBaseDir: VirtualFile = project.baseDir
    val child =  if (systemIndependentPath.isEmpty()) {
        projectBaseDir
    } else projectBaseDir.findFileByRelativePath(systemIndependentPath)

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
