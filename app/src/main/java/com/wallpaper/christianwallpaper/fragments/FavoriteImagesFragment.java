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
import com.rdev.coreutils.utils.DevUtils;
import com.wallpaper.christianwallpaper.ImageItemClicked;
import com.wallpaper.christianwallpaper.R;
import com.wallpaper.christianwallpaper.WallpaperFullScreenActivity;
import com.wallpaper.christianwallpaper.adapters.WallpaperAdapter;
import com.wallpaper.christianwallpaper.databinding.FragmentFavoriteImagesBinding;
import com.wallpaper.christianwallpaper.dialogs.ProcessDialogFragment;
import com.wallpaper.christianwallpaper.models.Favorite;
import com.wallpaper.christianwallpaper.models.ImageItemModel;
import com.wallpaper.christianwallpaper.utils.ApiDataManger;
import com.wallpaper.christianwallpaper.utils.DatabaseRepository;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class FavoriteImagesFragment extends Fragment {
    private FragmentFavoriteImagesBinding _binding;
    private WallpaperAdapter _wallPaperAdapter;
    private DatabaseRepository _databaseRepository;
    private ApiDataManger _apiDataManger;
    private LinkedList<ImageItemModel> _imageItemModelLinkedList = new LinkedList<>();
    private DevInterstitialAdTimeInterval _interstitialAdManager;
    public FavoriteImagesFragment() {
        // Required empty public constructor
    }

    public static FavoriteImagesFragment newInstance() {
        FavoriteImagesFragment fragment = new FavoriteImagesFragment();
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
        _binding = FragmentFavoriteImagesBinding.inflate(inflater, container, false);
        return _binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        _binding.loadMoreSpinner.setVisibility(View.VISIBLE);
        _databaseRepository.getAllFavoriteImages().observe(getViewLifecycleOwner(), new Observer<List<Favorite>>() {
            @Override
            public void onChanged(List<Favorite> favoriteList) {
                if (favoriteList == null) {
                    showOrHideViewOnDataSize();
                    return;
                }

                _binding.loadMoreSpinner.setVisibility(View.GONE);
                _imageItemModelLinkedList.clear();
                String wallRawDirectory = DevUtils.getDomain() + ApiDataManger.getInstance(requireContext()).getWallModelData().raw_dir;
                for (int i = 0; i < favoriteList.size(); i++) {
                    String keyDecrypted = AESNameEncryption.decryptKey(favoriteList.get(i).image_id);
                    String imageWithPath = wallRawDirectory + favoriteList.get(i).image_id;
                    _imageItemModelLinkedList.add(new ImageItemModel(keyDecrypted, favoriteList.get(i).image_id, imageWithPath));
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
            ArrayList<String> listMoveToFirst = ApiDataManger.getInstance(requireContext()).moveToFirstList(_imageItemModelLinkedList, itemModel.getImageKey());
            Intent intent = new Intent(requireContext(), WallpaperFullScreenActivity.class);
            intent.putStringArrayListExtra(WallpaperFullScreenActivity.ARG_IMAGE_LIST, listMoveToFirst);
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