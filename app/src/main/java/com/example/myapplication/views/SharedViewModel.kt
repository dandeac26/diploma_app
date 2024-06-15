package com.example.myapplication.views

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class SharedViewModel : ViewModel() {
    val refreshClientsTrigger = MutableLiveData<Boolean>()
    val refreshProductsTrigger = MutableLiveData<Boolean>()
    val refreshOrdersTrigger = MutableLiveData<Boolean>()
    val refreshStocksTrigger = MutableLiveData<Boolean>()
    val refreshDailyOrdersTrigger = MutableLiveData<Boolean>()
    val refreshHomeTrigger = MutableLiveData<Boolean>()
    val selectedDate: MutableLiveData<String> = MutableLiveData()
    val onBackPressed = MutableLiveData<Boolean>()
    fun handleBackPress() {
        onBackPressed.value = true
    }
}