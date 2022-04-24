package com.ps.filepicker

import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatImageView
import com.ps.filepickerlib.MediaSelectHelper
import com.ps.filepickerlib.MediaSelector

class MainActivity : AppCompatActivity() {
    private lateinit var imageView: AppCompatImageView
    private lateinit var textViewSelectImage: AppCompatButton
    private lateinit var textViewSelectVideo: AppCompatButton
    private val filePicker: MediaSelectHelper by lazy {
        MediaSelectHelper(this)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        filePicker.registerCallback(object : MediaSelector {
            override fun onImageUri(uri: Uri) {
                imageView.loadImage(this@MainActivity, uri)
            }

            override fun onImageUriList(uriArrayList: ArrayList<Uri>) {
                imageView.loadImage(this@MainActivity, uriArrayList[0])
            }

            override fun onVideoUri(uri: Uri) {
                imageView.loadImage(this@MainActivity, uri)
            }

            override fun onVideoURIList(uriArrayList: java.util.ArrayList<Uri>) {
                imageView.loadImage(this@MainActivity, uriArrayList[0])
            }
        })
        imageView = findViewById(R.id.imageViewMain)
        textViewSelectImage = findViewById(R.id.textViewSelectImage)
        textViewSelectVideo = findViewById(R.id.textViewSelectVideo)
        textViewSelectImage.setOnClickListener {
            filePicker.isSelectMultipleImage(false)
            filePicker.selectImageWithMenu(
                it,
                isCrop1 = true,
                cropType = MediaSelectHelper.Constant.CropSquare
            )
            /*filePicker.selectOptionsForImagePicker(
                isCrop1 = true,
                cropType = MediaSelectHelper.Constant.CropSquare
            )*/
        }

        textViewSelectVideo.setOnClickListener {

            filePicker.isSelectMultipleVideo(false)//set true if allow multiple
            filePicker.selectVideo()
        }
    }
}