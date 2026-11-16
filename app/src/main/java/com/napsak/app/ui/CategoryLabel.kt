package com.napsak.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.napsak.app.R
import java.util.Locale

/** Display name of a category; the built-in ones follow the app language, custom ones are shown as typed. */
@Composable
fun categoryLabel(category: String): String = when (category.lowercase(Locale.ROOT)) {
    "tümü" -> stringResource(R.string.cat_all)
    "yemek" -> stringResource(R.string.cat_yemek)
    "aktivite" -> stringResource(R.string.cat_aktivite)
    "film" -> stringResource(R.string.cat_film)
    "eğlence" -> stringResource(R.string.cat_eglence)
    else -> category
}
