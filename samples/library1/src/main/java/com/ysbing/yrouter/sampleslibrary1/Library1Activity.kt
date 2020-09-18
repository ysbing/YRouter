package com.ysbing.yrouter.sampleslibrary1

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.ysbing.yrouter.samples.JavaTest

class Library1Activity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        JavaTest.InnerClass1.f111(this)
    }
}