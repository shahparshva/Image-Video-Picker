package com.ps.filepickerlib


import android.Manifest
import android.app.Activity.RESULT_OK
import android.content.ClipData
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
import androidx.fragment.app.Fragment
import com.theartofdev.edmodo.cropper.CropImage
import com.theartofdev.edmodo.cropper.CropImageView
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.lang.ref.WeakReference
import java.text.SimpleDateFormat
import java.util.*


/*Created by Parshva Shah Shah on 01-01-2022*/

class FileSelector private constructor(val builder: Builder) : FileSelectorOptions {


    private var canSelectMultiple: Boolean = false
    lateinit var activity: WeakReference<AppCompatActivity>
    private var mMediaSelector: MediaSelectorBuilder? = null
    private var mCurrentPhotoPath: String = ""
    private var photoFile: File? = null
    /*  private var galleryIntent: Intent? = null
      private var videoIntent: Intent? = null*/


    private var selectionIntent: Intent? = null


    private val permission = arrayOf(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.CAMERA
    )


    private var cropType: CropType = CropType.CropSquare
    private var selectionType: SelectionType = SelectionType.Image

    private var isCrop = true

    private lateinit var cameraResult: ActivityResultLauncher<Intent>

    private lateinit var fileSelectionLauncher: ActivityResultLauncher<Intent>

    //    private var galleryResult: ActivityResultLauncher<Intent>


    private lateinit var cropResult: ActivityResultLauncher<Intent>


    private lateinit var cameraPermissionLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var galleryPermissionLauncher: ActivityResultLauncher<Array<String>>


    class Builder {
        lateinit var activity: WeakReference<AppCompatActivity>
        lateinit var fragmentRefrence: WeakReference<Fragment>
        lateinit var cropType: CropType
        var allowCrop: Boolean = false
        var isAllowMultiple: Boolean = false
        var isFromFragment: Boolean = false
        lateinit var mMediaSelector: MediaSelectorBuilder


        fun setActivityReference(weakReference: WeakReference<AppCompatActivity>): Builder {
            isFromFragment = false
            activity = weakReference
            return this
        }

        fun setFragmentReference(weakReference: WeakReference<Fragment>): Builder {
            isFromFragment = true
            fragmentRefrence = weakReference
//            activity = WeakReference(fragmentRefrence.get()?.activity as AppCompatActivity)
            return this
        }

        fun setMediaSelectCallBack(mMediaSelector: MediaSelectorBuilder): Builder {
            this.mMediaSelector = mMediaSelector
            return this
        }

        fun setAllowMultiple(isAllowMultiple: Boolean): Builder {
            this.isAllowMultiple = isAllowMultiple
            return this
        }

        fun setAllowCrop(boolean: Boolean): Builder {
            allowCrop = boolean
            return this
        }

        fun setCropType(type: CropType): Builder {
            this.cropType = type
            return this
        }
/*
        fun setSelectionType(selectionType: SelectionType): Builder {
            this.selectionType = selectionType
            return this
        }*/

        fun build(): FileSelector {
            return FileSelector(this)
        }


    }

    init {


        /* if (builder.isFromFragment) {
             initCallBackWithFragment()
         } else {
             initCallBack()
         }*/

        if (!builder.isFromFragment) {
            this.activity = builder.activity
        }


        this.isCrop = builder.allowCrop
        this.canSelectMultiple = builder.isAllowMultiple
        this.cropType = builder.cropType
        this.mMediaSelector = builder.mMediaSelector

        initCallBack()
    }


    enum class CropType {
        CropSquare, CropRectangle, CropCircle
    }

    enum class SelectionType {
        Image, Video, PDF, Any
    }


    private fun initCallBack() {
        cameraResult = let {
            if (builder.isFromFragment)
                builder.fragmentRefrence.get()
            else
                activity.get()
        }?.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == RESULT_OK) {
                val file = File(mCurrentPhotoPath)
                if (Build.MANUFACTURER.equals("samsung")) {
                    val bitmap =
                        getRottedBitmap(BitmapFactory.decodeFile(file.path), file.path)
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
        }!!

        cameraPermissionLauncher =
            let {
                if (builder.isFromFragment)
                    builder.fragmentRefrence.get()
                else
                    activity.get()
            }?.registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grantResults ->
                if (checkAllPermission(grantResults)) {
                    openCamera()
                } else if (!checkAllPermission(grantResults)) {
                    if (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            !deniedForever(grantResults)
                        } else {
                            false
                        }
                    ) {
                        //Show error to user
                        Toast.makeText(
                            activity.get(),
                            "Please allow permission from setting ",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }!!

        galleryPermissionLauncher =
            let {
                if (builder.isFromFragment)
                    builder.fragmentRefrence.get()
                else
                    activity.get()
            }?.registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grantResults ->
                if (checkAllPermission(grantResults)) {

                    when (selectionType) {
                        SelectionType.Image -> openGallery()
                        SelectionType.Video -> selectVideo()
                        SelectionType.PDF -> {}
                        SelectionType.Any -> {}
                    }
                } else if (!checkAllPermission(grantResults)) {
                    if (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            !deniedForever(grantResults)
                        } else {
                            false
                        }
                    ) {
                        // show error to user
                        Toast.makeText(
                            activity.get(),
                            "Please allow permission from setting ",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }!!


        fileSelectionLauncher =
            let {
                if (builder.isFromFragment)
                    builder.fragmentRefrence.get()
                else
                    activity.get()
            }?.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
                if (result.resultCode == RESULT_OK) {
                    result.data?.apply {


                        when (selectionType) {
                            SelectionType.Image -> {
                                if (!canSelectMultiple) {
                                    val retriever = MediaMetadataRetriever()
                                    val selectedMedia: Uri? = data
                                    val cR = activity.get()?.contentResolver
                                    val mime = MimeTypeMap.getSingleton()
                                    val type =
                                        mime.getExtensionFromMimeType(cR!!.getType(selectedMedia!!))
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
                                        mMediaSelector?.onMultipleFileSelected(mArrayUri)
                                    } else {
                                        val selectedMedia: Uri? = data
                                        selectedMedia?.let {
                                            ArrayList<Uri>().apply {
                                                add(it)
                                                mMediaSelector?.onMultipleFileSelected(this)
                                            }
                                        }
                                    }
                                }
                            }
                            else -> {
                                if (!canSelectMultiple) {
                                    val retriever = MediaMetadataRetriever()
                                    val selectedMedia: Uri? = data
                                    val cR = activity.get()?.contentResolver
                                    val mime = MimeTypeMap.getSingleton()
                                    mime.getExtensionFromMimeType(cR!!.getType(selectedMedia!!))
                                    mMediaSelector?.onSingleFileSelected(selectedMedia)
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
                                        mMediaSelector?.onMultipleFileSelected(mArrayUri)
                                    } else {
                                        val selectedMedia: Uri? = data
                                        selectedMedia?.let {
                                            ArrayList<Uri>().apply {
                                                add(it)
                                                mMediaSelector?.onMultipleFileSelected(this)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }!!

        cropResult =
            let {
                if (builder.isFromFragment)
                    builder.fragmentRefrence.get()
                else
                    activity.get()
            }?.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { data: ActivityResult ->
                if (data.resultCode == RESULT_OK) {
                    val result = CropImage.getActivityResult(data.data)
                    val resultUri = result.uri
                    try {
                        mMediaSelector?.onSingleFileSelected(resultUri)
                    } catch (e: FileNotFoundException) {
                        e.printStackTrace()
                    }
                }
            }!!


    }


    private fun setupIntent() {

        if (builder.isFromFragment)
            activity = WeakReference(builder.fragmentRefrence.get()?.activity as AppCompatActivity)

        when (selectionType) {
            SelectionType.Image -> {
                selectionIntent = Intent(
                    Intent.ACTION_PICK,
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                )
                selectionIntent!!.type = "image/*"
                selectionIntent!!.putExtra(
                    Intent.EXTRA_MIME_TYPES,
                    arrayOf("image/jpeg", "image/png")
                )

                if (canSelectMultiple) {
                    selectionIntent!!.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                }
            }
            SelectionType.Video -> {
                selectionIntent = Intent(
                    Intent.ACTION_PICK,
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                )
                selectionIntent!!.type = "video/*"

                if (canSelectMultiple) {
                    selectionIntent!!.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                }
            }
            SelectionType.PDF -> {
                selectionIntent = Intent(Intent.ACTION_GET_CONTENT)
                selectionIntent!!.type = "application/pdf"

                if (canSelectMultiple) {
                    selectionIntent!!.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                }

            }
            SelectionType.Any -> {}
        }
    }


    override fun selectOptionsForImagePicker() {

        selectionType = SelectionType.Image

        setupIntent()

        val builder = AlertDialog.Builder(activity.get()!!)
        builder.setTitle("Choose Image Source")

        val items = activity.get()?.resources?.let {
            arrayOf(
                activity.get()?.resources?.getString(R.string.label_camera),
                it.getString(R.string.label_gallery)
            )
        }

        builder.setItems(items) { _, which ->

            when (which) {
                0 -> openCamera()
                1 -> openGallery()
            }
        }

        builder.show()
    }


    override fun selectVideo() {
        selectionType = SelectionType.Video
        setupIntent()

        if (ContextCompat.checkSelfPermission(
                activity.get()!!,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            galleryPermissionLauncher.launch(permission)
        } else {
            fileSelectionLauncher.launch(selectionIntent)
        }
    }

    override fun selectPdf() {
        selectionType = SelectionType.PDF
        setupIntent()

        if (ContextCompat.checkSelfPermission(
                activity.get()!!,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            galleryPermissionLauncher.launch(permission)
        } else {
            fileSelectionLauncher.launch(selectionIntent)
        }
    }

    /*To show it as popup menu*/
    override fun selectImageWithMenu(
        view: View,
    ) {
        selectionType = SelectionType.Image
        setupIntent()

        val popup = PopupMenu(activity.get()!!, view)
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
    override fun openCamera() {

        if (ContextCompat.checkSelfPermission(
                activity.get()!!,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                activity.get()!!,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                activity.get()!!,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            cameraPermissionLauncher.launch(permission)
        } else {
            dispatchTakePictureIntent()
        }

    }

    private fun dispatchTakePictureIntent() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(activity.get()!!.packageManager) != null) {
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
                    activity.get()!!,
                    "${activity.get()!!.packageName}.provider",
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
    override fun openGallery() {
        if (ContextCompat.checkSelfPermission(
                activity.get()!!,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            galleryPermissionLauncher.launch(permission)
        } else {
            fileSelectionLauncher.launch(selectionIntent)
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
                CropType.CropSquare -> CropImage.activity(file)
                    .setAspectRatio(4, 4)
                    .getIntent(activity.get()!!)
                CropType.CropRectangle -> CropImage.activity(file)
                    .setAspectRatio(6, 4)
                    .getIntent(activity.get()!!)
                CropType.CropCircle -> CropImage.activity(file)
                    .setCropShape(CropImageView.CropShape.OVAL)
                    .setAspectRatio(1, 1)
                    .getIntent(activity.get()!!)
            }
            cropResult.launch(intent)
        } else {
            mMediaSelector?.onSingleFileSelected(file)
        }
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat(
            "yyyyMMdd_HHmmss",
            Locale.getDefault()
        ).format(Date())
        val storageDir = activity.get()?.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
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
            if (!activity.get()!!.shouldShowRequestPermissionRationale(data.key))
                return false
        }
        return true
    }
}


interface MediaSelectorBuilder {


    fun onSingleFileSelected(uri: Uri) {}

    fun onMultipleFileSelected(uriArrayList: ArrayList<Uri>) {}


}
