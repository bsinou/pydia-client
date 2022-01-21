package org.sinou.android.pydia.db.browse

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = arrayOf(RTreeNode::class), version = 1, exportSchema = false)
abstract class TreeNodeDB : RoomDatabase() {

    abstract fun treeNodeDao(): TreeNodeDao

    companion object {
        @Volatile
        private var INSTANCE: TreeNodeDB? = null

        fun getDatabase(context: Context): TreeNodeDB {
            val tempInstance = INSTANCE
            if (tempInstance != null) {
                return tempInstance
            }
            synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TreeNodeDB::class.java,
                    "tree_nodes"
                ).build()
                INSTANCE = instance
                return instance
            }
        }
    }
}