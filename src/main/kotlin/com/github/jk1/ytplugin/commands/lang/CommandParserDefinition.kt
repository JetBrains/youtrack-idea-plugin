package com.github.jk1.ytplugin.commands.lang

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.lang.ParserDefinition
import com.intellij.lang.PsiBuilder
import com.intellij.lang.PsiParser
import com.intellij.lexer.Lexer
import com.intellij.lexer.LexerBase
import com.intellij.openapi.project.Project
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.IFileElementType
import com.intellij.psi.tree.TokenSet

class CommandParserDefinition : ParserDefinition {

    val ANY_TEXT = IElementType("ANY_TEXT", CommandLanguage)
    val QUERY = IElementType("QUERY", CommandLanguage)
    val FILE = IFileElementType(CommandLanguage)

    override fun getFileNodeType(): IFileElementType = FILE

    override fun getCommentTokens(): TokenSet = TokenSet.EMPTY

    override fun getStringLiteralElements(): TokenSet = TokenSet.EMPTY

    override fun getWhitespaceTokens(): TokenSet = TokenSet.EMPTY

    override fun createLexer(project: Project): Lexer = CommandLexer()

    override fun createParser(project: Project): PsiParser? = CommandPsiParser()

    override fun createElement(node: ASTNode): PsiElement = CommandQueryElement(node)

    override fun createFile(provider: FileViewProvider): PsiFile? = CommandFile(provider)

    override fun spaceExistanceTypeBetweenTokens(start: ASTNode, end: ASTNode): ParserDefinition.SpaceRequirements?
            = ParserDefinition.SpaceRequirements.MAY
    /**
     * Sole element that represents YouTrack command in PSI tree
     */
    class CommandQueryElement(node: ASTNode) : ASTWrapperPsiElement(node)

    /**
     * Tokenize whole command as single {@code ANY_TEXT} token
     */
    inner class CommandLexer : LexerBase() {
        var start: Int = 0
        var end: Int = 0
        lateinit var buffer: CharSequence

        override fun start(buffer: CharSequence , startOffset: Int, endOffset: Int, initialState: Int) {
            //LOG.debug(String.format("buffer: '%s', start: %d, end: %d", buffer, startOffset, endOffset))
            this.buffer = buffer
            this.start = startOffset
            this.end = endOffset
        }

        override fun getState(): Int = 0

        override fun getTokenType(): IElementType? = if (start >= end) null else ANY_TEXT

        override fun getTokenStart() = start

        override fun getTokenEnd() = end

        override fun getBufferSequence() = buffer

        override fun getBufferEnd() = end

        override fun advance() {
            start = end
        }
    }

    /**
     * Parse whole YouTrack command as single {@code QUERY} element
     */
    inner class CommandPsiParser : PsiParser {

        override fun parse(root: IElementType, builder: PsiBuilder): ASTNode {
            val rootMarker = builder.mark()
            val queryMarker = builder.mark()
            assert(builder.tokenType == null || builder.tokenType == ANY_TEXT)
            builder.advanceLexer()
            queryMarker.done(QUERY)
            assert(builder.eof())
            rootMarker.done(root)
            return builder.treeBuilt
        }
    }
}