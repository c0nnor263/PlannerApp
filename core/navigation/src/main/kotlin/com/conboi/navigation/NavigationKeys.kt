package com.conboi.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed class NavigationKeys {
//    @Serializable
//    data object Login : NavigationKeys() {
//        @Serializable
//        data object SignIn : NavigationKeys()
//
//        @Serializable
//        data object SignUp : NavigationKeys()
//    }

    @Serializable
    data object Home : NavigationKeys() {
        @Serializable
        data class TaskDetail(val taskId: Int) : NavigationKeys()

        @Serializable
        data object Search : NavigationKeys()
    }

//    @Serializable
//    data object Friends : NavigationKeys() {
//        @Serializable
//        data class FriendDetail(val friendDetail: FriendType) : NavigationKeys()
//    }NavigationKeys

//    @Serializable
//    data object Profile : NavigationKeys() {
//        @Serializable
//        data object Settings : NavigationKeys()
//
//        @Serializable
//        data object Subscriptions : NavigationKeys()
//    }
}