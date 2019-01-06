package com.anwesh.uiprojects.linkedcircletornbylineview

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.anwesh.uiprojects.circletornbylinesview.CircleTornByLinesView

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CircleTornByLinesView.create(this)
    }
}
