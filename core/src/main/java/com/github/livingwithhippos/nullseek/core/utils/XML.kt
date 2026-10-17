package com.github.livingwithhippos.nullseek.core.utils

import org.jsoup.nodes.Element

fun Element.directChild(tagName: String): Element? =
    children().firstOrNull { it.tagName() == tagName }

fun Element.directChildren(tagName: String): List<Element> =
    children().filter { it.tagName() == tagName }

fun Element.directChildText(tagName: String): String = directChild(tagName)?.text().orEmpty()