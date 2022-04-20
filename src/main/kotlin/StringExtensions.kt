package org.jetbrains.dokka.hugo

import java.util.*
import kotlin.collections.HashMap

internal fun String.getTitle(hugoConfiguration: HugoConfiguration) =
    this.replaceString(hugoConfiguration.titleReplace).capitalizeIfNecessary(hugoConfiguration.titleCapitalize)


internal fun String.getLinkTitle(hugoConfiguration: HugoConfiguration) =
    this.replaceString(hugoConfiguration.linkTitleReplace).capitalizeIfNecessary(hugoConfiguration.linkTitleCapitalize)

private fun String.replaceString(replace: HashMap<String, String>?): String {
    var newString = this
    replace?.keys?.forEach { key ->
        replace[key]?.let {
            newString = newString.replace(key, it)
        }
    }
    return newString
}

private fun String.capitalizeIfNecessary(shouldCapitalize: Boolean) =
    if (shouldCapitalize) this.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() } else this