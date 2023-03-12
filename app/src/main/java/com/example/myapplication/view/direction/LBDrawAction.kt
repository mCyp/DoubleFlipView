package com.example.myapplication.view.direction

import com.qidian.fonttest.view.BOTTOM_SIDE

class LBDrawAction: LeftBaseDirectDrawAction() {
    override fun flipSide(): Int {
        return BOTTOM_SIDE
    }
}