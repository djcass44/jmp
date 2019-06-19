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

import dev.castive.fav2.Fav
import dev.castive.jmp.except.InsecureDomainException
import dev.castive.log2.Log

class FaviconGrabber(private val domain: String) {
    private val urlInsecure = "http://"
    init {
        Fav.DEBUG = true
        Fav.ALLOW_HTTP = false // Do not load insecure origins
    }
    fun get(listener: OnLoadCallback) {
        Fav.loadDomain(domain, object : Fav.OnLoadedCallback {
            override fun onLoad(favicon: String?) {
                Log.d(javaClass, "Found link: $favicon")
                if(favicon == null) return
                listener.onLoad(Favicon(domain, arrayOf(Icon(favicon, null, null))))
            }
        })
    }
    fun get(): Favicon? {
        return try {
            if(domain.startsWith(urlInsecure)) return null
            val link = Fav.loadDomain(domain) ?: return null
            Log.d(javaClass, "Found link: $link")
            Favicon(domain, arrayOf(Icon(link, null, null)))
        }
        catch (e: Exception) {
            Log.e(javaClass, "Failed to load favicon [$domain]: $e")
            null
        }
    }
    interface OnLoadCallback {
        fun onLoad(favicon: Favicon)
    }
}
class Favicon(val domain: String, val icons: Array<Icon>?) {
    private val urlInsecure = "http://"
    fun get(): Icon? {
        return try {
            if (icons == null || icons.isEmpty())
                return null
            val result = getBest(icons)
            if (result.src.startsWith(urlInsecure)) throw InsecureDomainException()
            Log.d(javaClass, "domain: $domain, maxSize: ${result.sizes}")
            result
        }
        catch (e: Exception) {
            Log.e(javaClass, "Failed to generate favicon [$domain]: $e")
            null
        }
    }
    private fun getBest(icons: Array<Icon>): Icon {
        var result = icons[0]
        for (i in icons) {
            if (i.sizes != null) {
                if (result.sizes == null)
                    result = i
                if (i.getSize() > result.getSize())
                    result = i
            }
        }
        return result
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