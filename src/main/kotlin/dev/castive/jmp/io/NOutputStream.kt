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

package dev.castive.jmp.io

import java.io.OutputStream

class NOutputStream: OutputStream() {
    private val streams = arrayListOf<OutputStream>()

    fun add(stream: OutputStream) {
        streams.add(stream)
    }
    fun rm(stream: OutputStream) {
        streams.remove(stream)
    }

    override fun write(b: Int) {
        streams.forEach {
            it.write(b)
        }
    }

    override fun write(b: ByteArray?) {
        streams.forEach {
            it.write(b)
        }
    }

    override fun write(b: ByteArray?, off: Int, len: Int) {
        streams.forEach {
            it.write(b, off, len)
        }
    }

    override fun flush() {
        streams.forEach {
            it.flush()
        }
    }

    override fun close() {
        streams.forEach {
            it.close()
        }
    }
}