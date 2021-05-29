package com.github.jk1.ytplugin.scriptsDebugConfiguration

import com.google.common.base.Joiner
import com.google.common.collect.BiMap
import com.google.common.collect.HashBiMap
import com.google.common.collect.ImmutableBiMap
import com.intellij.javascript.debugger.*
import com.intellij.javascript.debugger.execution.RemoteUrlMappingBean
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.impl.http.HttpVirtualFile
import com.intellij.pom.Navigatable
import com.intellij.util.Url
import com.intellij.util.Urls
import org.jetbrains.io.LocalFileFinder


class RemoteDebuggingFileFinder(
    private var mappings: BiMap<String, VirtualFile> = ImmutableBiMap.of(),
    private val parent: DebuggableFileFinder? = null)
    : DebuggableFileFinder {

    @Deprecated("Use constructor with DebuggableFileFinder")
    constructor(mappings: BiMap<String, VirtualFile>) : this(mappings, null)

    override fun findNavigatable(url: Url, project: Project): Navigatable? {
        findMapping(url, project)?.let {
            return JsFileUtil.createNavigatable(project, it)
        }
        return parent?.findNavigatable(url, project)
    }

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
