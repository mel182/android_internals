package com.mel182.processdeath

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel

class CounterViewModel(
    private val screenName: String,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    val counter = savedStateHandle.getStateFlow("counter",0)

    val greeting = savedStateHandle.get<String>("greeting") // The key is equal to the parameter name in this case 'data class ScreenB(val --> greeting :String)'

    init {
        println("$screenName -> greeting message: $greeting")
    }

    // Your ordinary mutable state of Int
//    var counter by mutableIntStateOf(0)
//        private set

    fun increment() {
        // Use this approach only for screen specific state
        savedStateHandle.update<Int>("counter") {
            it + 1
        }
    }

    override fun onCleared() {
        println("CounterViewModel '${screenName}' cleared!")
    }
}

// Generic way to store to saved state handle and make it thread safe as well
inline fun <T> SavedStateHandle.update(key: String, update: (T) -> T) {
    synchronized(this) { // Lock the instance and make it thread-safe
        val current = get<T>(key) // Get current value
        val updated = update(current ?: return) // Check if value exist otherwise do nothing
        set(key, updated)
    }
}