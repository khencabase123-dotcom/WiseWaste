package com.example.wisewaste

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.wisewaste.EducationRepository
import com.example.wisewaste.EducationalContent
import com.example.wisewaste.Resource
import com.example.wisewaste.WasteCategory
import com.example.wisewaste.WasteItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EducationViewModel @Inject constructor(
    private val educationRepository: EducationRepository
) : ViewModel() {

    private val _categories = MutableStateFlow<Resource<List<WasteCategory>>>(Resource.Loading())
    val categories: StateFlow<Resource<List<WasteCategory>>> = _categories.asStateFlow()

    private val _wasteItems = MutableStateFlow<Resource<List<WasteItem>>>(Resource.Loading())
    val wasteItems: StateFlow<Resource<List<WasteItem>>> = _wasteItems.asStateFlow()

    private val _content = MutableStateFlow<Resource<List<EducationalContent>>>(Resource.Loading())
    val content: StateFlow<Resource<List<EducationalContent>>> = _content.asStateFlow()

    fun loadCategories() {
        viewModelScope.launch {
            educationRepository.getWasteCategories().collect { _categories.value = it }
        }
    }

    fun loadWasteItems(categoryId: String) {
        viewModelScope.launch {
            educationRepository.getWasteItemsByCategory(categoryId).collect { _wasteItems.value = it }
        }
    }

    fun loadEducationalContent() {
        viewModelScope.launch {
            educationRepository.getEducationalContent().collect { _content.value = it }
        }
    }

    fun markContentCompleted(contentId: String, pointsAwarded: Int) {
        viewModelScope.launch {
            educationRepository.markContentCompleted(contentId, pointsAwarded)
        }
    }
}