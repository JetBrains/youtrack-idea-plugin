package com.github.jk1.ytplugin.ui

import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.text.StringUtil


class ResourceTemplate(val resource: String) {

    private val variables = mutableMapOf<String, String>()

    fun put(key: String, value: String): ResourceTemplate {
        variables.put(key, value)
        return this
    }

    fun render() = StringUtil.replace(
            FileUtil.loadTextAndClose(javaClass.getResourceAsStream(resource)),
            variables.keys.map { "{##$it}" }.toTypedArray(),
            variables.values.toTypedArray())
}