package com.mel182.androidinternals

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

class CounterViewModel(private val screenName: String) : ViewModel() {

    var counter by mutableIntStateOf(0)
        private set

    fun increment() {
        counter++
    }

    override fun onCleared() {
        println("CounterViewModel '${screenName}' cleared!")
    }
}