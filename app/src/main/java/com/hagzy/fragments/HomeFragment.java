package com.hagzy.fragments;

import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.hagzy.ui.MainLayout;

public class HomeFragment extends Fragment {

    private FrameLayout root;

    @Nullable
    @Override
    public View onCreateView(@NonNull android.view.LayoutInflater inflater,
                             @Nullable android.view.ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        // نعمل root programmatically
        root = new FrameLayout(requireContext());
        root.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));

        // نضيف اللاوت الرئيسي
        root.addView(new MainLayout(getContext()).getView());

        return root;
    }
}
