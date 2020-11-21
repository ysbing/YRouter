package com.ysbing.yrouter.samples

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.ysbing.yrouter.api.YRouterApi
import com.ysbing.yrouter.sampleslibrary1.Library1Api
import com.ysbing.yrouter.sampleslibrary2.Library2Api
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mockTest.setOnClickListener {
            //使用dependencies的yrouter可体验模拟测试
            Library2Api().b(123f)
        }
        callLibrary1.setOnClickListener {
            Toast.makeText(this, Library1Api().c(1), Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        @YRouterApi
        fun getName(): String {
            return MainActivity::class.java.name
        }
    }
}