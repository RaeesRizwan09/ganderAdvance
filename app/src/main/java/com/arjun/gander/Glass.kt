package com.arjun.gander

import android.view.View
import android.view.ViewOutlineProvider

fun View.clipToGlass() {
    clipToOutline = true
    outlineProvider = ViewOutlineProvider.BACKGROUND
}
