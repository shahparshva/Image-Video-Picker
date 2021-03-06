package com.ps.filepicker

import android.content.ClipData
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatImageView
import com.ps.filepickerlib.FileSelector
import com.ps.filepickerlib.MediaSelectorBuilder
import com.ps.filepickerlib.getFileType
import java.io.File
import java.lang.ref.WeakReference


class MainActivity : AppCompatActivity(), MediaSelectorBuilder {
    private lateinit var imageView: AppCompatImageView
    private lateinit var textViewSelectImage: AppCompatButton
    private lateinit var textViewSelectVideo: AppCompatButton
    private val filePicker: FileSelector =
        FileSelector.Builder()
            .setActivityReference(WeakReference(this))
            .setCropType(FileSelector.CropType.CropSquare)
            .setAllowCrop(false)
            .setAllowMultiple(true)
            .setMediaSelectCallBack(this)
            .build()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE)


        imageView = findViewById(R.id.imageViewMain)
        textViewSelectImage = findViewById(R.id.textViewSelectImage)
        textViewSelectVideo = findViewById(R.id.textViewSelectVideo)


        textViewSelectImage.setOnClickListener {
            filePicker.selectOptionsForImagePicker()
        }

        textViewSelectVideo.setOnClickListener {

            /*filePicker.isSelectMultipleVideo(true)//set true if allow multiple
            filePicker.selectVideo()*/

            selectPdf(false)
        }
    }


    private val pdfPickerIntent =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == RESULT_OK) {
                result.data?.apply {

                    if (this.clipData != null) {
                        val mClipData: ClipData? = this.clipData
                        val mArrayUri = java.util.ArrayList<Uri>()
                        for (i in 0 until mClipData?.itemCount!!) {
                            val item = mClipData.getItemAt(i)
                            val uri: Uri = item.uri
                            mArrayUri.add(uri)
                        }
                        Log.e("Path_Multiple", mArrayUri.toString())
                    } else {
                        val selectedMedia: Uri? = data
                        selectedMedia?.let {
                            Log.e("Path_Single", this.data.toString())
                            val f = File("" + this.data)
                            Log.e("Path_Single", f.name)
//                            Log.e("Path_Single", this.data?.getFileType().toString())

                        }
                    }
                }
            }

        }

    private fun selectPdf(canSelectMultiple: Boolean) {
        val pdfIntent = Intent(Intent.ACTION_GET_CONTENT)
        pdfIntent.type = "application/pdf"
        if (canSelectMultiple) {
            pdfIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        }
        pdfPickerIntent.launch(pdfIntent)
    }

    override fun onSingleFileSelected(uri: Uri) {

        uri.path?.let { Log.e("Path", it) }
        Log.e("extension", uri.getFileType())

        imageView.loadImage(this@MainActivity, uri)
    }

    override fun onMultipleFileSelected(uriArrayList: ArrayList<Uri>) {
        imageView.loadImage(this@MainActivity, uriArrayList[0])
    }


}
