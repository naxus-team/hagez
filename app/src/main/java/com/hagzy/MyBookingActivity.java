package com.hagzy;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PorterDuff;
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
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;
import com.hagzy.helpers.ThemeManager;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MyBookingActivity extends AppCompatActivity {

    private static final int QR_SCANNER_REQUEST = 100;

    private DatabaseReference realtimeDB;
    private FirebaseAuth mAuth;
    private LinearLayout mainContainer;
    private ScrollView scrollView;

    private String bookingId;
    private String userId;
    private ValueEventListener bookingListener;

    // Booking data
    private String bookingName;
    private String category;
    private String fieldId;
    private double price;
    private long startTime;
    private long endTime;
    private String userActivationCode;
    private String providerActivationCode;
    private String activationStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ThemeManager.setDarkMode(this, false);

        mAuth = FirebaseAuth.getInstance();
        realtimeDB = FirebaseDatabase.getInstance().getReference();

        bookingId = getIntent().getStringExtra("bookingId");
        userId = getIntent().getStringExtra("userId");

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null && userId == null) {
            userId = currentUser.getUid();
        }

        if (bookingId == null || userId == null) {
            Toast.makeText(this, "خطأ: معلومات الحجز غير كاملة", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        buildUI();
        loadBookingDetails();
    }

    private void buildUI() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.WHITE);
        root.setLayoutParams(new LinearLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT));

        root.addView(createHeader());

        scrollView = new ScrollView(this);
        scrollView.setLayoutParams(new LinearLayout.LayoutParams(MATCH_PARENT, 0, 1f));
        scrollView.setFillViewport(true);

        mainContainer = new LinearLayout(this);
        mainContainer.setOrientation(LinearLayout.VERTICAL);
        mainContainer.setPadding(dp(16), dp(16), dp(16), dp(16));
        mainContainer.setLayoutParams(new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT));

        showLoadingSkeleton();

        scrollView.addView(mainContainer);
        root.addView(scrollView);

        setContentView(root);
    }

    private LinearLayout createHeader() {
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(16), dp(12), dp(16), dp(12));
        header.setBackgroundColor(Color.WHITE);

        ImageView backBtn = new ImageView(this);
        backBtn.setImageResource(R.drawable.chevron_right);
        backBtn.setRotation(180);
        backBtn.setColorFilter(Color.parseColor("#4B463D"), PorterDuff.Mode.SRC_IN);
        LinearLayout.LayoutParams backParams = new LinearLayout.LayoutParams(dp(32), dp(32));
        backParams.setMarginEnd(dp(12));
        backBtn.setLayoutParams(backParams);
        backBtn.setOnClickListener(v -> finish());

        TextView title = new TextView(this);
        title.setText("تفاصيل الحجز");
        title.setTextSize(18);
        title.setTypeface(ThemeManager.fontBold());
        title.setTextColor(Color.parseColor("#4B463D"));
        title.setTranslationY(-dpf(1.5f));

        header.addView(backBtn);
        header.addView(title);

        GradientDrawable headerBg = new GradientDrawable();
        headerBg.setColor(Color.WHITE);
        header.setBackground(headerBg);
        header.setElevation(dp(4));

        return header;
    }

    private void showLoadingSkeleton() {
        mainContainer.removeAllViews();

        for (int i = 0; i < 4; i++) {
            View skeleton = new View(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(MATCH_PARENT, dp(100));
            params.bottomMargin = dp(12);
            skeleton.setLayoutParams(params);

            GradientDrawable bg = new GradientDrawable();
            bg.setCornerRadius(dp(16));
            bg.setColor(Color.parseColor("#F0F0F0"));
            skeleton.setBackground(bg);

            mainContainer.addView(skeleton);
        }
    }

    private void loadBookingDetails() {
        bookingListener = realtimeDB.child("bookings")
                .child(userId)
                .child(bookingId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!snapshot.exists()) {
                            Toast.makeText(MyBookingActivity.this,
                                    "الحجز غير موجود!", Toast.LENGTH_SHORT).show();
                            finish();
                            return;
                        }

                        bookingName = snapshot.child("bookingName").getValue(String.class);
                        category = snapshot.child("category").getValue(String.class);
                        fieldId = snapshot.child("fieldId").getValue(String.class);

                        Double priceValue = snapshot.child("price").getValue(Double.class);
                        price = priceValue != null ? priceValue : 0.0;

                        userActivationCode = snapshot.child("userActivationCode").getValue(String.class);
                        providerActivationCode = snapshot.child("providerActivationCode").getValue(String.class);
                        activationStatus = snapshot.child("activationStatus").getValue(String.class);

                        DataSnapshot durationSnap = snapshot.child("duration").child("startEnd");
                        Long start = durationSnap.child("start").getValue(Long.class);
                        Long end = durationSnap.child("end").getValue(Long.class);
                        startTime = start != null ? start : 0L;
                        endTime = end != null ? end : 0L;

                        displayBookingDetails();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(MyBookingActivity.this,
                                "فشل تحميل تفاصيل الحجز", Toast.LENGTH_SHORT).show();
                        Log.e("MyBookingActivity", "Error loading booking", error.toException());
                    }
                });
    }

    private void displayBookingDetails() {
        mainContainer.removeAllViews();

        mainContainer.addView(createHeroSection());
        mainContainer.addView(createTimeCard());
        mainContainer.addView(createDetailsCard());
        mainContainer.addView(createActivationSection());
        mainContainer.addView(createActionButtons());
        mainContainer.addView(createBookingIdCard());
    }

    private CardView createHeroSection() {
        CardView card = new CardView(this);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
        cardParams.bottomMargin = dp(16);
        card.setLayoutParams(cardParams);
        card.setRadius(dp(24));
        card.setCardElevation(0);

        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(24));
        bg.setColor(Color.parseColor("#EFEDE9"));
        card.setBackground(bg);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setGravity(Gravity.CENTER);
        content.setPadding(dp(24), dp(24), dp(24), dp(24));

        TextView nameText = new TextView(this);
        nameText.setText(bookingName != null ? bookingName : "حجز");
        nameText.setTextSize(24);
        nameText.setTypeface(ThemeManager.fontBold());
        nameText.setTextColor(Color.parseColor("#4B463D"));
        nameText.setGravity(Gravity.CENTER);
        nameText.setTranslationY(-dpf(2f));

        LinearLayout badge = new LinearLayout(this);
        badge.setOrientation(LinearLayout.HORIZONTAL);
        badge.setGravity(Gravity.CENTER);
        badge.setPadding(dp(12), dp(6), dp(12), dp(6));
        LinearLayout.LayoutParams badgeParams = new LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
        badgeParams.topMargin = dp(8);
        badge.setLayoutParams(badgeParams);

        GradientDrawable badgeBg = new GradientDrawable();
        badgeBg.setCornerRadius(dp(20));

        String status = activationStatus != null ? activationStatus : "pending";
        TextView badgeText = new TextView(this);
        badgeText.setTextSize(14);
        badgeText.setTypeface(ThemeManager.fontBold());
        badgeText.setTextColor(Color.WHITE);
        badgeText.setTranslationY(-dpf(1.5f));

        switch (status) {
            case "activated":
                badgeBg.setColor(Color.parseColor("#4CAF50"));
                badgeText.setText("✓ نشط");
                break;
            case "user_scanned":
                badgeBg.setColor(Color.parseColor("#FF9800"));
                badgeText.setText("⏳ في الانتظار");
                break;
            case "completed":
                badgeBg.setColor(Color.parseColor("#757575"));
                badgeText.setText("✓ مكتمل");
                break;
            default:
                badgeBg.setColor(Color.parseColor("#2196F3"));
                badgeText.setText("🔐 قيد التفعيل");
                break;
        }

        badge.setBackground(badgeBg);
        badge.addView(badgeText);

        content.addView(nameText);
        content.addView(badge);
        card.addView(content);

        return card;
    }

    private CardView createTimeCard() {
        CardView card = new CardView(this);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
        cardParams.bottomMargin = dp(12);
        card.setLayoutParams(cardParams);
        card.setRadius(dp(16));
        card.setCardElevation(0);

        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(16));
        bg.setStroke(dp(2), Color.parseColor("#EFEDE9"));
        bg.setColor(Color.WHITE);
        card.setBackground(bg);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(16), dp(16), dp(16));

        TextView title = new TextView(this);
        title.setText("⏰ الوقت والتاريخ");
        title.setTextSize(14);
        title.setTypeface(ThemeManager.fontBold());
        title.setTextColor(Color.parseColor("#804B463D"));
        title.setTranslationY(-dpf(1.5f));
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
        titleParams.bottomMargin = dp(12);
        title.setLayoutParams(titleParams);

        LinearLayout timeRow = new LinearLayout(this);
        timeRow.setOrientation(LinearLayout.HORIZONTAL);
        timeRow.setGravity(Gravity.CENTER_VERTICAL);
        timeRow.setPadding(dp(12), dp(12), dp(12), dp(12));

        GradientDrawable timeRowBg = new GradientDrawable();
        timeRowBg.setColor(Color.parseColor("#EFEDE9"));
        timeRowBg.setCornerRadius(dp(12));
        timeRow.setBackground(timeRowBg);

        SimpleDateFormat sdf = new SimpleDateFormat("d MMM\nhh:mm a", new Locale("ar"));

        TextView startText = new TextView(this);
        startText.setText(startTime > 0 ? sdf.format(new Date(startTime)) : "—");
        startText.setTextColor(Color.parseColor("#804B463D"));
        startText.setTypeface(ThemeManager.fontBold());
        startText.setTextSize(14);
        startText.setGravity(Gravity.CENTER);
        startText.setTranslationY(-dpf(1.5f));
        LinearLayout.LayoutParams startParams = new LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f);
        startText.setLayoutParams(startParams);

        ImageView arrow = new ImageView(this);
        arrow.setImageResource(R.drawable.chevron_right);
        arrow.setColorFilter(Color.parseColor("#504B463D"));
        LinearLayout.LayoutParams arrowParams = new LinearLayout.LayoutParams(dp(16), dp(16));
        arrowParams.setMargins(dp(8), 0, dp(8), 0);
        arrow.setLayoutParams(arrowParams);

        TextView endText = new TextView(this);
        endText.setText(endTime > 0 ? sdf.format(new Date(endTime)) : "—");
        endText.setTextColor(Color.parseColor("#4B463D"));
        endText.setTypeface(ThemeManager.fontBold());
        endText.setTextSize(16);
        endText.setGravity(Gravity.CENTER);
        endText.setTranslationY(-dpf(1.5f));
        LinearLayout.LayoutParams endParams = new LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f);
        endText.setLayoutParams(endParams);

        timeRow.addView(startText);
        timeRow.addView(arrow);
        timeRow.addView(endText);

        content.addView(title);
        content.addView(timeRow);
        card.addView(content);

        return card;
    }

    private CardView createDetailsCard() {
        CardView card = new CardView(this);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
        cardParams.bottomMargin = dp(12);
        card.setLayoutParams(cardParams);
        card.setRadius(dp(16));
        card.setCardElevation(0);

        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(16));
        bg.setStroke(dp(2), Color.parseColor("#EFEDE9"));
        bg.setColor(Color.WHITE);
        card.setBackground(bg);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(16), dp(16), dp(16));

        TextView title = new TextView(this);
        title.setText("📋 تفاصيل الحجز");
        title.setTextSize(14);
        title.setTypeface(ThemeManager.fontBold());
        title.setTextColor(Color.parseColor("#804B463D"));
        title.setTranslationY(-dpf(1.5f));
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
        titleParams.bottomMargin = dp(12);
        title.setLayoutParams(titleParams);

        if (category != null && !category.isEmpty()) {
            content.addView(createDetailRow("النوع", category));
        }

        String priceText = String.format(Locale.getDefault(), "%.0f ج.م", price);
        content.addView(createDetailRow("السعر", priceText));

        content.addView(title, 0);
        card.addView(content);

        return card;
    }

    private LinearLayout createActivationSection() {
        LinearLayout section = new LinearLayout(this);
        section.setOrientation(LinearLayout.VERTICAL);
        section.setPadding(dp(16), dp(16), dp(16), dp(16));

        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(16));

        LinearLayout.LayoutParams sectionParams = new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
        sectionParams.bottomMargin = dp(12);
        section.setLayoutParams(sectionParams);

        TextView title = new TextView(this);
        title.setTextSize(15);
        title.setTypeface(ThemeManager.fontBold());
        title.setTranslationY(-dpf(1.5f));
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
        titleParams.bottomMargin = dp(8);
        title.setLayoutParams(titleParams);

        TextView instructions = new TextView(this);
        instructions.setTextSize(13);
        instructions.setTypeface(ThemeManager.fontSemiBold());
        instructions.setTranslationY(-dpf(1f));
        instructions.setLineSpacing(dp(2), 1.0f);

        String status = activationStatus != null ? activationStatus : "pending";

        switch (status) {
            case "pending":
                bg.setColor(Color.parseColor("#E3F2FD"));
                bg.setStroke(dp(2), Color.parseColor("#2196F3"));
                title.setTextColor(Color.parseColor("#1976D2"));
                title.setText("🔐 خطوة 1: التفعيل");
                instructions.setTextColor(Color.parseColor("#60000000"));
                instructions.setText("اضغط على 'عرض QR Code' لتفعيل حجزك عند مقدم الخدمة");
                break;

            case "user_scanned":
                bg.setColor(Color.parseColor("#FFF3E0"));
                bg.setStroke(dp(2), Color.parseColor("#FF9800"));
                title.setTextColor(Color.parseColor("#F57C00"));
                title.setText("🔓 خطوة 2: في انتظار التفعيل النهائي");
                instructions.setTextColor(Color.parseColor("#60000000"));
                instructions.setText("يجب على مقدم الخدمة مسح QR Code الثاني\nسيظهر الكود هنا بعد المسح");

                if (providerActivationCode != null) {
                    ImageView qrImage = new ImageView(this);
                    LinearLayout.LayoutParams qrParams = new LinearLayout.LayoutParams(dp(200), dp(200));
                    qrParams.gravity = Gravity.CENTER;
                    qrParams.topMargin = dp(12);
                    qrParams.bottomMargin = dp(8);
                    qrImage.setLayoutParams(qrParams);

                    Bitmap qrBitmap = generateQRCode(providerActivationCode);
                    if (qrBitmap != null) {
                        qrImage.setImageBitmap(qrBitmap);

                        section.addView(title);
                        section.addView(instructions);
                        section.addView(qrImage);

                        TextView codeText = new TextView(this);
                        codeText.setText("كود مقدم الخدمة");
                        codeText.setTextSize(12);
                        codeText.setTypeface(ThemeManager.fontSemiBold());
                        codeText.setTextColor(Color.parseColor("#60000000"));
                        codeText.setGravity(Gravity.CENTER);
                        codeText.setTranslationY(-dpf(1f));
                        section.addView(codeText);
                    }
                }
                break;

            case "activated":
                bg.setColor(Color.parseColor("#E8F5E9"));
                bg.setStroke(dp(2), Color.parseColor("#4CAF50"));
                title.setTextColor(Color.parseColor("#2E7D32"));
                title.setText("✅ الحجز مفعّل");
                instructions.setTextColor(Color.parseColor("#60000000"));
                instructions.setText("يمكنك الآن استخدام الخدمة. استمتع بوقتك!");
                break;

            case "completed":
                bg.setColor(Color.parseColor("#F5F5F5"));
                bg.setStroke(dp(2), Color.parseColor("#9E9E9E"));
                title.setTextColor(Color.parseColor("#616161"));
                title.setText("✓ الحجز مكتمل");
                instructions.setTextColor(Color.parseColor("#60000000"));
                instructions.setText("تم إنهاء هذا الحجز بنجاح");
                break;
        }

        section.setBackground(bg);

        if (!status.equals("user_scanned") || providerActivationCode == null) {
            section.addView(title);
            section.addView(instructions);
        }

        return section;
    }

    private LinearLayout createActionButtons() {
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams containerParams = new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
        containerParams.bottomMargin = dp(12);
        container.setLayoutParams(containerParams);

        String status = activationStatus != null ? activationStatus : "pending";

        switch (status) {
            case "pending":
                container.addView(createButton("عرض QR Code للتفعيل",
                        Color.parseColor("#2196F3"), Color.WHITE, this::showUserQRDialog));
                break;

            case "user_scanned":
                container.addView(createButton("مسح QR Code مقدم الخدمة",
                        Color.parseColor("#FF9800"), Color.WHITE, this::startQRScanner));
                break;

            case "activated":
                container.addView(createButton("إلغاء الحجز",
                        Color.parseColor("#F44336"), Color.WHITE, this::cancelBooking));
                break;
        }

        return container;
    }

    private LinearLayout createButton(String text, int bgColor, int textColor, Runnable onClick) {
        LinearLayout btn = new LinearLayout(this);
        btn.setOrientation(LinearLayout.HORIZONTAL);
        btn.setGravity(Gravity.CENTER);
        btn.setPadding(dp(16), dp(14), dp(16), dp(14));

        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(16));
        bg.setColor(bgColor);
        btn.setBackground(bg);

        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
        btnParams.bottomMargin = dp(8);
        btn.setLayoutParams(btnParams);

        TextView btnText = new TextView(this);
        btnText.setText(text);
        btnText.setTextSize(16);
        btnText.setTypeface(ThemeManager.fontBold());
        btnText.setTextColor(textColor);
        btnText.setTranslationY(-dpf(1.5f));

        btn.addView(btnText);
        btn.setOnClickListener(v -> onClick.run());

        return btn;
    }

    private void showUserQRDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        LinearLayout dialogContent = new LinearLayout(this);
        dialogContent.setOrientation(LinearLayout.VERTICAL);
        dialogContent.setGravity(Gravity.CENTER);
        dialogContent.setPadding(dp(24), dp(24), dp(24), dp(24));

        TextView title = new TextView(this);
        title.setText("QR Code للتفعيل");
        title.setTextSize(20);
        title.setTypeface(ThemeManager.fontBold());
        title.setTextColor(Color.parseColor("#4B463D"));
        title.setGravity(Gravity.CENTER);
        title.setTranslationY(-dpf(1.5f));
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
        titleParams.bottomMargin = dp(16);
        title.setLayoutParams(titleParams);

        TextView instructions = new TextView(this);
        instructions.setText("اعرض هذا الكود لمقدم الخدمة للمسح");
        instructions.setTextSize(14);
        instructions.setTypeface(ThemeManager.fontSemiBold());
        instructions.setTextColor(Color.parseColor("#60000000"));
        instructions.setGravity(Gravity.CENTER);
        instructions.setTranslationY(-dpf(1f));
        LinearLayout.LayoutParams insParams = new LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
        insParams.bottomMargin = dp(16);
        instructions.setLayoutParams(insParams);

        ImageView qrImage = new ImageView(this);
        LinearLayout.LayoutParams qrParams = new LinearLayout.LayoutParams(dp(280), dp(280));
        qrParams.bottomMargin = dp(12);
        qrImage.setLayoutParams(qrParams);

        if (userActivationCode != null) {
            Bitmap qrBitmap = generateQRCode(userActivationCode);
            if (qrBitmap != null) {
                qrImage.setImageBitmap(qrBitmap);
            }
        }

        dialogContent.addView(title);
        dialogContent.addView(instructions);
        dialogContent.addView(qrImage);

        builder.setView(dialogContent);
        builder.setPositiveButton("إغلاق", null);
        builder.show();
    }

    private void startQRScanner() {
        Intent intent = new Intent(this, QRScannerActivity.class);
        intent.putExtra("bookingId", bookingId);
        intent.putExtra("expectedCode", providerActivationCode);
        startActivityForResult(intent, QR_SCANNER_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == QR_SCANNER_REQUEST && resultCode == RESULT_OK && data != null) {
            String scannedCode = data.getStringExtra("scannedCode");

            if (scannedCode != null && scannedCode.equals(providerActivationCode)) {
                activateBooking();
            } else {
                Toast.makeText(this, "❌ كود التفعيل غير صحيح", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void activateBooking() {
        Map<String, Object> updates = new HashMap<>();
        updates.put("activationStatus", "activated");
        updates.put("activatedAt", ServerValue.TIMESTAMP);

        realtimeDB.child("bookings")
                .child(userId)
                .child(bookingId)
                .updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "✅ تم تفعيل الحجز بنجاح!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "❌ فشل التفعيل", Toast.LENGTH_SHORT).show();
                    Log.e("MyBookingActivity", "Activation failed", e);
                });
    }

    private void cancelBooking() {
        new AlertDialog.Builder(this)
                .setTitle("إلغاء الحجز")
                .setMessage("هل أنت متأكد من إلغاء هذا الحجز؟")
                .setPositiveButton("نعم، إلغاء", (dialog, which) -> {
                    realtimeDB.child("bookings")
                            .child(userId)
                            .child(bookingId)
                            .removeValue()
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "تم إلغاء الحجز", Toast.LENGTH_SHORT).show();
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "فشل إلغاء الحجز", Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("لا", null)
                .show();
    }

    private Bitmap generateQRCode(String content) {
        try {
            BitMatrix bitMatrix = new MultiFormatWriter().encode(
                    content,
                    BarcodeFormat.QR_CODE,
                    500, 500
            );

            int width = bitMatrix.getWidth();
            int height = bitMatrix.getHeight();
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);

            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    bitmap.setPixel(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }
            return bitmap;
        } catch (Exception e) {
            Log.e("QRCode", "Error generating QR", e);
            return null;
        }
    }

    private CardView createBookingIdCard() {
        CardView card = new CardView(this);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
        cardParams.bottomMargin = dp(12);
        card.setLayoutParams(cardParams);
        card.setRadius(dp(16));
        card.setCardElevation(0);

        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(16));
        bg.setColor(Color.parseColor("#F8F8F8"));
        card.setBackground(bg);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setGravity(Gravity.CENTER);
        content.setPadding(dp(16), dp(12), dp(16), dp(12));

        TextView label = new TextView(this);
        label.setText("رقم الحجز");
        label.setTextSize(12);
        label.setTypeface(ThemeManager.fontSemiBold());
        label.setTextColor(Color.parseColor("#804B463D"));
        label.setTranslationY(-dpf(1f));

        TextView idText = new TextView(this);
        idText.setText(bookingId != null ? bookingId.substring(0, Math.min(8, bookingId.length())) : "—");
        idText.setTextSize(14);
        idText.setTypeface(ThemeManager.fontBold());
        idText.setTextColor(Color.parseColor("#4B463D"));
        idText.setTranslationY(-dpf(1.5f));
        LinearLayout.LayoutParams idParams = new LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
        idParams.topMargin = dp(4);
        idText.setLayoutParams(idParams);

        content.addView(label);
        content.addView(idText);
        card.addView(content);

        return card;
    }

    private LinearLayout createDetailRow(String label, String value) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(8), 0, dp(8));

        TextView labelText = new TextView(this);
        labelText.setText(label);
        labelText.setTextSize(14);
        labelText.setTypeface(ThemeManager.fontSemiBold());
        labelText.setTextColor(Color.parseColor("#804B463D"));
        labelText.setTranslationY(-dpf(1.5f));
        labelText.setLayoutParams(new LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f));

        TextView valueText = new TextView(this);
        valueText.setText(value);
        valueText.setTextSize(16);
        valueText.setTypeface(ThemeManager.fontBold());
        valueText.setTextColor(Color.parseColor("#4B463D"));
        valueText.setGravity(Gravity.END);
        valueText.setTranslationY(-dpf(1.5f));
        valueText.setLayoutParams(new LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f));

        row.addView(labelText);
        row.addView(valueText);

        return row;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Remove listener to prevent memory leaks
        if (bookingListener != null) {
            realtimeDB.child("bookings")
                    .child(userId)
                    .child(bookingId)
                    .removeEventListener(bookingListener);
        }
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }

    private float dpf(float value) {
        return value * getResources().getDisplayMetrics().density;
    }
}