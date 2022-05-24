package com.ps.filepickerlib

import android.net.Uri


fun Uri.getFileType(): String {
    this.path?.apply {
        if (lastIndexOf(".") != -1) {
            return substring(lastIndexOf("."))
        } else {
            split("/").apply {
                if (this.size > 1) {
                    return "." + this[this.size - 2]
                }
            }
        }
    }

    return ""
}