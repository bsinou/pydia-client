package org.sinou.android.pydia.utils

import android.content.Context
import android.net.ConnectivityManager
import android.os.Build
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.Fragment
import com.pydio.cells.api.SDKException
import com.pydio.cells.utils.Log
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

fun Fragment.hideKeyboard() {
    val imm = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.hideSoftInputFromWindow(requireView().windowToken, 0)
}

@Suppress("DEPRECATION")
fun hasUnMeteredNetwork(context: Context): Boolean {

    val connMgr = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        return connMgr.isDefaultNetworkActive && !connMgr.isActiveNetworkMetered
    } else {
        connMgr.allNetworks.forEach { network ->
            connMgr.getNetworkInfo(network)?.let {
                if (it.type == ConnectivityManager.TYPE_WIFI) {
                    return true
                }
            }
        }
        return false
    }
}

@Suppress("DEPRECATION")
fun hasMeteredNetwork(context: Context): Boolean {

    val connMgr = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        return connMgr.isDefaultNetworkActive && connMgr.isActiveNetworkMetered
    } else {
        connMgr.allNetworks.forEach { network ->
            connMgr.getNetworkInfo(network)?.let {
                if (it.type == ConnectivityManager.TYPE_MOBILE) {
                    return true
                }
            }
        }
        return false
    }
}

fun hasAtLeastMeteredNetwork(context: Context): Boolean {

    val connMgr = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        connMgr.activeNetwork?.let {
            // Log.i("ConnectionCheck", "Active network: ${it.networkHandle}")
            return true
        }
        return false
    } else {
        connMgr.allNetworks.forEach { network ->
            connMgr.getNetworkInfo(network)?.let {
                if (it.type == ConnectivityManager.TYPE_MOBILE || it.type == ConnectivityManager.TYPE_WIFI) {
                    return true
                }
            }
        }
        return false
    }
}

fun logException(caller: String?, msg: String, e: Exception) {
    Log.e(caller, "$msg ${if (e is SDKException) "(Code #${e.code} )" else ""}")
    e.printStackTrace()
}


/* HELPERS TO MANAGE DATES */

fun Date.asFormattedString(format: String, locale: Locale = Locale.getDefault()): String {
    val formatter = SimpleDateFormat(format, locale)
    return formatter.format(this)
}

fun getCurrentDateTime(): Date {
    return Calendar.getInstance().time
}

fun getTimestampAsString(timestamp: Long): String {
    return DateFormat.getDateInstance(DateFormat.MEDIUM).format(timestamp)
}

fun getTimestampAsENString(timestamp: Long): String {
    return DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.ENGLISH).format(timestamp)
}

//@RequiresApi(Build.VERSION_CODES.N)
//private fun getTimestampAsStringRecent(timestamp: Long): String {
//    return try {
//        val netDate = Date(timestamp)
//        val sdf = android.icu.text.SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
//        sdf.format(netDate)
//    } catch (e: Exception) {
//        e.toString()
//    }
//}

//private fun getTimestampAsStringOld(timestamp: Long): String {
//    return try {
//        val netDate = Date(timestamp)
//    } catch (e: Exception) {
//        e.toString()
//    }
//}
