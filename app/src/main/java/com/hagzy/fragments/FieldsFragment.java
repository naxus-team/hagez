package com.hagzy.fragments;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
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
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.hagzy.FieldActivity;
import com.hagzy.helpers.ThemeManager;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

public class FieldsFragment extends Fragment {

    private FusedLocationProviderClient fusedLocationClient;
    private double userLat = 0.0;
    private double userLng = 0.0;

    @SuppressLint("MissingPermission")
    @Nullable
    @Override
    public View onCreateView(@NonNull android.view.LayoutInflater inflater,
                             @Nullable android.view.ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        Context context = getContext();

        // ScrollView رئيسي
        ScrollView scrollView = new ScrollView(context);
        scrollView.setFillViewport(true);

        // Layout عمودي للكروت
        LinearLayout mainLayout = new LinearLayout(context);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setPadding(24, 24, 24, 24);
        mainLayout.setBackgroundColor(Color.WHITE);

        scrollView.addView(mainLayout);
        if (androidx.core.content.ContextCompat.checkSelfPermission(requireContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                != android.content.pm.PackageManager.PERMISSION_GRANTED) {

            requestPermissions(
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    1001 // كود مميز للطلب
            );
            return scrollView; // نوقف التنفيذ لحد ما المستخدم يوافق
        }

        // 🔹 Firebase & Location clients
        FirebaseFirestore db = FirebaseFirestore.getInstance();


//
//        String fieldId = "field_001"; // ID الملعب اللي بتحجز له
//
//        Map<String, Object> booking = new HashMap<>();
//        booking.put("userId", "UID_123");
//        booking.put("username", "Ahmed");
//        booking.put("time", "2025-11-10 18:00");
//        booking.put("duration", "2h");
//        booking.put("status", "confirmed");
//        booking.put("createdAt", com.google.firebase.Timestamp.now());
//
//// أهو الكود اللي بيعمل subcollection اسمها "bookings"
//        db.collection("fields")
//                .document(fieldId)
//                .collection("bookings")   // <--- دي الـ subcollection
//                .add(booking)
//                .addOnSuccessListener(docRef -> {
//                    Log.d("BOOKING", "تمت إضافة الحجز بنجاح برقم: " + docRef.getId());
//                })
//                .addOnFailureListener(e -> {
//                    Log.e("BOOKING", "فشل في إضافة الحجز", e);
//                });
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext());
        // 🔹 الحصول على موقع المستخدم الحالي أولًا
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(loc -> {
                    if (loc != null) {
                        userLat = loc.getLatitude();
                        userLng = loc.getLongitude();
                    }

                    // بعد ما نحصل على موقع المستخدم نقرأ البيانات
                    loadServices(context, db, mainLayout);
                })
                .addOnFailureListener(e -> {
                    Log.e("LOCATION", "فشل في تحديد الموقع: " + e.getMessage());
                    loadServices(context, db, mainLayout); // نكمل حتى لو فشل الموقع
                });
        return scrollView;
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

    private void loadServices(Context context,FirebaseFirestore db, LinearLayout mainLayout) {
// 🔹 دالة لإنشاء كارت شبح (placeholder)
        Runnable addGhostCards = () -> {
            int ghostCount = 3;

            for (int i = 0; i < ghostCount; i++) {
                CardView ghostCard = new CardView(context);
                GradientDrawable bg = new GradientDrawable();
                bg.setCornerRadius(dp(12));
                bg.setColor(Color.parseColor("#EFEDE9"));
                ghostCard.setBackground(bg);
                ghostCard.setCardBackgroundColor(Color.parseColor("#C0BBB3"));
                ghostCard.setUseCompatPadding(true);
                ghostCard.setContentPadding(dp(16), dp(16), dp(16), dp(16));

                LinearLayout ghostColumn = new LinearLayout(context);
                ghostColumn.setOrientation(LinearLayout.VERTICAL);
                ghostColumn.setGravity(Gravity.CENTER_VERTICAL);

                // 🩶 عناصر السكليتون (العنوان + الوصف)
                int[][] ghostSpecs = {
                        {dp(18), Color.parseColor("#C0BBB3")},
                        {dp(14), Color.parseColor("#EFEDE9")}
                };

                for (int[] spec : ghostSpecs) {
                    View ghostLine = new View(context);
                    LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(MATCH_PARENT, spec[0]);
                    p.setMargins(0, dp(4), 0, dp(4));
                    ghostLine.setLayoutParams(p);
                    ghostLine.setBackgroundColor(spec[1]);
                    ghostColumn.addView(ghostLine);
                }

                ghostCard.addView(ghostColumn);
                mainLayout.addView(ghostCard);

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
                params.setMargins(0, dp(6), 0, 0);
                ghostCard.setLayoutParams(params);
            }


        };

// 🔹 أضف الكروت المؤقتة أولًا
        addGhostCards.run();
        db.collection("fields")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    mainLayout.removeAllViews(); // إزالة أي عناصر سابقة

                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {

                        Boolean available = doc.getBoolean("available");
                        if (available == null || !available) continue; // ✅ عرض المتاح فقط

                        // 🧱 البيانات الأساسية
                        String description = doc.getString("description");
                        Timestamp createdAt = doc.getTimestamp("createdAt");

                        Map<String, Object> meta = (Map<String, Object>) doc.get("meta");
                        Map<String, Object> location = (Map<String, Object>) doc.get("location");

                        String name = meta != null ? (String) meta.get("name") : "ملعب غير معروف";
                        String fieldType = meta != null ? (String) meta.get("fieldType") : "";
                        String type = meta != null ? (String) meta.get("type") : "";
                        Double price = meta != null && meta.get("price") != null ? ((Number) meta.get("price")).doubleValue() : 0;
                        Double rating = meta != null && meta.get("rating") != null ? ((Number) meta.get("rating")).doubleValue() : 0;

                        double fieldLat = 0.0, fieldLng = 0.0;
                        if (location != null) {
                            fieldLat = location.get("lat") != null ? ((Number) location.get("lat")).doubleValue() : 0.0;
                            fieldLng = location.get("lng") != null ? ((Number) location.get("lng")).doubleValue() : 0.0;
                        }

                        double distanceKm = calculateDistance(userLat, userLng, fieldLat, fieldLng);
                        String distanceText = distanceKm > 0 ? String.format(Locale.getDefault(), "%.1f كم", distanceKm) : "";

                        Date createdDate = createdAt != null ? createdAt.toDate() : new Date();
                        String friendlyDate = formatFriendlyDate(context, createdDate);

                        // 🪶 الكارت الخارجي
                        CardView card = new CardView(context);
                        GradientDrawable cardBg = new GradientDrawable();
                        cardBg.setStroke(dp(2), Color.parseColor("#EFEDE9"));
                        cardBg.setCornerRadius(dp(16));
                        card.setBackground(cardBg);
                        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
                        cardParams.bottomMargin = dp(8);
                        card.setLayoutParams(cardParams);
                        card.setCardBackgroundColor(Color.WHITE);
                        card.setUseCompatPadding(true);
                        card.setContentPadding(dp(12), dp(6), dp(12), dp(12));

                        // 🔹 صف أفقي: صورة على اليسار + تفاصيل على اليمين
                        LinearLayout row = new LinearLayout(context);
                        row.setOrientation(LinearLayout.HORIZONTAL);
                        row.setGravity(Gravity.CENTER_VERTICAL);

                        // 🖼️ الصورة
                        ImageView imageView = new ImageView(context);
                        int imageWidth = dp(72);
                        int imageHeight = dp(72);
                        LinearLayout.LayoutParams imageParams = new LinearLayout.LayoutParams(imageWidth, imageHeight);
                        imageParams.setMarginEnd(dp(12));
                        imageView.setLayoutParams(imageParams);
                        GradientDrawable placeholderBG = new GradientDrawable();
                        placeholderBG.setShape(GradientDrawable.RECTANGLE);
                        placeholderBG.setCornerRadius(dp(72));
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
                        fieldName.setTextSize(18);
                        fieldName.setTextColor(Color.parseColor("#4B463D"));
                        fieldName.setTypeface(ThemeManager.fontBold());
                        column.addView(fieldName);

             /*           // 🗂️ التفاصيل
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
                        column.addView(metaText);*/

                        // 💰 السعر + المسافة
                        LinearLayout infoRow = new LinearLayout(context);
                        infoRow.setOrientation(LinearLayout.HORIZONTAL);
                        infoRow.setGravity(Gravity.CENTER_VERTICAL);
                        infoRow.setWeightSum(2f);

                        TextView priceText = new TextView(context);
                        priceText.setText(String.format(Locale.getDefault(), "%.0f ج.م", price));
                        priceText.setTextColor(Color.BLACK);
                        priceText.setTextSize(14);
                        priceText.setTypeface(ThemeManager.fontMedium());
                        priceText.setLayoutParams(new LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f));
                        infoRow.addView(priceText);

                        LinearLayout distanceInfo = new LinearLayout(context);
                        distanceInfo.setOrientation(LinearLayout.HORIZONTAL);
                        distanceInfo.setGravity(Gravity.CENTER_VERTICAL);

                        double walkingTimeMinutes  = (distanceKm / 5) * 60;
                        TextView distanceWalkView = new TextView(context);
                        distanceWalkView.setText(String.format(Locale.getDefault(), " • %.0f دقيقة مشي", walkingTimeMinutes));
                        distanceWalkView.setTextColor(Color.parseColor("#C0BBB3"));
                        distanceWalkView.setTextSize(16);
                        distanceWalkView.setPadding(dp(8), 0, dp(8), 0);
                        distanceWalkView.setGravity(Gravity.END);
                        distanceWalkView.setTypeface(ThemeManager.fontBold());
                        distanceWalkView.setLayoutParams(new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT));
                        distanceInfo.addView(distanceWalkView);

                        TextView distanceView = new TextView(context);
                        distanceView.setText(distanceText);
                        distanceView.setTextColor(Color.parseColor("#C0BBB3"));
                        distanceView.setTextSize(16);
                        distanceView.setPadding(dp(8), 0, dp(8), 0);
                        distanceView.setGravity(Gravity.END);
                        distanceView.setTypeface(ThemeManager.fontBold());
                        distanceView.setLayoutParams(new LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f));
                        distanceInfo.addView(distanceView);
                        infoRow.addView(distanceInfo);

                        column.addView(infoRow);

                        // ⭐ التقييم
                        TextView ratingView = new TextView(context);
                        ratingView.setText(String.format(Locale.getDefault(), "★ %.1f", rating));
                        ratingView.setTextColor(Color.parseColor("#FFA000"));
                        ratingView.setTypeface(ThemeManager.fontMedium());
                        ratingView.setTextSize(13);
                        ratingView.setPadding(0, dp(4), 0, 0);
                        column.addView(ratingView);

                        // إضافة العمود للصف
                        row.addView(column);
                        card.addView(row);
                        mainLayout.addView(card);

                        card.setOnClickListener(v -> {
                            Intent i = new Intent(context, FieldActivity.class);
                            i.putExtra("fieldId", doc.getId());
                            context.startActivity(i);
                        });

                        // 🖼️ تحميل الصورة
                        int imageIndex = 0; // 0 = أول صورة، 1 = تانية صورة، وهكذا

                        db.collection("fields")
                                .document(doc.getId())
                                .collection("images")
                                .get()
                                .addOnSuccessListener(imagesSnapshot -> {
                                    if (!imagesSnapshot.isEmpty() && imagesSnapshot.size() > imageIndex) {
                                        String imageUrl = imagesSnapshot.getDocuments().get(imageIndex).getString("url");
                                        if (imageUrl != null && !imageUrl.isEmpty()) {
                                            imageUrl = imageUrl.replace("\"", ""); // إزالة أي علامات اقتباس زائدة
                                            Glide.with(context).load(imageUrl).into(imageView);
                                        }
                                    } else {
                                        Log.w("FIELDS", "الصورة المطلوبة غير موجودة أو فارغة");
                                    }
                                })
                                .addOnFailureListener(e -> Log.e("FIELDS", "فشل تحميل الصورة", e));
                    }

                })
                .addOnFailureListener(e -> {
                    mainLayout.removeAllViews();
                    Toast.makeText(context, "حدث خطأ أثناء تحميل البيانات", Toast.LENGTH_SHORT).show();
                });
    }

    // 🔹 دالة حساب المسافة بالكيلومتر

    public static double distanceInKm(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // نصف قطر الأرض بالكيلومتر
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c; // المسافة بالكيلومتر
    }
    public static double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // نصف قطر الأرض بالكيلومتر
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }


    private TextView textView(String text, float size, int color, boolean bold) {
        TextView tv = new TextView(getContext());
        tv.setText(text);
        tv.setTextSize(size);
        tv.setTextColor(color);
        if (bold) tv.setTypeface(tv.getTypeface(), android.graphics.Typeface.BOLD);
        return tv;
    }

    private String safe(String s) {
        return (s != null && !s.isEmpty()) ? s : "-";
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }
}
