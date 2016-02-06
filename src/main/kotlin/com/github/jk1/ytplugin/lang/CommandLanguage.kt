package com.github.jk1.ytplugin.lang

import com.intellij.extapi.psi.PsiFileBase
import com.intellij.lang.Language
import com.intellij.openapi.fileTypes.FileTypeConsumer
import com.intellij.openapi.fileTypes.FileTypeFactory
import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.psi.FileViewProvider
import icons.TasksIcons
import javax.swing.Icon

object CommandLanguage : Language("YouTrack Commands") {

    override fun isCaseSensitive() = false
}

object CommandFileType : LanguageFileType(CommandLanguage) {

    override fun getIcon(): Icon = TasksIcons.Youtrack

    override fun getDefaultExtension() = "youtrack"

    override fun getDescription() = "YouTrack Command Language"

    override fun getName() = CommandLanguage.id

}

class CommandFile(provider: FileViewProvider) : PsiFileBase(provider, CommandLanguage) {

    override fun getFileType() = CommandFileType
}

class CommandFileFactory : FileTypeFactory() {

    override fun createFileTypes(consumer: FileTypeConsumer) {
        consumer.consume(CommandFileType, CommandFileType.defaultExtension)
    }
}