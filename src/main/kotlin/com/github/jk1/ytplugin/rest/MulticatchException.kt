package com.github.jk1.ytplugin.rest

class MulticatchException {

    companion object{
        fun <R> Throwable.multicatchException(vararg classes: Class<*>, block: () -> R): R {
            if (classes.any { this.javaClass.isAssignableFrom(it) }) {
                return block()
            } else throw this
        }
    }

}