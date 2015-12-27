package com.github.jk1.ytplugin

import org.testng.annotations.AfterMethod
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test
import org.testng.Assert.*

@Test
class DummyTest {

    @BeforeMethod
    fun setUp() {
        print("Before test")
    }

    @Test
    fun sampleTest() {
        assertEquals(1, 1)
    }

    @AfterMethod
    fun tearDown() {
        print("After test")
    }
}