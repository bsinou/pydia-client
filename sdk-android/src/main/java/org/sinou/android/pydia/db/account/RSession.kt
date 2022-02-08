package org.sinou.android.pydia.db.account

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import org.sinou.android.pydia.AppNames
import org.sinou.android.pydia.db.Converters
import java.net.URL

@Entity(tableName = "session_table")
@TypeConverters(Converters::class)
data class RSession(

    @PrimaryKey
    @ColumnInfo(name = "account_id") val accountID: String,

    // foreground, background, paused, new
    @ColumnInfo(name = "lifecycle_state") var lifecycleState: String,

    @ColumnInfo(name = "dir_name") val dirName: String,

    @ColumnInfo(name = "db_name") val dbName: String,
) {


    companion object {

        fun newInstance(account: RAccount, index: Int): RSession {
            var cleanUrl = URL(account.url).host
            var cleanDbName = "nodes.$cleanUrl"

            if (index > 0) {
                cleanUrl += "-$index"
                cleanDbName += "-$index"
            }

            return RSession(
                accountID = account.accountID,
                lifecycleState = AppNames.SESSION_STATE_NEW,
                dirName = cleanUrl,
                dbName = cleanDbName,
            )
        }
    }
}