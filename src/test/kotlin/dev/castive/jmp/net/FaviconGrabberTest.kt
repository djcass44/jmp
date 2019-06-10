/*
 *    Copyright 2019 Django Cass
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package dev.castive.jmp.net

import dev.castive.log2.Log
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class FaviconGrabberTest {
    @ParameterizedTest
    @ValueSource(strings = [
        "https://github.com",
        "https://apple.com",
        "https://atlassian.com"
    ])
    fun getDefiniteIcon(value: String) { // Used to test grabber works on site with valid favicon
        val grabber = FaviconGrabber(value)
        val f = grabber.get()
        assertNotNull(f)
        assertTrue(f!!.icons!!.isNotEmpty())
        Log.d(javaClass, f.get()!!.src)
        assertTrue(f.get()!!.src.startsWith("https://"))
    }
    @ParameterizedTest
    @ValueSource(strings = [
        "http://www.website.org"
    ])
    fun getHttpIcon(value: String) { // Used to test grabber works on site with valid favicon
        val grabber = FaviconGrabber(value)
        val f = grabber.get()
        assertNull(f)
    }
}