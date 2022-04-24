package com.ps.filepickerlib

import android.view.View

interface MediaSelectOptions {
    fun selectVideo()
    fun isSelectMultipleImage(canSelect: Boolean)
    fun isSelectMultipleVideo(canSelect: Boolean)
    fun selectImageWithMenu(
        view: View,
        isCrop1: Boolean,
        cropType: String = MediaSelectHelper.Constant.CropSquare
    )

    fun selectOptionsForImagePicker(
        isCrop1: Boolean,
        cropType: String = MediaSelectHelper.Constant.CropSquare
    )
}