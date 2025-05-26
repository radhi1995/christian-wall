package com.wallpaper.christianwallpaper.fragments;

import static com.wallpaper.christianwallpaper.utils.AppUtils.SHOW_ADS_DIALOG_DELAY;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.rdev.coreutils.ads.DevInterstitialAdTimeInterval;
import com.wallpaper.christianwallpaper.ImageItemClicked;
import com.wallpaper.christianwallpaper.R;
import com.wallpaper.christianwallpaper.WallpaperFullScreenActivity;
import com.wallpaper.christianwallpaper.adapters.WallpaperAdapter;
import com.wallpaper.christianwallpaper.databinding.FragmentLatestImagesBinding;
import com.wallpaper.christianwallpaper.dialogs.ProcessDialogFragment;
import com.wallpaper.christianwallpaper.models.ImageItemModel;
import com.wallpaper.christianwallpaper.utils.ApiDataManger;
import com.wallpaper.christianwallpaper.utils.DatabaseRepository;

import java.util.ArrayList;
import java.util.LinkedList;

public class LatestImagesFragment extends Fragment {
    private FragmentLatestImagesBinding _binding;
    private WallpaperAdapter _wallPaperAdapter;
    private ApiDataManger _apiDataManger;
    private DatabaseRepository _databaseRepository;

    private boolean isLoading = false;
    private LinkedList<ImageItemModel> _imageItemModelLinkedList = new LinkedList<>();
    private DevInterstitialAdTimeInterval _interstitialAdManager;
    public LatestImagesFragment() {
        // Required empty public constructor
    }

    public static LatestImagesFragment newInstance() {
        LatestImagesFragment fragment = new LatestImagesFragment();
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
        _binding = FragmentLatestImagesBinding.inflate(inflater, container, false);
        return _binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        _imageItemModelLinkedList = _apiDataManger.getLoadedLatestItems();
        showOrHideViewOnDataSize();
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

        _binding.recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                if (!isLoading && !recyclerView.canScrollVertically(1)) { // Check if at the bottom
                    if (_imageItemModelLinkedList.size() < _apiDataManger.getWallModelData().wallCount) {
                        loadMoreData();
                    }
                }
            }
        });
    }

    private void openFullPage(ImageItemModel itemModel) {
        if (itemModel != null) {
            ArrayList<String> listMoveToFirst = _apiDataManger.moveToFirstList(_imageItemModelLinkedList, itemModel.getImageKey());
            Intent intent = new Intent(requireContext(), WallpaperFullScreenActivity.class);
            intent.putStringArrayListExtra(WallpaperFullScreenActivity.ARG_IMAGE_LIST, listMoveToFirst);
            startActivity(intent);
        }
    }
    @SuppressLint("NotifyDataSetChanged")
    private void loadMoreData() {
        if (isLoading) {
            return;
        }
        isLoading = true;
        _binding.loadMoreSpinner.setVisibility(View.VISIBLE);
        new Handler().postDelayed(() -> {
            if (!isAdded()) {
                return;
            }
            isLoading = false;
            _binding.loadMoreSpinner.setVisibility(View.GONE);
            _imageItemModelLinkedList.addAll(_apiDataManger.getNextLatestData());
            showOrHideViewOnDataSize();
            _wallPaperAdapter.notifyDataSetChanged();
        }, 500);


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
        _binding = null;
    }
}