@file:Suppress("UNCHECKED_CAST")

package com.mel182.androidinternals

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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import com.mel182.androidinternals.ui.theme.AndroidInternalsTheme
import kotlinx.serialization.Serializable


/**
 * Understanding View Model Lifecycle
 * Delve into the intricacies of view models and their lifecycles,
 * essential for managing UI state in Android applications.
 * While simpler than the activity lifecycle, it is crucial for preventing
 * incorrect or unnecessary state updates.
 *
 * Important: Misunderstanding the view model lifecycle can lead to incorrect state updates in Android apps.
 *
 * View Model Implementation
 * Explore how to create and bind a view model to a screen using navigation compose dependencies. Learn about
 * state management using the view model as a state holder for a counter state.
 * - Create a counter view model and use it on different screens.
 * - Understand how state is managed across navigation actions.
 *
 * Understanding Backstack and View Model Binding
 * Learn how view models are bound to backstack entries and how this influences their lifecycle.
 * Recognize that a backstack entry can be different things, such as a screen, activity, or navigation graph.
 *
 * Important: Every view model is bound to a backstack entry, which determines its lifecycle.
 *
 * Shared View Models
 * Understand shared view models and how these can be bound to a nested navigation graph.
 * This allows data to be preserved and shared across multiple related screens but not across all screens in an app.
 *
 * Important: Use shared view models with care to prevent unintended state sharing across unrelated screens.
 *
 * Practical Example: Implementing a Shared View Model
 * Discover how to implement a shared view model between screens using helper functions. This includes managing a shared
 * counter state and understanding when view models are destroyed.
 * - Navigate between screens while maintaining state.
 * - Use helper functions for binding view models to navigation graphs.
 */
@Serializable
data object ScreenA

@Serializable
data object ScreenB

@Serializable
data object ScreenC

@Serializable
data object ScreenD

@Serializable
data object ScreenCAndDGraph

class MainActivity : ComponentActivity() {
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
                            val viewModel = viewModel<CounterViewModel>(factory = object: ViewModelProvider.Factory {
                                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                                    return CounterViewModel(screenName = "Screen A") as T
                                }
                            })
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CounterUi(
                                    screenName = "Screen A",
                                    counter = viewModel.counter,
                                    onIncrementClick = viewModel::increment,
                                    modifier = Modifier.weight(1f)
                                )
                                Button(onClick = {
                                    navController.navigate(ScreenB)
                                }) {
                                    Text("Navigate to screen B")
                                }
                            }
                        }

                        composable<ScreenB> {
                            val viewModel = viewModel<CounterViewModel>(factory = object : ViewModelProvider.Factory {
                                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                                    return CounterViewModel(screenName = "Screen B") as T
                                }
                            },
//                                viewModelStoreOwner = this@MainActivity // If you want to bond the lifecycle of the View model factory to the
                                                                        // lifecycle of the Activity. This is called an Activity bound view model.
                            )
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CounterUi(
                                    screenName = "Screen B",
                                    counter = viewModel.counter,
                                    onIncrementClick = viewModel::increment,
                                    modifier = Modifier.weight(1f)
                                )
                                Button(onClick = {
                                    //navController.navigate(ScreenC)
                                    navController.navigate(ScreenCAndDGraph) // Navigate to the navigation graph
                                }) {
                                    Text("Navigate to screen C")
                                }
                            }
                        }

                        // Nested navigation graph example
                        // were you share the same state between Screen C and D, but not with Screen B and A
                        navigation<ScreenCAndDGraph>(
                            startDestination = ScreenC
                        ) {
                            composable<ScreenC> {

                                val viewModel = it.sharedViewModel(navController) { CounterViewModel(screenName = "Screen C") }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CounterUi(
                                        screenName = "Screen C",
                                        counter = viewModel.counter,
                                        onIncrementClick = viewModel::increment,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Button(onClick = {
                                        //navController.navigate(ScreenC)
                                        navController.navigate(ScreenD)
                                    }) {
                                        Text("Navigate to screen D")
                                    }
                                }
                            }

                            composable<ScreenD> {

                                val viewModel = it.sharedViewModel(navController) { CounterViewModel(screenName = "Screen D") }
                                CounterUi(
                                    screenName = "Screen D",
                                    counter = viewModel.counter,
                                    onIncrementClick = viewModel::increment
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private inline fun <reified T : ViewModel> NavBackStackEntry.sharedViewModel(
    navController: NavController,
    crossinline factory: () -> T
): T {
    val navGraphRoute = destination.parent?.route ?: return viewModel<T>()
    val parentEntry = remember(this) {
        navController.getBackStackEntry(navGraphRoute)
    }
    return viewModel(
        viewModelStoreOwner = parentEntry,
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <VM : ViewModel> create(modelClass: Class<VM>): VM {
                return factory() as VM
            }
        }
    )
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