package com.example.wisewaste

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.wisewaste.ReportRepository
import com.example.wisewaste.ReportStatus
import com.example.wisewaste.Resource
import com.example.wisewaste.WasteReport
import com.example.wisewaste.WasteType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReportViewModel @Inject constructor(
    private val reportRepository: ReportRepository
) : ViewModel() {

    private val _userReports = MutableStateFlow<Resource<List<WasteReport>>>(Resource.Loading())
    val userReports: StateFlow<Resource<List<WasteReport>>> = _userReports.asStateFlow()

    private val _allReports = MutableStateFlow<Resource<List<WasteReport>>>(Resource.Loading())
    val allReports: StateFlow<Resource<List<WasteReport>>> = _allReports.asStateFlow()

    private val _submitState = MutableStateFlow<Resource<String>?>(null)
    val submitState: StateFlow<Resource<String>?> = _submitState.asStateFlow()

    // Form state
    val description = MutableStateFlow("")
    val selectedWasteType = MutableStateFlow(WasteType.NON_BIODEGRADABLE)
    val address = MutableStateFlow("")
    val latitude = MutableStateFlow(0.0)
    val longitude = MutableStateFlow(0.0)
    val imageBytes = MutableStateFlow<ByteArray?>(null)

    fun loadUserReports() {
        viewModelScope.launch {
            reportRepository.getUserReports().collect { _userReports.value = it }
        }
    }

    fun loadAllReports() {
        viewModelScope.launch {
            reportRepository.getAllReports().collect { _allReports.value = it }
        }
    }

    fun submitReport(username: String) {
        viewModelScope.launch {
            _submitState.value = Resource.Loading()
            val report = WasteReport(
                username = username,
                wasteType = selectedWasteType.value,
                description = description.value,
                latitude = latitude.value,
                longitude = longitude.value,
                address = address.value
            )
            _submitState.value = reportRepository.submitReport(report, imageBytes.value)
        }
    }

    fun updateReportStatus(reportId: String, status: ReportStatus, notes: String = "") {
        viewModelScope.launch {
            reportRepository.updateReportStatus(reportId, status, notes)
            loadAllReports()
        }
    }

    fun resetSubmitState() {
        _submitState.value = null
        description.value = ""
        address.value = ""
        imageBytes.value = null
        selectedWasteType.value = WasteType.NON_BIODEGRADABLE
    }
}