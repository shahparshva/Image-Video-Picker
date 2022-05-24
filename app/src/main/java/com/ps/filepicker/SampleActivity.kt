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
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import com.ps.filepickerlib.MediaSelectorBuilder
import java.io.File


class SampleActivity : AppCompatActivity(), MediaSelectorBuilder {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sample)
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE)


        val manager: FragmentManager = supportFragmentManager
        val transaction: FragmentTransaction = manager.beginTransaction()
        transaction.replace(R.id.container, SampleFragmentFragment())
        transaction.commit()

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

}
