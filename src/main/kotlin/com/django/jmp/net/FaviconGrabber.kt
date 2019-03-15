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

package com.django.jmp.net

import com.django.jmp.except.InsecureDomainException
import com.django.log2.logging.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import io.github.rybalkinsd.kohttp.ext.httpGet
import okhttp3.Response

class FaviconGrabber(private val domain: String, private val pretty: Boolean = true) {
    companion object {
        private val gson: Gson = GsonBuilder().setPrettyPrinting().create()
    }
    fun get(listener: OnLoadCallback) {
        val t = Thread {
            val url = "https://favicongrabber.com/api/grab/$domain?pretty=$pretty"
            try {
                val r: Response = url.httpGet()
                val text = r.body()?.string()
                listener.onLoad(gson.fromJson(text ?: "", Favicon::class.java))
            }
            catch (e: Exception) {
                Log.e(javaClass, "Failed to load favicon [$domain]: $e")
            }
        }
        t.apply {
            isDaemon = true
            start()
        }
    }
    interface OnLoadCallback {
        fun onLoad(favicon: Favicon)
    }
}
class Favicon(val domain: String, val icons: Array<Icon>?) {
    fun get(): Icon? {
        try {
            if (icons == null || icons.isEmpty())
                return null
            if (icons.size == 1) {
                if (icons[0].src.startsWith("http://")) throw InsecureDomainException()
                return icons[0]
            }
            var result = icons[0]
            for (i in icons) {
                if (i.sizes != null) {
                    if (result.sizes == null)
                        result = i
                    if (i.getSize() > result.getSize())
                        result = i
                }
            }
            if (result.src.startsWith("http://")) throw InsecureDomainException()
            Log.d(javaClass, "domain: $domain, maxSize: ${result.sizes}")
            return result
        }
        catch (e: Exception) {
            Log.e(javaClass, "Failed to get favicon [$domain]: $e")
            return null
        }
    }
}
class Icon(val src: String, val type: String?, val sizes: String?) {
    fun getSize(): Int {
        if(sizes == null)
            return 0
        val sizes = sizes.split("x")
        if(sizes.size != 2)
            return 0
        return try {
            sizes[0].toInt()
        }
        catch (e: Exception) {
            0
        }
    }
}