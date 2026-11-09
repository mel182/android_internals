@file:Suppress("UNCHECKED_CAST")

package com.mel182.processdeath

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mel182.processdeath.ui.theme.AndroidInternalsTheme
import kotlinx.serialization.Serializable

/**
 * Understanding Process Death
 *
 * In Android, process death can occur when the system kills an app
 * running in the background. This leads to the entire memory space
 * attached to that process being cleared, resulting in loss of data,
 * such as class instances and variables. The back stack is, however,
 * restored, allowing the app to resume from the last screen when reopened.
 *
 * Limitations of ViewModel
 * While ViewModel helps preserve state across configuration changes like
 * screen rotations, it cannot survive process death. This is because ViewModel
 * instances and in-memory instances are cleared when the process dies, and new
 * ViewModel instances are created upon restoration.
 *
 * Solutions for Handling Process Death
 * - Saved State Handle: This is a bundle attached to each navigation back stack entry,
 *                       allowing the app to store and restore state after process death.
 * - Persistent Storage: Storing data persistently in files, preferences, or databases
 *                       ensures data survives process death.
 * - Saved Instance State: An older method that survives process death, though it is less
 *                         commonly used than ViewModel in modern applications.
 *
 * When to Use Saved State Handle
 * Saved State Handle is primarily used for screen-specific UI states within a single ViewModel.
 * However, application-wide states, such as user session tokens, should be stored persistently
 * for easier access and management across the app.
 *
 * Leveraging Saved State Handle Features
 * - Passing Navigation Arguments: Saved State Handle can be used to pass navigation arguments to
 *                                 ViewModel instances, allowing easy retrieval of data across screens.
 * - Communicating Between Screens: This feature can also pass data back to previous screens, enabling
 *                                  result-based navigation in Jetpack Compose.
 *
 *  Remember: Saved State Handle and persistent storage offer robust solutions for managing and restoring app state during process death.
 */

@Serializable
data object ScreenA

@Serializable
data class ScreenB(val greeting: String)

class ProcessDeathSavedStateMainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AndroidInternalsTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    val navController = rememberNavController()
                    NavHost(
                        navController = navController,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        startDestination = ScreenA
                    ) {

                        composable<ScreenA> {
                            val viewModel = viewModel<CounterViewModel>(factory = object :
                                ViewModelProvider.Factory {
                                override fun <T : ViewModel> create(
                                    modelClass: Class<T>,
                                    extras: CreationExtras
                                ): T {
                                    // With only the saved state handle as an argument you can leave it as-is without the factory
                                    return CounterViewModel(
                                        screenName = "Screen A",
                                        savedStateHandle = extras.createSavedStateHandle()
                                    ) as T
                                }
                            })
                            val counter = viewModel.counter.collectAsStateWithLifecycle()

                            // Get value passed from Screen B
                            val screenBCounter by it.savedStateHandle.getStateFlow("greeting", "").collectAsStateWithLifecycle()

                            // Read the value passed from Screen B
                            LaunchedEffect(true) {
                                println("Screen B value passed to back state: '${screenBCounter}'")
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CounterUi(
                                    screenName = "Screen A",
                                    counter = counter.value,
                                    onIncrementClick = viewModel::increment,
                                    modifier = Modifier.weight(1f)
                                )
                                Button(onClick = {
                                    navController.navigate(ScreenB(greeting = "Hello World!")) // In this approach the greeting string will be passed
                                    // automatically to the saved state handle instance in
                                    // the view model
                                }) {
                                    Text("Navigate to screen B")
                                }
                            }
                        }

                        composable<ScreenB> {
                            val viewModel = viewModel<CounterViewModel>(
                                factory = object : ViewModelProvider.Factory {
                                    override fun <T : ViewModel> create(
                                        modelClass: Class<T>,
                                        extras: CreationExtras
                                    ): T {
                                        return CounterViewModel(
                                            screenName = "Screen B",
                                            savedStateHandle = extras.createSavedStateHandle()
                                        ) as T
                                    }
                                },
//                                viewModelStoreOwner = this@MainActivity // If you want to bond the lifecycle of the View model factory to the
                                // lifecycle of the Activity. This is called an Activity bound view model.
                            )
                            val counter = viewModel.counter.collectAsStateWithLifecycle()
                            CounterUi(
                                screenName = "Screen B",
                                counter = counter.value,
                                onIncrementClick = {
                                    // Set value to return to previous backstack entry
                                    navController
                                        .previousBackStackEntry
                                        ?.savedStateHandle
                                        ?.set(
                                        "greeting",
                                        "Return value passed!"
                                    )
                                    viewModel.increment()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CounterUi(
    screenName: String,
    counter: Int,
    onIncrementClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = screenName,
            modifier = Modifier
                .align(Alignment.TopCenter)
        )
        Button(onClick = onIncrementClick) {
            Text("Counter: $counter")
        }
    }
}