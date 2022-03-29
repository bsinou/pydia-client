package org.sinou.android.pydia.ui.account

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.pydio.cells.transport.StateID
import kotlinx.coroutines.launch
import org.sinou.android.pydia.*
import org.sinou.android.pydia.databinding.FragmentAccountListBinding
import org.sinou.android.pydia.tasks.loginAccount
import org.sinou.android.pydia.ui.common.deleteAccount
import org.sinou.android.pydia.ui.common.logoutAccount

/**
 * Holds a list with the defined accounts and their status to switch between accounts
 * and log in and out.
 * Account details is not yet implemented
 */
class AccountListFragment : Fragment() {

    private val fTag = AccountListFragment::class.java.simpleName

    private lateinit var binding: FragmentAccountListBinding
    private lateinit var accountListViewModel: AccountListViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.e(fTag, "onCreateView ${savedInstanceState?.getString(AppNames.EXTRA_STATE)}")
        binding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_account_list, container, false
        )

        val application = requireNotNull(this.activity).application
        val viewModelFactory = AccountListViewModel.AccountListViewModelFactory(
            CellsApp.instance.accountService,
            application
        )
        val tmp: AccountListViewModel by viewModels { viewModelFactory }
        accountListViewModel = tmp

        val adapter = AccountListAdapter { accountID, action ->
            onAccountClicked(accountID, action)
        }
        binding.accountList.adapter = adapter

        accountListViewModel.sessions.observe(viewLifecycleOwner) {
            if (it.isEmpty()) {
                binding.emptyContent.visibility = View.VISIBLE
                binding.accountList.visibility = View.GONE
            } else {
                binding.accountList.visibility = View.VISIBLE
                binding.emptyContent.visibility = View.GONE
                adapter.submitList(it)
            }
        }

        binding.newAccountFab.setOnClickListener {
            val toAuthIntent = Intent(requireActivity(), AuthActivity::class.java)
            startActivity(toAuthIntent)
        }

        setHasOptionsMenu(true)
        return binding.root
    }

    private fun onAccountClicked(accountID: String, action: String) {
        Log.i(fTag, "ID: $accountID, do $action")
        when (action) {
            AppNames.ACTION_LOGIN -> {
                val currSession = accountListViewModel.sessions.value
                    ?.filter { it.accountID == accountID }
                    ?.get(0)
                if (currSession == null) {
                    Log.i(fTag, "No live session found for: $accountID in ViewModel, aborting...")
                    return
                }
                loginAccount(
                    requireContext(),
                    accountListViewModel.accountService.authService,
                    accountListViewModel.accountService,
                    currSession
                )
            }
            AppNames.ACTION_LOGOUT -> lifecycleScope.launch {
                logoutAccount(requireContext(), accountID)
            }
            AppNames.ACTION_FORGET -> {
                deleteAccount(requireContext(), accountID)
            }
            AppNames.ACTION_OPEN -> lifecycleScope.launch {
                CellsApp.instance.accountService.openSession(accountID)
                CellsApp.instance.setCurrentState(StateID.fromId(accountID))
                val intent = Intent(requireActivity(), MainActivity::class.java)
                intent.putExtra(AppNames.EXTRA_STATE, accountID)
                startActivity(intent)
            }
            else -> return // do nothing
        }
    }
}
