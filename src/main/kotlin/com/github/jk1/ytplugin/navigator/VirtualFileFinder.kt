package com.github.jk1.ytplugin.navigator

import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import java.io.File


object VirtualFileFinder {

    // todo: rewrite towards more concise and readable style
    fun findFile(relativePath: String, project: Project): VirtualFile? {
        val fileIndex = ProjectRootManager.getInstance(project).fileIndex
        val file = File(relativePath)
        val candidates = FilenameIndex.getVirtualFilesByName(project, file.name, GlobalSearchScope.allScope(project))
        var mostMatchedCandidate: VirtualFile? = null
        var maxMatchedParentsCount = -1
        val candidatesIterator = candidates.iterator()

        while (true) {
            var candidate: VirtualFile
            var matchedParentsCount: Int
            do {
                do {
                    do {
                        do {
                            if (!candidatesIterator.hasNext()) {
                                return mostMatchedCandidate
                            }

                            candidate = candidatesIterator.next() as VirtualFile
                        } while (!candidate.exists())
                    } while (candidate.isDirectory)
                } while (!fileIndex.isInContent(candidate) && !fileIndex.isInLibrarySource(candidate))

                matchedParentsCount = matchParents(candidate, file)
            } while (matchedParentsCount <= maxMatchedParentsCount && (matchedParentsCount != maxMatchedParentsCount || (!fileIndex.isInContent(candidate) || !fileIndex.isInLibrarySource(mostMatchedCandidate!!)) && countParents(candidate) >= countParents(mostMatchedCandidate!!)))

            mostMatchedCandidate = candidate
            maxMatchedParentsCount = matchedParentsCount
        }
    }

    fun matchParents(candidate: VirtualFile, relativeFile: File): Int {
        val fileParent: File? = relativeFile.parentFile
        val candidateParent: VirtualFile? = candidate.parent
        if (fileParent != null && candidateParent != null && candidateParent.name == fileParent.name) {
            return 1 + matchParents(candidateParent, fileParent)
        } else {
            return 0
        }
    }

    private fun countParents(file: VirtualFile): Int =
            when (file.parent) {
                null -> 0
                else -> 1 + countParents(file.parent)
            }

}