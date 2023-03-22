package com.raccoongang.course.data.storage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.raccoongang.core.data.model.room.CourseEntity
import com.raccoongang.core.data.model.room.discovery.EnrolledCourseEntity
import com.raccoongang.course.data.model.BlockDbEntity

@Dao
interface CourseDao {

    @Query("SELECT * FROM course_discovery_table WHERE id=:id")
    suspend fun getCourseById(id: String): CourseEntity?

    @Query("SELECT * FROM course_enrolled_table WHERE id=:id")
    suspend fun getEnrolledCourseById(id: String): EnrolledCourseEntity?

    @Query("SELECT * FROM course_blocks_table WHERE courseId=:id")
    suspend fun getCourseBlocksById(id: String): List<BlockDbEntity>?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCourseBlocks(vararg BlockDbEntityEntity: BlockDbEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCourseEntity(vararg courseEntity: CourseEntity)

    @Query("DELETE FROM course_blocks_table")
    suspend fun clearAllCourseBlocks()
}