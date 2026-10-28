package com.ascdev.keyra.domain

import com.ascdev.keyra.domain.model.Category
import com.ascdev.keyra.domain.model.VaultEntry

fun entry(
    id: Long,
    title: String = "Entry $id",
    username: String = "user$id@mail.com",
    password: String = "k8#Rt!vQ2@pLm9\$wZx4-$id",
    url: String = "",
    category: Category = Category.LOGIN,
) = VaultEntry(
    id = id,
    title = title,
    username = username,
    password = password,
    url = url,
    notes = "",
    category = category,
    createdAt = 0,
    updatedAt = 0,
)
