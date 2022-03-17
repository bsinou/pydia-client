package org.sinou.android.pydia.tasks

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat.startActivity
import com.pydio.cells.transport.ServerURLImpl
import kotlinx.coroutines.launch
import org.sinou.android.pydia.AppNames
import org.sinou.android.pydia.AuthActivity
import org.sinou.android.pydia.CellsApp
import org.sinou.android.pydia.db.accounts.RLiveSession
import org.sinou.android.pydia.services.AccountService
import org.sinou.android.pydia.services.AuthService

fun loginAccount(
    context: Context,
    authService: AuthService,
    accountService: AccountService,
    session: RLiveSession,
): Boolean {

    CellsApp.instance.appScope.launch {

        // FIXME clean this when implementing custom certificate acceptance.
        val serverURL = ServerURLImpl.fromAddress(session.url, session.tlsMode == 1)

        if (session.isLegacy) {
            val toAuthIntent = Intent(context, AuthActivity::class.java)
            toAuthIntent.putExtra(AppNames.EXTRA_SERVER_URL, serverURL.toJson())
            toAuthIntent.putExtra(AppNames.EXTRA_SERVER_IS_LEGACY, true)
            toAuthIntent.putExtra(
                AppNames.EXTRA_AFTER_AUTH_ACTION,
                AuthService.NEXT_ACTION_ACCOUNTS
            )
            startActivity(context, toAuthIntent, null)

        } else {
            authService.createOAuthIntent(
                accountService, serverURL, AuthService.NEXT_ACTION_ACCOUNTS
            )?.let {
                startActivity(context, it, null)
            } ?: run {
                Log.e("LoginAccount()", "Could not create OAuth intent for ${serverURL.url}")
            }
        }
    }

    return true
}