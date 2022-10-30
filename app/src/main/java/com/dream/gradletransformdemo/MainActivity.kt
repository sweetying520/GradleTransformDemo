package com.dream.gradletransformdemo

import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.dream.androidutils.StringUtils
import com.dream.gradletransformdemo.annotation.CostTime

class MainActivity : AppCompatActivity() {

    @CostTime
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        StringUtils.getCharArray(null)
        StringUtils.getLength(null)
    }
}