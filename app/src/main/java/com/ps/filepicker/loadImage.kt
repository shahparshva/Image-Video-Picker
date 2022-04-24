package com.ps.filepicker

import android.content.Context
import android.widget.ImageView
import com.bumptech.glide.Glide

fun ImageView?.loadImage(context: Context, path: Any?) {
    Glide.with(context)
        .load(path)
        .into(this!!)
}