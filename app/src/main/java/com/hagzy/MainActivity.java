package com.hagzy;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;
import com.hagzy.fragments.HomeFragment;
import com.hagzy.helpers.DirectionHelper;
import com.hagzy.helpers.LocaleManager;
import com.hagzy.helpers.ThemeManager;
import com.hagzy.helpers.TranslationManager;
import com.hagzy.ui.SettingsLayout;
import com.hagzy.ui.WalletLayout;

public class MainActivity extends AppCompatActivity {
    private ViewPager2 pager;
    public DynamicFragment dynamicFragment = new DynamicFragment();

    private void setupInit() {
        LocaleManager.setLocale(this, "ar_AR");
        LocaleManager.applyLocale(this);
        ThemeManager.setDarkMode(this, false);
        ThemeManager.init(this);
        DirectionHelper.applyDirection(this, LocaleManager.getSavedLanguage(this));
        ThemeManager.applySystemBars(this);
        TranslationManager.load(this, LocaleManager.getSavedLanguage(this));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupInit();
        pager = new ViewPager2(this);
        pager.setId(View.generateViewId());
        pager.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,FrameLayout.LayoutParams.MATCH_PARENT));
        setContentView(pager);
        pager.setAdapter(new MainPagerAdapter(this));
        pager.setUserInputEnabled(false);
    }

    public void openPage(String type){
        dynamicFragment = new DynamicFragment();
        dynamicFragment.setPage(type);
        pager.setAdapter(new MainPagerAdapter(this));
        pager.setCurrentItem(1,true);
    }

    @SuppressLint("GestureBackNavigation")
    @Override
    public void onBackPressed(){
        if(pager.getCurrentItem()==1){
            pager.setCurrentItem(0,true);
        }else{
            super.onBackPressed();
        }
    }

    public class MainPagerAdapter extends FragmentStateAdapter{
        public MainPagerAdapter(@NonNull FragmentActivity fa){super(fa);}
        @NonNull
        @Override
        public Fragment createFragment(int position){
            if(position==0) return new HomeFragment();
            else return dynamicFragment;
        }
        @Override
        public int getItemCount(){return 2;}
    }

    public static class DynamicFragment extends Fragment{
        private String pageType="none";
        private FrameLayout root;

        public void setPage(String type){
            pageType=type;
            render();
        }

        @Nullable
        @Override
        public View onCreateView(@NonNull android.view.LayoutInflater inflater,@Nullable android.view.ViewGroup container,@Nullable Bundle savedInstanceState){
            root=new FrameLayout(requireContext());
            root.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,FrameLayout.LayoutParams.MATCH_PARENT));
            render();
            return root;
        }

        private void render(){
            if(root==null) return;
            root.removeAllViews();
            switch(pageType){
                case "wallet":
                    root.addView(new WalletLayout(getContext()).getView());
                    break;
                case "settings":
                    root.addView(new SettingsLayout(getContext()).getView());
                    break;
                default:
                    android.widget.TextView t=new android.widget.TextView(getContext());
                    t.setText("Unknown Page");
                    t.setTextSize(24);
                    root.addView(t);
            }
        }

        @Override
        public void onDestroyView() {
            super.onDestroyView();
            if (root != null) {
                root.removeAllViews();
                root = null;
            }
        }
    }
}
