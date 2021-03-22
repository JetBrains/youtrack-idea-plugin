package com.github.jk1.ytplugin.workflowsDebugConfiguration

import com.github.jk1.ytplugin.ComponentAware
import com.github.jk1.ytplugin.rest.WorkflowsRestClient
import com.intellij.javascript.debugger.execution.RemoteUrlMappingBean
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.javascript.debugger.JSDebuggerBundle
import com.intellij.javascript.debugger.JavaScriptDebugProcess
import com.intellij.javascript.debugger.JsFileUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.impl.DirectoryIndex
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileFilter
import com.intellij.ui.ScrollPaneFactory
import com.intellij.util.ui.tree.AbstractFileTreeTable
import com.intellij.xdebugger.XDebuggerManager
import gnu.trove.THashMap
import org.jetbrains.io.LocalFileFinder
import java.awt.BorderLayout
import java.awt.LayoutManager
import javax.swing.JPanel

import com.intellij.testFramework.PlatformTestUtil.getOrCreateProjectBaseDir
import com.intellij.testFramework.VfsTestUtil
import com.intellij.util.SmartList


open class WorkflowsLocalFilesMappingPanel(val project: Project, layout: LayoutManager) : JPanel(layout) {

    protected val mappingTreePanel: JPanel = JPanel(BorderLayout())
    private val mappingTree: AbstractFileTreeTable<String>? =
            if (project.isDefault) null
            else AbstractFileTreeTable(project, String::class.java, JSDebuggerBundle.message("column.title.remote.url"),
                    VisibleNodeFileFilter(DirectoryIndex.getInstance(project)), true, false)

    open fun initUI() {
        if (mappingTree != null) {
            mappingTreePanel.add(ScrollPaneFactory.createScrollPane(mappingTree))
        } else {
            isVisible = false
        }
    }

    fun resetEditorFrom(mappings: MutableList<RemoteUrlMappingBean>, allowConfigureMappings: Boolean) {
        if (!allowConfigureMappings) {
            isVisible = false
        } else if (mappingTree != null) {
            val map = THashMap<VirtualFile, String>()
            var toSelect: VirtualFile? = null
            for (bean in mappings) {
                val file = LocalFileFinder.findFile(bean.localFilePath)
                if (file != null) {
                    map[file] = bean.remoteUrl
                    if (toSelect == null || VfsUtilCore.isAncestor(file, toSelect, false)) {
                        toSelect = file
                    }
                }
            }
            mappingTree.reset(map)
            mappingTree.select(toSelect)

        }
    }

    fun applyEditorTo(mappings: MutableList<RemoteUrlMappingBean>, configuration: RunConfiguration) {

        val name = "fff"
        name.split("/").size
        val mapings: MutableList<RemoteUrlMappingBean> = SmartList()
        mapings.add(RemoteUrlMappingBean("/home/alina.boshchenko/IdeaProjects/untitled16/src/change-color-over-time.js", "color-scheme-workflow/change-color-over-time.js"))
        if (mappingTree != null) {
            for (process in XDebuggerManager.getInstance(mappingTree.project).getDebugProcesses(JavaScriptDebugProcess::class.java)) {
                if (process.session.runProfile === configuration) {
                    process.updateRemoteUrlMappings(mapings)
                }
            }
        }

//        if (mappingTree != null) {
//            mappings.clear()
//            for (mapping in mapings) {
//                val remote = mapping
//
//                if (!isWorkflowLoaded){
//                    loadWorkflowRules((configuration as JSRemoteWorkflowsDebugConfiguration).workflowName)
//                    isWorkflowLoaded = true
//                }
//
//                if (!remote.isEmpty()) {
//                    mappings.add(createMapping(mapping.key.path, remote))
//                }
//            }
//
//            for (process in XDebuggerManager.getInstance(mappingTree.project).getDebugProcesses(JavaScriptDebugProcess::class.java)) {
//                if (process.session.runProfile === configuration) {
//                    process.updateRemoteUrlMappings(mappings)
//                }
//            }
//        }
    }

    protected open fun createMapping(localPath: String, remote: String): RemoteUrlMappingBean = RemoteUrlMappingBean(localPath, remote)
}

private class VisibleNodeFileFilter(private val directoryIndex: DirectoryIndex) : VirtualFileFilter {

    override fun accept(file: VirtualFile): Boolean {
        if (file.isDirectory) {
            val info = directoryIndex.getInfoForFile(file)
            return info.isInProject(file) || info.isExcluded(file)
        } else {
            return file.isDirectory || JsFileUtil.isHtmlOrJavaScript(file)
        }
    }
}