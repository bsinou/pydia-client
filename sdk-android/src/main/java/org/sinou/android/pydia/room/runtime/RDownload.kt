package org.sinou.android.pydia.room.runtime

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import org.sinou.android.pydia.room.Converters
import java.sql.Timestamp
import java.util.*

@Entity(tableName = "download_table")
data class RDownload(

    @PrimaryKey(autoGenerate = true)
    var downloadId: Long = 0L,

    @ColumnInfo(name = "remote_state") val targetState: String,

    @ColumnInfo(name = "bytesize") val bytesize: Long,

    @ColumnInfo(name = "mime") val mime: String,

    @ColumnInfo(name = "start_ts") var startTimestamp: Long = -1,

    @ColumnInfo(name = "done_ts") var doneTimestamp: Long = -1,

    @ColumnInfo(name = "error") var error: String?,

    @ColumnInfo(name = "progress") val progress: Int = 0,

)