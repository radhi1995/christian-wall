package com.wallpaper.christianwallpaper.fragments;

import static com.wallpaper.christianwallpaper.utils.AppUtils.SHOW_ADS_DIALOG_DELAY;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.GridLayoutManager;

import com.rdev.coreutils.ads.DevInterstitialAdTimeInterval;
import com.rdev.coreutils.utils.AESNameEncryption;
import com.wallpaper.christianwallpaper.ImageItemClicked;
import com.wallpaper.christianwallpaper.R;
import com.wallpaper.christianwallpaper.WallpaperFullScreenActivity;
import com.wallpaper.christianwallpaper.adapters.WallpaperAdapter;
import com.wallpaper.christianwallpaper.databinding.FragmentSavedImagesBinding;
import com.wallpaper.christianwallpaper.dialogs.ProcessDialogFragment;
import com.wallpaper.christianwallpaper.models.ImageItemModel;
import com.wallpaper.christianwallpaper.models.SavedImage;
import com.wallpaper.christianwallpaper.utils.ApiDataManger;
import com.wallpaper.christianwallpaper.utils.DatabaseRepository;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class SavedViewImagesFragment extends Fragment {
    private FragmentSavedImagesBinding _binding;
    private WallpaperAdapter _wallPaperAdapter;
    private DatabaseRepository _databaseRepository;
    private ApiDataManger _apiDataManger;
    private LinkedList<ImageItemModel> _imageItemModelLinkedList = new LinkedList<>();
    private DevInterstitialAdTimeInterval _interstitialAdManager;
    public SavedViewImagesFragment() {
        // Required empty public constructor
    }

    public static SavedViewImagesFragment newInstance() {
        SavedViewImagesFragment fragment = new SavedViewImagesFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {

        }
        _apiDataManger = ApiDataManger.getInstance(requireContext());
        _databaseRepository = DatabaseRepository.getInstance(requireContext());
        if (_apiDataManger.isAdsEnabled()) {
            _interstitialAdManager = DevInterstitialAdTimeInterval.getInstance(requireContext(), getString(R.string.admob_interstitial), _apiDataManger.getInterstitialMilli());
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        _binding = FragmentSavedImagesBinding.inflate(inflater, container, false);
        return _binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        getLifecycle().addObserver(_databaseRepository.lifecycleObserver);
        _binding.loadMoreSpinner.setVisibility(View.VISIBLE);
        _databaseRepository.getAllSavedImages().observe(getViewLifecycleOwner(), new Observer<List<SavedImage>>() {
            @Override
            public void onChanged(List<SavedImage> recentImages) {
                if (recentImages == null) {
                    showOrHideViewOnDataSize();
                    return;
                }

                _binding.loadMoreSpinner.setVisibility(View.GONE);
                _imageItemModelLinkedList.clear();
                for (int i = 0; i < recentImages.size(); i++) {
                    String keyDecrypted = AESNameEncryption.decryptKey(recentImages.get(i).image_id);
                    String imageWithPath = _apiDataManger.getLocalImagePath(recentImages.get(i).image_id);
                    _imageItemModelLinkedList.add(new ImageItemModel(keyDecrypted, recentImages.get(i).image_id, imageWithPath));
                }
                showOrHideViewOnDataSize();
                _wallPaperAdapter.notifyDataSetChanged();

            }
        });
        _wallPaperAdapter = new WallpaperAdapter(requireContext(), _imageItemModelLinkedList, new ImageItemClicked() {
            @Override
            public void onItemClicked(ImageItemModel itemModel) {
                if (_interstitialAdManager != null && _interstitialAdManager.isThisClickShowAds()) {
                    ProcessDialogFragment.showDialog(getChildFragmentManager(), getViewLifecycleOwner(), getString(R.string.ads_loading), false, false);
                    new Handler().postDelayed(() -> {
                        if (!isAdded()) {
                            return;
                        }
                        ProcessDialogFragment.dismissDialog(getChildFragmentManager(), getViewLifecycleOwner());
                        _interstitialAdManager.showAdOrContinue(requireActivity(), new Runnable() {
                            @Override
                            public void run() {
                                openFullPage(itemModel);
                            }
                        });
                    }, SHOW_ADS_DIALOG_DELAY);
                } else {
                    openFullPage(itemModel);
                }
            }
        });

        GridLayoutManager gridLayoutManager = new GridLayoutManager(requireContext(), 3);
        _binding.recyclerView.setLayoutManager(gridLayoutManager);
        _binding.recyclerView.setAdapter(_wallPaperAdapter);

    }

    private void openFullPage(ImageItemModel itemModel) {
        if (itemModel != null) {
            ArrayList<String> listMoveToFirst = _apiDataManger.moveToFirstList(_imageItemModelLinkedList, itemModel.getImageKey());
            Intent intent = new Intent(requireContext(), WallpaperFullScreenActivity.class);
            intent.putStringArrayListExtra(WallpaperFullScreenActivity.ARG_IMAGE_LIST, listMoveToFirst);
            intent.putExtra(WallpaperFullScreenActivity.ARG_FROM_SAVED, true);
            startActivity(intent);
        }
    }
    private void showOrHideViewOnDataSize() {
        if (_imageItemModelLinkedList == null || _imageItemModelLinkedList.isEmpty()) {
            _binding.recyclerView.setVisibility(View.GONE);
            _binding.linearNoData.setVisibility(View.VISIBLE);
        } else {
            _binding.recyclerView.setVisibility(View.VISIBLE);
            _binding.linearNoData.setVisibility(View.GONE);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        _wallPaperAdapter = null;
        _binding = null;
    }
}