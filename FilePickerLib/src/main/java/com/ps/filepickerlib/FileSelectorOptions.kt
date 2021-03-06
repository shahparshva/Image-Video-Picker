package com.ps.filepickerlib

import android.view.View

interface FileSelectorOptions {
    fun selectVideo()
    fun openCamera()
    fun openGallery()

    fun selectImageWithMenu(
        view: View,
    )

    fun selectPdf()

    fun selectOptionsForImagePicker()
}