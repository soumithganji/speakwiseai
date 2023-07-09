package chat.gpt.speakwise.gpt3.ai.chatbot.app.Utils;

import android.app.Activity;
import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.OnLifecycleEvent;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.AcknowledgePurchaseResponseListener;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.ProductDetailsResponseListener;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesResponseListener;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.android.billingclient.api.QueryPurchasesParams;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import chat.gpt.speakwise.gpt3.ai.chatbot.app.CallBacks.SubscriptionSuccessCallBack;

public class BillingClientLifecycle implements LifecycleObserver, PurchasesUpdatedListener,
        BillingClientStateListener, ProductDetailsResponseListener, PurchasesResponseListener {

    private static final String TAG = "BillingLifecycle";

    private static final List<String> LIST_OF_SUBSCRIPTION_PRODUCTS = Collections.unmodifiableList(
            new ArrayList<String>() {{
                add(Common.BASIC_PRODUCT);
            }});

    /**
     * The purchase event is observable. Only one observer will be notified.
     */
    public SingleLiveEvent<List<Purchase>> purchaseUpdateEvent = new SingleLiveEvent<>();

    /**
     * Purchases are observable. This list will be updated when the Billing Library
     * detects new or existing purchases. All observers will be notified.
     */
    public MutableLiveData<List<Purchase>> purchases = new MutableLiveData<>();

    /**
     * SkuDetails for all known SKUs.
     */
    public MutableLiveData<ProductDetails> basicSubProductWithProductDetails = new MutableLiveData<>();

    private static volatile BillingClientLifecycle INSTANCE;

    private Application app;
    private BillingClient billingClient;
    private SubscriptionSuccessCallBack callBack;

    private BillingClientLifecycle(Application app) {
        this.app = app;
    }

    public static BillingClientLifecycle getInstance(Application app) {
        if (INSTANCE == null) {
            synchronized (BillingClientLifecycle.class) {
                if (INSTANCE == null) {
                    INSTANCE = new BillingClientLifecycle(app);
                }
            }
        }
        return INSTANCE;
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    public void create() {
        Log.d(TAG, "ON_CREATE");
        // Create a new BillingClient in onCreate().
        // Since the BillingClient can only be used once, we need to create a new instance
        // after ending the previous connection to the Google Play Store in onDestroy().
        billingClient = BillingClient.newBuilder(app)
                .setListener(this)
                .enablePendingPurchases() // Not used for subscriptions.
                .build();
        if (!billingClient.isReady()) {
            Log.d(TAG, "BillingClient: Start connection...");
            billingClient.startConnection(this);
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    public void destroy() {
        Log.d(TAG, "ON_DESTROY");
        if (billingClient.isReady()) {
            Log.d(TAG, "BillingClient can only be used once -- closing connection");
            // BillingClient can only be used once.
            // After calling endConnection(), we must create a new BillingClient.
            billingClient.endConnection();
        }
    }

    @Override
    public void onBillingSetupFinished(BillingResult billingResult) {
        int responseCode = billingResult.getResponseCode();
        String debugMessage = billingResult.getDebugMessage();
        Log.d(TAG, "onBillingSetupFinished: " + responseCode + " " + debugMessage);
        if (responseCode == BillingClient.BillingResponseCode.OK) {
            // The billing client is ready. You can query purchases here.
            querySubscriptionProductDetails();
            queryPurchases();
        }
    }

    @Override
    public void onBillingServiceDisconnected() {
        Log.d(TAG, "onBillingServiceDisconnected");
        // TODO: Try connecting again with exponential backoff.
    }

    /**
     * Receives the result from {@link #querySubscriptionProductDetails()}}.
     * <p>
     * Store the SkuDetails and post them in the {@link #onProductDetailsResponse}. This allows other
     * parts of the app to use the {@link ProductDetails} to show SKU information and make purchases.
     */

    @Override
    public void onProductDetailsResponse(@NonNull BillingResult billingResult, @NonNull List<ProductDetails> productDetailsList) {
        BillingResponse response = new BillingResponse(billingResult.getResponseCode());
        String debugMessage = billingResult.getDebugMessage();
        if (response.isOk()) {
            processProductDetails(productDetailsList);
        } else if (response.isTerribleFailure()) {
            // These response codes are not expected.
            Log.w(TAG, "onProductDetailsResponse - Unexpected error: " + response.code + " " + debugMessage);
        } else {
            Log.e(TAG, "onProductDetailsResponse: " + response.code + " " + debugMessage);
        }
    }

    private void processProductDetails(List<ProductDetails> productDetailsList) {
        int expectedProductDetailsCount = LIST_OF_SUBSCRIPTION_PRODUCTS.size();
        if (productDetailsList.isEmpty()) {
            Log.e(
                    TAG, "processProductDetails: " +
                            "Expected " + expectedProductDetailsCount + ", " +
                            "Found null ProductDetails. " +
                            "Check to see if the products you requested are correctly published " +
                            "in the Google Play Console."
            );
            postProductDetails(Collections.emptyList());
        } else {
            postProductDetails(productDetailsList);
        }
    }

    private void postProductDetails(List<ProductDetails> productDetailsList) {
        for (ProductDetails productDetails : productDetailsList) {
            if (productDetails.getProductType().equals(BillingClient.ProductType.SUBS)) {
                if (productDetails.getProductId().equals(Common.BASIC_PRODUCT)) {
                    basicSubProductWithProductDetails.postValue(productDetails);
                }
            }
        }
    }


    /**
     * Query Google Play Billing for existing purchases.
     * <p>
     * New purchases will be provided to the PurchasesUpdatedListener.
     * You still need to check the Google Play Billing API to know when purchase tokens are removed.
     */
    public void queryPurchases() {
        if (!billingClient.isReady()) {
            Log.e(TAG, "queryPurchases: BillingClient is not ready");
        }
        Log.d(TAG, "queryPurchases: SUBS");
        billingClient.queryPurchasesAsync(QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.SUBS)
                .build(), this);
    }

    /**
     * Callback from the billing library when queryPurchasesAsync is called.
     */
    @Override
    public void onQueryPurchasesResponse(@NonNull BillingResult billingResult, @NonNull List<Purchase> list) {
        processPurchases(list);
    }

    /**
     * Called by the Billing Library when new purchases are detected.
     */
    public void onPurchasesUpdated(BillingResult billingResult, List<Purchase> purchases) {
        if (billingResult == null) {
            Log.wtf(TAG, "onPurchasesUpdated: null BillingResult");
            return;
        }
        int responseCode = billingResult.getResponseCode();
        String debugMessage = billingResult.getDebugMessage();
        Log.d(TAG, String.format("onPurchasesUpdated: %s %s", responseCode, debugMessage));
        switch (responseCode) {
            case BillingClient.BillingResponseCode.OK:
                if (purchases == null) {
                    Log.d(TAG, "onPurchasesUpdated: null purchase list");
                    processPurchases(null);
                } else {
                    callBack.onSuccess();
                    processPurchases(purchases);
                }
                break;
            case BillingClient.BillingResponseCode.USER_CANCELED:
                Log.i(TAG, "onPurchasesUpdated: User canceled the purchase");
                break;
            case BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED:
                Log.i(TAG, "onPurchasesUpdated: The user already owns this item");
                break;
            case BillingClient.BillingResponseCode.DEVELOPER_ERROR:
                Log.e(TAG, "onPurchasesUpdated: Developer error means that Google Play " +
                        "does not recognize the configuration. If you are just getting started, " +
                        "make sure you have configured the application correctly in the " +
                        "Google Play Console. The SKU product ID must match and the APK you " +
                        "are using must be signed with release keys."
                );
                break;
        }
    }

    /**
     * Send purchase SingleLiveEvent and update purchases LiveData.
     * <p>
     * The SingleLiveEvent will trigger network call to verify the subscriptions on the sever.
     * The LiveData will allow Google Play settings UI to update based on the latest purchase data.
     */
    private void processPurchases(List<Purchase> purchasesList) {
        if (purchasesList != null) {
            Log.d(TAG, "processPurchases: " + purchasesList.size() + " purchase(s)");
        } else {
            Log.d(TAG, "processPurchases: with no purchases");
        }
        if (isUnchangedPurchaseList(purchasesList)) {
            Log.d(TAG, "processPurchases: Purchase list has not changed");
            return;
        }
        purchaseUpdateEvent.postValue(purchasesList);
        purchases.postValue(purchasesList);
        if (purchasesList != null) {
            logAcknowledgementStatus(purchasesList);
        }
    }

    /**
     * Log the number of purchases that are acknowledge and not acknowledged.
     * <p>
     * https://developer.android.com/google/play/billing/billing_library_releases_notes#2_0_acknowledge
     * <p>
     * When the purchase is first received, it will not be acknowledge.
     * This application sends the purchase token to the server for registration. After the
     * purchase token is registered to an account, the Android app acknowledges the purchase token.
     * The next time the purchase list is updated, it will contain acknowledged purchases.
     */
    private void logAcknowledgementStatus(List<Purchase> purchasesList) {
        int ack_yes = 0;
        int ack_no = 0;
        for (Purchase purchase : purchasesList) {
            if (purchase.isAcknowledged()) {
                ack_yes++;
            } else {
                acknowledgePurchase(purchase.getPurchaseToken());
                ack_no++;
            }
        }
        Log.d(TAG, "logAcknowledgementStatus: acknowledged=" + ack_yes +
                " unacknowledged=" + ack_no);
    }

    /**
     * Check whether the purchases have changed before posting changes.
     */
    private boolean isUnchangedPurchaseList(List<Purchase> purchasesList) {
        // TODO: Optimize to avoid updates with identical data.
        return false;
    }

    /**
     * In order to make purchases, you need the {@link ProductDetails} for the item or subscription.
     * This is an asynchronous call that will receive a result in {@link #querySubscriptionProductDetails}.
     */
    public void querySubscriptionProductDetails() {
        Log.d(TAG, "querySkuDetails");
        QueryProductDetailsParams.Builder params = QueryProductDetailsParams.newBuilder();

        List<QueryProductDetailsParams.Product> productList = new ArrayList<>();
        for (String product : LIST_OF_SUBSCRIPTION_PRODUCTS) {
            productList.add(
                    QueryProductDetailsParams.Product.newBuilder()
                            .setProductId(product)
                            .setProductType(BillingClient.SkuType.SUBS)
                            .build()
            );
        }

        params.setProductList(productList);
        billingClient.queryProductDetailsAsync(params.build(), this);
    }


    public String getPlanPrice(String tag) {
        ProductDetails basicSubProductDetails = basicSubProductWithProductDetails.getValue();
        if (basicSubProductDetails == null) {
            Log.e(TAG, "Could not find Basic product details.");
            return "";
        }

        ProductDetails.SubscriptionOfferDetails finalOffer = null;

        for (ProductDetails.SubscriptionOfferDetails offer : basicSubProductDetails.getSubscriptionOfferDetails()) {
            if (offer.getOfferTags().contains(tag)) {
                finalOffer = offer;
            }
        }

        ProductDetails.PricingPhase pricingPhase = finalOffer.getPricingPhases().getPricingPhaseList().get(0);

        return (new Common()).getCurrencySymbol(pricingPhase.getPriceCurrencyCode()) + (pricingPhase.getPriceAmountMicros() / 1000000);
    }


    public void buyBasePlans(Activity activity, String tag) {
        ProductDetails basicSubProductDetails = basicSubProductWithProductDetails.getValue();
        if (basicSubProductDetails == null) {
            Log.e(TAG, "Could not find Basic product details.");
            return;
        }

        List<ProductDetails.SubscriptionOfferDetails> basicOffers = retrieveEligibleOffers(basicSubProductDetails.getSubscriptionOfferDetails(), tag);

        String offerToken = leastPricedOfferToken(basicOffers);
        launchFlow(activity, offerToken, basicSubProductDetails);
    }

    private List<ProductDetails.SubscriptionOfferDetails> retrieveEligibleOffers(
            List<ProductDetails.SubscriptionOfferDetails> offerDetails, String tag) {
        List<ProductDetails.SubscriptionOfferDetails> eligibleOffers = new ArrayList<>();
        for (ProductDetails.SubscriptionOfferDetails offerDetail : offerDetails) {
            if (offerDetail.getOfferTags().contains(tag)) {
                eligibleOffers.add(offerDetail);
            }
        }
        return eligibleOffers;
    }

    private String leastPricedOfferToken(List<ProductDetails.SubscriptionOfferDetails> offerDetails) {
        String offerToken = "";
        long lowestPrice = Long.MAX_VALUE;

        if (!offerDetails.isEmpty()) {
            for (ProductDetails.SubscriptionOfferDetails offer : offerDetails) {
                for (ProductDetails.PricingPhase price : offer.getPricingPhases().getPricingPhaseList()) {
                    if (price.getPriceAmountMicros() < lowestPrice) {
                        lowestPrice = price.getPriceAmountMicros();
                        offerToken = offer.getOfferToken();
                    }
                }
            }
        }
        return offerToken;
    }

    private void launchFlow(Activity activity, String offerToken, ProductDetails productDetails) {
        String oldToken = null;
        List<Purchase> purchaseList = purchases.getValue();
        if (purchaseList != null) {
            for (Purchase purchase : purchaseList) {
                if (purchase.getProducts().contains(Common.BASIC_PRODUCT)) {
                    String purchaseToken = purchase.getPurchaseToken();
                    if (purchaseToken != null && !purchaseToken.isEmpty()) {
                        oldToken = purchaseToken;
                        break;
                    }
                }
            }
        }

        BillingFlowParams.Builder billingBuilder = BillingFlowParams.newBuilder();
        billingBuilder.setProductDetailsParamsList(
                Collections.singletonList(
                        BillingFlowParams.ProductDetailsParams.newBuilder()
                                .setProductDetails(productDetails)
                                .setOfferToken(offerToken)
                                .build()
                )
        );

        if (oldToken != null && purchases.getValue() != null && !purchases.getValue().isEmpty()) {
            for (Purchase purchase : purchases.getValue()) {
                if (purchase.getProducts().get(0).equals(oldToken)) {
                    BillingFlowParams.SubscriptionUpdateParams updateParams =
                            BillingFlowParams.SubscriptionUpdateParams.newBuilder()
                                    .setOldPurchaseToken(purchase.getPurchaseToken())
                                    .setReplaceProrationMode(BillingFlowParams.ProrationMode.IMMEDIATE_AND_CHARGE_FULL_PRICE)
                                    .build();
                    billingBuilder.setSubscriptionUpdateParams(updateParams);
                    break;
                }
            }
        }

        BillingFlowParams billingParams = billingBuilder.build();
        launchBillingFlow(activity, billingParams);
    }


    /**
     * Launching the billing flow.
     * <p>
     * Launching the UI to make a purchase requires a reference to the Activity.
     */
    public int launchBillingFlow(Activity activity, BillingFlowParams params) {
        if (!billingClient.isReady()) {
            Log.e(TAG, "launchBillingFlow: BillingClient is not ready");
        }
        BillingResult billingResult = billingClient.launchBillingFlow(activity, params);
        int responseCode = billingResult.getResponseCode();
        String debugMessage = billingResult.getDebugMessage();
        Log.d(TAG, "launchBillingFlow: BillingResponse " + responseCode + " " + debugMessage);
        return responseCode;
    }

    /**
     * Acknowledge a purchase.
     * <p>
     * https://developer.android.com/google/play/billing/billing_library_releases_notes#2_0_acknowledge
     * <p>
     * Apps should acknowledge the purchase after confirming that the purchase token
     * has been associated with a user. This app only acknowledges purchases after
     * successfully receiving the subscription data back from the server.
     * <p>
     * Developers can choose to acknowledge purchases from a server using the
     * Google Play Developer API. The server has direct access to the user database,
     * so using the Google Play Developer API for acknowledgement might be more reliable.
     * TODO(134506821): Acknowledge purchases on the server.
     * <p>
     * If the purchase token is not acknowledged within 3 days,
     * then Google Play will automatically refund and revoke the purchase.
     * This behavior helps ensure that users are not charged for subscriptions unless the
     * user has successfully received access to the content.
     * This eliminates a category of issues where users complain to developers
     * that they paid for something that the app is not giving to them.
     */
    public void acknowledgePurchase(String purchaseToken) {
        Log.d(TAG, "acknowledgePurchase");
        AcknowledgePurchaseParams params = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchaseToken)
                .build();
        billingClient.acknowledgePurchase(params, new AcknowledgePurchaseResponseListener() {
            @Override
            public void onAcknowledgePurchaseResponse(BillingResult billingResult) {
                int responseCode = billingResult.getResponseCode();
                String debugMessage = billingResult.getDebugMessage();
                Log.d(TAG, "acknowledgePurchase: " + responseCode + " " + debugMessage);
            }
        });
    }

    public void setOnSubSuccessCallBack(SubscriptionSuccessCallBack subscriptionSuccessCallBack) {
        callBack = subscriptionSuccessCallBack;
    }

    private static class BillingResponse {
        private final int code;

        public BillingResponse(int code) {
            this.code = code;
        }

        public boolean isOk() {
            return code == BillingClient.BillingResponseCode.OK;
        }

        public boolean canFailGracefully() {
            return code == BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED;
        }

        public boolean isRecoverableError() {
            return code == BillingClient.BillingResponseCode.ERROR ||
                    code == BillingClient.BillingResponseCode.SERVICE_DISCONNECTED;
        }

        public boolean isNonrecoverableError() {
            return code == BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE ||
                    code == BillingClient.BillingResponseCode.BILLING_UNAVAILABLE ||
                    code == BillingClient.BillingResponseCode.DEVELOPER_ERROR;
        }

        public boolean isTerribleFailure() {
            return code == BillingClient.BillingResponseCode.ITEM_UNAVAILABLE ||
                    code == BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED ||
                    code == BillingClient.BillingResponseCode.ITEM_NOT_OWNED ||
                    code == BillingClient.BillingResponseCode.USER_CANCELED;
        }
    }
}

