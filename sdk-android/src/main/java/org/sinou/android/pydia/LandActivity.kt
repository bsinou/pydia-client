package org.sinou.android.pydia

import android.content.Intent
import android.graphics.drawable.TransitionDrawable
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.*

/**
 * Manage default pages of the app.
 */
class LandActivity : AppCompatActivity() {

    private val tag = "MainActivity"

    private var nextPage: Intent? = null

    companion object {
        const val tickDuration = 1000L
        const val logoCrossFadeDurationMillis = 300
        const val spacingAfterFadeDurationMillis = 150
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.i(tag, "### onCreate, bundle: $savedInstanceState")
        super.onCreate(savedInstanceState)
        // Compute next destination
        chooseFirstPage()

//        if (intent?.categories?.contains(Intent.CATEGORY_LAUNCHER) ?: false) {
//            chooseFirstPage()
//            fadeOut(savedInstanceState)
//        } else {
//            startActivity(Intent(this, HomeActivity::class.java))
//        }
    }

    override fun onResume() {
        Log.i(tag, "### onResume, next page: $nextPage")
        super.onResume()
    }

    private suspend fun waitForIt() {
        repeat(10) { // we wait at most ten seconds before crashing
            Log.i(tag, "Waiting for backend to be ready")
            if (CellsApp.instance.ready) {
                if (CellsApp.instance.accountService.sessionFactory.isReady()) {
                    Log.i(tag, "### Backend is now ready")
                    return
                }
            }
            delay(tickDuration)
        }
    }

    // Thanks to https://www.tiagoloureiro.tech/posts/definitive-guide-for-splash-screen-android/
    private fun fadeOut(savedInstanceState: Bundle?) {
        // Small trick: we check if we have a saved bundle to avoid showing the splash twice
        // val alreadyShown = savedInstanceState != null
        val alreadyShown = true
        if (!alreadyShown) {
            (window.decorView.background as TransitionDrawable).startTransition(
                logoCrossFadeDurationMillis
            )
            // Use a coroutine to block during animation, then sets the view
            lifecycleScope.launch {
                // Time between the cross fade and start screen animation
                delay(logoCrossFadeDurationMillis.toLong() + spacingAfterFadeDurationMillis)
                window.decorView.background = AppCompatResources.getDrawable(
                    this@LandActivity, R.drawable.background
                )
                Log.i(tag, "### Animation terminated")
                setContentView(R.layout.activity_land)
            }
        } else {
            // Splash was shown before, no need to animate the transition.
            // 1 - Sets the window background to the background without the logo (if needed)
            window.decorView.background = AppCompatResources.getDrawable(
                this, R.drawable.background
            )
            // setContentView(R.layout.activity_land)
        }
    }

    private fun chooseFirstPage() {
        val act = this
        lifecycleScope.launch {
            waitForIt()

            // Try to restart from where we left it
            CellsApp.instance.lastState()?.let {
                val tmp = Intent(act, BrowseActivity::class.java)
                tmp.putExtra(AppNames.EXTRA_STATE, it.id)
                nextPage = tmp
                navigate(act)
                return@launch
            }

            // Choose between new account or account list when we have no state.
            // We go to workspace list when we have only one account
            val accounts = withContext(Dispatchers.IO) {
                CellsApp.instance.accountService.accountDB.accountDao().getAccounts()
            }
            val size = accounts.size
            nextPage = when {
                size == 0 -> Intent(act, AuthActivity::class.java)
                size == 1 -> {
                    val tmp = Intent(act, BrowseActivity::class.java)
                    tmp.putExtra(AppNames.EXTRA_STATE, accounts[0].accountID)
                    tmp
                }
                size > 1 -> Intent(act, AccountActivity::class.java)
                else -> null
            }

            navigate(act)
        }
    }

    private fun navigate(act: LandActivity) {
        Log.i(tag, "### after init, next page: $nextPage")
        nextPage?.let {
            startActivity(it)
            nextPage = null
        } ?: run {
            startActivity(Intent(act, HomeActivity::class.java))
        }
    }
}
