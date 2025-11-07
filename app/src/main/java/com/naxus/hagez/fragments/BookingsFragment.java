package com.naxus.hagez.fragments;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.naxus.hagez.QrCodeActivity;
import com.naxus.hagez.R;
import com.naxus.hagez.helpers.ThemeManager;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class BookingsFragment extends Fragment {

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

// 🔹 حمل البيانات من Firestore
            db.collection("bookings")
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        content.removeAllViews(); // إزالة الـ Ghost cards أولًا

                        for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                            CardView card = new CardView(context);
                            GradientDrawable cardBg = new GradientDrawable();
                            cardBg.setStroke(dp(2), Color.parseColor("#f6f6f6"));
                            cardBg.setCornerRadius(dp(12));
                            card.setBackground(cardBg);
                            card.setCardBackgroundColor(Color.WHITE);
                            card.setUseCompatPadding(true);
                            card.setContentPadding(dp(16), dp(16), dp(16), dp(16));

                            LinearLayout column = new LinearLayout(context);
                            column.setOrientation(LinearLayout.VERTICAL);

                            String name = doc.getString("name");
                            String category = doc.getString("category");
                            String statusText = doc.getString("status");
                            double totalPrice = doc.getDouble("totalPrice") != null ? doc.getDouble("totalPrice") : 0;
                            Timestamp createdAt = doc.getTimestamp("createdAt");
                            Date createdDate = createdAt != null ? createdAt.toDate() : new Date();

                            String friendlyDate = formatFriendlyDate(context, createdDate);

                            LinearLayout statusLayout = new LinearLayout(context);
                            statusLayout.setOrientation(LinearLayout.HORIZONTAL);
                            statusLayout.setGravity(Gravity.CENTER_VERTICAL);
                            statusLayout.setWeightSum(2f);

                            TextView price = new TextView(context);
                            price.setText(String.format(Locale.getDefault(), "%.0f ج.م", totalPrice));
                            price.setTextColor(Color.parseColor("#777777"));
                            price.setTextSize(14);
                            price.setTypeface(ThemeManager.fontMedium());
                            LinearLayout.LayoutParams priceParams = new LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f);
                            price.setLayoutParams(priceParams);

                            LinearLayout bookingStatusLayout = new LinearLayout(context);
                            bookingStatusLayout.setOrientation(LinearLayout.HORIZONTAL);
                            bookingStatusLayout.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
                            bookingStatusLayout.setLayoutParams(new LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f));

                            View statusIndicator = new View(context);
                            LinearLayout.LayoutParams indicatorParams = new LinearLayout.LayoutParams(dp(12), dp(12));
                            indicatorParams.setMarginStart(dp(8));
                            statusIndicator.setLayoutParams(indicatorParams);
                            GradientDrawable indicatorBg = new GradientDrawable();
                            indicatorBg.setShape(GradientDrawable.RECTANGLE);
                            indicatorBg.setCornerRadius(dp(2));

                            TextView status = new TextView(context);
                            status.setTextColor(Color.parseColor("#777777"));
                            status.setTextSize(14);
                            status.setTypeface(ThemeManager.fontMedium());

                            int color;
                            switch (statusText == null ? "" : statusText) {
                                case "confirmed": color = Color.parseColor("#65e030"); status.setText("نشط"); break;
                                case "cancelled": color = Color.parseColor("#F44336"); status.setText("ملغي"); break;
                                default: color = Color.parseColor("#FFA000"); status.setText("قيد الانتظار"); break;
                            }
                            indicatorBg.setColor(color);
                            statusIndicator.setBackground(indicatorBg);

                            bookingStatusLayout.addView(status);
                            bookingStatusLayout.addView(statusIndicator);
                            statusLayout.addView(price);
                            statusLayout.addView(bookingStatusLayout);

                            TextView serviceName = new TextView(context);
                            serviceName.setText(name != null ? name : "بدون اسم");
                            serviceName.setTextSize(16);
                            serviceName.setTextColor(Color.parseColor("#000000"));
                            serviceName.setTypeface(ThemeManager.fontBold());
                            serviceName.setOnClickListener(v -> {
                                Intent intent = new Intent(context, QrCodeActivity.class);
                                intent.putExtra("serviceId", doc.getId());
                                context.startActivity(intent);
                            });

                            TextView meta = new TextView(context);
                            String[] parts = {friendlyDate, category};
                            String metaText = TextUtils.join(" · ",
                                    Arrays.stream(parts)
                                            .filter(s -> s != null && !s.isEmpty())
                                            .toArray(String[]::new)
                            );
                            meta.setText(metaText);
                            meta.setTextColor(Color.parseColor("#777777"));
                            meta.setTypeface(ThemeManager.fontRegular());
                            meta.setTextSize(14);

                            LinearLayout actionButton = new LinearLayout(context);
                            actionButton.setOrientation(LinearLayout.HORIZONTAL);
                            actionButton.setGravity(Gravity.CENTER);
                            actionButton.setBackground(new GradientDrawable() {{
                                setCornerRadius(dp(6));
                                setColor(Color.BLACK);
                            }});
                            actionButton.setLayoutParams(new LinearLayout.LayoutParams(MATCH_PARENT, dp(48)));
                            actionButton.setOnClickListener(v -> {
                                Intent qrIntent = new Intent(context, QrCodeActivity.class);
                                context.startActivity(qrIntent);
                            });

                            ImageView icon = new ImageView(context);
                            icon.setLayoutParams(new LinearLayout.LayoutParams(dp(24), dp(24)));
                            icon.setColorFilter(Color.WHITE);
                            icon.setImageResource(R.drawable.qrcode_reader);
                            actionButton.addView(icon);

                            column.addView(statusLayout);
                            column.addView(serviceName);
                            column.addView(meta);
                            column.addView(actionButton);
                            card.addView(column);
                            content.addView(card);
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
