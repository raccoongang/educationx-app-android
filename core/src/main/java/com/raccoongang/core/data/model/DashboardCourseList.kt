package com.raccoongang.core.data.model

import com.google.gson.annotations.SerializedName
import com.raccoongang.core.domain.model.DashboardCourseList

data class DashboardCourseList(
    @SerializedName("next")
    val next: String?,
    @SerializedName("previous")
    val previous: String?,
    @SerializedName("count")
    val count: Int,
    @SerializedName("num_pages")
    val numPages: Int,
    @SerializedName("current_page")
    val currentPage: Int,
    @SerializedName("results")
    val results: List<EnrolledCourse>
) {

    fun mapToDomain(): DashboardCourseList {
        return DashboardCourseList(
            com.raccoongang.core.domain.model.Pagination(
                count,
                next ?: "",
                numPages,
                previous ?: ""
            ),
            results.map { it.mapToDomain() }
        )
    }

}