package com.chastechgroup.taskpulse.viewmodel

import android.app.Activity
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.android.billingclient.api.*
import com.chastechgroup.taskpulse.util.PreferencesHelper
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class PointPackage(
    val id: String,
    val points: Int,
    val price: String,
    val productId: String
)

data class StoreUiState(
    val currentPoints: Int = 0,
    val packages: List<PointPackage> = emptyList(),
    val isLoading: Boolean = false,
    val purchaseResult: String = ""
)

class StoreViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(StoreUiState())
    val uiState: StateFlow<StoreUiState> = _uiState.asStateFlow()

    private val defaultPackages = listOf(
        PointPackage("p100", 100, "$0.50", "taskpulse_points_100"),
        PointPackage("p500", 500, "$2.00", "taskpulse_points_500"),
        PointPackage("p1000", 1000, "$3.50", "taskpulse_points_1000")
    )

    private var billingClient: BillingClient? = null

    init {
        _uiState.update { it.copy(packages = defaultPackages) }
        viewModelScope.launch {
            PreferencesHelper.getPoints(application).collect { pts ->
                _uiState.update { it.copy(currentPoints = pts) }
            }
        }
        setupBilling()
    }

    private fun setupBilling() {
        billingClient = BillingClient.newBuilder(getApplication())
            .setListener { billingResult, purchases ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
                    purchases.forEach { handlePurchase(it) }
                }
            }
            .enablePendingPurchases()
            .build()

        billingClient?.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    // Connected successfully
                }
            }
            override fun onBillingServiceDisconnected() { /* retry if needed */ }
        })
    }

    fun purchasePackage(activity: Activity, pkg: PointPackage) {
        // For production: query real product details from Play Store
        // Stub for dev/testing – directly add points
        viewModelScope.launch {
            PreferencesHelper.addPoints(getApplication(), pkg.points)
            _uiState.update { it.copy(purchaseResult = "✓ ${pkg.points} points added!") }
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        viewModelScope.launch {
            val points = when (purchase.products.firstOrNull()) {
                "taskpulse_points_100" -> 100
                "taskpulse_points_500" -> 500
                "taskpulse_points_1000" -> 1000
                else -> 0
            }
            if (points > 0) {
                PreferencesHelper.addPoints(getApplication(), points)
                _uiState.update { it.copy(purchaseResult = "✓ $points points added!") }
                billingClient?.acknowledgePurchase(
                    AcknowledgePurchaseParams.newBuilder()
                        .setPurchaseToken(purchase.purchaseToken).build()
                ) {}
            }
        }
    }

    fun clearResult() = _uiState.update { it.copy(purchaseResult = "") }

    override fun onCleared() {
        billingClient?.endConnection()
        super.onCleared()
    }
}
