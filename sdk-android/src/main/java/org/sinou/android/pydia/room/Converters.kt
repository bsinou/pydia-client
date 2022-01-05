package org.sinou.android.pydia.room

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.pydio.cells.api.SdkNames
import com.pydio.cells.api.ui.WorkspaceNode
import com.pydio.cells.transport.StateID
import java.lang.reflect.Type
import java.util.*
import kotlin.collections.ArrayList

class Converters {

    @TypeConverter
    fun fromString(value: String): List<String> {
        //val listType: Type = object : TypeToken<List<String>>() {}.getType()
        val listType: Type = object : TypeToken<ArrayList<String>>() {}.getType()
        return Gson().fromJson(value, listType)
    }

    @TypeConverter
    fun fromList(list: List<String>): String {
        val gson = Gson()
        return gson.toJson(list)
    }

    @TypeConverter
    fun fromJSON(value: String): List<WorkspaceNode> {
        //val listType: Type = object : TypeToken<List<String>>() {}.getType()
        val listType: Type = object : TypeToken<ArrayList<WorkspaceNode>>() {}.getType()
        return Gson().fromJson(value, listType)
    }

    @TypeConverter
    fun fromWorkspaces(list: List<WorkspaceNode>): String {
        return Gson().toJson(list)
    }

    @TypeConverter
    fun toProperties(value: String): Properties {
        val propType: Type = object : TypeToken<Properties>() {}.getType()
        return Gson().fromJson(value, propType)
    }

    @TypeConverter
    fun fromProperties(meta: Properties): String {
        return Gson().toJson(meta)
    }

    @TypeConverter
    fun toStateID(value: String): StateID {
        val propType: Type = object : TypeToken<StateID>() {}.getType()
        return Gson().fromJson(value, propType)
    }

    @TypeConverter
    fun fromStateID(stateID: StateID): String {
        return Gson().toJson(stateID)
    }


//    @TypeConverter
//    fun fromTimestamp(value: Long?): Date? {
//        return if (value == null) null else Date(value)
//    }
//
//    @TypeConverter
//    fun dateToTimestamp(date: Date?): Long? {
//        return date?.time
//    }

}