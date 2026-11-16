package com.napsak.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.napsak.app.R
import com.napsak.app.domain.model.VoteMode

@Composable
fun VoteMode.label(): String = stringResource(
    when (this) {
        VoteMode.LIKE -> R.string.mode_like
        VoteMode.SINGLE -> R.string.mode_single
        VoteMode.RANK -> R.string.mode_rank
    }
)

@Composable
fun VoteMode.hint(): String = stringResource(
    when (this) {
        VoteMode.LIKE -> R.string.mode_like_hint
        VoteMode.SINGLE -> R.string.mode_single_hint
        VoteMode.RANK -> R.string.mode_rank_hint
    }
)
