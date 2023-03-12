package com.example.myapplication.view.direction

import com.qidian.fonttest.view.BOTTOM_SIDE

class RBDrawAction: RightBaseDirectDrawAction() {
    override fun flipSide(): Int {
        return BOTTOM_SIDE
    }
}