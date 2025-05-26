package com.wallpaper.christianwallpaper.dialogs;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.Observer;

import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.rdev.coreutils.ads.AdsUtils;
import com.rdev.coreutils.ads.DevNativeAd;
import com.rdev.coreutils.event.DevEvent;
import com.rdev.coreutils.utils.DevUtils;
import com.wallpaper.christianwallpaper.R;
import com.wallpaper.christianwallpaper.databinding.CustomInformationDialogBinding;
import com.wallpaper.christianwallpaper.utils.ApiDataManger;
import com.wallpaper.christianwallpaper.utils.AppUtils;

import java.util.HashMap;

public class CustomInformationDialog extends BottomSheetDialogFragment {

    public static String TAG = "CustomInformationDialog";
    public static final String ARG_PARAM_EVENT_ID = "event_id";
    public static final String ARG_PARAM_ENABLE_ADS = "ads_enable";
    public static final String ARG_PARAM_TITLE = "title";
    public static final String ARG_PARAM_MESSAGE = "message";

    public static final int RESULT_CANCEL = -1;

    private int userOption = RESULT_CANCEL;

    private CustomInformationDialogBinding _binding;
    private DevNativeAd _devNativeAds;
    private String _eventId;
    private boolean _isAdsEnable;
    public CustomInformationDialog() {
        // empty constructor
    }

    public static CustomInformationDialog newInstance(String eventID, String title, String message, boolean isAdsEnable) {
        CustomInformationDialog fragment = new CustomInformationDialog();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM_EVENT_ID, eventID);
        args.putBoolean(ARG_PARAM_ENABLE_ADS, isAdsEnable);
        args.putString(ARG_PARAM_TITLE, title);
        args.putString(ARG_PARAM_MESSAGE, message);
        fragment.setArguments(args);
        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {

        Bundle params = getArguments();
        String title = "";
        String message = "";
        if (params != null) {
            _eventId = params.getString(ARG_PARAM_EVENT_ID);
            title = params.getString(ARG_PARAM_TITLE);
            message = params.getString(ARG_PARAM_MESSAGE);
        }

        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        _binding = CustomInformationDialogBinding.inflate(getLayoutInflater());
        dialog.setContentView(_binding.getRoot());

        View view = dialog.findViewById(R.id.dialog_container);
        assert view != null;

        _binding.txtTitle.setText(title);
        if (title == null || title.equalsIgnoreCase("")) {
            _binding.txtTitle.setVisibility(View.GONE);
        }

        _binding.txtMessage.setText(message);
        if (message == null || message.equalsIgnoreCase("")) {
            _binding.txtMessage.setVisibility(View.GONE);
        }

        _binding.btnPositive.setOnClickListener(v -> {
            userOption = RESULT_CANCEL;
            dismissAllowingStateLoss();
        });

        if (ApiDataManger.getInstance(requireContext()).isAdsEnabled()) {
            loadNativeAds();
        }

        // use Fragment method not dialog
        this.setCancelable(true);

        return dialog;
    }

    private void loadNativeAds() {
        _devNativeAds = DevNativeAd.getInstance(requireContext(), getString(R.string.admob_native));
        _devNativeAds.loadAd(false).observe(this, new Observer<HashMap<AdsUtils.AD_STATUS, NativeAd>>() {
            @Override
            public void onChanged(HashMap<AdsUtils.AD_STATUS, NativeAd> adStatusNativeAd) {
                if (adStatusNativeAd == null) {
                    return;
                }

                for (AdsUtils.AD_STATUS key : adStatusNativeAd.keySet()) {
                    if (key == AdsUtils.AD_STATUS.LOADING) {
                        _binding.adContainerNative.setVisibility(VISIBLE);

                        _binding.loadingNativeAd.loadingAdView.setVisibility(VISIBLE);
                        _binding.nativeAdTemplate.setVisibility(View.INVISIBLE);
                    } else if (key == AdsUtils.AD_STATUS.FAILED) {
                        _binding.adContainerNative.setVisibility(GONE);
                    } else if (key == AdsUtils.AD_STATUS.SUCCESS) {
                        _binding.adContainerNative.setVisibility(VISIBLE);

                        _binding.loadingNativeAd.loadingAdView.setVisibility(GONE);
                        _binding.nativeAdTemplate.setVisibility(VISIBLE);


                        NativeAd nativeAd = adStatusNativeAd.get(key);
                        _binding.nativeAdTemplate.setNativeAd(nativeAd);
                        _binding.nativeAdTemplate.setStyles(AppUtils.getNativeAdsCommonStyle(requireContext()));
                    }
                }

            }
        });
    }

    public static void showDialog(FragmentManager fragmentManager, LifecycleOwner lifecycleOwner,  String eventId, String title, String message, boolean isAdsEnable) {
        DevUtils.runWhenActive(lifecycleOwner, () -> {
            CustomInformationDialog fragment = (CustomInformationDialog) fragmentManager.findFragmentByTag(TAG);
            if (fragment == null) {
                fragment = CustomInformationDialog.newInstance(eventId, title, message, isAdsEnable);
                fragment.setCancelable(false);
                fragment.showNow(fragmentManager, TAG);
            }
        });
    }

    public static void dismissDialog(FragmentManager fragmentManager, LifecycleOwner lifecycleOwner) {
        // put little delay to make sure if dialog just created
        DevUtils.runWhenActive(lifecycleOwner, () -> {
            CustomInformationDialog fragment = (CustomInformationDialog) fragmentManager.findFragmentByTag(TAG);
            if (fragment != null) {
                fragment.dismiss();
            }
        });
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle bundle) {
        super.onSaveInstanceState(bundle);
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
        DevEvent.getInstance().setEvent(_eventId, userOption);
    }


    @Override
    public void onCancel(@NonNull DialogInterface dialog) {
        super.onCancel(dialog);
        userOption = RESULT_CANCEL;
    }
}