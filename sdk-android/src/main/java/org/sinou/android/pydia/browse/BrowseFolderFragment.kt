package org.sinou.android.pydia.browse

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.pydio.cells.api.SdkNames
import com.pydio.cells.transport.StateID
import com.pydio.cells.utils.Str
import kotlinx.coroutines.launch
import org.sinou.android.pydia.CellsApp
import org.sinou.android.pydia.MainActivity
import org.sinou.android.pydia.MainNavDirections
import org.sinou.android.pydia.R
import org.sinou.android.pydia.databinding.FragmentBrowseFolderBinding
import org.sinou.android.pydia.room.browse.RTreeNode
import org.sinou.android.pydia.utils.*

class BrowseFolderFragment : Fragment() {

    companion object {
        private const val fTag = "BrowseFolderFragment"

        const val ACTION_MORE = "more"
        const val ACTION_OPEN = "open"
    }

    private lateinit var binding: FragmentBrowseFolderBinding
    private lateinit var treeFolderVM: TreeFolderViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_browse_folder, container, false
        )

        val args: BrowseFolderFragmentArgs by navArgs()

        val viewModelFactory = TreeFolderViewModel.TreeFolderViewModelFactory(
            CellsApp.instance.accountService,
            CellsApp.instance.nodeService,
            StateID.fromId(args.state),
            requireActivity().application,
        )

        val tmpVM: TreeFolderViewModel by viewModels { viewModelFactory }
        treeFolderVM = tmpVM

        val adapter = NodeListAdapter { node, action -> onClicked(node, action) }
        binding.nodes.adapter = adapter
        treeFolderVM.children.observe(
            viewLifecycleOwner,
            {
                // When Adapter is a List adapter
                adapter.submitList(it)
            },
        )

        val backPressedCallback = BackStackAdapter.initialised(
            parentFragmentManager,
            findNavController(),
            StateID.fromId(args.state)
        )
        requireActivity().onBackPressedDispatcher.addCallback(this, backPressedCallback)
        return binding.root
    }

    private fun onClicked(node: RTreeNode, command: String) {
        Log.i(fTag, "Clicked on ${treeFolderVM.stateID} -> $command")
        when (command) {
            ACTION_OPEN -> navigateTo(node)
            ACTION_MORE -> {
                val action = BrowseFolderFragmentDirections
                    .openMoreMenu(
                        node.encodedState, when (node.mime) {
                            SdkNames.NODE_MIME_RECYCLE -> TreeNodeActionsFragment.CONTEXT_RECYCLE
                            else -> TreeNodeActionsFragment.CONTEXT_BROWSE
                        }
                    )
                binding.browseFolderFragment.findNavController().navigate(action)
            }

            else -> return // Unknown action, log warning and returns
        }
    }

    override fun onResume() {
        super.onResume()
        dumpBackStack(fTag, parentFragmentManager)

        treeFolderVM.resume()

        (requireActivity() as MainActivity).supportActionBar?.let {
            it.title = if (Str.empty(treeFolderVM.stateID.fileName)) {
                treeFolderVM.stateID.workspace
            } else if ("/recycle_bin" == treeFolderVM.stateID.file) {
                resources.getString(R.string.recycle_bin_label)
            } else {
                treeFolderVM.stateID.fileName
            }
            it.setDisplayHomeAsUpEnabled(true)
        }
    }

    override fun onPause() {
        super.onPause()
        treeFolderVM.pause()
    }

    override fun onDetach() {
        Log.i(fTag, "... About to detach:")
        super.onDetach()
        resetToHomeStateIfNecessary(parentFragmentManager, treeFolderVM.stateID)
    }

    private fun navigateTo(node: RTreeNode) = lifecycleScope.launch {
        if (isFolder(node)) {
            CellsApp.instance.setCurrentState(StateID.fromId(node.encodedState))
            findNavController().navigate(MainNavDirections.openFolder(node.encodedState))
            return@launch
        }
        val file = CellsApp.instance.nodeService.getOrDownloadFileToCache(node)
        file?.let {
            val intent = externallyView(requireContext(), file, node)
            try {
                startActivity(intent)
                // FIXME DEBUG only
                val msg = "Opened ${it.name} (${intent.type}) with external viewer"
                Log.e(tag, "Intent success: $msg")
            } catch (e: Exception) {
                val msg = "Cannot open ${it.name} (${intent.type}) with external viewer"
                Toast.makeText(requireActivity().application, msg, Toast.LENGTH_LONG).show()
                Log.e(tag, "Call to intent failed: $msg")
                e.printStackTrace()
            }
        }
    }
}
