package com.wallpaper.christianwallpaper.dialogs;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.rdev.coreutils.event.DevEvent;
import com.wallpaper.christianwallpaper.R;

public class SetWallpaperOptionDialog extends BottomSheetDialogFragment {
    public static final String TAG = "SetWallpaperOptionDialog";

    public static final String ARG_PARAM_EVENT_ID = "event_id";

    public static final int RESULT_CANCEL = -1;
    public static final int RESULT_HOME_SCREEN = 0;
    public static final int RESULT_LOCK_SCREEN = 1;
    public static final int RESULT_BOTH = 2;
    private int userOption = RESULT_CANCEL;
    private String _eventId;
    public SetWallpaperOptionDialog() {
        // empty constructor
    }
    public static SetWallpaperOptionDialog newInstance(String eventId) {
        SetWallpaperOptionDialog fragment = new SetWallpaperOptionDialog();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM_EVENT_ID, eventId);
        fragment.setArguments(args);
        return fragment;
    }
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        if (getArguments() != null) {
            _eventId = getArguments().getString(ARG_PARAM_EVENT_ID);
        }

        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        dialog.setContentView(R.layout.set_wallpaper_option);

        View view = dialog.findViewById(R.id.dialog_container);
        assert view != null;

        Button btnClose = view.findViewById(R.id.btnClose);
        Button btnSetHomeScreen = view.findViewById(R.id.btnSetHomeScreen);
        Button btnSetLockScreen = view.findViewById(R.id.btnSetLockScreen);
        Button btnSetASBoth = view.findViewById(R.id.btnSetASBoth);



        btnClose.setOnClickListener(v -> {
            userOption = RESULT_CANCEL;
            dismissAllowingStateLoss();
        });

        btnSetHomeScreen.setOnClickListener(v -> {
            userOption = RESULT_HOME_SCREEN;
            dismissAllowingStateLoss();
        });

        btnSetLockScreen.setOnClickListener(v -> {
            userOption = RESULT_LOCK_SCREEN;
            dismissAllowingStateLoss();
        });

        btnSetASBoth.setOnClickListener(v -> {
            userOption = RESULT_BOTH;
            dismissAllowingStateLoss();
        });

        // use Fragment method not dialog
        this.setCancelable(true);

        return dialog;
    }

    public static void showDialog(FragmentManager fragmentManager, String eventId) {
        SetWallpaperOptionDialog fragment = (SetWallpaperOptionDialog) fragmentManager.findFragmentByTag(TAG);
        if (fragment == null) {
            fragment = SetWallpaperOptionDialog.newInstance(eventId);
            fragment.showNow(fragmentManager, TAG);
        }
    }

    public static void dismissDialog(FragmentManager fragmentManager) {
        SetWallpaperOptionDialog fragment = (SetWallpaperOptionDialog) fragmentManager.findFragmentByTag(TAG);
        if (fragment != null) {
            fragment.dismissAllowingStateLoss();
        }
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
        DevEvent.getInstance().setEvent(_eventId, userOption);
    }

    @Override
    public void onCancel(@NonNull DialogInterface dialog) {
        super.onCancel(dialog);
    }
}
