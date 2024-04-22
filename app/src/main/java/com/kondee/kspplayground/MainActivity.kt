package com.kondee.kspplayground

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavBackStackEntry
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.kondee.kspplayground.ui.theme.KSPPlaygroundTheme
import com.kondee.navigationrouteprocessor.NavigationArgument
import com.kondee.navigationrouteprocessor.NavigationScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {

            KSPPlaygroundTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    val navController = rememberNavController()

                    val greetingScreenRoute = GreetingScreenRoute(name = "Kondee").apply {
                        setAge(30)
                        setEmail("kondeezaa@gmail.com")
                    }

                    val startDestination = greetingScreenRoute.getNavigationWithArgs()
                    NavHost(
                        navController = navController,
                        route = "main",
                        startDestination = startDestination
                    ) {

                        composable(
                            route = GreetingScreenRoute.getRoute(),
                            arguments = listOf(
                                androidx.navigation.navArgument("name") {
                                    defaultValue = "Hello #2"
                                },
                                androidx.navigation.navArgument("email") {
                                    defaultValue = "kondeezaa@gmail.com #12"
                                }
                            )
                        ) {
//                            val greetingScreenArguments = it.toGreetingScreenArguments()

                            val name = it.arguments?.getString("name")
                            val age = it.arguments?.getInt("age")
                            val email = it.arguments?.getString("email")
                            GreetingScreen(
                                modifier = Modifier.padding(innerPadding),
                                name = name ?: "",
                                age = age,
                                email = email,
                                onClick = {
                                    navController.navigate(
                                        route = SecondScreenRoute(
                                            name = "Kondee2",
                                            age = 30
                                        ).apply {
//                                            setDel(1234)
                                            setEmail("kondeezaa@gmail.com #2")
                                        }.getNavigationWithArgs()
                                    )
                                }
                            )
                        }

                        composable(route = SecondScreenRoute.getRoute()) { navBackStackEntry: NavBackStackEntry ->

                            val (name, age, del, email) = navBackStackEntry.toSecondScreenArguments()

                            SecondScreen(
                                name = name,
                                age = age,
                                email = email,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
@NavigationScreen(name = "greeting")
fun GreetingScreen(
    modifier: Modifier = Modifier,
    @NavigationArgument
    name: String,
    @NavigationArgument
    age: Int? = null,
    @NavigationArgument
    email: String? = null,
    onClick: () -> Unit = {}
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Text(
            modifier = Modifier.clickable(onClick = onClick),
            text = "Hello $name!",
        )

        Text(
            text = "Age: $age",
            modifier = Modifier
        )

        Text(
            text = "Email: $email",
            modifier = Modifier
        )
    }
}

@Composable
@NavigationScreen
fun SecondScreen(
    modifier: Modifier = Modifier,
    @NavigationArgument
    name: String,
    @NavigationArgument
    age: Int,
    @NavigationArgument
    del: Int? = null,
    @NavigationArgument
    email: String? = null,
) {

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Text(
            text = "Hello $name!",
            modifier = Modifier
        )

        Text(
            text = "Age: $age",
            modifier = Modifier
        )

        Text(
            text = "Email: $email",
            modifier = Modifier
        )
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    KSPPlaygroundTheme {
        GreetingScreen(
            name = "Android",
        )
    }
}