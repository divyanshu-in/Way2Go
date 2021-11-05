package com.application.myapplication

import android.content.Context
import android.view.View
import android.view.WindowManager
import android.widget.PopupWindow

fun View.gone(){
    this.visibility = View.GONE
}

fun View.visible(){
    this.visibility = View.VISIBLE
}

fun View.invisible(){
    this.visibility = View.INVISIBLE
}

fun PopupWindow.showPopupDimBehind() {

    val container = this.contentView.rootView
    val wm = container.context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    val p = container.layoutParams as WindowManager.LayoutParams
    p.flags = WindowManager.LayoutParams.FLAG_DIM_BEHIND
    p.dimAmount = 0.5f
    wm.updateViewLayout(container, p)
}