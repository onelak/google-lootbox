package com.demo.lootbox;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.AcknowledgePurchaseResponseListener;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.SkuDetails;
import com.android.billingclient.api.SkuDetailsParams;
import com.android.billingclient.api.SkuDetailsResponseListener;
import com.demo.lootbox.adapters.StoreRecyclerViewAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

public class StoreActivity extends AppCompatActivity implements PurchasesUpdatedListener {

    Button purchaseButton;
    RecyclerView storeRecyclerView;

    /* Setup Firebase Auth and Firestore */
    FirebaseAuth fbAuth;
    FirebaseFirestore firestore;

    /* Setup billing client so we can connect to google store */
    private BillingClient billingClient;

    AcknowledgePurchaseResponseListener acknowledgePurchaseResponseListener;

    String sku, itemName, itemDesc, itemPrice;
    SkuDetails skuDetails;

    Random random = new Random();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_store);

        /* Getting current instance of database from Firebase and Firestore */
        fbAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        purchaseButton = findViewById(R.id.purchaseButton);
//        storeRecyclerView = findViewById(R.id.storeRecyclerView);

        establishConnection();

        purchaseButton.setOnClickListener(x -> {
            purchaseItem();
        });
    }

    /* Connect to Google Play */
    public void establishConnection() {
        billingClient = BillingClient.newBuilder(this)
                .setListener(this)
                .enablePendingPurchases()
                .build();
        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(BillingResult billingResult) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    //todo: uncomment this
//                    Toast.makeText(StoreActivity.this, "Billing store successfully connected", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(StoreActivity.this, "Cannot connect to store: " + billingResult.getResponseCode() , Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onBillingServiceDisconnected() {
                Toast.makeText(StoreActivity.this, "You have disconnected from the Billing service", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void purchaseItem() {
        // Test Items
        List<String> itemsList = new ArrayList<>();
        itemsList.add("android.test.purchased");
        itemsList.add("apple_test");
        if(billingClient.isReady()){
            SkuDetailsParams skuDetailsParams = SkuDetailsParams.newBuilder()
                    .setSkusList(itemsList).setType(BillingClient.SkuType.INAPP).build();
            billingClient.querySkuDetailsAsync(skuDetailsParams,
                    new SkuDetailsResponseListener(){
                        @Override
                        public void onSkuDetailsResponse(BillingResult billingResult, List<SkuDetails> list) {
                            //process the result
                            BillingFlowParams flowParams = BillingFlowParams.newBuilder()
                                    .setSkuDetails(list.get(0))
                                    .build();
                            billingClient.launchBillingFlow(StoreActivity.this, flowParams);

                            if(billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && list != null){
                                for (SkuDetails skuDetail : list) {
                                    skuDetails = skuDetail;
                                    sku = skuDetail.getSku();
                                    itemName = skuDetail.getTitle();
                                    itemDesc = skuDetail.getDescription();
                                    itemPrice = skuDetail.getPrice();
                                }
                            } else {
                                Toast.makeText(StoreActivity.this, "Error: Cannot query product", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        } else {
            Toast.makeText(StoreActivity.this, "Error: The billing client is not ready!", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onPurchasesUpdated(BillingResult billingResult, @Nullable List<Purchase> purchases) {
        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK
                && purchases != null) {
            //for every purchase
//            for (Purchase purchase : purchases) {
//                handlePurchase(purchase);
//            }

            List<String> itemsList = new ArrayList<>();
            itemsList.add("Sandal");
            itemsList.add("Fork");
            itemsList.add("Spoon");
            itemsList.add("Couch");
            itemsList.add("Credit Card");
            itemsList.add("Book");
            itemsList.add("Nail Clipper");
            itemsList.add("Bed");
            itemsList.add("Paper");
            itemsList.add("Headphones");
            itemsList.add("Outlet");

            String randomItemName = itemsList.get( random.nextInt(itemsList.size()) );
            int randomNumber = random.nextInt(1000);

            String userId = fbAuth.getCurrentUser().getUid();

            HashMap<String, Object> items = new HashMap<>();
            items.put("itemName", randomItemName);
            items.put("itemPrice", randomNumber);

            CollectionReference colRef = firestore.collection("items").document(userId).collection("itemsList");
            colRef.add(items).addOnSuccessListener(aVoid -> Log.d("Success", "Success!"));

            Toast.makeText(StoreActivity.this, "Your item is: " + randomItemName, Toast.LENGTH_SHORT).show();

        } else if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.USER_CANCELED) {
            // Handle an error caused by a user cancelling the purchase flow.
            Toast.makeText(StoreActivity.this, "You have cancelled the purchase.", Toast.LENGTH_SHORT).show();
        } else if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED) {
            Toast.makeText(StoreActivity.this, "Item already owned!", Toast.LENGTH_SHORT).show();
        }
        else {
            // Handle any other error codes.
            Toast.makeText(StoreActivity.this, "Error: " + billingResult.getResponseCode() + billingResult.getDebugMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void handlePurchase(Purchase purchase) {
        if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
            Toast.makeText(StoreActivity.this, "You have purchased an item: " + itemName, Toast.LENGTH_SHORT).show();
//            purchaseButton.setEnabled(false);

            // Grant entitlement to the user.
            // Acknowledge the purchase if it hasn't already been acknowledged.
            if (!purchase.isAcknowledged()) {
                AcknowledgePurchaseParams acknowledgePurchaseParams =
                        AcknowledgePurchaseParams.newBuilder()
                                .setPurchaseToken(purchase.getPurchaseToken())
                                .build();
                billingClient.acknowledgePurchase(acknowledgePurchaseParams, acknowledgePurchaseResponseListener);
            }
        }
    }

}
