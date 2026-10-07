package com.example.plotmanager.plot;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.plotmanager.R;
import com.example.plotmanager.core.storage.*;
import com.example.plotmanager.setting.SettingActivity;
import com.example.plotmanager.story.MainActivity;
import com.example.plotmanager.story.Story;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class SearchFragment extends DialogFragment {

    public interface OnSearchListener {
        void onSearch();
    }

    private OnSearchListener listener;
    private EditText etKeyWord;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof OnSearchListener) {
            listener = (OnSearchListener) context;
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (getDialog() != null) {
            getDialog().requestWindowFeature(Window.FEATURE_NO_TITLE);
        }
        return inflater.inflate(R.layout.fragment_create_plot, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        etKeyWord = view.findViewById(R.id.et_key_word);
        TextView tvCancel = view.findViewById(R.id.tv_cancel);
        TextView tvConfirm = view.findViewById(R.id.tv_confirm);

        tvCancel.setOnClickListener(v -> dismiss());

        tvConfirm.setOnClickListener(v -> {
            String keyWord = etKeyWord.getText().toString().trim();
            if (keyWord.isEmpty()) {
                Toast.makeText(getContext(), "关键词不能为空", Toast.LENGTH_SHORT).show();
                return;
            }

            Intent intent = new Intent(getContext(), ReadStoryActivity.class);
            intent.putExtra("key_word",keyWord);
            startActivity(intent);

            if (listener != null) {
                listener.onSearch();
            }

            dismiss();
        });

        etKeyWord.requestFocus();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getDialog() != null) {
            Window window = getDialog().getWindow();
            if (window != null) {
                WindowManager.LayoutParams params = window.getAttributes();
                params.width = WindowManager.LayoutParams.MATCH_PARENT;
                params.height = WindowManager.LayoutParams.WRAP_CONTENT;
                window.setAttributes(params);
            }
        }

        InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.showSoftInput(etKeyWord, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
        InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null && getActivity() != null && getActivity().getCurrentFocus() != null) {
            imm.hideSoftInputFromWindow(getActivity().getCurrentFocus().getWindowToken(), 0);
        }
    }
}