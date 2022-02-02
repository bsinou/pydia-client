package org.sinou.android.pydia.transfer

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.sinou.android.pydia.AppNames
import org.sinou.android.pydia.CellsApp
import org.sinou.android.pydia.services.NodeService
import org.sinou.android.pydia.ui.browse.TreeNodeMenuViewModel
import org.sinou.android.pydia.utils.DEFAULT_FILE_PROVIDER_ID
import org.sinou.android.pydia.utils.asFormattedString
import org.sinou.android.pydia.utils.getCurrentDateTime
import java.io.File
import java.io.IOException

class FileImporter(
    private val registry: ActivityResultRegistry,
    private val nodeService: NodeService,
    private val nodeMenuVM: TreeNodeMenuViewModel,
    private val caller: String,
    private val callingFragment: BottomSheetDialogFragment,
) : DefaultLifecycleObserver {

    private val tag = "FileImporter"
    private val getContentKey = AppNames.KEY_PREFIX_ + "select.files"
    private val takePictureKey = AppNames.KEY_PREFIX_ + "take.picture"

    private lateinit var getMultipleContents: ActivityResultLauncher<String>
    private lateinit var takePicture: ActivityResultLauncher<Uri>

    override fun onCreate(owner: LifecycleOwner) {
        getMultipleContents = registry.register(
            getContentKey,
            owner,
            ActivityResultContracts.GetMultipleContents()
        )
        { uris ->
            for (uri in uris) {
                nodeService.enqueueUpload(nodeMenuVM.stateID, uri)
            }
            callingFragment.dismiss()
        }

        takePicture = registry.register(
            takePictureKey,
            owner,
            TakePictureToInternalStorage()
        ) { pictureTaken ->
            if (!pictureTaken) {
                // Does not work...
                // Toast.makeText(callingFragment.requireContext(), "blah", Toast.LENGTH_LONG).show()
                // showLongMessage(callingFragment.requireActivity().baseContext, "Operation aborted by user")
                callingFragment.dismiss()
            } else {
                nodeMenuVM.targetUri?.let {
                    nodeService.enqueueUpload(nodeMenuVM.stateID, it)
                    callingFragment.dismiss()
                }

                //            parentID?.let {
//                Log.i(caller, "Received file at $uri")
//
//            } ?: run {
//                Log.w(caller, "Received file at $uri with **no** parent stateID")
//            }
            }
        }
    }

    fun selectFiles() { // we do not assume any specific type
        getMultipleContents.launch("*/*")
    }

    suspend fun takePicture() = withContext(Dispatchers.IO) {
        doTakePicture()
    }

    private fun doTakePicture() {
        val photoFile: File? = try {
            createImageFile()
        } catch (ex: IOException) {
            Log.e(tag, "Cannot create photo file")
            ex.printStackTrace()
            // Error occurred while creating the File
            null
        }
        // Continue only if the File was successfully created
        photoFile?.also {
            val uri: Uri = FileProvider.getUriForFile(
                callingFragment.requireContext(),
                DEFAULT_FILE_PROVIDER_ID,
                it
            )
            nodeMenuVM.prepareImport(uri)
            takePicture.launch(uri)
        }
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        // Create an image file name
        val timestamp = getCurrentDateTime().asFormattedString("yyMMdd_HHmmss")
        val storageDir =
            File(CellsApp.instance.filesDir.absolutePath + "/" + (Environment.DIRECTORY_PICTURES)!!)
        storageDir.mkdirs()
        return File(storageDir.absolutePath + File.pathSeparator + "IMG_${timestamp}_.jpg")

        // Would be safer but with an ugly name :(
        //        return File.createTempFile(
//            "IMG_${timestamp}_", /* prefix */
//            ".jpg", /* suffix */
//            storageDir /* directory */
//        )
    }
}

private class TakePictureToInternalStorage : ActivityResultContracts.TakePicture() {
    override fun createIntent(context: Context, input: Uri): Intent {
        return super.createIntent(context, input).addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
    }
}