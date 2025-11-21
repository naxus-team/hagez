package com.hagzy.fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class ProfileFragment extends Fragment {
    @Nullable
    @Override
    public View onCreateView(@NonNull android.view.LayoutInflater inflater,
                             @Nullable android.view.ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        TextView t = new TextView(getContext());
        t.setText("الصفحة الرئيسية");
        t.setTextColor(Color.BLACK);
        t.setGravity(Gravity.CENTER);
        return t;
    }
}
