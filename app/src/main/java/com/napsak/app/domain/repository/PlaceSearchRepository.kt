package com.napsak.app.domain.repository

import com.napsak.app.domain.model.Place

interface PlaceSearchRepository {
    suspend fun search(query: String): Result<List<Place>>
}
