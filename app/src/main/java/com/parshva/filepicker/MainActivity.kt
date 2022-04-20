package com.parshva.filepicker

import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import com.parshva.filepickerlib.MediaSelectHelper
import com.parshva.filepickerlib.MediaSelector

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
            filePicker.canSelectMultiple(false)
            filePicker.showImageMenu(it,true)
//            filePicker.selectOptionsForImagePicker(true)
        }

        textViewSelectVideo.setOnClickListener {

            filePicker.canSelectMultipleVideo(false)//set true if allow multiple
            filePicker.selectVideo()
        }
    }
}