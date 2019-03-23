package com.django.jmp.auth.provider

import com.django.jmp.db.dao.GroupData
import com.django.jmp.db.dao.UserData

interface BaseProvider {
    fun setup()
    fun getUsers(): ArrayList<UserData>
    fun getGroups(): ArrayList<GroupData>
    fun tearDown()
}