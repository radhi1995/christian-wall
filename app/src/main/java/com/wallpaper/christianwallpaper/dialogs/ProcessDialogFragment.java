package com.wallpaper.christianwallpaper.dialogs;

import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.LifecycleOwner;

import com.rdev.coreutils.event.DevEvent;
import com.rdev.coreutils.utils.DevUtils;
import com.wallpaper.christianwallpaper.R;

public class ProcessDialogFragment extends DialogFragment {

    public static final int RESULT_CANCEL = -1;
    public static final int RESULT_OK = 0;

    public static String TAG = "ProcessDialogFragment";
    public static final String ARG_PARAM_FULLSCREEN = "fullscreen";
    public static final String ARG_PARAM_MESSAGE = "message";
    public static final String ARG_PARAM_CANCELABLE = "cancelable";

    private int userOption = RESULT_OK;

    public ProcessDialogFragment() {
        // empty constructor
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {

        Bundle params = getArguments();
        boolean fullScreen = false;
        String msg = "";
        boolean cancelable = false;
        if (params != null) {
            fullScreen = params.getBoolean(ARG_PARAM_FULLSCREEN);
            msg = params.getString(ARG_PARAM_MESSAGE);
            cancelable = params.getBoolean(ARG_PARAM_CANCELABLE);
        }

        Dialog dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        // if want to customize the background color need to set transparent
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.setContentView(R.layout.process_dialog);
        // if need to make it fullscreen
        if (fullScreen) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        }
        View view = dialog.findViewById(R.id.dialog_container);
        TextView txtMessage = view.findViewById(R.id.txtMessage);
        LinearLayout linSpinner = view.findViewById(R.id.linSpinner);

        txtMessage.setText(msg);
        if (fullScreen) {
            linSpinner.setBackgroundResource(0);
        }

        // use Fragment method not dialog
        this.setCancelable(cancelable);

        return dialog;
    }


    public static void showDialog(FragmentManager fragmentManager, LifecycleOwner lifecycleOwner, String message, boolean cancelable, boolean fullScreen) {
        DevUtils.runWhenActive(lifecycleOwner, () -> {
            ProcessDialogFragment fragment = (ProcessDialogFragment) fragmentManager.findFragmentByTag(TAG);
            if (fragment == null) {
                Bundle params = new Bundle();
                params.putBoolean(ARG_PARAM_FULLSCREEN, fullScreen);
                params.putString(ARG_PARAM_MESSAGE, message);
                params.putBoolean(ARG_PARAM_CANCELABLE, cancelable);
                fragment = new ProcessDialogFragment();
                fragment.setArguments(params);
                fragment.setCancelable(cancelable);

                fragment.show(fragmentManager, TAG);
            }
        });
    }

    public static void updateDialogMessage(FragmentManager fragmentManager, String message, boolean cancelable) {
        ProcessDialogFragment fragment = (ProcessDialogFragment) fragmentManager.findFragmentByTag(TAG);
        if (fragment != null) {
            Dialog dialog = (Dialog) fragment.getDialog();
            if (dialog != null) {
                TextView txtMessage = dialog.findViewById(R.id.txtMessage);
                txtMessage.setText(message);
            }

            fragment.setCancelable(cancelable);
        }
    }

    public static void dismissDialog(FragmentManager fragmentManager, LifecycleOwner lifecycleOwner) {
        // put little delay to make sure if dialog just created
        DevUtils.runWhenActive(lifecycleOwner, () -> {
            ProcessDialogFragment fragment = (ProcessDialogFragment) fragmentManager.findFragmentByTag(TAG);
            if (fragment != null) {
                fragment.dismiss();
            }
        });
    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
        DevEvent.getInstance().setEvent(TAG, userOption);
    }


    @Override
    public void onCancel(@NonNull DialogInterface dialog) {
        super.onCancel(dialog);
        userOption = RESULT_CANCEL;
    }

}
