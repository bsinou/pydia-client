package org.sinou.android.pydia.home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import org.sinou.android.pydia.AccountActivity
import org.sinou.android.pydia.AppNames
import org.sinou.android.pydia.R
import org.sinou.android.pydia.databinding.FragmentLandingBinding

/**
 * Default fragment that is shown when no other better choice is found.
 * Presents the user with the option to create a first account.
 */
class LandingFragment : Fragment(), AppNames {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        val binding: FragmentLandingBinding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_landing, container, false
        )

        binding.addAccountButton.setOnClickListener {
            // Launch the account activity with a new intent
            val toBrowseIntent = Intent(requireActivity(), AccountActivity::class.java)
            // TODO not yet plugged
            // toBrowseIntent.putExtra(AppNames.KEY_DESTINATION, "ServerUrlFragment")
            startActivity(toBrowseIntent);
        }

        setHasOptionsMenu(true)
        return binding.root
    }
}
