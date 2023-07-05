package com.raccoongang.core.domain.model

import java.util.*

data class CourseStructure(
    val root: String,
    val blockData: List<Block>,
    val id: String,
    val name: String,
    val number: String,
    val org: String,
    val start: Date?,
    val startDisplay: String,
    val startType: String,
    val end: Date?,
    val coursewareAccess: CoursewareAccess,
    val media: Media?,
    val certificate: Certificate?,
    val isSelfPaced: Boolean
)