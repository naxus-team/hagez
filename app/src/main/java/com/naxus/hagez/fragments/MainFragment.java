package com.naxus.hagez.fragments;

import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.naxus.hagez.helpers.ThemeManager;

public class MainFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(
            @NonNull android.view.LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
        layout.setBackgroundColor(ThemeManager.background());
        layout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
        ));

        TextView title = new TextView(requireContext());
        title.setText("الصفحة الرئيسية");
        title.setTextColor(ThemeManager.textPrimary());
        title.setTextSize(22);

        Button openMenu = new Button(requireContext());
        openMenu.setText("فتح القائمة");
        openMenu.setOnClickListener(v -> {
//            if (getActivity() instanceof com.naxus.hagez.MainActivity) {
//                ((com.naxus.hagez.MainActivity) getActivity())
//                        .getMainLayout()
//                        .openMenu();
//            }
        });

        layout.addView(title);
        layout.addView(openMenu);
        return layout;
    }
}
