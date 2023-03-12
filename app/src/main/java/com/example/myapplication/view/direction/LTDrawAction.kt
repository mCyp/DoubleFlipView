package com.example.myapplication.view.direction

import com.qidian.fonttest.view.TOP_SIDE

class LTDrawAction: LeftBaseDirectDrawAction() {
    override fun flipSide(): Int {
        return TOP_SIDE
    }
}