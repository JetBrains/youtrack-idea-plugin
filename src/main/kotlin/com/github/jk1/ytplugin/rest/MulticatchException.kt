package com.github.jk1.ytplugin.rest

import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf


class MulticatchException {

    companion object{
        fun <R> Throwable.multicatchException(vararg classes: KClass<*>, block: () -> R): R {
            if (classes.any { this::class.isSubclassOf(it) }) {
                return block()
            } else throw this
        }
    }

}