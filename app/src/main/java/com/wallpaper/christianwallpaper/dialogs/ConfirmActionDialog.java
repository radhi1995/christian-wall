package com.wallpaper.christianwallpaper.dialogs;

import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.LifecycleOwner;

import com.rdev.coreutils.event.DevEvent;
import com.rdev.coreutils.utils.DevUtils;
import com.wallpaper.christianwallpaper.R;
import com.wallpaper.christianwallpaper.databinding.ConfirmDialogBinding;

public class ConfirmActionDialog extends DialogFragment {

    public static String TAG = "ConfirmActionDialog";
    public static final String ARG_PARAM_EVENT_ID = "event_id";
    public static final String ARG_PARAM_TITLE = "title";
    public static final String ARG_PARAM_MESSAGE = "message";
    public static final String ARG_PARAM_CANCELABLE = "cancelable";
    public static final String ARG_PARAM_POSITIVE_TEXT = "positive_text";
    public static final String ARG_PARAM_NEGATIVE_TEXT = "negative_text";

    public static final int RESULT_CANCEL = -1;
    public static final int RESULT_POSITIVE = 0;
    public static final int RESULT_NEGATIVE = 1;

    private int userOption = RESULT_CANCEL;

    private ConfirmDialogBinding _binding;

    private String _eventId;
    public ConfirmActionDialog() {
        // empty constructor
    }

    public static ConfirmActionDialog newInstance(String eventID, String title, String message, String positiveTxt, String negativeTxt, boolean isCancelable) {
        ConfirmActionDialog fragment = new ConfirmActionDialog();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM_EVENT_ID, eventID);
        args.putString(ARG_PARAM_TITLE, title);
        args.putString(ARG_PARAM_MESSAGE, message);
        args.putBoolean(ARG_PARAM_CANCELABLE, isCancelable);
        args.putString(ARG_PARAM_POSITIVE_TEXT, positiveTxt);
        args.putString(ARG_PARAM_NEGATIVE_TEXT, negativeTxt);
        fragment.setArguments(args);
        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {

        Bundle params = getArguments();
        String title = "";
        String message = "";
        String positiveText = "";
        String negativeText = "";
        boolean cancellabel = false;
        if (params != null) {
            _eventId = params.getString(ARG_PARAM_EVENT_ID);
            title = params.getString(ARG_PARAM_TITLE);
            message = params.getString(ARG_PARAM_MESSAGE);
            cancellabel = params.getBoolean(ARG_PARAM_CANCELABLE);
            positiveText = params.getString(ARG_PARAM_POSITIVE_TEXT);
            negativeText = params.getString(ARG_PARAM_NEGATIVE_TEXT);
        }


        Dialog dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        // if want to customize the background color need to set transparent
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        _binding = ConfirmDialogBinding.inflate(getLayoutInflater());
        dialog.setContentView(_binding.getRoot());
        // if need to make it fullscreen
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        View view = dialog.findViewById(R.id.dialog_container);

        _binding.txtTitle.setText(title);
        if (title == null || title.equalsIgnoreCase("")) {
            _binding.txtTitle.setVisibility(View.GONE);
        }

        _binding.txtMessage.setText(message);
        if (message == null || message.equalsIgnoreCase("")) {
            _binding.txtMessage.setVisibility(View.GONE);
        }

        Button btnPositive = view.findViewById(R.id.btnPositive);
        btnPositive.setText(positiveText);

        Button btnNegative = view.findViewById(R.id.btnNegative);
        btnNegative.setText(negativeText);

        btnPositive.setOnClickListener(v -> {
            userOption = RESULT_POSITIVE;
            dismissAllowingStateLoss();

        });

        btnNegative.setOnClickListener(v -> {
            userOption = RESULT_NEGATIVE;
            dismissAllowingStateLoss();
        });

        // use Fragment method not dialog
        this.setCancelable(cancellabel);

        return dialog;
    }

    public static void showDialog(FragmentManager fragmentManager, LifecycleOwner lifecycleOwner,  String eventId, String title, String message, String positiveBtnTxt, String negativeBtnText, boolean isCancelLabel) {
        DevUtils.runWhenActive(lifecycleOwner, () -> {
            ConfirmActionDialog fragment = (ConfirmActionDialog) fragmentManager.findFragmentByTag(TAG);
            if (fragment == null) {
                fragment = ConfirmActionDialog.newInstance(eventId, title, message, positiveBtnTxt, negativeBtnText, isCancelLabel);
                fragment.setCancelable(false);
                fragment.showNow(fragmentManager, TAG);
            }
        });
    }

    public static void dismissDialog(FragmentManager fragmentManager, LifecycleOwner lifecycleOwner) {
        // put little delay to make sure if dialog just created
        DevUtils.runWhenActive(lifecycleOwner, () -> {
            ConfirmActionDialog fragment = (ConfirmActionDialog) fragmentManager.findFragmentByTag(TAG);
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