package com.hagzy.fragments;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.mapbox.maps.CameraOptions;
import com.mapbox.maps.MapInitOptions;
import com.mapbox.maps.MapView;
import com.mapbox.maps.Style;
import com.mapbox.maps.plugin.Plugin;
import com.mapbox.maps.plugin.attribution.AttributionPlugin;
import com.mapbox.maps.plugin.gestures.GesturesPlugin;
import com.mapbox.maps.plugin.logo.LogoPlugin;
import com.mapbox.maps.plugin.scalebar.ScaleBarPlugin;
import com.hagzy.FieldActivity;
import com.hagzy.R;
import com.hagzy.helpers.ThemeManager;
import com.hagzy.utils.SessionManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class FieldMainFragment extends Fragment {

    FirebaseFirestore db;
    static CoordinatorLayout  root;
    public ImageView bannerImage, logoImage;
    public  LinearLayout bottomContainer;
    TextView nameText, typeText;
    LinearLayout aboutTab, reviewsTab;
    ViewPager2 viewPager;
    Integer insetTop, insetBottom;
    FrameLayout header;

    private static LocationInfo location;

    public static class LocationInfo {
        public String address;
        public double lat;
        public double lng;

        public LocationInfo(String address, double lat, double lng) {
            this.address = address;
            this.lat = lat;
            this.lng = lng;
        }
    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        moveHeaderInit();
        ThemeManager.setDarkMode(requireContext(), true);

        root = new CoordinatorLayout(requireContext());
        FrameLayout touchArea = new FrameLayout(requireContext());
        CoordinatorLayout.LayoutParams touchParams = new CoordinatorLayout.LayoutParams(
                MATCH_PARENT, MATCH_PARENT
        );
        touchParams.gravity = Gravity.TOP;
        touchParams.topMargin = dp(0); // يبدأ من أعلى الهيدر

// نخلي touchArea شفاف أو لون للتجربة
        touchArea.setBackgroundColor(Color.parseColor("#00000000")); // شفاف

        root.addView(touchArea, touchParams);

// نضمن أن tabs تبقى فوق الـ touchArea


        db = FirebaseFirestore.getInstance();
        String fieldId = "yEgrOqM8pLmUhRV1CIIA"; // getIntent().getStringExtra("fieldId")
        Log.d("Tagness", "onCreate: "+fieldId);
        if (fieldId == null) {
            Toast.makeText(requireContext(), "❌ لم يتم تمرير معرف الملعب", Toast.LENGTH_SHORT).show();
            getActivity().finish();
        }

        // 🏞️ رأس الصفحة: بانر + شعار دائري + نصوص + زر علوي
        header = new FrameLayout(requireContext());
        CoordinatorLayout.LayoutParams headerParams =
                new CoordinatorLayout.LayoutParams(MATCH_PARENT, dp(240));
        headerParams.gravity = Gravity.TOP;
        root.addView(header, headerParams);

// Banner الخلفية
        bannerImage = new ImageView(requireContext());
        bannerImage.setLayoutParams(new FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT));
        bannerImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
        bannerImage.setBackgroundColor(Color.parseColor("#F3F0EC"));
        header.addView(bannerImage);

        View gradientOverlay = new View(requireContext());
        FrameLayout.LayoutParams gradientParams = new FrameLayout.LayoutParams(
                MATCH_PARENT, dp(100)
        );
        gradientParams.gravity = Gravity.BOTTOM;
        GradientDrawable gradient = new GradientDrawable() {{setColor(Color.parseColor("#80000000"));}};
        gradientOverlay.setBackground(gradient);
        header.addView(gradientOverlay);

// 🧭 الحاوية الرئيسية الأفقية (الشعار + النصوص + الزرين في الأسفل)
        bottomContainer = new LinearLayout(requireContext());
        bottomContainer.setOrientation(LinearLayout.HORIZONTAL);
        bottomContainer.setGravity(Gravity.CENTER_VERTICAL);
        FrameLayout.LayoutParams bottomParams = new FrameLayout.LayoutParams(
                MATCH_PARENT, WRAP_CONTENT
        );
        bottomParams.gravity = Gravity.BOTTOM;
        bottomParams.bottomMargin = dp(70);
        bottomContainer.setLayoutParams(bottomParams);
        header.addView(bottomContainer);

// 🟠 شعار دائري على اليسار
        logoImage = new ImageView(requireContext());
        int logoSize = dp(64);
        LinearLayout.LayoutParams logoParams = new LinearLayout.LayoutParams(logoSize, logoSize);
        logoParams.setMargins(0, 0, dp(12), 0);
        logoImage.setLayoutParams(logoParams);
        logoImage.setScaleType(ImageView.ScaleType.CENTER_CROP);

        GradientDrawable logoBG = new GradientDrawable();
        logoBG.setShape(GradientDrawable.OVAL);
        logoImage.setBackground(logoBG);
        bottomContainer.addView(logoImage);

// 🧾 النصوص (اسم + نوع)
        LinearLayout textContainer = new LinearLayout(requireContext());
        textContainer.setOrientation(LinearLayout.VERTICAL);
        textContainer.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                0, WRAP_CONTENT, 1f
        );
        textContainer.setLayoutParams(textParams);

        LinearLayout nameRow = new LinearLayout(requireContext());
        nameRow.setOrientation(LinearLayout.HORIZONTAL);
        nameRow.setGravity(Gravity.CENTER_VERTICAL);
        nameRow.setPadding(dp(12), 0, dp(12),0);

        ImageView checkIcon = new ImageView(requireContext());
        checkIcon.setImageResource(R.drawable.check_badge); // غيّرها حسب اسم الأيقونة عندك
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dp(20), dp(20)) {{setMarginStart(dp(4)); topMargin = dp(1);}};
        checkIcon.setLayoutParams(iconParams);
        checkIcon.setColorFilter(Color.parseColor("#FFFFFF"), PorterDuff.Mode.SRC_IN); // لون ذهبي أو غامق

        nameText = textBold(18);
        nameText.setTextColor(Color.WHITE);
        nameText.setTypeface(ThemeManager.fontBold());
        nameText.setTranslationY(-dpf(1.5f));


        typeText = textNormal(14);
        typeText.setTextColor(Color.parseColor("#FFFFFF"));
        typeText.setTypeface(ThemeManager.fontSemiBold());
        typeText.setPadding(dp(12), dp(2), dp(12),dp(2));
        typeText.setTranslationY(-dpf(4f));
        nameRow.addView(nameText);
        nameRow.addView(checkIcon);
        textContainer.addView(nameRow);
        textContainer.addView(typeText);
        bottomContainer.addView(textContainer);

        LinearLayout actionButtons = new LinearLayout(requireContext());
        actionButtons.setOrientation(LinearLayout.HORIZONTAL);
        actionButtons.setGravity(Gravity.CENTER);
        actionButtons.setPadding(dp(4), dp(4), dp(4), 12);
        GradientDrawable actionBG = new GradientDrawable();
        actionBG.setColor(Color.parseColor("#FFFFFF"));
        actionBG.setCornerRadii(
                new float[]{dp(28), dp(28), dp(28), dp(28),
                        0, 0,0, 0}
        );
        actionButtons.setBackground(actionBG);
        actionButtons.setClipToOutline(true);
        LinearLayout.LayoutParams actionParams = new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
        actionButtons.setLayoutParams(actionParams);
/*
// زر المراسلة
        LinearLayout msgBtn = new LinearLayout(requireContext());
        msgBtn.setBackgroundColor(Color.TRANSPARENT);
        msgBtn.setElevation(0);

        // تخلي المحتوى في المنتصف
        msgBtn.setGravity(Gravity.CENTER);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(40), 1) {{setMarginEnd(dp(4)); gravity = Gravity.CENTER;}};
        msgBtn.setLayoutParams(params);
        GradientDrawable msgBtnBG = new GradientDrawable();
        msgBtnBG.setColor(Color.parseColor("#000000"));
        msgBtnBG.setCornerRadius(dp(40));
        msgBtn.setBackground(msgBtnBG);
        msgBtn.setPadding(0, dp(4), 0, dp(4)); // لتصغير المسافة داخل الزرار


        TextView msgBtnText = new TextView(requireContext());
        msgBtnText.setText("مراسلة واتساب");
        msgBtnText.setAllCaps(false);
        msgBtnText.setTranslationY(-dpf(1f));
        msgBtnText.setTypeface(ThemeManager.fontBold());
        msgBtnText.setTextColor(Color.WHITE);
        msgBtnText.setTextSize(14);
        msgBtnText.setGravity(Gravity.CENTER);
        msgBtn.addView(msgBtnText, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
        ));

        msgBtn.setOnClickListener(v -> {
            String phoneNumber = "+201001724808";
            String message = "مرحباً، أتيت من تطبيق حجز";

            try {
                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType("text/plain");
                intent.putExtra(Intent.EXTRA_TEXT, message);
                intent.putExtra("jid", phoneNumber.replace("+", "") + "@s.whatsapp.net");
                intent.setPackage("com.whatsapp");
                startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(requireContext(), "واتساب غير مثبت على الجهاز", Toast.LENGTH_SHORT).show();
            }
        });*/

        LinearLayout bookingBtn = new LinearLayout(requireContext());
        bookingBtn.setBackgroundColor(Color.TRANSPARENT);
        bookingBtn.setElevation(0);

        // تخلي المحتوى في المنتصف
        bookingBtn.setGravity(Gravity.CENTER);

        LinearLayout.LayoutParams dparams = new LinearLayout.LayoutParams(0, dp(48), 1) {{ gravity = Gravity.CENTER;}};
        bookingBtn.setLayoutParams(dparams);
        GradientDrawable bookingBtnBG = new GradientDrawable();
        bookingBtnBG.setColor(Color.parseColor("#000000"));
        bookingBtnBG.setCornerRadius(dp(48));
        bookingBtn.setBackground(bookingBtnBG);
        bookingBtn.setPadding(0, dp(4), 0, dp(4)); // لتصغير المسافة داخل الزرار


        TextView bookingBtnText = new TextView(requireContext());
        bookingBtnText.setText("حجز");
        bookingBtnText.setAllCaps(false);
        bookingBtnText.setTranslationY(-dpf(1f));
        bookingBtnText.setTypeface(ThemeManager.fontBold());
        bookingBtnText.setTextColor(Color.parseColor("#FFFFFF"));
        bookingBtnText.setTextSize(16);
        bookingBtnText.setGravity(Gravity.CENTER);
        bookingBtn.addView(bookingBtnText, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
        ));

        bookingBtn.setOnClickListener(v -> {
            if (getActivity() instanceof FieldActivity) {
                ((FieldActivity) getActivity()).goToBookings("FIELD_ID_123");
            }
        });

// إضافة الزرين للحاوية
        actionButtons.addView(bookingBtn);

// إضافة الحاوية للـ root فوق التبويبات
        FrameLayout.LayoutParams actionBtnsParams =
                new FrameLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
        actionBtnsParams.gravity = Gravity.BOTTOM;
        header.addView(actionButtons, actionBtnsParams);

// 🧩 ViewPager2
        viewPager = new ViewPager2(requireContext());
        LinearLayout.LayoutParams pagerParams = new LinearLayout.LayoutParams(
                MATCH_PARENT, 0, 1f
        );
        viewPager.setLayoutParams(pagerParams);
        viewPager.setPadding(0, 0, 0, dp(200));
        viewPager.setTranslationY(dp(240));
        viewPager.setClipToPadding(false);
        attachHeaderTouchHandler(viewPager);

        CoordinatorLayout.LayoutParams viewPagerParams =
                new CoordinatorLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT);
        viewPagerParams.gravity = Gravity.BOTTOM;
        root.addView(viewPager, viewPagerParams);

// 🧩 Tabs
        LinearLayout tabs = new LinearLayout(requireContext());
        tabs.setOrientation(LinearLayout.HORIZONTAL);
        tabs.setPadding(dp(8), dp(4), dp(8), dp(4));
        tabs.setLayoutParams(new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT));

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor("#FFFFFF"));
        tabs.setBackground(bg);

        aboutTab = makeTab("حول");
        reviewsTab = makeTab("الآراء");

        tabs.addView(aboutTab);
        tabs.addView(reviewsTab);
        CoordinatorLayout.LayoutParams tabsParams =
                new CoordinatorLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
        tabsParams.gravity = Gravity.BOTTOM;
        root.addView(tabs, tabsParams);

// ✅ عدّل فقط الهامش السفلي عند وجود شريط التنقل
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            insetTop = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            insetBottom = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom;

            // بعد ما نعرف الـ insetTop نرسم الزر:
            addTopButtons(insetTop);

            ViewGroup.MarginLayoutParams paramsNav = (ViewGroup.MarginLayoutParams) tabs.getLayoutParams();
            paramsNav.bottomMargin = insetBottom;
            tabs.setLayoutParams(paramsNav);
            tabs.bringToFront();

            return insets;
        });

// حمل البيانات
        loadData(fieldId);


        return root;
    }

    private TextView textBold(int sp) {
        TextView t = new TextView(getContext());
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTextColor(Color.BLACK);
        t.setTypeface(null, android.graphics.Typeface.BOLD);
        return t;
    }

    private TextView textNormal(int sp) {
        TextView t = new TextView(getContext());
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTextColor(Color.parseColor("#444444"));
        return t;
    }

    private float headerHeightFloat;
    private float minHeight;
    private float maxHeight;

    private float smoothedHeaderHeight; // للـ smoothing

    public  void moveHeaderInit() {
        minHeight = dp(145);
        maxHeight = dp(240);
    }

    @SuppressLint("ClickableViewAccessibility")
    private void attachHeaderTouchHandler(View scrollable) {
        final float[] lastY = new float[1];
        final float[] lastYPrev = new float[1];
        final float[] lastTime = new float[1];
        final boolean[] isDragging = new boolean[1];
        final boolean[] isFirstDelta = new boolean[1];

        scrollable.setOnTouchListener((v, event) -> {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    // سجل الوضع الحالي للهيدر قبل أي حركة
                    headerHeightFloat = smoothedHeaderHeight = maxHeight + header.getTranslationY();
                    smoothedHeaderHeight = headerHeightFloat;

                    lastY[0] = event.getRawY();
                    isDragging[0] = false;
                    isFirstDelta[0] = true;
                    break;

                case MotionEvent.ACTION_MOVE:
                    float currentY = event.getRawY();

                    if (!isDragging[0]) {
                        // أول حركة، نعمل sync لتجنب القفزة
                        lastY[0] = currentY;
                        lastYPrev[0] = lastY[0];
                        lastTime[0] = System.currentTimeMillis();

                        isDragging[0] = true;
                        return true;
                    }

                    float deltaY = currentY - lastY[0];
                    lastY[0] = currentY;
                    lastYPrev[0] = lastY[0];
                    lastTime[0] = System.currentTimeMillis();


                    if (isFirstDelta[0]) {
                        // تجاهل أول delta لتفادي القفزة
                        isFirstDelta[0] = false;
                        return true;
                    }

                    moveHeaderDynamically(deltaY);
                    v.getParent().requestDisallowInterceptTouchEvent(true);
                    return true;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    isDragging[0] = false;

                    // نسبة progress الحالية
                    float progress = (headerHeightFloat - minHeight) / (maxHeight - minHeight);

                    // سرعة السحب: delta آخر / الوقت
                    long currentTime = System.currentTimeMillis();
                    float velocityY = (lastY[0] - lastYPrev[0]) / (currentTime - lastTime[0] + 1); // pixels/ms
                    // نحتفظ بالقيم السابقة داخل ACTION_MOVE
                    lastYPrev[0] = lastY[0];
                    lastTime[0] = currentTime;

                    float targetHeight;

                    if (Math.abs(velocityY) > 0.5f) { // threshold لتجاهل السحب البطيء
                        // سحب سريع: نتبع الاتجاه
                        targetHeight = (velocityY < 0.2f) ? minHeight : maxHeight; // للأعلى collapse، للأسفل expand
                    } else {
                        // سحب بطيء: نعتمد على progress
                        targetHeight = (progress >= 0.5f) ? maxHeight : minHeight;
                    }

                    animateHeaderTo(targetHeight);
                    break;
            }
            return false;
        });
    }

    // دوال collapse/expand مع translateY
    boolean isHeaderCollapsed = false;

    public void collapseHeader() {
        if (isHeaderCollapsed) return;
        isHeaderCollapsed = true;
        animateHeaderTo(minHeight);
    }

    public void expandHeader() {
        if (!isHeaderCollapsed) return;
        isHeaderCollapsed = false;
        animateHeaderTo(maxHeight);
    }

    private void animateHeaderTo(float targetHeight) {
        ValueAnimator anim = ValueAnimator.ofFloat(headerHeightFloat, targetHeight);
        anim.setDuration(180);
        anim.addUpdateListener(a -> {
            headerHeightFloat = (float) a.getAnimatedValue();

            // translation للهيدر
            header.setTranslationY(headerHeightFloat - maxHeight);

            // تحريك viewPager
            viewPager.setTranslationY(headerHeightFloat);

            // نسبة السحب
            float progress = (headerHeightFloat - minHeight) / (maxHeight - minHeight);
            bannerImage.setAlpha(progress);

            // scale مع حدود
            float minScale = 0.8f;
            float maxScale = 1f;
            float scale = minScale + (maxScale - minScale) * progress;
            bottomContainer.setScaleX(scale);
            bottomContainer.setScaleY(scale);

            // translationY للهيدر السفلي
            float maxTranslation = dp(12); // أقصى نزول
            bottomContainer.setTranslationY((1 - progress) * maxTranslation);
        });
        anim.start();
    }

    public void moveHeaderDynamically(float deltaY) {
        // تعديل ارتفاع الهيدر
        headerHeightFloat += deltaY;
        headerHeightFloat = Math.max(minHeight, Math.min(maxHeight, headerHeightFloat));

        // تحريك الهيدر مباشرة بدون smoothing زائد
        header.setTranslationY(headerHeightFloat - maxHeight);

        // تحريك viewPager مع الهيدر
        viewPager.setTranslationY(headerHeightFloat);

        // نسبة السحب
        float progress = (headerHeightFloat - minHeight) / (maxHeight - minHeight);
        bannerImage.setAlpha(progress);

        // scale مع حدود
        float minScale = 0.8f;
        float maxScale = 1f;
        float scale = minScale + (maxScale - minScale) * progress;
        bottomContainer.setScaleX(scale);
        bottomContainer.setScaleY(scale);

        // translationY للهيدر السفلي
        float maxTranslation = dp(12); // أقصى نزول
        bottomContainer.setTranslationY((1 - progress) * maxTranslation);
    }




    private void loadData(String fieldId) {
        DocumentReference ref = db.collection("fields").document(fieldId);

        ref.get().addOnSuccessListener(doc -> {
            if (!doc.exists()) {
                Toast.makeText(requireContext(), "الملعب غير موجود", Toast.LENGTH_SHORT).show();
                return;
            }

            Map<String, Object> meta = (Map<String, Object>) doc.get("meta");
            Map<String, Object> loc = (Map<String, Object>) doc.get("location");

            if (loc != null) {
                String address = (String) loc.get("address");
                double lat = loc.get("lat") != null ? ((Number) loc.get("lat")).doubleValue() : 0;
                double lng = loc.get("lng") != null ? ((Number) loc.get("lng")).doubleValue() : 0;

                location = new FieldMainFragment.LocationInfo(address, lat, lng);
            } else {
                location = new FieldMainFragment.LocationInfo("غير محدد", 0, 0);
            }


            String name = meta != null ? (String) meta.get("name") : "بدون اسم";
            String desc = String.valueOf(doc.get("description"));
            String address = loc != null ? (String) loc.get("address") : "غير محدد";
            String rating = meta != null ? String.valueOf(meta.get("rating")) : "-";
            nameText.setText(name);
            int index = doc.get("category") != null ? ((Number) doc.get("category")).intValue() : 0;
            String typeTextString;
            switch (index) {
                case 0:
                    typeTextString = "نادي رياضي";
                    break;
                case 1:
                    typeTextString = "مستشفى";
                    break;
                case 2:
                    typeTextString = "مطعم";
                    break;
                case 3:
                    typeTextString = "فندق";
                    break;
                case 4:
                    typeTextString = "شركة";
                    break;
                default:
                    typeTextString = "غير محدد";
                    break;
            }

// تعيين النص
            typeText.setText(typeTextString);


            Task<QuerySnapshot> imagesTask = ref.collection("images").get();
            Task<QuerySnapshot> reviewsTask = ref.collection("reviews").get();
            AtomicReference<String> image = new AtomicReference<>();

            Tasks.whenAllSuccess(imagesTask, reviewsTask).addOnSuccessListener(results -> {
                String bannerUrl = null;
                QuerySnapshot imgs = imagesTask.getResult();
                if (imgs != null && !imgs.isEmpty()) {
                    if (imgs.size() > 0) image.set(imgs.getDocuments().get(0).getString("url"));
                    if (imgs.size() > 1) bannerUrl = imgs.getDocuments().get(1).getString("url");
                }

                if (bannerUrl != null)
                    Glide.with(requireContext()).load(bannerUrl).centerCrop().into(bannerImage);

                if (image.get() != null)
                    Glide.with(requireContext()).load(image.get()).circleCrop().into(logoImage);
                else
                    logoImage.setImageResource(android.R.drawable.sym_def_app_icon);

                // الآراء
                List<Map<String, Object>> reviewsList = new ArrayList<>();
                QuerySnapshot revs = reviewsTask.getResult();
                if (revs != null) {
                    for (DocumentSnapshot r : revs) {
                        Map<String, Object> m = new HashMap<>();
                        m.put("username", r.getString("username"));
                        m.put("comment", r.getString("comment"));
                        m.put("rating", r.getDouble("rating"));
                        reviewsList.add(m);
                    }
                }

                InfoFragment info = InfoFragment.newInstance(desc, address, rating, image);
/*
                BookingsFragment bookings = new BookingsFragment("FIELD_ID_123");
*/
                ReviewsFragment reviews = ReviewsFragment.newInstance(new ArrayList<>(reviewsList));

                viewPager.setAdapter(new FragmentStateAdapter(this) {
                    @NonNull
                    @Override
                    public Fragment createFragment(int position) {
                        switch (position) {
                            case 0:
                                return info;       // حول
                            case 1:
                                return reviews;    // الآراء
                            default:
                                return info;
                        }
                    }

                    @Override
                    public int getItemCount() {
                        return 2; // عندنا 3 تبويبات
                    }
                });

// ✅ عند الضغط على التبويبات
                aboutTab.setOnClickListener(v -> {
                    viewPager.setCurrentItem(0, true);
                    selectTab(aboutTab,  reviewsTab);
                });

                reviewsTab.setOnClickListener(v -> {
                    viewPager.setCurrentItem(2, true);
                    selectTab(reviewsTab, aboutTab);
                });

// ✅ تحديث المظهر عند التمرير بين الصفحات
                viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
                    @Override
                    public void onPageSelected(int position) {
                        switch (position) {
                            case 0:
                                selectTab(aboutTab, reviewsTab);
                                break;
                            case 1:
                                selectTab(reviewsTab, aboutTab);
                                break;
                        }

                        // منع السحب فقط لو الصفحة الأولى
                        RecyclerView recyclerView = (RecyclerView) viewPager.getChildAt(0);
                        recyclerView.setOverScrollMode(position == 0 ? RecyclerView.OVER_SCROLL_NEVER : RecyclerView.OVER_SCROLL_ALWAYS);

                        // إزالة أي OnTouchListener سابق
                        recyclerView.setOnTouchListener(null);

                        if (position == 0) {
                            recyclerView.addOnItemTouchListener(new RecyclerView.SimpleOnItemTouchListener() {
                                @Override
                                public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent e) {
                                    // رجع true لمنع السحب بالـ X فقط
                                    return Math.abs(e.getX() - e.getRawX()) > Math.abs(e.getY() - e.getRawY());
                                }
                            });
                        }
                    }
                });


            });
        });
    }
    private void addTopButtons(int insetTop) {
        FrameLayout headerContainer = new FrameLayout(requireContext());
        CoordinatorLayout.LayoutParams headerParams =
                new CoordinatorLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
        headerParams.gravity = Gravity.TOP;
        root.addView(headerContainer, headerParams);

// 🔹 زر الرجوع (على اليسار)
        LinearLayout backBtn = createCircleButton(R.drawable.arrow_left, v -> getActivity().finish());
        FrameLayout.LayoutParams backParams = new FrameLayout.LayoutParams(dp(48), dp(48));
        backParams.gravity = Gravity.TOP | Gravity.START;
        backParams.topMargin = insetTop;
        backParams.leftMargin = dp(12);
        backBtn.setLayoutParams(backParams);
        headerContainer.addView(backBtn);

// 🔹 حاوية الأزرار اليمنى
        LinearLayout rightButtons = new LinearLayout(requireContext());
        rightButtons.setOrientation(LinearLayout.HORIZONTAL);
        FrameLayout.LayoutParams rightParams = new FrameLayout.LayoutParams(
                WRAP_CONTENT, WRAP_CONTENT
        );
        rightParams.gravity = Gravity.TOP | Gravity.END;
        rightParams.topMargin = insetTop;
        rightParams.rightMargin = dp(12);
        rightButtons.setLayoutParams(rightParams);

        LinearLayout chatBtn = createCircleButton(R.drawable.whatsapp, v -> getActivity().finish());
        FrameLayout.LayoutParams chatParams = new FrameLayout.LayoutParams(dp(48), dp(48));
        chatBtn.setLayoutParams(chatParams);
        chatBtn.setOnClickListener(v -> {
            String phoneNumber = "+201001724808";
            String message = "مرحباً، أتيت من تطبيق حجز";

            try {
                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType("text/plain");
                intent.putExtra(Intent.EXTRA_TEXT, message);
                intent.putExtra("jid", phoneNumber.replace("+", "") + "@s.whatsapp.net");
                intent.setPackage("com.whatsapp");
                startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(requireContext(), "واتساب غير مثبت على الجهاز", Toast.LENGTH_SHORT).show();
            }
        });
        rightButtons.addView(chatBtn);

        LinearLayout moreBtn = createCircleButton(R.drawable.ellipsis_vertical, v -> getActivity().finish());
        FrameLayout.LayoutParams moreParams = new FrameLayout.LayoutParams(dp(48), dp(48));
        moreBtn.setLayoutParams(moreParams);
        rightButtons.addView(moreBtn);

// أضف المجموعة اليمنى للحاوية
        headerContainer.addView(rightButtons);
    }


    /** 🔹 كلاس بسيط لتجميع بيانات كل زر **/
    private static class ButtonData {
        int gravitySide;
        int iconRes;
        View.OnClickListener listener;

        ButtonData(int gravitySide, int iconRes, View.OnClickListener listener) {
            gravitySide = gravitySide;
            iconRes = iconRes;
            listener = listener;
        }
    }

    /** 🔹 دالة تنشئ زر دائري يحتوي على أيقونة **/
    private LinearLayout createCircleButton(int iconRes, View.OnClickListener listener) {
        LinearLayout button = new LinearLayout(requireContext());
        button.setGravity(Gravity.CENTER);

        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.OVAL);
        button.setBackground(bg);
        button.setClipToOutline(true);

        button.setClickable(true);
        button.setFocusable(true);
        TypedValue outValue = new TypedValue();
        requireContext().getTheme().resolveAttribute(
                android.R.attr.selectableItemBackgroundBorderless,
                outValue,
                true
        );
        button.setForeground(ContextCompat.getDrawable(requireContext(), outValue.resourceId));

        ImageView icon = new ImageView(requireContext());
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dp(24), dp(24));
        icon.setLayoutParams(iconParams);
        Drawable drawable = ContextCompat.getDrawable(requireContext(), iconRes);
        if (drawable != null) drawable.setTint(Color.WHITE);
        icon.setImageDrawable(drawable);
        button.addView(icon);

        button.setOnClickListener(listener);
        return button;
    }


    // 🧩 أدوات واجهة
    private LinearLayout makeTab(String text) {
        LinearLayout b = new LinearLayout(requireContext());
        b.setBackgroundColor(Color.TRANSPARENT);
        b.setElevation(0);

        // تخلي المحتوى في المنتصف
        b.setGravity(Gravity.CENTER);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(40), 1);
        b.setLayoutParams(params);

        b.setPadding(0, dp(4), 0, dp(4)); // لتصغير المسافة داخل الزرار

        TextView c = new TextView(requireContext());
        c.setText(text);
        c.setAllCaps(false);
        c.setTextColor(Color.parseColor("#FFFFFF"));
        c.setTextSize(14);
        c.setTranslationY(-dpf(1.5f));
        c.setGravity(Gravity.CENTER); // النص نفسه في المنتصف
        b.addView(c, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
        ));

        return b;
    }


    private void selectTab(LinearLayout selected, LinearLayout... others) {
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor("#F2EDE8"));
        bg.setCornerRadius(dp(16));
        selected.setElevation(0);
        selected.setBackground(bg);
        TextView tv = (TextView) selected.getChildAt(0);
        tv.setTextColor(Color.parseColor("#4B463D"));
        tv.setTypeface(ThemeManager.fontBold());
        tv.setTranslationY(-dpf(1.5f));

        for (LinearLayout other : others) {
            other.setBackground(null);
            TextView rtv = (TextView) other.getChildAt(0);
            rtv.setTextColor(Color.parseColor("#4F4F4F"));
            rtv.setTypeface(ThemeManager.fontBold());
        }
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }

    float dpf(float value) {
        return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, value, getResources().getDisplayMetrics()
        );
    }
    // ℹ️ حول
    public static class InfoFragment extends Fragment {
        String desc, address, rating;
        AtomicReference<String> image;
        MapView mapView;

        public InfoFragment() {
            // لازم يكون public وفاضي علشان Android يقدر يعيد بناء الـ Fragment
        }

        public static InfoFragment newInstance(String desc, String address, String rating, AtomicReference<String> image) {
            InfoFragment fragment = new InfoFragment();
            Bundle args = new Bundle();
            args.putString("desc", desc);
            args.putString("address", address);
            args.putString("rating", rating);
            if (image != null && image.get() != null)
                args.putString("image", image.get());
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            if (getArguments() != null) {
                desc = getArguments().getString("desc");
                address = getArguments().getString("address");
                rating = getArguments().getString("rating");
                String img = getArguments().getString("image");
                if (img != null) image = new AtomicReference<>(img);
            }
        }

        @SuppressLint("ClickableViewAccessibility")
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            // Root container
            ScrollView scroll = new ScrollView(requireContext());
            scroll.setFillViewport(true);
            if (getParentFragment() instanceof FieldMainFragment) {
                ((FieldMainFragment) getParentFragment()).attachHeaderTouchHandler(scroll);
            }


            LinearLayout layouts = new LinearLayout(requireContext());
            layouts.setOrientation(LinearLayout.VERTICAL);
            layouts.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT
            ));


            //



            TextView addressText = new TextView(requireContext());
            addressText.setText(address);
            addressText.setTextColor(Color.parseColor("#4B463D"));
            addressText.setTextSize(dp(5));
            addressText.setTypeface(ThemeManager.fontBold());
            addressText.setTranslationY(-dpf(1.5f));
            LinearLayout addressBox = createInfoBox("العنوان", addressText);

            Map<String, String> schedule = new HashMap<>();
            schedule.put("الأحد", "09:00 - 17:00");
            schedule.put("الإثنين", "10:00 - 18:00");

            LinearLayout weekTable = createWeekTable(schedule);
            LinearLayout tableBox = createInfoBox("الجدول الأسبوعي", weekTable);


            TextView subTitle = new TextView(requireContext());
            subTitle.setText(desc);
            subTitle.setTextColor(Color.parseColor("#4B463D"));
            subTitle.setTextSize(dp(5));
            subTitle.setTypeface(ThemeManager.fontBold());
            subTitle.setTranslationY(-dpf(1.5f));
            LinearLayout detailsBox = createInfoBox("الوصف", subTitle);

            layouts.addView(createInfoBox("المسافة", createMapView()));
            layouts.addView(addressBox);
            layouts.addView(tableBox);
            layouts.addView(detailsBox);


            scroll.addView(layouts);
            return scroll;
        }

        @NonNull
        private LinearLayout createInfoBox(String titleText, View contentView) {
            // الـ container الأساسي
            LinearLayout box = new LinearLayout(requireContext());
            box.setOrientation(LinearLayout.VERTICAL);
            box.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            ) {{
                bottomMargin = dp(6);
                leftMargin = dp(6);
                rightMargin = dp(6);
            }});

            // الخلفية بإطار وزوايا
            GradientDrawable boxBG = new GradientDrawable();
            boxBG.setStroke(dp(2), Color.parseColor("#EFEDE9"));
            boxBG.setCornerRadius(dp(16));
            box.setBackground(boxBG);
            box.setPadding(dp(12), dp(6), dp(12), dp(12));

            // العنوان (title)
            TextView title = new TextView(requireContext());
            title.setText(titleText);
            title.setTextColor(Color.parseColor("#804B463D"));
            title.setTextSize(dp(4));
            title.setTranslationY(-dpf(1.5f));
            title.setTypeface(ThemeManager.fontSemiBold());

            // إضافة الـ title والمحتوى
            box.addView(title);
            box.addView(contentView);

            return box;
        }

        @NonNull
        private LinearLayout createMapView() {
            LinearLayout mapLayout = new LinearLayout(requireContext());
            mapLayout.setOrientation(LinearLayout.VERTICAL);
            mapLayout.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            ));

            LinearLayout mapDetailsLayout = new LinearLayout(requireContext());
            mapDetailsLayout.setOrientation(LinearLayout.HORIZONTAL);
            mapDetailsLayout.setGravity(Gravity.CENTER_VERTICAL);
            mapDetailsLayout.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            ) {{
                topMargin = dp(8);
            }});

            ImageView userImage = new ImageView(requireContext());
            SessionManager session = new SessionManager(requireContext());
            if (session.isLoggedIn()) {
                String photoUrl = session.getPhoto();
                if (!photoUrl.isEmpty()) {
                    Glide.with(requireContext())
                            .load(photoUrl)
                            .circleCrop()
                            .into(userImage);
                }
            }

            LinearLayout.LayoutParams userParams = new LinearLayout.LayoutParams(dp(28), dp(28)) {{ setMarginEnd(dp(8)); }};
            userImage.setLayoutParams(userParams);
            userImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
            userImage.setClipToOutline(true);
            GradientDrawable userShape = new GradientDrawable();
            userShape.setCornerRadius(dp(18));
            userImage.setBackground(userShape);

            LinearLayout clubLayout = new LinearLayout(requireContext());
            clubLayout.setOrientation(LinearLayout.HORIZONTAL);
            clubLayout.setGravity(Gravity.CENTER_VERTICAL);

            View leftLine = new View(requireContext());
            leftLine.setLayoutParams(new LinearLayout.LayoutParams(0, dp(2), 1f));
            leftLine.setBackgroundColor(Color.parseColor("#C0BBB3"));

            View rightLine = new View(requireContext());
            rightLine.setLayoutParams(new LinearLayout.LayoutParams(0, dp(2), 1f));
            rightLine.setBackgroundColor(Color.parseColor("#C0BBB3"));

            ImageView clubImage = new ImageView(requireContext());
            if (image != null && image.get() != null && !image.get().isEmpty()) {
                Glide.with(requireContext())
                        .load(image.get())
                        .circleCrop()
                        .into(clubImage);
            } else {
                clubImage.setImageResource(android.R.drawable.sym_def_app_icon);
            }
            LinearLayout.LayoutParams clubParams = new LinearLayout.LayoutParams(dp(28), dp(28)) {{ setMarginStart(dp(8)); }};
            clubImage.setLayoutParams(clubParams);
            clubImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
            clubImage.setClipToOutline(true);
            GradientDrawable clubShape = new GradientDrawable();
            clubShape.setCornerRadius(dp(20));
            clubImage.setBackground(clubShape);

// إضافة المسافات بين العناصر
            LinearLayout distanceLayout = new LinearLayout(requireContext());
            LinearLayout.LayoutParams dParams = new LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT) {{ leftMargin=dp(8); rightMargin=dp(8); }};
            distanceLayout.setLayoutParams(dParams);

            LinearLayout distance = new LinearLayout(requireContext());
      /*      GradientDrawable distanceBG = new GradientDrawable();
            distanceBG.setStroke(dp(2), Color.parseColor("#C0BBB3"));
            distanceBG.setCornerRadius(dp(16));
            distance.setBackground(distanceBG);
            distance.setPadding(dp(16), dp(6), dp(16), dp(6));*/

            TextView distanceText = new TextView(requireContext());
            String distanceStatus = "5.9 كم"; // متغيرك هنا

            distanceText.setText(distanceStatus);
            distanceText.setTextColor(Color.parseColor("#4B463D"));
            distanceText.setTextSize(dp(8));
            distanceText.setTranslationY(-dpf(1.5f));
            distanceText.setTypeface(ThemeManager.fontBold());
            distance.addView(distanceText);
            distanceLayout.addView(distance);

            clubLayout.addView(userImage);
            clubLayout.addView(leftLine);
            clubLayout.addView(distanceLayout);
            clubLayout.addView(rightLine);
            clubLayout.addView(clubImage);

            mapDetailsLayout.addView(clubLayout);

            // MapView
            MapInitOptions options = new MapInitOptions(requireContext());
            mapView = new MapView(requireContext(), options);


            // تعطيل كل الplugins قبل أي render لتفادي flicker
            mapView.getMapboxMap().loadStyleUri(Style.MAPBOX_STREETS, style -> {
                LogoPlugin logo = mapView.getPlugin(Plugin.MAPBOX_LOGO_PLUGIN_ID);
                if (logo != null) logo.setEnabled(false);

                AttributionPlugin attribution = mapView.getPlugin(Plugin.MAPBOX_ATTRIBUTION_PLUGIN_ID);
                if (attribution != null) attribution.setEnabled(false);

                ScaleBarPlugin scaleBar = mapView.getPlugin(Plugin.MAPBOX_SCALEBAR_PLUGIN_ID);
                if (scaleBar != null) scaleBar.setEnabled(false);

                GesturesPlugin gestures = mapView.getPlugin(Plugin.MAPBOX_GESTURES_PLUGIN_ID);
                if (gestures != null) {
                    gestures.setScrollEnabled(false);
                    gestures.setRotateEnabled(false);
                    gestures.setPitchEnabled(false);
                }

                mapView.getMapboxMap().setCamera(
                        new CameraOptions.Builder()
                                .center(com.mapbox.geojson.Point.fromLngLat(location.lng, location.lat)) // مثال: القاهرة
                                .zoom(16.0)
                                .build()
                );
            });

            // LayoutParams: height = 0 + weight = 1 → تاخد كل المساحة الباقية
            LinearLayout.LayoutParams mapParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(128)
            );
            mapView.setLayoutParams(mapParams);
            GradientDrawable bgMap = new GradientDrawable();
            bgMap.setCornerRadius(dp(8));
            mapView.setClipToOutline(true);
            mapView.setBackground(bgMap);

            LinearLayout directionBtn = new LinearLayout(requireContext());
            directionBtn.setBackgroundColor(Color.TRANSPARENT);
            directionBtn.setElevation(0);

            // تخلي المحتوى في المنتصف
            directionBtn.setGravity(Gravity.CENTER);

            LinearLayout.LayoutParams directionBtnParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(40) // ارتفاع ثابت
            );
            directionBtnParams.topMargin = dp(8);
            directionBtn.setLayoutParams(directionBtnParams);
            GradientDrawable directionBtnBG = new GradientDrawable();
            directionBtnBG.setColor(Color.parseColor("#C0BBB3"));
            directionBtnBG.setCornerRadius(dp(8));
            directionBtn.setBackground(directionBtnBG);



            TextView directionBtnText = new TextView(requireContext());
            directionBtnText.setText("الإتجاهات");
            directionBtnText.setAllCaps(false);
            directionBtnText.setTranslationY(-dpf(1f));
            directionBtnText.setTypeface(ThemeManager.fontBold());
            directionBtnText.setTextColor(Color.parseColor("#4B463D"));
            directionBtnText.setTextSize(14);
            directionBtnText.setGravity(Gravity.CENTER);
            directionBtn.addView(directionBtnText, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT
            ));

            directionBtn.setOnClickListener(v -> {
                double lat = location.lat; // إحداثيات الملعب
                double lng = location.lng;
                String label = "الملعب"; // اسم المكان يظهر في الخرائط

                // صيغة URI للخرائط
                String uri = "geo:" + lat + "," + lng + "?q=" + lat + "," + lng + "(" + Uri.encode(label) + ")";
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                intent.setPackage("com.google.android.apps.maps"); // يفتح Google Maps مباشرة
                try {
                    startActivity(intent);
                } catch (Exception e) {
                    // لو Google Maps مش متثبت، افتح أي تطبيق خرائط متاح
                    intent.setPackage(null);
                    startActivity(intent);
                }
            });

            mapLayout.addView(mapView);
            mapLayout.addView(mapDetailsLayout);
            mapLayout.addView(directionBtn);

            return mapLayout;
        }

        @NonNull
        private LinearLayout createWeekTable(Map<String, String> schedule) {
            LinearLayout table = new LinearLayout(requireContext());
            table.setOrientation(LinearLayout.VERTICAL);
            table.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            ));

            // أيام الأسبوع
            String[] days = {"الأحد", "الإثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة", "السبت"};

            for (String day : days) {
                LinearLayout row = new LinearLayout(requireContext());
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                ));
                row.setPadding(0, dp(4), 0, dp(4));

                TextView dayText = new TextView(requireContext());
                dayText.setText(day);
                dayText.setTextColor(Color.parseColor("#4B463D"));
                dayText.setTypeface(ThemeManager.fontBold());
                LinearLayout.LayoutParams dayParams = new LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f);
                dayText.setLayoutParams(dayParams);

                TextView timeText = new TextView(requireContext());
                String time = schedule.containsKey(day) ? schedule.get(day) : "مغلق";
                timeText.setText(time);
                timeText.setTextColor(Color.parseColor(schedule.containsKey(day) ? "#4B463D" : "#804B463D"));
                timeText.setTypeface(ThemeManager.fontSemiBold());

                row.addView(dayText);
                row.addView(timeText);
                table.addView(row);
            }

            return table;
        }
        private int dp(int value) {
            return (int) (value * getContext().getResources().getDisplayMetrics().density);
        }

        float dpf(float value) {
            return TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP, value, getResources().getDisplayMetrics()
            );
        }

        @Override
        public void onStart() {
            super.onStart();
            mapView.onStart(); // هنا mapView الخاص بالـ Fragment
        }

        @Override
        public void onStop() {
            super.onStop();
            mapView.onStop();
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            mapView.onDestroy();
        }

    }

    // ⭐ آراء
    public static class ReviewsFragment extends Fragment {
        private static final String ARG_REVIEWS = "arg_reviews";
        private List<Map<String, Object>> reviews;

        public ReviewsFragment() { }

        public static ReviewsFragment newInstance(ArrayList<Map<String, Object>> reviewsList) {
            ReviewsFragment fragment = new ReviewsFragment(); // constructor فارغ
            Bundle args = new Bundle();
            args.putSerializable("arg_reviews", reviewsList);
            fragment.setArguments(args);
            return fragment;
        }



        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            if (getArguments() != null) {
                reviews = (ArrayList<Map<String, Object>>) getArguments().getSerializable("arg_reviews");
            }
        }

        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            ScrollView scroll = new ScrollView(requireContext());
            scroll.setFillViewport(true);
            if (getParentFragment() instanceof FieldMainFragment) {
                ((FieldMainFragment) getParentFragment()).attachHeaderTouchHandler(scroll);
            }

            LinearLayout layout = new LinearLayout(getContext());
            layout.setOrientation(LinearLayout.VERTICAL);
            layout.setPadding(dp(16), dp(16), dp(16), dp(16));
            scroll.addView(layout);

            if (reviews == null || reviews.isEmpty()) {
                TextView empty = new TextView(getContext());
                empty.setText("لا توجد آراء بعد 👀");
                layout.addView(empty);
            } else {
                for (Map<String, Object> r : reviews) {
                    String username = (String) r.get("username");
                    String comment = (String) r.get("comment");
                    double rate = r.get("rating") != null ? (double) r.get("rating") : 0;

                    LinearLayout item = new LinearLayout(getContext());
                    item.setOrientation(LinearLayout.VERTICAL);
                    item.setPadding(0, dp(6), 0, dp(6));

                    TextView head = new TextView(getContext());
                    head.setText("⭐ " + rate + " - " + username);
                    head.setTextColor(Color.BLACK);
                    head.setTypeface(null, android.graphics.Typeface.BOLD);

                    TextView body = new TextView(getContext());
                    body.setText(comment);
                    body.setTextColor(Color.parseColor("#555555"));

                    item.addView(head);
                    item.addView(body);
                    layout.addView(item);
                }
            }
            return scroll;
        }

        private int dp(int value) {
            return (int) (value * getContext().getResources().getDisplayMetrics().density);
        }
    }

}
