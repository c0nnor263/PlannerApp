package com.conboi.plannerapp.old.ui.profile

//
// @HiltViewModel
// class SubscribeViewModel
//    @Inject
//    constructor(
//        private val userRepository: UserRepository,
//        private val appSettingsRepository: AppSettingsRepository
//    ) : ViewModel() {
//        val premiumType = userRepository.getPremiumType()
//        val selectedPremiumType = MutableLiveData<Int>()
//
//        fun updateSelectedPremium(value: Int) {
//            selectedPremiumType.value = value
//        }
//
//        fun updatePremium(state: Boolean) =
//            viewModelScope.launch {
//                userRepository.updatePremium(state)
//            }
//
//        fun setNewPremium(
//            newPremiumType: PremiumType,
//            userInfo: HashMap<String, Any>,
//            importConfirmed: Boolean = false
//        ) = viewModelScope.launch {
//            userRepository.updateUser(userInfo)
//            userRepository.updatePremiumType(newPremiumType)
//            appSettingsRepository.updateImportConfirmed(importConfirmed)
//            appSettingsRepository.updateResubscribeShowed(false)
//            updatePremium(true)
//        }
//    }