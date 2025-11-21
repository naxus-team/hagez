package com.hagzy.fragments;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.hagzy.FieldActivity;
import com.hagzy.helpers.ThemeManager;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class TournamentFragment extends Fragment {

    private final String[] titles = {"الملغاة", "السابقة", "الموصى بها"};
    private LinearLayout root;

    @Nullable
    @Override
    public View onCreateView(@NonNull android.view.LayoutInflater inflater,
                             @Nullable android.view.ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        root = new LinearLayout(getContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.WHITE);
        root.setPadding(dp(8), dp(8), dp(8), dp(8));

        root.addView(createCurrentSegment());
//        for (String title : titles) root.addView(createSegment(title));

        return root;
    }

    private String formatFriendlyDate(Context context, Date date) {
        // الوقت الحالي
        Calendar now = Calendar.getInstance();
        Calendar target = Calendar.getInstance();
        target.setTime(date);

        // نحسب الفرق بالأيام
        long diffMillis = now.getTimeInMillis() - target.getTimeInMillis();
        long diffDays = diffMillis / (1000 * 60 * 60 * 24);

        // تنسيقات للوقت
        SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.forLanguageTag("ar"));
        SimpleDateFormat dateFormat = new SimpleDateFormat("d MMMM yyyy", Locale.forLanguageTag("ar"));
        String timeText = timeFormat.format(date);

        // المنطق
        if (diffDays == 0) {
            return "اليوم الساعة " + timeText;
        } else if (diffDays == 1) {
            return "أمس الساعة " + timeText;
        } else if (diffDays < 7) {
            // عرض اليوم فقط (مثلاً الثلاثاء الساعة 4:00 م)
            SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE", Locale.forLanguageTag("ar"));
            String dayName = dayFormat.format(date);
            return dayName + " الساعة " + timeText;
        } else {
            // تاريخ كامل لو قديم
            return dateFormat.format(date) + " الساعة " + timeText;
        }
    }

    private View createCurrentSegment() {
        Context context = getContext();

        LinearLayout segment = new LinearLayout(context);
        segment.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams segParams = new LinearLayout.LayoutParams(
                MATCH_PARENT, WRAP_CONTENT);
        segment.setLayoutParams(segParams);

        GradientDrawable background = new GradientDrawable();
        background.setColor(Color.WHITE);
        background.setCornerRadius(dp(6));
        segment.setBackground(background);

        LinearLayout content = new LinearLayout(context);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setGravity(Gravity.CENTER);

        boolean hasBookings = true;

        if (!hasBookings) {
            TextView emptyText = new TextView(context);
            emptyText.setText("لا يوجد حجز بعد");
            emptyText.setTypeface(ThemeManager.fontBold());
            emptyText.setTextColor(ThemeManager.textSecondary());
            emptyText.setTextSize(16);
            emptyText.setGravity(Gravity.CENTER);

            LinearLayout actionButton = new LinearLayout(context);
            actionButton.setOrientation(LinearLayout.HORIZONTAL);
            actionButton.setGravity(Gravity.CENTER);
            actionButton.setPadding(dp(16), 0, dp(16), 0);

            GradientDrawable btnBg = new GradientDrawable();
            btnBg.setCornerRadius(dp(4));
            btnBg.setColor(ThemeManager.black());
            actionButton.setBackground(btnBg);

            LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                    MATCH_PARENT,
                    dp(48)
            );
            btnParams.topMargin = dp(12);
            btnParams.bottomMargin = dp(8);
            btnParams.gravity = Gravity.CENTER;
            actionButton.setLayoutParams(btnParams);

            TextView btnText = new TextView(context);
            btnText.setText("ابدأ الحجز الآن");
            btnText.setTextSize(14);
            btnText.setTextColor(Color.WHITE);
            btnText.setTypeface(ThemeManager.fontBold());
            btnText.setGravity(Gravity.CENTER);

            actionButton.addView(btnText);

            actionButton.setOnClickListener(v -> {
                FirebaseFirestore db = FirebaseFirestore.getInstance();
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

                if (user != null) {
                    String uid = user.getUid();

                    Map<String, Object> updates = new HashMap<>();
                    updates.put("name", "Abdelrahman Khadr");
                    updates.put("email", user.getEmail());

                    db.collection("users").document(uid)
                            .set(updates) // أو .update() لو المستند موجود مسبقًا
                            .addOnSuccessListener(aVoid ->
                                    Toast.makeText(getContext(), "تم تحديث البيانات ✅", Toast.LENGTH_SHORT).show()
                            )
                            .addOnFailureListener(e ->
                                    Toast.makeText(getContext(), "فشل التحديث ❌: " + e.getMessage(), Toast.LENGTH_LONG).show()
                            );
                }

            });

            content.addView(emptyText);
            content.addView(actionButton);

        } else {
            FirebaseFirestore db = FirebaseFirestore.getInstance();

// 🔹 دالة لإنشاء كارت شبح (placeholder)
            Runnable addGhostCards = () -> {
                int ghostCount = 3;

                for (int i = 0; i < ghostCount; i++) {
                    CardView ghostCard = new CardView(context);
                    GradientDrawable bg = new GradientDrawable();
                    bg.setCornerRadius(dp(12));
                    bg.setColor(Color.parseColor("#EEEEEE"));
                    ghostCard.setBackground(bg);
                    ghostCard.setCardBackgroundColor(Color.parseColor("#EEEEEE"));
                    ghostCard.setUseCompatPadding(true);
                    ghostCard.setContentPadding(dp(16), dp(16), dp(16), dp(16));

                    LinearLayout ghostColumn = new LinearLayout(context);
                    ghostColumn.setOrientation(LinearLayout.VERTICAL);
                    ghostColumn.setGravity(Gravity.CENTER_VERTICAL);

                    // 🩶 عناصر السكليتون (العنوان + الوصف)
                    int[][] ghostSpecs = {
                            {dp(18), Color.parseColor("#DDDDDD")},
                            {dp(14), Color.parseColor("#E0E0E0")}
                    };

                    for (int[] spec : ghostSpecs) {
                        View ghostLine = new View(context);
                        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(MATCH_PARENT, spec[0]);
                        p.setMargins(0, dp(6), 0, dp(6));
                        ghostLine.setLayoutParams(p);
                        ghostLine.setBackgroundColor(spec[1]);
                        ghostColumn.addView(ghostLine);
                    }

                    ghostCard.addView(ghostColumn);
                    content.addView(ghostCard);

                    // 💡 تدرج الشفافية (ثابتة)
                    float[] alphaLevels = {0.5f, 0.25f, 0.1f};
                    ghostCard.setAlpha(alphaLevels[Math.min(i, alphaLevels.length - 1)]);

                    // 🪄 أنيميشن شفافية متكررة
                    ObjectAnimator fadeAnim = ObjectAnimator.ofFloat(ghostCard, "alpha",
                     ghostCard.getAlpha(), ghostCard.getAlpha() + 0.15f, ghostCard.getAlpha());
                    fadeAnim.setDuration(1200);
                    fadeAnim.setRepeatCount(ValueAnimator.INFINITE);
                    fadeAnim.setRepeatMode(ValueAnimator.REVERSE);
                    fadeAnim.setStartDelay(i * 200); // تأخير بسيط بين كل كارد
                    fadeAnim.start();

                    // ⚙️ الهامش بين الكروت
                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
                    params.setMargins(0, dp(8), 0, 0);
                    ghostCard.setLayoutParams(params);
                }


            };

// 🔹 أضف الكروت المؤقتة أولًا
            addGhostCards.run();
            db.collection("fields")
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        content.removeAllViews(); // إزالة أي عناصر سابقة

                        for (DocumentSnapshot doc : querySnapshot.getDocuments()) {

                            // 🧱 البيانات الأساسية
                            Boolean available = doc.getBoolean("available");
                            String description = doc.getString("description");
                            Timestamp createdAt = doc.getTimestamp("createdAt");

                            Map<String, Object> meta = (Map<String, Object>) doc.get("meta");
                            String name = meta != null ? (String) meta.get("name") : "ملعب غير معروف";
                            String fieldType = meta != null ? (String) meta.get("fieldType") : "";
                            String type = meta != null ? (String) meta.get("type") : "";
                            Double price = meta != null && meta.get("price") != null ? ((Number) meta.get("price")).doubleValue() : 0;
                            Double rating = meta != null && meta.get("rating") != null ? ((Number) meta.get("rating")).doubleValue() : 0;

                            Date createdDate = createdAt != null ? createdAt.toDate() : new Date();
                            String friendlyDate = formatFriendlyDate(context, createdDate);

                            // 🪶 الكارت الخارجي
                            CardView card = new CardView(context);
                            GradientDrawable cardBg = new GradientDrawable();
                            cardBg.setStroke(dp(2), Color.parseColor("#f6f6f6"));
                            cardBg.setCornerRadius(dp(12));
                            card.setBackground(cardBg);
                            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                                    MATCH_PARENT,
                                    WRAP_CONTENT
                            );
                            cardParams.bottomMargin = (dp(8));
                            card.setLayoutParams(cardParams);
                            card.setCardBackgroundColor(Color.WHITE);
                            card.setUseCompatPadding(true);
                            card.setContentPadding(dp(12), dp(12), dp(12), dp(12));

                            // 🔹 صف أفقي: صورة على اليسار + تفاصيل على اليمين
                            LinearLayout row = new LinearLayout(context);
                            row.setOrientation(LinearLayout.HORIZONTAL);
                            row.setGravity(Gravity.CENTER_VERTICAL);

                            // 🖼️ حاوية الصورة
                            ImageView imageView = new ImageView(context);
                            int imageWidth = dp(86);
                            int imageHeight = dp(128);

                            LinearLayout.LayoutParams imageParams = new LinearLayout.LayoutParams(imageWidth, imageHeight);
                            imageParams.setMarginEnd(dp(12));
                            imageView.setLayoutParams(imageParams);
                            GradientDrawable placeholderBG = new GradientDrawable();
                            placeholderBG.setShape(GradientDrawable.RECTANGLE);
                            placeholderBG.setCornerRadius(dp(4));
                            placeholderBG.setColor(Color.parseColor("#F0F0F0"));
                            imageView.setBackground(placeholderBG);
                            imageView.setClipToOutline(true);
                            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                            row.addView(imageView);

                            // 📄 عمود النصوص
                            LinearLayout column = new LinearLayout(context);
                            column.setOrientation(LinearLayout.VERTICAL);
                            column.setLayoutParams(new LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f));

                            // 🏟️ الاسم
                            TextView fieldName = new TextView(context);
                            fieldName.setText(name);
                            fieldName.setTextSize(16);
                            fieldName.setTextColor(Color.BLACK);
                            fieldName.setTypeface(ThemeManager.fontBold());
                            fieldName.setOnClickListener(v -> {
                                Intent i = new Intent(context, FieldActivity.class);
                                i.putExtra("fieldId", doc.getId());
                                context.startActivity(i);
                            });
                            column.addView(fieldName);

                            // 🗂️ التفاصيل (category - type - fieldType)
                            TextView metaText = new TextView(context);
                            String metaInfo = String.format(Locale.getDefault(),
                                    "%s · %s",
                                    fieldType != null ? fieldType : "",
                                    type != null ? type : "");
                            metaText.setText(metaInfo);
                            metaText.setTextColor(Color.parseColor("#777777"));
                            metaText.setTextSize(14);
                            metaText.setTypeface(ThemeManager.fontRegular());
                            metaText.setPadding(0, dp(2), 0, dp(4));
                            column.addView(metaText);

                            // 💰 السعر + الحالة (available)
                            LinearLayout statusLayout = new LinearLayout(context);
                            statusLayout.setOrientation(LinearLayout.HORIZONTAL);
                            statusLayout.setGravity(Gravity.CENTER_VERTICAL);
                            statusLayout.setWeightSum(2f);

                            TextView priceText = new TextView(context);
                            priceText.setText(String.format(Locale.getDefault(), "%.0f ج.م", price));
                            priceText.setTextColor(Color.parseColor("#000000"));
                            priceText.setTextSize(14);
                            priceText.setTypeface(ThemeManager.fontMedium());
                            LinearLayout.LayoutParams priceParams = new LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f);
                            priceText.setLayoutParams(priceParams);
                            statusLayout.addView(priceText);

                            // ⚪ الحالة
                            LinearLayout statusRight = new LinearLayout(context);
                            statusRight.setOrientation(LinearLayout.HORIZONTAL);
                            statusRight.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
                            statusRight.setLayoutParams(new LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f));

                            View statusIndicator = new View(context);
                            LinearLayout.LayoutParams indicatorParams = new LinearLayout.LayoutParams(dp(10), dp(10));
                            indicatorParams.setMarginStart(dp(6));
                            statusIndicator.setLayoutParams(indicatorParams);
                            GradientDrawable indicatorBg = new GradientDrawable();
                            indicatorBg.setShape(GradientDrawable.RECTANGLE);
                            indicatorBg.setCornerRadius(dp(2));

                            TextView availableText = new TextView(context);
                            availableText.setTextColor(Color.parseColor("#777777"));
                            availableText.setTextSize(13);
                            availableText.setTypeface(ThemeManager.fontMedium());

                            int color;
                            if (available != null && available) {
                                color = Color.parseColor("#65e030");
                                availableText.setText("متاح");
                            } else {
                                color = Color.parseColor("#F44336");
                                availableText.setText("غير متاح");
                            }

                            indicatorBg.setColor(color);
                            statusIndicator.setBackground(indicatorBg);

                            statusRight.addView(availableText);
                            statusRight.addView(statusIndicator);
                            statusLayout.addView(statusRight);
                            column.addView(statusLayout);

                            // ⭐ التقييم
                            TextView ratingView = new TextView(context);
                            ratingView.setText(String.format(Locale.getDefault(), "★ %.1f", rating));
                            ratingView.setTextColor(Color.parseColor("#FFA000"));
                            ratingView.setTypeface(ThemeManager.fontMedium());
                            ratingView.setTextSize(13);
                            ratingView.setPadding(0, dp(4), 0, 0);
                            column.addView(ratingView);

                            // 🧩 إضافة العمود للصف
                            row.addView(column);
                            card.addView(row);
                            content.addView(card);

                            // 🖼️ تحميل الصورة من subcollection "images"
                            db.collection("fields")
                                    .document(doc.getId())
                                    .collection("images")
                                    .limit(1)
                                    .get()
                                    .addOnSuccessListener(imagesSnapshot -> {
                                        if (!imagesSnapshot.isEmpty()) {
                                            String imageUrl = imagesSnapshot.getDocuments().get(0).getString("url");
                                            if (imageUrl != null && !imageUrl.isEmpty()) {
                                                imageUrl = imageUrl.replace("\"", "");

                                                Glide.with(context)
                                                        .load(imageUrl)
                                                        .into(imageView);
                                                Log.e("FIELDS", "the Image: "+ imageUrl);

                                            }
                                        }
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e("FIELDS", "فشل تحميل الصورة", e);
                                    });
                        }
                    })
                    .addOnFailureListener(e -> {
                        content.removeAllViews();
                        Toast.makeText(context, "حدث خطأ أثناء تحميل البيانات", Toast.LENGTH_SHORT).show();
                    });

        }


        segment.addView(content);

        return segment;
    }

    private View createSegment(String title) {
        Context context = getContext();

        LinearLayout segment = new LinearLayout(context);
        segment.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams segParams = new LinearLayout.LayoutParams(
                MATCH_PARENT, WRAP_CONTENT);
        segParams.setMargins(0, dp(8), 0, dp(8));
        segment.setLayoutParams(segParams);

        GradientDrawable background = new GradientDrawable();
        background.setColor(Color.WHITE);
        background.setCornerRadius(dp(4));
        background.setStroke(dp(2), ThemeManager.divider());
        segment.setBackground(background);

        TextView header = new TextView(context);
        header.setText(title);
        header.setTextColor(Color.BLACK);
        header.setTextSize(14);
        header.setTypeface(ThemeManager.fontSemiBold());
        header.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
        header.setPadding(dp(8), dp(8), dp(8), dp(8));

        LinearLayout content = new LinearLayout(context);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(8), dp(8), dp(8), dp(8));
        content.setVisibility(View.GONE);

        TextView details = new TextView(context);
        details.setText("تفاصيل " + title);
        details.setTextColor(Color.parseColor("#555555"));
        details.setTextSize(14);
        content.addView(details);

        header.setOnClickListener(v -> {
            boolean isOpen = content.getVisibility() == View.VISIBLE;
            content.setVisibility(isOpen ? View.GONE : View.VISIBLE);
        });

        segment.addView(header);
        segment.addView(content);

        return segment;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }
}
