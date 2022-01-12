package org.sinou.android.pydia.account

import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.databinding.BindingAdapter
import org.sinou.android.pydia.AppNames.*
import org.sinou.android.pydia.R
import org.sinou.android.pydia.room.account.RLiveSession

@BindingAdapter("accountStatus")
fun ImageView.setAccountImage(item: RLiveSession?) {
    item?.let {
        setImageResource(
            when (item.authStatus) {
                //AUTH_STATUS_NEW -> R.drawable.icon_folder
                AUTH_STATUS_NO_CREDS -> R.drawable.ic_outline_running_with_errors_24
                AUTH_STATUS_EXPIRED -> R.drawable.ic_outline_running_with_errors_24
                AUTH_STATUS_REFRESHING -> R.drawable.ic_baseline_wifi_protected_setup_24
                AUTH_STATUS_CONNECTED -> R.drawable.ic_baseline_check_24
                else -> R.drawable.empty
            }
        )

        imageTintList = ContextCompat.getColorStateList(
            this.context,
            when (item.authStatus) {
                //AUTH_STATUS_NEW -> R.drawable.icon_folder
                AUTH_STATUS_NO_CREDS -> R.color.danger
                AUTH_STATUS_EXPIRED -> R.color.danger
                AUTH_STATUS_REFRESHING -> R.color.warning
                AUTH_STATUS_CONNECTED -> R.color.ok
                else -> R.color.transparent
            }
        )
    }
}

@BindingAdapter("authAction")
fun ImageView.setAuthAction(item: RLiveSession?) {
    item?.let {

        setImageResource(
            when (item.authStatus) {
                //AUTH_STATUS_NEW -> R.drawable.icon_folder
                AUTH_STATUS_NO_CREDS -> R.drawable.ic_baseline_login_24
                AUTH_STATUS_EXPIRED -> R.drawable.ic_baseline_login_24
                AUTH_STATUS_REFRESHING -> R.drawable.ic_baseline_login_24
                AUTH_STATUS_CONNECTED -> R.drawable.ic_baseline_logout_24
                else -> R.drawable.empty
            }
        )
    }
}


@BindingAdapter("account_primary_text")
fun TextView.setAccountPrimaryText(item: RLiveSession?) {
    item?.let {
        var legacy = ""
        if (item.isLegacy) {
            legacy = "(Legacy)"
        }
        text = "${item.serverLabel} ${legacy} "
    }

}

@BindingAdapter("account_secondary_text")
fun TextView.setAccountSecondaryText(item: RLiveSession?) {
    item?.let {
        text = "${item.username}@${item.url}"
    }
}