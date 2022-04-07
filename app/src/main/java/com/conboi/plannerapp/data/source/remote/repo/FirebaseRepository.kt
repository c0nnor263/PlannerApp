package com.conboi.plannerapp.data.source.remote.repo

import android.app.AlarmManager
import android.content.Context
import android.util.Log
import com.conboi.plannerapp.data.model.FriendType
import com.conboi.plannerapp.data.model.TaskType
import com.conboi.plannerapp.utils.*
import com.conboi.plannerapp.utils.shared.AlarmUtil
import com.conboi.plannerapp.utils.shared.firebase.FirebaseResult
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.auth.ktx.userProfileChangeRequest
import com.google.firebase.firestore.*
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import kotlinx.coroutines.tasks.await
import javax.inject.Inject


@Module
@InstallIn(ActivityRetainedComponent::class)
class FirebaseRepository @Inject constructor(
    private val alarmUtil: AlarmUtil
) {
    private var firestore: FirebaseFirestore? = Firebase.firestore
    private var auth: FirebaseAuth? = Firebase.auth

    private var user = auth?.currentUser

    private val userFriendIdReference: (String?) -> DocumentReference? = { id ->
        firestore?.document(
            "Users/${user?.uid}/FriendList/${id}"
        )
    }

    private val friendUserIdReference: (String?) -> DocumentReference? = { id ->
        firestore?.document("Users/${id}/FriendList/${user?.uid}")
    }


    fun signIn(newUser: FirebaseUser) {
        auth = Firebase.auth
        firestore = Firebase.firestore
        user = newUser
    }

    fun signOut() {
        auth = null
        firestore = null
    }

    private fun usersReference() = firestore?.collection("Users")

    private fun userInfoReference() = firestore?.document("Users/${user?.uid}")

    private fun userTaskReference() = firestore?.document("Users/${user?.uid}/TaskList/Tasks")

    private fun userFriendReference() = firestore?.collection("Users/${user?.uid}/FriendList")

    private fun userBackupTaskReference() =
        firestore?.document("Users/${user?.uid}/TaskList/BackupTasks")


    fun getCurrentUser() = user

    fun getFriendQuery() = userFriendReference()!!

    fun checkForNewFriends(): FirebaseResult<List<FriendType>?> = firebaseCall {
        val callbackResult: MutableList<FriendType> = arrayListOf()

        userFriendReference()?.addSnapshotListener { snapshots, e ->
            if (e != null) {
                FirebaseResult.Error<Exception>(e)
                return@addSnapshotListener
            }

            for (documentChangeOne in snapshots?.documentChanges ?: return@addSnapshotListener) {
                if (documentChangeOne.type == DocumentChange.Type.ADDED &&
                    documentChangeOne.document.getLong(UserKey.KEY_USER_REQUEST)
                        ?.toInt() == 2
                ) {
                    if (documentChangeOne.document.getBoolean("isShown") != true) {
                        val id = documentChangeOne.document.getString(UserKey.KEY_USER_ID)
                        val name = documentChangeOne.document[UserKey.KEY_USER_NAME].toString()

                        val formattedId =
                            documentChangeOne.document[UserKey.KEY_USER_ID].toString()
                                .filter { it.isDigit() }
                                .toInt() + snapshots.documents.size

                        userFriendIdReference(id)?.set(
                            hashMapOf("isShown" to true),
                            SetOptions.merge()
                        )

                        callbackResult.add(
                            FriendType(
                                user_id = formattedId.toString(),
                                user_name = name
                            )
                        )
                    } else {
                        FirebaseResult.Error<Exception>(null)
                        return@addSnapshotListener
                    }
                }
            }
        }
        FirebaseResult.Success(callbackResult)
    }

    suspend fun checkEveryFriendInfo(friendList: List<DocumentSnapshot>) = firebaseCall {
        // Get Users collections
        val usersCollection = usersReference()?.get()?.await()

        val usersList: MutableList<QueryDocumentSnapshot> =
            ArrayList()
        for (user in usersCollection!!) {
            usersList.add(user)
        }
        //Searching a friend
        friendList.forEach { friend ->
            usersList.forEach { user ->
                // Check if friend info is exists
                if (friend.getString(UserKey.KEY_USER_ID) == user.getString(
                        UserKey.KEY_USER_ID
                    )
                ) {
                    //Get a friend info
                    val friendMap: MutableMap<String, Any> = HashMap()
                    friendMap.apply {
                        set(
                            UserKey.KEY_USER_ID,
                            user.getString(UserKey.KEY_USER_ID).toString()
                        )
                        set(
                            UserKey.KEY_USER_PHOTO_URL,
                            user.getString(UserKey.KEY_USER_PHOTO_URL).toString()
                        )
                        set(
                            UserKey.KEY_USER_NAME,
                            user.getString(UserKey.KEY_USER_NAME).toString()
                        )
                        set(
                            UserKey.KEY_USER_EMAIL,
                            user.getString(UserKey.KEY_USER_EMAIL).toString()
                        )
                        set(
                            UserKey.KEY_USER_COUNT_COMPLETED_TASKS,
                            user.getLong(UserKey.KEY_USER_COUNT_COMPLETED_TASKS) ?: 0
                        )
                        set(
                            UserKey.KEY_USER_PRIVATE_MODE,
                            user.getBoolean(UserKey.KEY_USER_PRIVATE_MODE) ?: false
                        )
                    }

                    // Update user's friend info
                    userFriendIdReference(friendMap[UserKey.KEY_USER_ID] as String)?.set(
                        friendMap,
                        SetOptions.merge()
                    )
                }
            }
        }
        FirebaseResult.Success(null)
    }


    // User actions
    fun initUser(privateMode: Boolean) {
        user = auth?.currentUser

        user?.let { currentUser ->
            val uid = currentUser.uid
            val email = currentUser.email.toString()
            val name = currentUser.displayName
            val photoUrl = currentUser.photoUrl.toString()
            val isEmailVerified = currentUser.isEmailVerified

            val userInfo: MutableMap<String, Any> = HashMap()
            userInfo.apply {
                set(UserKey.KEY_USER_ID, uid)
                set(UserKey.KEY_USER_EMAIL, email)
                set(UserKey.KEY_USER_NAME, name ?: email)
                set(UserKey.KEY_USER_PHOTO_URL, photoUrl)
                set(UserKey.KEY_USER_EMAIL_CONFIRM, isEmailVerified)
                set(UserKey.KEY_USER_PRIVATE_MODE, privateMode)

                userInfoReference()?.set(userInfo, SetOptions.merge())
            }
        }
    }

    suspend fun reauthenticate(currentPassword: String): FirebaseResult<Any?> =
        firebaseCall {
            user?.reauthenticate(
                EmailAuthProvider.getCredential(
                    user?.email.toString(),
                    currentPassword
                )
            )?.await()
            FirebaseResult.Success(null)
        }

    suspend fun verifyBeforeUpdateEmail(newEmail: String): FirebaseResult<Any?> =
        firebaseCall {
            user?.verifyBeforeUpdateEmail(newEmail)?.await()
            FirebaseResult.Success(null)
        }


    suspend fun updateUser(userInfo: MutableMap<String, Any>) = firebaseCall {
        userInfoReference()?.update(userInfo)?.await()
        FirebaseResult.Success(null)
    }

    suspend fun updatePassword(newPassword: String): FirebaseResult<Any?> = firebaseCall {
        user?.updatePassword(newPassword)?.await()
        FirebaseResult.Success(null)
    }

    suspend fun updateUserProfileName(newName: String): FirebaseResult<Any?> = firebaseCall {
        user?.updateProfile(userProfileChangeRequest { displayName = newName })?.await()
        FirebaseResult.Success(null)
    }

    suspend fun uploadTasks(
        currentList: List<TaskType>,
        isBackupDownloaded: Boolean,
        withBackupUpload: Boolean
    ) =
        firebaseCall {
            userTaskReference()?.set(
                currentList.associateBy(
                    { (it.idTask + it.created).toString() },
                    { it })
            )?.await()
            if (withBackupUpload) {
                uploadBackupTasks(currentList, isBackupDownloaded)
            } else {
                FirebaseResult.Success(null)
            }
        }

    private suspend fun uploadBackupTasks(
        currentList: List<TaskType>,
        isBackupDownloaded: Boolean
    ) = firebaseCall {
        val result = userInfoReference()?.get()?.await()
        val currentTime = System.currentTimeMillis()
        val lastSync = result?.getLong(UserKey.KEY_USER_LAST_SYNC)
            ?: GLOBAL_START_DATE

        // Check last backup sync time
        if (currentTime - lastSync >= AlarmManager.INTERVAL_DAY * 3 && !isBackupDownloaded) {
            val processedMapTasks = currentList.associateBy(
                { (it.idTask + it.created).toString() },
                { it }
            )

            userBackupTaskReference()?.set(processedMapTasks)?.await()
            userInfoReference()?.update(UserKey.KEY_USER_LAST_SYNC, currentTime)?.await()
        }
        FirebaseResult.Success<Any?>(null)
    }

    suspend fun sendConfirmationEmail(): FirebaseResult<Any?> = firebaseCall {
        val isEmailVerified = user?.isEmailVerified == false
        if (isEmailVerified) {
            Firebase.auth.useAppLanguage()
            user?.sendEmailVerification()?.await()
        }
        FirebaseResult.Success(null)
    }

    suspend fun sendResetPasswordEmail(): FirebaseResult<Any?> = firebaseCall {
        val email = user?.email.toString()
        auth?.sendPasswordResetEmail(email)?.await()
        FirebaseResult.Success(null)
    }


    suspend fun downloadLatestBackupInfo(isBackupDownloaded: Boolean) = firebaseCall {
        val resultUserInfo = userInfoReference()?.get()?.await()
        val lastSyncTime =
            resultUserInfo?.getLong(UserKey.KEY_USER_LAST_SYNC) ?: GLOBAL_START_DATE

        val backupTasks =
            userBackupTaskReference()?.get()?.await()

        if (backupTasks?.exists() == false &&
            backupTasks.data?.isEmpty() == true &&
            isBackupDownloaded
        ) {
            return@firebaseCall FirebaseResult.Error(null)
        }
        FirebaseResult.Success(lastSyncTime)
    }

    suspend fun downloadPremiumType(): FirebaseResult<String> = firebaseCall {
        val result = userInfoReference()?.get()?.await()
        val premiumType = result?.getString(UserKey.KEY_USER_PREMIUM_TYPE)
        FirebaseResult.Success(premiumType)
    }

    suspend fun downloadTotalCompleted(): FirebaseResult<Int> = firebaseCall {
        val result = userInfoReference()?.get()?.await()
        val totalCompleted = result?.getLong(UserKey.KEY_USER_COUNT_COMPLETED_TASKS)?.toInt()
        FirebaseResult.Success(totalCompleted)
    }

    suspend fun downloadUserTasks(context: Context, currentList: List<TaskType>) = firebaseCall {
        currentList.toMutableList().sortBy { it.idTask }

        val tasksDocument = userBackupTaskReference()?.get()?.await()
        val mapFromTasks: MutableMap<String, Any>? = tasksDocument?.data


        val stringTaskList: MutableList<String> = ArrayList()
        val processedList: MutableList<TaskType> = ArrayList()

        if (mapFromTasks != null) {
            //Get map value from key task
            for ((key) in mapFromTasks) {
                stringTaskList.add(key)
            }

            stringTaskList.forEach { task ->
                Log.d("TAG", "downloadUserTasks:$task ")
                val currentTime = System.currentTimeMillis() + stringTaskList.indexOf(task)

                val id = tasksDocument.getLong(
                    "$task.${TaskType.COLUMN_ID}"
                )?.toInt()!!

                val title = tasksDocument.getString("$task.${TaskType.COLUMN_TITLE}")
                    ?: "Error getting title"

                val description = tasksDocument.getString("$task.${TaskType.COLUMN_DESCRIPTION}")
                    ?: "Error getting description"

                val priority = Priority.valueOf(
                    tasksDocument.getString("$task.${TaskType.COLUMN_PRIORITY}")
                        ?: Priority.DEFAULT.name
                )

                val time = tasksDocument.getLong("$task.${TaskType.COLUMN_TIME}")
                    ?: GLOBAL_START_DATE

                val deadline = tasksDocument.getLong("$task.${TaskType.COLUMN_DEADLINE}")
                    ?: GLOBAL_START_DATE

                val lastOvercheck = tasksDocument.getLong("$task.${TaskType.COLUMN_LAST_OVERCHECK}")
                    ?: GLOBAL_START_DATE

                val completed = tasksDocument.getLong("$task.${TaskType.COLUMN_COMPLETED}")
                    ?: GLOBAL_START_DATE

                val repeatMode = RepeatMode.valueOf(
                    tasksDocument.getString("$task.${TaskType.COLUMN_REPEAT_MODE}")
                        ?: RepeatMode.Once.name
                )

                val missed = tasksDocument.getBoolean("$task.${TaskType.COLUMN_MISSED}")
                    ?: false

                val checked = tasksDocument.getBoolean("$task.${TaskType.COLUMN_CHECKED}")
                    ?: false

                val totalChecked = tasksDocument.getLong("$task.${TaskType.COLUMN_TOTAL_CHECKED}")
                    ?.toInt() ?: 0


                val taskType =
                    TaskType(
                        idTask = id,
                        title = title,
                        description = description,
                        priority = priority,

                        time = time,
                        deadline = deadline,
                        lastOvercheck = lastOvercheck,
                        created = currentTime,
                        completed = completed,

                        repeatMode = repeatMode,
                        missed = missed,
                        checked = checked,
                        totalChecked = totalChecked
                    )

                if (taskType.time != GLOBAL_START_DATE) {
                    if (taskType.time <= currentTime && taskType.repeatMode == RepeatMode.Once) {
                        taskType.time = GLOBAL_START_DATE
                    } else {
                        alarmUtil.setReminder(
                            context,
                            taskType.idTask,
                            taskType.repeatMode,
                            taskType.time
                        )
                    }
                }

                if (taskType.deadline != GLOBAL_START_DATE) {
                    if (taskType.deadline <= currentTime) {
                        taskType.missed = true
                    } else {
                        alarmUtil.setDeadline(
                            context,
                            taskType.idTask,
                            taskType.deadline
                        )
                    }
                }
                processedList.add(taskType)
            }
        } else {
            // Map of tasks is  null
            return@firebaseCall FirebaseResult.Error(Exception("There is no tasks for download"))
        }

        processedList.removeAll(currentList.ifEmpty { arrayListOf() })

        // Creating unique task list
        currentList.forEach { cTask ->
            processedList.forEach { pTask ->
                if (pTask.idTask == cTask.idTask) {
                    val newPTask = pTask.copy(
                        idTask = currentList.last().idTask + 10 + processedList.indexOf(pTask)
                    )

                    processedList.remove(pTask)
                    processedList.add(newPTask)
                }
            }
        }
        processedList.sortBy { it.created }
        Log.d("TAG", "downloadUserTasks:result success $processedList")
        FirebaseResult.Success(processedList)
    }


    // Friend actions
    suspend fun acceptFriendRequest(friendId: String): FirebaseResult<Any?> = firebaseCall {
        val map: MutableMap<String, Any> = HashMap()
        map[UserKey.KEY_USER_REQUEST] = 1
        friendUserIdReference(friendId)?.update(map)?.await()
        userFriendIdReference(friendId)?.update(map)?.await()
        FirebaseResult.Success(null)
    }

    suspend fun inviteFriend(
        userPrivateState: Boolean,
        totalCompleted: Int,
        searchId: String,
    ): FirebaseResult<Any?> = firebaseCall {
        if (searchId != user?.uid) {
            //Get all users in Firestore
            val usersCollection = usersReference()?.get()?.await()

            //Getting current user friend list
            val friendList = userFriendReference()?.get()?.await()?.documents

            if (usersCollection == null || friendList == null) {
                return@firebaseCall FirebaseResult.Error(
                    Exception("There is no users")
                )
            }

            val usersList: MutableList<QueryDocumentSnapshot> =
                ArrayList()
            for (document in usersCollection) {
                usersList.add(document)
            }

            //Searching a friend
            usersList.forEach { listUser ->
                Log.d("TAG", "inviteFriend: ${listUser.getString(UserKey.KEY_USER_ID)} $searchId")
                if (listUser.getString(UserKey.KEY_USER_ID) == searchId) {

                    for (friendDocument in friendList) {
                        if (searchId == friendDocument.getString(
                                UserKey.KEY_USER_ID
                            )
                        ) {
                            return@firebaseCall FirebaseResult.Error(
                                Exception(
                                    InviteFriendError.FRIEND_ALREADY.name
                                )
                            )
                        }
                    }

                    //Get and set a friend info
                    val friendMap: MutableMap<String, Any> =
                        HashMap()
                    friendMap.apply {
                        set(
                            UserKey.KEY_USER_ID,
                            listUser.getString(UserKey.KEY_USER_ID).toString()
                        )

                        set(UserKey.KEY_USER_REQUEST, 0)

                        set(
                            UserKey.KEY_USER_PHOTO_URL,
                            listUser.getString(UserKey.KEY_USER_PHOTO_URL).toString()
                        )

                        set(
                            UserKey.KEY_USER_NAME,
                            listUser.getString(UserKey.KEY_USER_NAME).toString()
                        )

                        set(
                            UserKey.KEY_USER_EMAIL,
                            listUser.getString(UserKey.KEY_USER_EMAIL).toString()
                        )

                        set(UserKey.KEY_USER_FRIEND_ADDING_TIME, System.currentTimeMillis())

                        set(
                            UserKey.KEY_USER_COUNT_COMPLETED_TASKS,
                            listUser.getLong(UserKey.KEY_USER_COUNT_COMPLETED_TASKS) ?: 0
                        )

                        set(
                            UserKey.KEY_USER_PRIVATE_MODE,
                            listUser.getBoolean(UserKey.KEY_USER_PRIVATE_MODE) ?: false
                        )
                    }


                    //Send friend request
                    val userInfo: MutableMap<String, Any> =
                        HashMap()

                    user?.let { currentUser ->
                        val uid = currentUser.uid
                        val email = currentUser.email.toString()
                        val name = currentUser.displayName.toString()
                        val photoUrl = currentUser.photoUrl.toString()

                        userInfo.apply {
                            userInfo[UserKey.KEY_USER_ID] =
                                uid
                            userInfo[UserKey.KEY_USER_REQUEST] =
                                2
                            userInfo[UserKey.KEY_USER_PHOTO_URL] =
                                photoUrl
                            userInfo[UserKey.KEY_USER_NAME] =
                                name
                            userInfo[UserKey.KEY_USER_FRIEND_ADDING_TIME] =
                                System.currentTimeMillis()
                            userInfo[UserKey.KEY_USER_EMAIL] =
                                email
                            userInfo[UserKey.KEY_USER_COUNT_COMPLETED_TASKS] =
                                totalCompleted
                            userInfo[UserKey.KEY_USER_PRIVATE_MODE] =
                                userPrivateState
                        }
                    }

                    userFriendIdReference(friendMap[UserKey.KEY_USER_ID] as String)?.set(
                        friendMap
                    )?.await()

                    friendUserIdReference(friendMap[UserKey.KEY_USER_ID] as String)?.set(userInfo)
                        ?.await()

                    FirebaseResult.Success(null)
                } else {
                    return@firebaseCall FirebaseResult.Error(Exception(InviteFriendError.NOT_EXIST.name))
                }
            }
        } else {
            return@firebaseCall FirebaseResult.Error(Exception(InviteFriendError.ADD_YOURSELF.name))
        }
        FirebaseResult.Success(null)
    }

    suspend fun deleteFriend(friendId: String) = firebaseCall {
        userFriendIdReference(friendId)?.delete()?.await()
        friendUserIdReference(friendId)?.delete()?.await()
        FirebaseResult.Success(null)
    }

    suspend fun denyFriendRequest(friendId: String) = firebaseCall {
        val map: MutableMap<String, Any> = HashMap()
        map[UserKey.KEY_USER_REQUEST] = 3

        userFriendIdReference(friendId)?.delete()?.await()
        friendUserIdReference(user?.uid)?.update(map)?.await()
        FirebaseResult.Success(null)
    }

    suspend fun downloadFriendTasks(friendId: String): FirebaseResult<List<TaskType>> =
        firebaseCall {
            // Getting friend's tasks
            val tasksDocument =
                firestore?.document("Users/${friendId}/TaskList/Tasks")?.get()?.await()

            val mapFromTasks: MutableMap<String, Any>? = tasksDocument?.data
            if (mapFromTasks != null) {

                val processedTaskList: MutableList<TaskType> = ArrayList()
                val tasksList: MutableList<String> = ArrayList()

                //Get map value from key task
                for ((key) in mapFromTasks) {
                    tasksList.add(key)
                }

                tasksList.forEach { task ->
                    val title = tasksDocument.getString("$task.${TaskType.COLUMN_TITLE}")
                        ?: "Error getting title"

                    if (title.isNotBlank()) {
                        val idTask = tasksList.size + tasksList.indexOf(task)

                        val priority = Priority.valueOf(
                            tasksDocument.getString("$task.${TaskType.COLUMN_PRIORITY}")
                                ?: Priority.DEFAULT.name
                        )

                        val checked = tasksDocument.getBoolean("$task.${TaskType.COLUMN_CHECKED}")
                            ?: false

                        val missed = tasksDocument.getBoolean("$task.${TaskType.COLUMN_MISSED}")
                            ?: false

                        val totalChecked =
                            tasksDocument.getLong("$task.${TaskType.COLUMN_TOTAL_CHECKED}")
                                ?.toInt() ?: 0

                        val taskLiteType = TaskType(
                            idTask = idTask,
                            title = title,
                            priority = priority,
                            checked = checked,
                            missed = missed,
                            totalChecked = totalChecked,
                        )
                        processedTaskList.add(taskLiteType)
                    }
                }
                val sortedList =
                    processedTaskList.sortedBy { it.title }
                        .sortedByDescending { it.priority.ordinal }
                FirebaseResult.Success(sortedList)
            } else {
                return@firebaseCall FirebaseResult.Error(null)
            }
        }

    suspend fun downloadFriendList(): FirebaseResult<List<DocumentSnapshot>> = firebaseCall {
        val result = userFriendReference()?.get()?.await()
        FirebaseResult.Success(result?.documents)
    }

    suspend fun updatePrivateFriend(friendId: String, privateState: Boolean) = firebaseCall {
        val userIndividualPrivate =
            hashMapOf<String, Any>(UserKey.KEY_USER_INDIVIDUAL_PRIVATE to privateState)
        val friendUserFriendPrivate =
            hashMapOf<String, Any>(UserKey.KEY_USER_FRIEND_PRIVATE to privateState)

        userFriendIdReference(friendId)?.update(userIndividualPrivate)?.await()
        friendUserIdReference(user?.uid)?.update(friendUserFriendPrivate)?.await()
        FirebaseResult.Success(null)
    }

    object UserKey {
        const val KEY_USER_ID = "user_id"
        const val KEY_USER_EMAIL = "user_email"
        const val KEY_USER_NAME = "user_name"
        const val KEY_USER_PHOTO_URL = "user_photo_url"
        const val KEY_USER_REQUEST = "user_request_code"
        const val KEY_USER_FRIEND_ADDING_TIME = "user_friend_adding_time"
        const val KEY_USER_COUNT_COMPLETED_TASKS = "user_count_completed"

        const val KEY_USER_PRIVATE_MODE = "user_private_mode"
        const val KEY_USER_INDIVIDUAL_PRIVATE = "user_individual_private"
        const val KEY_USER_FRIEND_PRIVATE = "user_friend_private"

        const val KEY_USER_EMAIL_CONFIRM = "user_email_confirm"
        const val KEY_USER_PREMIUM_TYPE = "user_premium_type"
        const val KEY_USER_LAST_SYNC = "user_last_sync"
    }
}