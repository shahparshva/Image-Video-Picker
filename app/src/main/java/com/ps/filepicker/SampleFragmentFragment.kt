package com.ps.filepicker


import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatImageView
import androidx.fragment.app.Fragment
import com.ps.filepickerlib.FileSelector
import com.ps.filepickerlib.MediaSelectorBuilder
import com.ps.filepickerlib.getFileType
import java.lang.ref.WeakReference


class SampleFragmentFragment : Fragment(), MediaSelectorBuilder {


    private lateinit var imageView: AppCompatImageView
    private lateinit var textViewSelectImage: AppCompatButton
    private lateinit var textViewSelectVideo: AppCompatButton

    private val filePicker: FileSelector =
        FileSelector.Builder()
            .setFragmentReference(WeakReference(this))
            .setCropType(FileSelector.CropType.CropSquare)
            .setAllowCrop(false)
            .setAllowMultiple(true)
            .setMediaSelectCallBack(this)
            .build()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
// Inflate the layout for this fragment
        return inflater.inflate(R.layout.activity_main, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        imageView = view.findViewById(R.id.imageViewMain)
        textViewSelectImage = view.findViewById(R.id.textViewSelectImage)
        textViewSelectVideo = view.findViewById(R.id.textViewSelectVideo)


        textViewSelectImage.setOnClickListener {
            filePicker.selectPdf()
        }
    }

    override fun onSingleFileSelected(uri: Uri) {
        uri.path?.let { Log.e("Path", it) }
        Log.e("extension", uri.getFileType())
        imageView.loadImage(requireContext(), uri)
    }

    override fun onMultipleFileSelected(uriArrayList: ArrayList<Uri>) {
        Log.e("Path", uriArrayList[0].path.toString())
        Log.e("Path", uriArrayList[0].encodedPath.toString())
        Log.e("extension", uriArrayList[0].getFileType())
        imageView.loadImage(requireContext(), uriArrayList[0])
    }

}
