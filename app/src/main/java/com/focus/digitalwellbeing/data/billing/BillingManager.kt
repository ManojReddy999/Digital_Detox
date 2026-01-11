package com.focus.digitalwellbeing.data.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import android.widget.Toast
import com.android.billingclient.api.*
import com.focus.digitalwellbeing.data.repository.CoinWalletRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BillingManager(
    private val context: Context,
    private val coinWalletRepository: CoinWalletRepository
) {
    private val _billingFlowInProcess = MutableStateFlow(false)
    val billingFlowInProcess: StateFlow<Boolean> = _billingFlowInProcess.asStateFlow()

    private val _purchases = MutableStateFlow<List<Purchase>>(emptyList())
    val purchases: StateFlow<List<Purchase>> = _purchases.asStateFlow()

    // Debug mode to simulate purchases without Play Store connection
    private val isDebugMode = false // Set to false for production

    // Track connection state
    private var isConnected = false

    private val billingClient = BillingClient.newBuilder(context)
        .setListener(this::onPurchasesUpdated)
        .enablePendingPurchases()
        .build()

    fun startConnection() {
        if (isDebugMode) return

        Log.d(TAG, "Starting billing connection...")
        
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d(TAG, "Billing setup finished successfully")
                    isConnected = true
                    querySkuDetails()
                } else {
                    Log.e(TAG, "Billing setup failed: ${billingResult.responseCode} - ${billingResult.debugMessage}")
                    isConnected = false
                }
            }

            override fun onBillingServiceDisconnected() {
                Log.d(TAG, "Billing service disconnected")
                isConnected = false
            }
        })
    }

    fun querySkuDetails() {
        if (isDebugMode) return

        val skuList = ArrayList<QueryProductDetailsParams.Product>()
        skuList.add(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(SKU_COINS_100)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        )
        skuList.add(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(SKU_COINS_750)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        )
        skuList.add(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(SKU_COINS_2000)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        )

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(skuList)
            .build()

        billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                Log.d(TAG, "Product details found: ${productDetailsList.size} products")
                productDetailsList.forEach { product ->
                    Log.d(TAG, "Product: ${product.productId} - ${product.name}")
                }
            } else {
                Log.e(TAG, "Failed to query products: ${billingResult.responseCode} - ${billingResult.debugMessage}")
            }
        }
    }

    fun launchPurchaseFlow(activity: Activity, skuId: String) {
        Log.d(TAG, "launchPurchaseFlow called for: $skuId")
        
        if (isDebugMode) {
            simulatePurchase(skuId)
            return
        }

        // Check connection status
        if (!billingClient.isReady) {
            Log.e(TAG, "Billing client not ready, attempting to reconnect...")
            showToast(activity, "Connecting to Play Store...")
            
            // Try to reconnect
            billingClient.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(billingResult: BillingResult) {
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        isConnected = true
                        // Now launch the purchase
                        performPurchase(activity, skuId)
                    } else {
                        Log.e(TAG, "Failed to connect: ${billingResult.debugMessage}")
                        showToast(activity, "Failed to connect to Play Store")
                    }
                }

                override fun onBillingServiceDisconnected() {
                    isConnected = false
                }
            })
            return
        }

        performPurchase(activity, skuId)
    }

    private fun performPurchase(activity: Activity, skuId: String) {
        Log.d(TAG, "performPurchase for: $skuId")
        
        val skuList = ArrayList<QueryProductDetailsParams.Product>()
        skuList.add(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(skuId)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        )
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(skuList)
            .build()

        billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            Log.d(TAG, "Query result: ${billingResult.responseCode}, products: ${productDetailsList.size}")
            
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && productDetailsList.isNotEmpty()) {
                val productDetails = productDetailsList[0]
                Log.d(TAG, "Found product: ${productDetails.productId}")
                
                val productDetailsParamsList = listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(productDetails)
                        .build()
                )

                val billingFlowParams = BillingFlowParams.newBuilder()
                    .setProductDetailsParamsList(productDetailsParamsList)
                    .build()

                val responseCode = billingClient.launchBillingFlow(activity, billingFlowParams)
                Log.d(TAG, "Launch billing flow response: ${responseCode.responseCode}")
            } else {
                Log.e(TAG, "Product not found: $skuId, response: ${billingResult.responseCode} - ${billingResult.debugMessage}")
                showToast(activity, "Product not found. Please try again later.")
            }
        }
    }

    private fun showToast(activity: Activity, message: String) {
        CoroutineScope(Dispatchers.Main).launch {
            Toast.makeText(activity, message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        Log.d(TAG, "onPurchasesUpdated: ${billingResult.responseCode}")
        
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) {
                handlePurchase(purchase)
            }
        } else if (billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
            Log.d(TAG, "User canceled purchase")
        } else {
            Log.e(TAG, "Purchase error: ${billingResult.responseCode} - ${billingResult.debugMessage}")
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        Log.d(TAG, "handlePurchase: ${purchase.products}")
        
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            if (!purchase.isAcknowledged) {
                val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()
                
                billingClient.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        Log.d(TAG, "Purchase acknowledged")
                        // Grant coins
                        grantCoinsForSku(purchase.products.firstOrNull() ?: "", purchase.orderId)
                    } else {
                        Log.e(TAG, "Failed to acknowledge: ${billingResult.debugMessage}")
                    }
                }
            }
        }
    }

    private fun simulatePurchase(skuId: String) {
        CoroutineScope(Dispatchers.IO).launch {
            Log.d(TAG, "Simulating purchase for $skuId")
            // Simulate network delay
            kotlinx.coroutines.delay(1000)
            grantCoinsForSku(skuId, "debug_order_${System.currentTimeMillis()}")
        }
    }

    private fun grantCoinsForSku(skuId: String, orderId: String?) {
        val amount = when (skuId) {
            SKU_COINS_100 -> 100
            SKU_COINS_750 -> 750
            SKU_COINS_2000 -> 2000
            else -> 0
        }

        if (amount > 0) {
            CoroutineScope(Dispatchers.IO).launch {
                coinWalletRepository.purchaseCoins(amount, orderId ?: "unknown")
                Log.d(TAG, "Granted $amount coins for $skuId")
            }
        }
    }

    companion object {
        private const val TAG = "BillingManager"
        const val SKU_COINS_100 = "coin_pack_100"
        const val SKU_COINS_750 = "coin_pack_750"
        const val SKU_COINS_2000 = "coin_pack_2000"
    }
}
