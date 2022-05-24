package com.ps.filepickerlib


import android.Manifest
import android.app.Activity.RESULT_OK
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import com.theartofdev.edmodo.cropper.CropImage
import com.theartofdev.edmodo.cropper.CropImageView
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*


/*Created by Parshva Shah Shah on 01-01-2022*/

class MediaSelectHelper(private val activity: AppCompatActivity) : MediaSelectOptions {


    private var canSelectMultipleFlag: Boolean = false
    private var canSelectMultipleVideo: Boolean = false
    private var isSelectingVideo: Boolean = false
    private var mMediaSelector: MediaSelector? = null
    private var mCurrentPhotoPath: String = ""
    private var photoFile: File? = null
    private var galleryIntent: Intent? = null
    private var videoIntent: Intent? = null
    private val permission = arrayOf(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.CAMERA
    )



    object Constant {
        const val CropSquare = "CropSquare"
        const val CropRectangle = "CropRectangle"
        const val CropCircle = "CropCircle"
    }

    private var cropType = Constant.CropSquare
    private var isCrop = true

    private var cameraResult: ActivityResultLauncher<Intent>
    private var activityResultLauncherCamera: ActivityResultLauncher<Array<String>>
    private var cropResult: ActivityResultLauncher<Intent>
    private var galleryResult: ActivityResultLauncher<Intent>
    private var galleryVideoResult: ActivityResultLauncher<Intent>
    private var activityResultLauncherGallery: ActivityResultLauncher<Array<String>>

    init {
        cameraResult =
            activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
                if (result.resultCode == RESULT_OK) {
                    val file = File(mCurrentPhotoPath)
                    if (Build.MANUFACTURER.equals("samsung")) {
                        val bitmap = getRottedBitmap(BitmapFactory.decodeFile(file.path), file.path)
                        try {
                            FileOutputStream(file).use { out ->
                                bitmap.compress(
                                    Bitmap.CompressFormat.PNG,
                                    50,
                                    out
                                ) // bmp is your Bitmap instance
                                // PNG is a lossless format, the compression factor (100) is ignored
                            }
                        } catch (e: IOException) {
                            e.printStackTrace()
                            Log.e("TAG", e.message ?: "")
                        }

                        openCropViewOrNot(Uri.fromFile(file))
                    } else {
                        openCropViewOrNot(Uri.fromFile(file))
                    }
                }
            }

        activityResultLauncherCamera =
            activity.registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grantResults ->
                if (checkAllPermission(grantResults)) {
                    openCamera()
                } else if (!checkAllPermission(grantResults)) {
                    if (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            !deniedForever(grantResults)
                        } else {
                            false
                        }
                    ) {
                        activity.showMessage("Please allow permission from setting ")
                    }
                }
            }

        activityResultLauncherGallery =
            activity.registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grantResults ->
                if (checkAllPermission(grantResults)) {
                    if (isSelectingVideo)
                        selectVideo()
                    else
                        openGallery()
                } else if (!checkAllPermission(grantResults)) {
                    if (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            !deniedForever(grantResults)
                        } else {
                            false
                        }
                    ) {
                        activity.showMessage("Please allow permission from setting ")
                    }
                }
            }

        galleryVideoResult =
            activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
                if (result.resultCode == RESULT_OK) {
                    if (result.resultCode == RESULT_OK) {
                        result.data?.apply {
                            if (!canSelectMultipleVideo) {
                                val retriever = MediaMetadataRetriever()
                                val selectedMedia: Uri? = data
                                val cR = activity.contentResolver
                                val mime = MimeTypeMap.getSingleton()
                                mime.getExtensionFromMimeType(cR.getType(selectedMedia!!))
                                mMediaSelector?.onVideoUri(selectedMedia)
                                retriever.release()
                            } else {
                                if (this.clipData != null) {
                                    val mClipData: ClipData? = this.clipData
                                    val mArrayUri = ArrayList<Uri>()
                                    for (i in 0 until mClipData?.itemCount!!) {
                                        val item = mClipData.getItemAt(i)
                                        val uri: Uri = item.uri
                                        mArrayUri.add(uri)
                                        // Get the cursor
                                    }
                                    mMediaSelector?.onVideoURIList(mArrayUri)
                                } else {
                                    val selectedMedia: Uri? = data
                                    selectedMedia?.let {
                                        ArrayList<Uri>().apply {
                                            add(it)
                                            mMediaSelector?.onVideoURIList(this)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        galleryResult =
            activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
                if (result.resultCode == RESULT_OK) {
                    result.data?.apply {
                        if (!canSelectMultipleFlag) {
                            val retriever = MediaMetadataRetriever()
                            val selectedMedia: Uri? = data
                            val cR = activity.contentResolver
                            val mime = MimeTypeMap.getSingleton()
                            val type = mime.getExtensionFromMimeType(cR.getType(selectedMedia!!))
                            if (Objects.requireNonNull(type)
                                    .equals("png", ignoreCase = true) || type!!.equals(
                                    "jpeg",
                                    ignoreCase = true
                                ) || type.equals("jpg", ignoreCase = true)
                            ) {
                                if (selectedMedia.toString().contains("image")) {
                                    openCropViewOrNot(selectedMedia)
                                } else {
                                    openCropViewOrNot(selectedMedia)
                                }
                            }
                            retriever.release()
                        } else {
                            if (this.clipData != null) {
                                val mClipData: ClipData? = this.clipData
                                val mArrayUri = ArrayList<Uri>()
                                for (i in 0 until mClipData?.itemCount!!) {
                                    val item = mClipData.getItemAt(i)
                                    val uri: Uri = item.uri
                                    mArrayUri.add(uri)
                                    // Get the cursor
                                }
                                mMediaSelector?.onImageUriList(mArrayUri)
                            } else {
                                val selectedMedia: Uri? = data
                                selectedMedia?.let {
                                    ArrayList<Uri>().apply {
                                        add(it)
                                        mMediaSelector?.onImageUriList(this)
                                    }
                                }
                            }
                        }
                    }
                }
            }

        cropResult =
            activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { data: ActivityResult ->
                if (data.resultCode == RESULT_OK) {
                    val result = CropImage.getActivityResult(data.data)
                    val resultUri = result.uri
                    try {
                        mMediaSelector?.onImageUri(resultUri)
                    } catch (e: FileNotFoundException) {
                        e.printStackTrace()
                    }
                }
            }



        isSelectMultipleImage(false)
        isSelectMultipleVideo(false)
    }


    /*Register This for getting callbacks*/
    fun registerCallback(mMediaSelector: MediaSelector) {
        this.mMediaSelector = null
        this.mMediaSelector = mMediaSelector
    }

    override fun selectOptionsForImagePicker(isCrop1: Boolean, cropType: String) {
        isSelectingVideo = false
        this.cropType = cropType
        this.isCrop = isCrop1
        val builder = AlertDialog.Builder(activity)
        builder.setTitle("Choose Image Source")

        val items = arrayOf(
            activity.resources.getString(R.string.label_camera),
            activity.resources.getString(R.string.label_gallery)
        )

        builder.setItems(items) { _, which ->

            when (which) {
                0 -> openCamera()
                1 -> openGallery()
            }
        }

        builder.show()
    }


    override fun selectVideo() {
        isSelectingVideo = true
        if (ContextCompat.checkSelfPermission(
                activity,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            activityResultLauncherGallery.launch(permission)
        } else {
            galleryVideoResult.launch(videoIntent)
        }
    }


    /*To show it as popup menu*/
    override fun selectImageWithMenu(
        view: View,
        isCrop1: Boolean,
        cropType: String
    ) {
        isSelectingVideo = false
        this.cropType = cropType
        this.isCrop = isCrop1
        val popup = PopupMenu(activity, view)
        popup.menu.add("Camera")
        popup.menu.add("Gallery")
        popup.setOnMenuItemClickListener { item ->

            when (item.title.toString()) {
                "Camera" -> {
                    openCamera()
                }
                "Gallery" -> {
                    openGallery()
                }
            }
            true
        }
        popup.show()


    }

    // Core Code for selecting image
    // ****************************
    // ****************************
    // ****************************

    /**
     * Open camera to click image
     */
    private fun openCamera() {

        if (ContextCompat.checkSelfPermission(
                activity,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                activity,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                activity,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            activityResultLauncherCamera.launch(permission)
        } else {
            dispatchTakePictureIntent()
        }

    }

    private fun dispatchTakePictureIntent() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(activity.packageManager) != null) {
            // Create the File where the photo should go
            try {
                photoFile = createImageFile()
            } catch (ex: IOException) {
                // Error occurred while creating the File
                ex.printStackTrace()
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                val photoURI = FileProvider.getUriForFile(
                    activity,
                    "${activity.packageName}.provider",
                    photoFile!!
                )
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                takePictureIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                cameraResult.launch(takePictureIntent)
            } else {
                takePictureIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                cameraResult.launch(takePictureIntent)
            }
        }
    }

    /**
     * Open gallery for select single image
     */
    private fun openGallery() {
        if (ContextCompat.checkSelfPermission(
                activity,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            activityResultLauncherGallery.launch(permission)
        } else {
//            galleryResult.launch(galleryIntent)
            isSelectMultipleImage(canSelectMultipleFlag)
            galleryResult.launch(galleryIntent)
        }
    }

    override fun isSelectMultipleImage(canSelect: Boolean) {
        canSelectMultipleFlag = canSelect
        if (canSelect) {
            galleryIntent = Intent(Intent.ACTION_GET_CONTENT)
            galleryIntent!!.type = "image/*"
            galleryIntent!!.putExtra(
                Intent.EXTRA_MIME_TYPES,
                arrayOf("image/jpeg", "image/png")
            )

            galleryIntent!!.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        } else {
            galleryIntent = Intent(
                Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            )
            galleryIntent!!.type = "image/*"
        }
    }

    override fun isSelectMultipleVideo(canSelect: Boolean) {
        canSelectMultipleVideo = canSelect
        if (canSelect) {
            canSelectMultipleVideo = canSelect
            videoIntent = Intent(Intent.ACTION_GET_CONTENT)
            videoIntent!!.type = "*/*"
            videoIntent!!.putExtra(
                Intent.EXTRA_MIME_TYPES,
                arrayOf("video/*")
            )

            videoIntent!!.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        } else {
            videoIntent = Intent(
                Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            )
            videoIntent!!.type = "video/*"
        }
    }

    private fun getRottedBitmap(bitmap: Bitmap, photoPath: String): Bitmap {
        val orientation: Int
        var ei: ExifInterface? = null
        try {
            ei = ExifInterface(photoPath)
        } catch (e: IOException) {
            e.printStackTrace()
        }

        if (ei != null) {
            orientation = ei.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_UNDEFINED
            )
        } else {
            return bitmap
        }

        return when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> rotateImage(bitmap, 90f)

            ExifInterface.ORIENTATION_ROTATE_180 -> rotateImage(bitmap, 180f)

            ExifInterface.ORIENTATION_ROTATE_270 -> rotateImage(bitmap, 270f)


            ExifInterface.ORIENTATION_NORMAL -> bitmap
            else -> rotateImage(bitmap, 90f)
        }
    }

    private fun rotateImage(source: Bitmap, angle: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(angle)
        return Bitmap.createBitmap(
            source, 0, 0, source.width, source.height,
            matrix, true
        )
    }

    private fun openCropViewOrNot(file: Uri) {
        if (isCrop) {
            val intent: Intent = when (this.cropType) {
                Constant.CropSquare -> CropImage.activity(file)
                    .setAspectRatio(4, 4)
                    .getIntent(activity)
                Constant.CropRectangle -> CropImage.activity(file)
                    .setAspectRatio(6, 4)
                    .getIntent(activity)
                Constant.CropCircle -> CropImage.activity(file)
                    .setCropShape(CropImageView.CropShape.OVAL)
                    .setAspectRatio(1, 1)
                    .getIntent(activity)
                else -> CropImage.activity(file)
                    .getIntent(activity)
            }
            cropResult.launch(intent)
        } else {
            mMediaSelector?.onImageUri(file)
        }
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat(
            "yyyyMMdd_HHmmss",
            Locale.getDefault()
        ).format(Date())
        val storageDir = activity.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val image = File.createTempFile(
            timeStamp, /* prefix */
            ".jpg", /* suffix */
            storageDir      /* directory */
        )
        mCurrentPhotoPath = image.absolutePath
        return image
    }

    private fun checkAllPermission(grantResults: MutableMap<String, Boolean>): Boolean {
        for (data in grantResults) {
            if (!data.value)
                return false
        }
        return true
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun deniedForever(grantResults: MutableMap<String, Boolean>): Boolean {
        for (data in grantResults) {
            if (!activity.shouldShowRequestPermissionRationale(data.key))
                return false
        }
        return true
    }
}


interface MediaSelector {
    fun onImageUri(uri: Uri) {}
    fun onVideoUri(uri: Uri) {
    }

    fun onImageUriList(uriArrayList: ArrayList<Uri>) {}
    fun onVideoURIList(uriArrayList: ArrayList<Uri>) {

    }

}

fun Context.showMessage(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}