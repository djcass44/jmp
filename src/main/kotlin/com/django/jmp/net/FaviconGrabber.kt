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

import ch.bisi.jicon.common.Util
import ch.bisi.jicon.fetcher.link.JsoupFaviconsLinksFetcher
import com.django.jmp.except.InsecureDomainException
import com.django.log2.logging.Log
import org.jsoup.Jsoup
import java.net.URL

class FaviconGrabber(private val domain: String) {

    fun get(listener: OnLoadCallback) {
        val t = Thread {
            val f = get()
            if(f != null) listener.onLoad(f)
            else Log.v(javaClass, "Favicon for $domain is null")
        }
        t.apply {
            isDaemon = true
            start()
        }
    }
    fun get(): Favicon? {
        return try {
            if(domain.startsWith("http://")) return null
            val document = Jsoup.connect(Util.getDomain(URL(domain))).get()
            val icons = JsoupFaviconsLinksFetcher(document).fetchLinks()
            val links = Array(icons.size) { i ->
                return@Array Icon(icons[i].toString(), null, null)
            }
            Favicon(domain, links)
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