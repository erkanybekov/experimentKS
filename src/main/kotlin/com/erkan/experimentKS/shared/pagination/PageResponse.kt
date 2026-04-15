package com.erkan.experimentks.shared.pagination

import org.springframework.data.domain.Page

data class PageResponse<T : Any>(
	val items: List<T>,
	val page: Int,
	val size: Int,
	val totalElements: Long,
	val totalPages: Int,
	val hasNext: Boolean,
)

fun <T : Any> Page<T>.toPageResponse(): PageResponse<T> =
	PageResponse(
		items = content,
		page = number,
		size = size,
		totalElements = totalElements,
		totalPages = totalPages,
		hasNext = hasNext(),
	)
