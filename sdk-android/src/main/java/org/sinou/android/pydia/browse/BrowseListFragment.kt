package org.sinou.android.pydia.browse

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import org.sinou.android.pydia.R
import org.sinou.android.pydia.databinding.FragmentBrowseListBinding

class BrowseListFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val binding: FragmentBrowseListBinding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_browse_list, container, false
        )

        return binding.root
    }
}
