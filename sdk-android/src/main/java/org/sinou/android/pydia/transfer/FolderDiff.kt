package org.sinou.android.pydia.transfer

import android.util.Log
import com.pydio.cells.api.Client
import com.pydio.cells.api.SDKException
import com.pydio.cells.api.ui.FileNode
import com.pydio.cells.api.ui.PageOptions
import com.pydio.cells.transport.StateID
import kotlinx.coroutines.*
import org.sinou.android.pydia.AppNames
import org.sinou.android.pydia.db.nodes.RTreeNode
import org.sinou.android.pydia.db.nodes.TreeNodeDao
import org.sinou.android.pydia.services.FileService
import org.sinou.android.pydia.utils.areNodeContentEquals
import java.io.File

class FolderDiff(
    private val client: Client,
    private val fileService: FileService,
    private val dao: TreeNodeDao,
    private val fileDL: FileDownloader?,
    private val thumbDL: ThumbDownloader,
    private val parentId: StateID,
//     private val downloadFiles: Boolean,
) {

    private val tag = FolderDiff::class.java.simpleName

    companion object {

        private const val PAGE_SIZE = 100

        fun firstPage(): PageOptions {
            val page = PageOptions()
            page.limit = PAGE_SIZE
            page.offset = 0
            page.currentPage = 0
            page.total = -1
            page.totalPages = -1
            return page
        }
    }

    private val folderDiffJob = Job()
    private val diffScope = CoroutineScope(Dispatchers.IO + folderDiffJob)

    private var changeNumber = 0

    /** Retrieve the meta of all readable nodes that are at the passed stateID */
    @Throws(SDKException::class)
    suspend fun compareWithRemote() = withContext(Dispatchers.IO) {
        val remotes = RemoteNodeIterator(parentId)
        val locals = dao.getNodesForDiff(parentId.id, parentId.file).iterator()
        // val locals = LocalNodeIterator(dao.getNodesForDiff(parentId.id, parentId.file).iterator())
        processChanges(remotes, locals)
        Log.d(tag, "Done with $changeNumber changes")
        return@withContext changeNumber
    }

    private fun processChanges(rit: Iterator<FileNode>, lit: Iterator<RTreeNode>) {

        var local = if (lit.hasNext()) lit.next() else null
        while (rit.hasNext()) {
            val remote = rit.next()
            if (local == null) {
                putAddChange(remote)
                continue
            } else {
                var order = remote.label.compareTo(local.name)

                while (order > 0 && lit.hasNext()) { // Next local is lexicographically smaller
                    putDeleteChange(local!!)
                    local = lit.next()
                    order = remote.label.compareTo(local.name)
                }
                if (order > 0) {
                    // last local is smaller than next remote, no more matches for any next remote
                    local = null
                } else if (order == 0) {
                    if (areNodeContentEquals(
                            remote,
                            local!!
                        )
                    ) { // Found a match, no change to report.
                        alsoCheckFile(remote, local)
                        alsoCheckThumb(remote, local)
                    } else {
                        putUpdateChange(remote, local)
                    }
                    // Move local cursor to next and restart the loop
                    local = if (lit.hasNext()) lit.next() else null
                    continue
                } else {
                    putAddChange(remote)
                    continue
                }
            }
        }

        // Delete remaining local nodes that have name greater than the last remote node
        local?.let { putDeleteChange(it) }
        while (lit.hasNext()) {
            local = lit.next()
            putDeleteChange(local)
        }
    }

    private fun alsoCheckFile(remote: FileNode, local: RTreeNode) {
        fileDL?.let {
            diffScope.launch {
                if (local.isFolder()) { // we do only files
                    return@launch
                }
                var doIt = local.localFilePath == null
                if (!doIt) { // we might have recorded the name but have a missing file
                    doIt = File(local.localFilePath!!).exists()
                }
                if (doIt) {
                    it.orderFileDL(local.encodedState)
                }
            }
        }
    }

    private fun alsoCheckThumb(remote: FileNode, local: RTreeNode) {
        // FIXME we miss the event when we have a filename but the thumb is not present
        if (remote.isImage && local.thumbFilename == null) {
            diffScope.launch {
                val childStateID = parentId.child(remote.label)
                thumbDL.orderThumbDL(childStateID.id)
            }
        }
    }

    private fun putAddChange(remote: FileNode) {
        Log.d(tag, "add for ${remote.label}")
        changeNumber++
        val childStateID = parentId.child(remote.label)
        val rNode = RTreeNode.fromFileNode(childStateID, remote)
        dao.insert(rNode)
        if (remote.isImage) {
            diffScope.launch {
                thumbDL.orderThumbDL(childStateID.id)
            }
        }
        fileDL?.let {
            diffScope.launch {
                it.orderFileDL(childStateID.id)
            }
        }
    }

    private fun putUpdateChange(remote: FileNode, local: RTreeNode) {
        Log.d(tag, "update for ${remote.label}")

        changeNumber++

        // TODO: Insure corner cases are correctly handled, typically on type switch
        val childStateID = parentId.child(remote.label)
        val rNode = RTreeNode.fromFileNode(childStateID, remote)

        if (local.isFolder() && remote.isFile) {
            dao.delete(local.encodedState)
            dao.deleteUnder(local.encodedState)
            // TODO also delete  thumbs **and** local files that have been created for this folder
//            val file = File(it)
//            if (file.exists()) {
//                file.delete()
//            }
            dao.insert(rNode)
        } else {
            dao.update(rNode)
        }
        if (remote.isImage) {
            diffScope.launch {
                thumbDL.orderThumbDL(childStateID.id)
            }
        }
        fileDL?.let {
            diffScope.launch {
                it.orderFileDL(childStateID.id)
            }
        }
    }

    private fun putDeleteChange(local: RTreeNode) {
        Log.d(tag, "delete for ${local.name}")
        changeNumber++

        // Also remove:
        // folder recursively
        // thumbs and thumb for children if we are in the case of a folder

        // FIXME rather implement clean recursive delete to correctly address:
        //   - potential offline roots
        //   - thumbs in child folders
        //   - ... what else ?
        // File and
        when {
            local.isFolder() -> {
                deleteLocalFolder(local)
            }
            else -> deleteLocalFile(local)
        }
    }

    private fun deleteLocalFile(local: RTreeNode) {
        // Cache or Offline
        fileService.getLocalPath(local, AppNames.LOCAL_FILE_TYPE_CACHE)?.let {
            val file = File(it)
            if (file.exists()) {
                file.delete()
            }
        }
        // thumbs
        fileService.getLocalPath(local, AppNames.LOCAL_FILE_TYPE_THUMB)?.let {
            val tf = File(it)
            if (tf.exists()) {
                tf.delete()
            }
        }
        dao.delete(local.encodedState)
    }

    private fun deleteLocalFolder(local: RTreeNode) {
        // Cache or Offline
        fileService.getLocalPath(local, AppNames.LOCAL_FILE_TYPE_CACHE)?.let {
            val file = File(it)
            if (file.exists()) {
                file.deleteRecursively()
            }
        }
        // TODO look for all thumbs defined for pre-viewable files that are in the child path
        //   and remove them.
        dao.delete(local.encodedState)
        dao.deleteUnder(local.encodedState)
    }

// Temp wrapper to add more logs
//    inner class LocalNodeIterator(private val nodes: Iterator<RTreeNode>) : Iterator<RTreeNode> {
//        override fun hasNext(): Boolean {
//            return nodes.hasNext()
//        }
//
//        override fun next(): RTreeNode {
//            val next = nodes.next()
//            Log.i(TAG, "Local: ${next.name}")
//            return next
//        }
//    }

    inner class RemoteNodeIterator(private val parentId: StateID) : Iterator<FileNode> {

        private val nodes = mutableListOf<FileNode>()

        private var nodeIterator = nodes.iterator()
        private var nextPage = firstPage()

        override fun hasNext(): Boolean {
            if (nodeIterator.hasNext()) {
                return true
            }

            if (nextPage.currentPage != nextPage.totalPages) {
                getNextPage(nextPage)
                nodeIterator = nodes.iterator()
            }

            return nodeIterator.hasNext()
        }

        override fun next(): FileNode {
            return nodeIterator.next()
        }

        private fun getNextPage(page: PageOptions) {
            nodes.clear()
            nextPage = client.ls(parentId.workspace, parentId.file, page) {
                if (it !is FileNode) {
                    Log.w(tag, "could not store node: $it")
                } else {
                    nodes.add(it)
                }
            }
        }
    }
}
