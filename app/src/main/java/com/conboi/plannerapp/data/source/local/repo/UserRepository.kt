package com.conboi.plannerapp.data.source.local.repo

import android.content.Context
import androidx.lifecycle.asLiveData
import com.conboi.plannerapp.data.model.TaskType
import com.conboi.plannerapp.data.source.local.preferences.PremiumPreferencesDataStore
import com.conboi.plannerapp.data.source.local.preferences.UserPreferencesDataStore
import com.conboi.plannerapp.data.source.remote.repo.FirebaseRepository
import com.conboi.plannerapp.utils.PremiumType
import com.conboi.plannerapp.utils.SynchronizationState
import com.conboi.plannerapp.utils.shared.firebase.FirebaseResult
import com.google.firebase.auth.FirebaseUser
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import javax.inject.Inject

@Module
@InstallIn(ActivityRetainedComponent::class)
class UserRepository @Inject constructor(
    private val userPreferencesDataStore: UserPreferencesDataStore,
    private val premiumPreferencesDataStore: PremiumPreferencesDataStore,
    private val taskRepository: TaskRepository,
    private val firebaseRepository: FirebaseRepository,
    private val appSettingsRepository: AppSettingsRepository,
) {
    private val premiumPreferencesFlow = premiumPreferencesDataStore.preferencesFlow
    private val userPreferencesFlow = userPreferencesDataStore.preferencesFlow

    fun getUser() = firebaseRepository.getCurrentUser()

    suspend fun incrementTotalCompleted(differ: Int) =
        userPreferencesDataStore.incrementTotalCompleted(differ)

    suspend fun decrementTotalCompleted(differ: Int) =
        userPreferencesDataStore.decrementTotalCompleted(differ)


    suspend fun increaseRateUs() = userPreferencesDataStore.increaseRateUs()

    suspend fun resetRateUs() = userPreferencesDataStore.updateRateUs(0)

    suspend fun neverShowRateUs() = userPreferencesDataStore.updateRateUs(16)


    fun getHideCompleted() =
        combine(userPreferencesFlow) { it.first().isHideCompleted }.asLiveData()

    fun getHideOvercompleted() =
        combine(userPreferencesFlow) { it.first().isHideOvercompleted }.asLiveData()

    fun getMiddleListTime() =
        combine(premiumPreferencesFlow) { it.first().middleListTime }.asLiveData()

    fun getPremiumState() = combine(premiumPreferencesFlow) { it.first().isPremium }.asLiveData()

    suspend fun getPremiumTypeValue() = premiumPreferencesFlow.first().premiumType

    fun getPremiumType() = combine(premiumPreferencesFlow) { it.first().premiumType }.asLiveData()


    fun getSyncState() = combine(premiumPreferencesFlow) { it.first().syncState }.asLiveData()

    suspend fun getTotalCompletedValue() = userPreferencesFlow.first().totalCompleted

    fun getTotalCompleted() =
        combine(userPreferencesFlow) { it.first().totalCompleted }.asLiveData()

    suspend fun getRateUsCount() =
        combine(userPreferencesFlow) { it.first().rateUsCount }.first()


    private suspend fun downloadTotalCompleted() {
        if (getTotalCompletedValue() == 0) {
            val result = firebaseRepository.downloadTotalCompleted()
            result.data?.let { updateTotalCompleted(it) }
        }
    }

    private suspend fun downloadPremiumType() {
        val result = firebaseRepository.downloadPremiumType()
        updatePremiumType(
            when (result.data) {
                PremiumType.MONTH.name -> {
                    PremiumType.MONTH
                }
                PremiumType.SIX_MONTH.name -> {
                    PremiumType.SIX_MONTH
                }
                PremiumType.YEAR.name -> {
                    PremiumType.YEAR
                }
                else -> {
                    return
                }
            }
        )
    }


    // User actions
    fun reloadUser() {
        getUser()?.reload()
    }

    suspend fun initUser(newSignIn: Boolean) {
        if (!newSignIn) {
            val taskSize = taskRepository.getTaskSize().value
            if (taskSize != 0 && getUser() != null) {
                val newTotalCompleted = getTotalCompletedValue()
                updateServerTotalCompleted(newTotalCompleted)
            }
        } else {
            userPreferencesDataStore.updateTotalCompleted(0)
        }

        val privateModeState = appSettingsRepository.getPrivateModeStateValue()
        firebaseRepository.initUser(privateModeState)
        downloadPremiumType()
        downloadTotalCompleted()
    }

    fun signIn(newUser: FirebaseUser) {
        firebaseRepository.signIn(newUser)
    }

    fun signOut() {
        firebaseRepository.signOut()
    }

    suspend fun signInNewUserWipe(context: Context) {
        taskRepository.deleteAllTasks(context)
        appSettingsRepository.clearPreferences()
        userPreferencesDataStore.clear()
        premiumPreferencesDataStore.clear()
    }

    suspend fun reauthenticate(currentPassword: String, callback: (Any?, Exception?) -> Unit) {
        val result = firebaseRepository.reauthenticate(currentPassword)
        callbackHandling(result, callback)
    }

    suspend fun signOutUploadTasks(
        currentList: List<TaskType>,
        callback: (Any?, Exception?) -> Unit
    ) {
        val result = firebaseRepository.uploadTasks(currentList, false, withBackupUpload = false)
        callbackHandling(result, callback)
    }

    suspend fun sendConfirmationEmail(callback: (Any?, Exception?) -> Unit) {
        val result = firebaseRepository.sendConfirmationEmail()
        callbackHandling(result, callback)
    }

    suspend fun resetPassword(callback: (Any?, Exception?) -> Unit) {
        val result = firebaseRepository.sendResetPasswordEmail()
        callbackHandling(result, callback)
    }


    // User updates
    suspend fun updateUserName(newName: String) = firebaseRepository.updateUserProfileName(newName)

    suspend fun updateUser(userInfo: MutableMap<String, Any>) =
        firebaseRepository.updateUser(userInfo)

    suspend fun updatePassword(newPassword: String, callback: (Any?, Exception?) -> Unit) {
        val result = firebaseRepository.updatePassword(newPassword)
        callbackHandling(result, callback)
    }

    suspend fun verifyBeforeUpdateEmail(newEmail: String, callback: (Any?, Exception?) -> Unit) {
        val result = firebaseRepository.verifyBeforeUpdateEmail(newEmail)
        callbackHandling(result, callback)
    }

    private suspend fun updateServerTotalCompleted(newTotal: Int) {
        val userInfo: MutableMap<String, Any> = HashMap()
        userInfo[FirebaseRepository.UserKey.KEY_USER_COUNT_COMPLETED_TASKS] =
            if (newTotal != 0) newTotal else return
        updateUser(userInfo)
    }

    suspend fun updateServerPrivateMode(privateModeState: Boolean) {
        val userInfo: MutableMap<String, Any> = HashMap()
        userInfo[FirebaseRepository.UserKey.KEY_USER_PRIVATE_MODE] = privateModeState
        updateUser(userInfo)
    }

    private suspend fun updateTotalCompleted(count: Int) =
        userPreferencesDataStore.updateTotalCompleted(count)

    suspend fun updateSyncState(sync: SynchronizationState) =
        premiumPreferencesDataStore.updateSyncState(sync)

    suspend fun updateMiddleList(downloadMiddleTime: Long) =
        premiumPreferencesDataStore.updateMiddleListTime(downloadMiddleTime)

    suspend fun updatePremium(state: Boolean) = premiumPreferencesDataStore.updatePremium(state)

    suspend fun updatePremiumType(type: PremiumType) {
        premiumPreferencesDataStore.updatePremiumType(type)
    }

    private fun callbackHandling(
        result: FirebaseResult<Any?>,
        callback: (Any?, Exception?) -> Unit
    ) =
        if (result.error == null) callback(null, null) else callback(null, result.error)
}