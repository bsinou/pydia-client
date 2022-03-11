package org.sinou.android.pydia.ui.browse

import android.app.Application
import androidx.lifecycle.*
import com.pydio.cells.transport.StateID
import kotlinx.coroutines.*
import org.sinou.android.pydia.CellsApp
import org.sinou.android.pydia.services.NodeService

/**
 * Holds a live list of the offline roots for the current session
 */
class OfflineRootsViewModel(
    private val nodeService: NodeService,
    val stateID: StateID,
    application: Application
) : AndroidViewModel(application) {

    private val tag = OfflineRootsViewModel::class.simpleName

    private var viewModelJob = Job()
    private val vmScope = CoroutineScope(Dispatchers.Main + viewModelJob)

    val offlineRoots = CellsApp.instance.nodeService.listOfflineRoots(stateID)

    // Manage UI
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean>
        get() = _isLoading
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?>
        get() = _errorMessage

    fun forceRefresh() {
        setLoading(true)
        vmScope.launch {
            withContext(Dispatchers.Main) {
                // TODO handle errors
                nodeService.syncAll(stateID)
                setLoading(false)
            }
        }
    }

    fun setLoading(loading: Boolean) {
        _isLoading.value = loading
    }

    class OfflineRootsViewModelFactory(
        private val nodeService: NodeService,
        private val stateID: StateID,
        private val application: Application
    ) : ViewModelProvider.Factory {
        @Suppress("unchecked_cast")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(OfflineRootsViewModel::class.java)) {
                return OfflineRootsViewModel(nodeService, stateID, application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
