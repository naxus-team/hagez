package com.hagzy.fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class MenuFragment extends Fragment {

    public MenuFragment() { super(); }

    @Nullable
    @Override
    public View onCreateView(@NonNull android.view.LayoutInflater inflater,
                             @Nullable android.view.ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setBackgroundColor(Color.WHITE);
        layout.setGravity(Gravity.CENTER);
        layout.setLayoutParams(new LinearLayout.LayoutParams(
                (int) (requireContext().getResources().getDisplayMetrics().widthPixels * 0.8f),
                LinearLayout.LayoutParams.MATCH_PARENT
        ));

        Button settings = new Button(requireContext());
        settings.setText("Settings");
        layout.addView(settings);

        Button logout = new Button(requireContext());
        logout.setText("Logout");
        layout.addView(logout);

        return layout;
    }
}
