package com.mawidplus.patient.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import android.content.Intent
import android.util.Log
import androidx.compose.ui.Modifier
import androidx.activity.ComponentActivity
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.mawidplus.patient.ui.components.BottomNavigationBar
import com.mawidplus.patient.ui.screens.appointments.AppointmentsScreen
import com.mawidplus.patient.ui.screens.home.HomeScreen
import com.mawidplus.patient.ui.screens.notifications.NotificationsScreen
import com.mawidplus.patient.ui.screens.login.LoginScreen
import com.mawidplus.patient.ui.screens.register.RegisterScreen
import com.mawidplus.patient.ui.screens.splash.SplashScreen
import com.mawidplus.patient.ui.screens.profile.ProfileScreen
import com.mawidplus.patient.ui.screens.queue.MyQueueScreen
import com.mawidplus.patient.ui.screens.booking.BookingScreen
import com.mawidplus.patient.ui.screens.search.DoctorDetailScreen
import com.mawidplus.patient.ui.screens.search.DoctorMapScreen
import com.mawidplus.patient.ui.screens.search.MapViewScreen
import com.mawidplus.patient.ui.screens.search.SearchFiltersScreen
import com.mawidplus.patient.core.network.SupabaseProvider
import com.mawidplus.patient.core.session.UiSessionPrefs
import com.mawidplus.patient.data.SeedDoctorIds
import com.mawidplus.patient.data.repository.AuthRepository
import com.mawidplus.patient.data.repository.Result
import com.mawidplus.patient.ui.screens.search.SearchViewModel
import com.mawidplus.patient.ui.screens.search.SearchScreen
import com.mawidplus.patient.ui.theme.Surface
import io.github.jan.supabase.gotrue.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private fun NavHostController.navigateHomeAfterAuth() {
    try {
        navigate(Routes.HOME) {
            launchSingleTop = true
            popUpTo(Routes.LOGIN) { inclusive = true }
        }
    } catch (e: Exception) {
        Log.e("AppNavGraph", "navigate HOME failed", e)
    }
}

@Composable
fun AppNavGraph(
    navController: NavHostController = rememberNavController(),
    startDestination: String = Routes.SPLASH
) {
    val activity = LocalContext.current as ComponentActivity
    LaunchedEffect(activity.intent?.data) {
        val intent = activity.intent
        if (intent?.action == Intent.ACTION_VIEW && intent.data != null) {
            navController.handleDeepLink(intent)
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Routes.SPLASH) {
            val context = LocalContext.current
            SplashScreen(
                onFinished = {
                    try {
                        withContext(Dispatchers.IO) {
                            SupabaseProvider.client.auth.awaitInitialization()
                        }
                    } catch (_: Throwable) {
                        // Session load failed; treat as logged out and continue
                    }
                    val currentUid = try {
                        SupabaseProvider.client.auth.currentUserOrNull()?.id?.toString()
                    } catch (_: Throwable) {
                        null
                    }
                    val storedUid = UiSessionPrefs.getLastAuthUserId(context)
                    if (currentUid != null && storedUid != null && currentUid != storedUid) {
                        Log.w(
                            "AppNavGraph",
                            "Session user id changed (likely new anon after token loss). Signing out. stored=$storedUid current=$currentUid",
                        )
                        try {
                            SupabaseProvider.client.auth.signOut()
                        } catch (_: Throwable) {
                        }
                        UiSessionPrefs.clear(context)
                    }
                    val hasSupabaseSession = try {
                        SupabaseProvider.client.auth.currentUserOrNull() != null
                    } catch (_: Throwable) {
                        false
                    }
                    if (!hasSupabaseSession && UiSessionPrefs.hasHomeAccess(context)) {
                        UiSessionPrefs.clear(context)
                    }
                    val rememberedLogin = UiSessionPrefs.hasHomeAccess(context)
                    val next =
                        if (hasSupabaseSession || rememberedLogin) Routes.HOME else Routes.LOGIN
                    navController.navigate(next) {
                        launchSingleTop = true
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.LOGIN) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigateHomeAfterAuth()
                },
                onNavigateToRegister = {
                    try {
                        navController.navigate(Routes.registerRoute()) {
                            launchSingleTop = true
                        }
                    } catch (e: Exception) {
                        Log.e("AppNavGraph", "navigate REGISTER failed", e)
                    }
                },
                onNavigateToRegisterWithPhone = { digits ->
                    try {
                        navController.navigate(Routes.registerRoute(digits)) {
                            launchSingleTop = true
                        }
                    } catch (e: Exception) {
                        Log.e("AppNavGraph", "navigate REGISTER with phone failed", e)
                    }
                },
            )
        }

        composable(
            route = Routes.REGISTER_WITH_PHONE_PATTERN,
            arguments = listOf(
                navArgument("phone") {
                    type = NavType.StringType
                    defaultValue = ""
                },
            ),
            deepLinks = listOf(
                navDeepLink { uriPattern = "mawidplus://auth/register" },
            ),
        ) { backStackEntry ->
            val phoneArg = backStackEntry.arguments?.getString("phone").orEmpty()
            RegisterScreen(
                preFilledPhoneLocalDigits = phoneArg,
                onRegisterSuccess = {
                    navController.navigateHomeAfterAuth()
                },
                onNavigateToLogin = { navController.navigateUp() },
            )
        }

        composable(Routes.HOME) { MainTabContainer(navController, startTab = Tabs.Home.route) }
        composable(
            route = Routes.SEARCH_WITH_QUERY_PATTERN,
            arguments = listOf(
                navArgument("query") {
                    type = NavType.StringType
                    defaultValue = ""
                }
            )
        ) { backStackEntry ->
            val q = backStackEntry.arguments?.getString("query").orEmpty()
            MainTabContainer(
                navController = navController,
                startTab = Tabs.Search.route,
                initialSearchQuery = q,
            )
        }
        composable(Routes.APPOINTMENTS) { MainTabContainer(navController, startTab = Tabs.Appointments.route) }
        composable(Routes.PROFILE) { MainTabContainer(navController, startTab = Tabs.Profile.route) }
        composable(
            route = Routes.MY_QUEUE_PATTERN,
            arguments = listOf(navArgument("doctorId") { type = NavType.StringType })
        ) { entry ->
            val doctorId = entry.arguments?.getString("doctorId").orEmpty()
            MyQueueScreen(
                doctorId = doctorId,
                onBack = { navController.navigateUp() },
                onNavigateToNotifications = {
                    navController.navigate(Routes.NOTIFICATIONS) {
                        launchSingleTop = true
                    }
                },
                onNavigateToTab = { route ->
                    navController.navigate(route) {
                        launchSingleTop = true
                        popUpTo(Routes.HOME) { inclusive = false }
                    }
                },
                onDoctorProfile = {
                    navController.navigate(Routes.doctorDetailRoute(doctorId)) {
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(Routes.NOTIFICATIONS) {
            NotificationsScreen(onBack = { navController.navigateUp() })
        }

        composable(Routes.MAP_VIEW) {
            MapViewScreen(onBack = { navController.navigateUp() })
        }

        composable(Routes.SEARCH_FILTERS) {
            SearchFiltersScreen(onBack = { navController.navigateUp() })
        }

        composable(
            route = Routes.DOCTOR_DETAIL_PATTERN,
            arguments = listOf(
                navArgument("doctorId") { type = NavType.StringType }
            )
        ) { entry ->
            val doctorId = entry.arguments?.getString("doctorId").orEmpty()
            DoctorDetailScreen(
                doctorId = doctorId,
                onBack = { navController.navigateUp() },
                onBook = {
                    navController.navigate(Routes.bookingRoute(doctorId)) {
                        launchSingleTop = true
                    }
                },
                onOpenMap = { id ->
                    navController.navigate(Routes.doctorMapRoute(id)) {
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(
            route = Routes.DOCTOR_MAP_PATTERN,
            arguments = listOf(navArgument("doctorId") { type = NavType.StringType })
        ) { entry ->
            val mapDoctorId = entry.arguments?.getString("doctorId").orEmpty()
            DoctorMapScreen(
                doctorId = mapDoctorId,
                onBack = { navController.navigateUp() },
                onBook = {
                    navController.navigate(Routes.bookingRoute(mapDoctorId)) {
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(
            route = Routes.BOOKING_PATTERN,
            arguments = listOf(
                navArgument("doctorId") { type = NavType.StringType }
            )
        ) { entry ->
            val doctorId = entry.arguments?.getString("doctorId").orEmpty()
            BookingScreen(
                doctorId = doctorId,
                onBack = { navController.navigateUp() },
                onNotifications = {
                    navController.navigate(Routes.NOTIFICATIONS) {
                        launchSingleTop = true
                    }
                },
                onConfirm = {
                    navController.navigate(Routes.APPOINTMENTS) {
                        launchSingleTop = true
                        popUpTo(Routes.HOME) { inclusive = false }
                    }
                }
            )
        }
    }
}

sealed class Tabs(val route: String, val label: String) {
    data object Home : Tabs("home", "Home")
    data object Search : Tabs("search", "Search")
    data object Appointments : Tabs("appointments", "My Appointments")
    data object Profile : Tabs("profile", "Profile")
}

@Composable
fun MainTabContainer(
    navController: NavHostController,
    startTab: String,
    initialSearchQuery: String = "",
) {
    val searchViewModel: SearchViewModel = viewModel()
    var selectedTab by rememberSaveable { mutableStateOf(startTab) }
    val scope = rememberCoroutineScope()
    val authRepository = remember { AuthRepository() }
    LaunchedEffect(initialSearchQuery) {
        if (initialSearchQuery.isNotEmpty()) {
            searchViewModel.setSearchQuery(initialSearchQuery)
            selectedTab = Tabs.Search.route
        }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Surface)
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when (selectedTab) {
                Tabs.Home.route -> HomeScreen(
                    onNavigateToQueue = { doctorId ->
                        navController.navigate(Routes.myQueueRoute(doctorId)) {
                            launchSingleTop = true
                        }
                    },
                    onNavigateToSearch = { query ->
                        navController.navigate(Routes.searchRoute(query)) {
                            launchSingleTop = true
                            popUpTo(Routes.HOME) { inclusive = false }
                        }
                    },
                    onNavigateToNotifications = {
                        navController.navigate(Routes.NOTIFICATIONS) {
                            launchSingleTop = true
                        }
                    },
                    onNewBooking = {
                        navController.navigate(Routes.searchRoute()) {
                            launchSingleTop = true
                            popUpTo(Routes.HOME) { inclusive = false }
                        }
                    },
                    onNearMe = {
                        navController.navigate(Routes.searchRoute()) {
                            launchSingleTop = true
                            popUpTo(Routes.HOME) { inclusive = false }
                        }
                    },
                    onVideoConsult = {
                        navController.navigate(Routes.searchRoute()) {
                            launchSingleTop = true
                            popUpTo(Routes.HOME) { inclusive = false }
                        }
                    },
                    // زر «مساعد موعد+» — اربط التنقل أو المنطق هنا عند إعادة بناء الميزة
                    onSmartAssistant = {},
                    onHealthTipClick = {
                        navController.navigate(Routes.searchRoute()) {
                            launchSingleTop = true
                            popUpTo(Routes.HOME) { inclusive = false }
                        }
                    }
                )
                Tabs.Search.route -> SearchScreen(
                    viewModel = searchViewModel,
                    onBack = { selectedTab = Tabs.Home.route },
                    onNotifications = {
                        navController.navigate(Routes.NOTIFICATIONS) {
                            launchSingleTop = true
                        }
                    },
                    onSearchFieldClick = {
                        navController.navigate(Routes.SEARCH_FILTERS) {
                            launchSingleTop = true
                        }
                    },
                    onSearchFilters = {
                        navController.navigate(Routes.SEARCH_FILTERS) {
                            launchSingleTop = true
                        }
                    },
                    onViewMap = {
                        navController.navigate(Routes.MAP_VIEW) {
                            launchSingleTop = true
                        }
                    },
                    onDoctorClick = { id ->
                        navController.navigate(Routes.doctorDetailRoute(id)) {
                            launchSingleTop = true
                        }
                    },
                    onBookAppointment = { id ->
                        navController.navigate(Routes.bookingRoute(id)) {
                            launchSingleTop = true
                        }
                    }
                )
                Tabs.Appointments.route -> AppointmentsScreen(
                    onNavigateToQueue = { doctorId ->
                        navController.navigate(Routes.myQueueRoute(doctorId)) {
                            launchSingleTop = true
                        }
                    },
                    onNavigateToNotifications = {
                        navController.navigate(Routes.NOTIFICATIONS) {
                            launchSingleTop = true
                        }
                    },
                    onNavigateToSearch = {
                        selectedTab = Tabs.Search.route
                    }
                )
                Tabs.Profile.route -> ProfileScreen(
                    onNavigateToNotifications = {
                        navController.navigate(Routes.NOTIFICATIONS) {
                            launchSingleTop = true
                        }
                    },
                    onNavigateToAppointments = { selectedTab = Tabs.Appointments.route },
                    onNavigateToSearch = { selectedTab = Tabs.Search.route },
                    onNavigateToQueue = {
                        navController.navigate(Routes.myQueueRoute(SeedDoctorIds.FAMILY_AHMED)) {
                            launchSingleTop = true
                        }
                    },
                    onNavigateToSearchFilters = {
                        navController.navigate(Routes.SEARCH_FILTERS) {
                            launchSingleTop = true
                        }
                    },
                    onSignOut = {
                        scope.launch {
                            authRepository.signOut().collect { r ->
                                if (r is Result.Error) {
                                    val ex = r.exception
                                    if (ex != null) {
                                        Log.w("AppNavGraph", "signOut: ${r.message}", ex)
                                    } else {
                                        Log.w("AppNavGraph", "signOut: ${r.message}")
                                    }
                                }
                            }
                            navController.navigate(Routes.LOGIN) {
                                launchSingleTop = true
                                popUpTo(Routes.HOME) { inclusive = true }
                            }
                        }
                    }
                )
            }
        }
        BottomNavigationBar(
            selectedRoute = selectedTab,
            onItemSelected = { route ->
                selectedTab = route
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}
