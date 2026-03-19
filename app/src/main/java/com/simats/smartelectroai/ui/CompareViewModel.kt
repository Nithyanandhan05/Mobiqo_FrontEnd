package com.simats.smartelectroai.ui

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.simats.smartelectroai.api.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

// --- State Wrapper ---
sealed class CompareUiState {
    object Loading : CompareUiState()
    data class Success(val data: CompareData) : CompareUiState()
    data class Error(val message: String) : CompareUiState()
}

class CompareViewModel : ViewModel() {
    private val _compareState = MutableStateFlow<CompareUiState>(CompareUiState.Loading)
    val compareState = _compareState.asStateFlow()

    private val _searchResults = MutableStateFlow<List<SearchDeviceResult>>(emptyList())
    val searchResults = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching = _isSearching.asStateFlow()

    private var lastComparedDevices: List<String> = emptyList()

    fun searchDevice(query: String) {
        // 🚀 FIXED: Do not send "a" anymore. Send the actual query, or an empty string.
        val finalQuery = query.trim()

        _isSearching.value = true

        RetrofitClient.instance.searchDevices(finalQuery).enqueue(object : Callback<SearchDeviceResponse> {
            override fun onResponse(call: Call<SearchDeviceResponse>, response: Response<SearchDeviceResponse>) {
                _isSearching.value = false
                if (response.isSuccessful && response.body()?.status == "success") {
                    _searchResults.value = response.body()?.results ?: emptyList()
                } else {
                    _searchResults.value = emptyList()
                }
            }
            override fun onFailure(call: Call<SearchDeviceResponse>, t: Throwable) {
                _isSearching.value = false
                _searchResults.value = emptyList()
            }
        })
    }

    fun fetchCompareData(deviceNames: List<String>) {
        if (deviceNames.size < 2) {
            _compareState.value = CompareUiState.Error("At least 2 devices are required.")
            return
        }

        if (lastComparedDevices == deviceNames && _compareState.value is CompareUiState.Success) {
            return
        }

        lastComparedDevices = deviceNames
        _compareState.value = CompareUiState.Loading

        val request = CompareRequest(device1 = deviceNames[0], device2 = deviceNames[1])
        RetrofitClient.instance.compareDevices(request).enqueue(object : Callback<CompareResponse> {
            override fun onResponse(call: Call<CompareResponse>, response: Response<CompareResponse>) {
                if (response.isSuccessful && response.body()?.status == "success") {
                    val data = response.body()?.data
                    if (data != null) {
                        _compareState.value = CompareUiState.Success(data)
                    } else {
                        _compareState.value = CompareUiState.Error("Invalid response data")
                    }
                } else {
                    _compareState.value = CompareUiState.Error("Failed to fetch comparison")
                }
            }
            override fun onFailure(call: Call<CompareResponse>, t: Throwable) {
                _compareState.value = CompareUiState.Error(t.message ?: "Network error")
            }
        })
    }
}