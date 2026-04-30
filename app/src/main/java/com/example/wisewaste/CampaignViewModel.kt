package com.example.wisewaste

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.wisewaste.Campaign
import com.example.wisewaste.CampaignRepository
import com.example.wisewaste.LeaderboardEntry
import com.example.wisewaste.Resource
import com.example.wisewaste.Reward
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CampaignViewModel @Inject constructor(
    private val campaignRepository: CampaignRepository
) : ViewModel() {

    private val _campaigns = MutableStateFlow<Resource<List<Campaign>>>(Resource.Loading())
    val campaigns: StateFlow<Resource<List<Campaign>>> = _campaigns.asStateFlow()

    private val _leaderboard = MutableStateFlow<Resource<List<LeaderboardEntry>>>(Resource.Loading())
    val leaderboard: StateFlow<Resource<List<LeaderboardEntry>>> = _leaderboard.asStateFlow()

    private val _rewards = MutableStateFlow<Resource<List<Reward>>>(Resource.Loading())
    val rewards: StateFlow<Resource<List<Reward>>> = _rewards.asStateFlow()

    private val _actionState = MutableStateFlow<Resource<Unit>?>(null)
    val actionState: StateFlow<Resource<Unit>?> = _actionState.asStateFlow()

    fun loadCampaigns() {
        viewModelScope.launch {
            campaignRepository.getCampaigns().collect { _campaigns.value = it }
        }
    }

    fun loadLeaderboard() {
        viewModelScope.launch {
            campaignRepository.getLeaderboard().collect { _leaderboard.value = it }
        }
    }

    fun loadRewards() {
        viewModelScope.launch {
            campaignRepository.getRewards().collect { _rewards.value = it }
        }
    }

    fun joinCampaign(campaignId: String) {
        viewModelScope.launch {
            _actionState.value = Resource.Loading()
            _actionState.value = campaignRepository.joinCampaign(campaignId)
        }
    }

    fun redeemReward(reward: Reward, userPoints: Int) {
        viewModelScope.launch {
            _actionState.value = Resource.Loading()
            _actionState.value = campaignRepository.redeemReward(reward, userPoints)
        }
    }

    fun resetActionState() { _actionState.value = null }
}