package im.getsocial.demo.fragment;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.ConsumeResponseListener;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.SkuDetails;
import com.android.billingclient.api.SkuDetailsParams;
import com.android.billingclient.api.SkuDetailsResponseListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import im.getsocial.demo.R;

public class PurchaseFragment extends BaseFragment implements PurchasesUpdatedListener {

    private BillingClient _billingClient;
    private PurchaseFragment.ViewContainer _viewContainer;
    private final List<IAPProduct> _availableProducts = new ArrayList<>();

    @Override
    public String getFragmentTag() {
        return null;
    }

    @Override
    public String getTitle() {
        return null;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_iap, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        _viewContainer = new PurchaseFragment.ViewContainer(view);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        _billingClient = BillingClient.newBuilder(this.getContext()).setListener(this).build();
    }

    @Override
    public void onResume() {
        super.onResume();
        _availableProducts.clear();
        setupBillingConnection();
    }

    private void setupBillingConnection() {

        _billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(@BillingClient.BillingResponse int billingResponseCode) {
                if (billingResponseCode == BillingClient.BillingResponse.OK) {
                    // The billing client is ready. You can query purchases here.
                    loadConsumableItems();
                    loadSubscriptions();
                    loadPurchaseHistory();
                }
            }
            @Override
            public void onBillingServiceDisconnected() {
                // Try to restart the connection on the next request to
                // Google Play by calling the startConnection() method.
                System.out.println("purchase client disconnected");
            }
        });

    }

    private void loadConsumableItems() {
        SkuDetailsParams.Builder detailsParamsBuilder = SkuDetailsParams.newBuilder();
        detailsParamsBuilder.setSkusList(Arrays.asList(new String[]{"im.getsocial.sdk.demo.internal.iap.managed"}));
        detailsParamsBuilder.setType(BillingClient.SkuType.INAPP);
        loadProducts(detailsParamsBuilder.build());
    }

    private void loadSubscriptions() {
        SkuDetailsParams.Builder detailsParamsBuilder = SkuDetailsParams.newBuilder();
        detailsParamsBuilder.setSkusList(Arrays.asList(new String[]{"im.getsocial.sdk.demo.internal.iap.subscription"}));
        detailsParamsBuilder.setType(BillingClient.SkuType.SUBS);
        loadProducts(detailsParamsBuilder.build());
    }

    private void loadPurchaseHistory() {
        Purchase.PurchasesResult result = _billingClient.queryPurchases(BillingClient.SkuType.INAPP);
        for (Purchase purchase : result.getPurchasesList()){
            consumePurchasedItem(purchase);
        }
    }

    private void loadProducts(SkuDetailsParams detailsParams) {
        _billingClient.querySkuDetailsAsync(detailsParams, new SkuDetailsResponseListener() {
            @Override
            public void onSkuDetailsResponse(int i, List<SkuDetails> list) {
                System.out.println("details response code: " + i);
                if (list != null) {
                    for (SkuDetails detail : list) {
                        _availableProducts.add(new IAPProduct(detail));
                    }
                    _viewContainer._iapList.setAdapter(new PurchaseFragment.IAPAdapter(getContext(), _availableProducts));
                }
            }
        });
    }

    private void purchaseItem(String productId, String skuType) {
        BillingFlowParams.Builder flowParams = BillingFlowParams.newBuilder();
        flowParams.setSku(productId);
        flowParams.setType(skuType);
        _billingClient.launchBillingFlow(this.getActivity(), flowParams.build() );

    }

    private void consumePurchasedItem(Purchase purchase) {
        _billingClient.consumeAsync(purchase.getPurchaseToken(), new ConsumeResponseListener() {
            @Override
            public void onConsumeResponse(int i, String s) {
                System.out.println("got consumePurchase response");
            }
        });
    }

    @Override
    public void onPurchasesUpdated(int i, @Nullable List<Purchase> list) {
        if (i == BillingClient.BillingResponse.OK) {
            for(Purchase purchase : list) {
                consumePurchasedItem(purchase);
            }
        }
    }

    class ViewContainer {

        @BindView(R.id.iap_list)
        ListView _iapList;

        @BindView(R.id.button_manual_iap)
        Button _button_manual_iap;

        ViewContainer(View view) {
            ButterKnife.bind(this, view);
            _availableProducts.clear();
            _button_manual_iap.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    addContentFragment(new ManualIAPFragment());
                }
            });
        }

    }

    class IAPAdapter extends ArrayAdapter<IAPProduct> {

        IAPAdapter(Context context, List<IAPProduct> objects) {
            super(context, 0, objects);
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, ViewGroup parent) {
            PurchaseFragment.IAPAdapter.ViewHolder holder;
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_item_iap, null);
                convertView.setTag(holder = new PurchaseFragment.IAPAdapter.ViewHolder(convertView));
            } else {
                holder = (PurchaseFragment.IAPAdapter.ViewHolder) convertView.getTag();
            }
            holder.setIAPProduct(getItem(position));

            return convertView;
        }

        class ViewHolder {

            private IAPProduct _iapProduct;

            ViewHolder(View view) {
                ButterKnife.bind(this, view);
            }

            void setIAPProduct(IAPProduct product) {
                _iapProduct = product;
                populate();
            }

            private void populate() {
                _productTitle.setText(_iapProduct.getProductTitle());
            }

            @BindView(R.id.product_title)
            TextView _productTitle;

            @OnClick(R.id.button_buy)
            void buyItem() {
                PurchaseFragment.this.purchaseItem(_iapProduct.getProductId(), _iapProduct.getProductType());
            }

        }

    }

    class IAPProduct {
        String _productId;
        String _productTitle;
        String _productType;

        public IAPProduct(SkuDetails details) {
            super();
            _productId = details.getSku();
            _productType = details.getType();
            _productTitle = details.getTitle();
        }

        public String getProductId() {
            return _productId;
        }

        public String getProductTitle() {
            return _productTitle;
        }

        public String getProductType() {
            return _productType;
        }

    }
}
