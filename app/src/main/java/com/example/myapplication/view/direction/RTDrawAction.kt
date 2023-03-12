package com.example.myapplication.view.direction

import com.qidian.fonttest.view.TOP_SIDE

class RTDrawAction: RightBaseDirectDrawAction() {
    override fun flipSide(): Int {
        return TOP_SIDE
    }
}