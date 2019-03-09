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

package com.django.jmp.api.actions

import com.django.jmp.db.dao.Jump
import com.django.jmp.db.dao.Jumps
import com.django.jmp.net.Favicon
import com.django.jmp.net.FaviconGrabber
import org.jetbrains.exposed.sql.transactions.transaction

class ImageAction(private val address: String) {
    fun get() {
        // Get the favicon for an address (only https)
        val cutAddress = kotlin.runCatching { address.split("https://")[1] }.getOrNull() ?: address
        if (cutAddress.isBlank())
            return
        FaviconGrabber(cutAddress).get(object : FaviconGrabber.OnLoadCallback {
            override fun onLoad(favicon: Favicon) {
                val f = favicon.get()
                if(f != null) {
                    transaction {
                        val results = Jump.find {
                            Jumps.location eq address
                        }
                        for (r in results) {
                            if(r.image == null || r.image != f.src)
                                r.image = f.src
                        }
                    }
                }
            }
        })
    }
}