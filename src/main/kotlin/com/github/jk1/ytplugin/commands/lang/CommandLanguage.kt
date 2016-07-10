package com.github.jk1.ytplugin.commands.lang

import com.github.jk1.ytplugin.common.YouTrackPluginIcons
import com.intellij.extapi.psi.PsiFileBase
import com.intellij.lang.Language
import com.intellij.openapi.fileTypes.FileTypeConsumer
import com.intellij.openapi.fileTypes.FileTypeFactory
import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.psi.FileViewProvider
import javax.swing.Icon

object CommandLanguage : Language("YouTrack Commands") {

    override fun isCaseSensitive() = false
}

object CommandFileType : LanguageFileType(CommandLanguage) {

    override fun getIcon(): Icon = YouTrackPluginIcons.YOUTRACK

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