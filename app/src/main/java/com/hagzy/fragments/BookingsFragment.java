package com.hagzy.fragments;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

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
import com.hagzy.FieldActivity;
import com.hagzy.QRScannerActivity;
import com.hagzy.R;
import com.hagzy.helpers.ThemeManager;

import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class BookingsFragment extends Fragment {
    private String fieldId;
    private DatabaseReference bookingsRef;
    private DatabaseReference realtimeDB;
    private LinearLayout layout, contents;

    public static BookingsFragment newInstance(String fieldId) {
        BookingsFragment fragment = new BookingsFragment();
        Bundle args = new Bundle();
        args.putString("newfieldId", fieldId);
        fragment.setArguments(args);
        return fragment;
    }

    public void setFieldId(String fieldId) {
        this.fieldId = fieldId;
    }

    public void loadData() {
        if (fieldId == null || contents == null) return;

        realtimeDB = FirebaseDatabase.getInstance().getReference();

        // Get current user's bookings
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            contents.addView(createLoginPrompt());
            return;
        }

        bookingsRef = realtimeDB.child("bookings").child(currentUser.getUid());

        bookingsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                contents.removeAllViews();

                if (!snapshot.exists()) {
                    contents.addView(createEmptyState());
                    return;
                }

                for (DataSnapshot bookingSnap : snapshot.getChildren()) {
                    String bookingId = bookingSnap.getKey();
                    String bookingName = bookingSnap.child("bookingName").getValue(String.class);
                    String category = bookingSnap.child("category").getValue(String.class);
                    Double price = bookingSnap.child("price").getValue(Double.class);
                    String bookingFieldId = bookingSnap.child("fieldId").getValue(String.class);

                    // Activation codes
                    String userActivationCode = bookingSnap.child("userActivationCode").getValue(String.class);
                    String providerActivationCode = bookingSnap.child("providerActivationCode").getValue(String.class);
                    String status = bookingSnap.child("activationStatus").getValue(String.class);

                    // Times
                    Long startTimeLong = bookingSnap.child("duration").child("startEnd").child("start").getValue(Long.class);
                    Long endTimeLong = bookingSnap.child("duration").child("startEnd").child("end").getValue(Long.class);

                    Date startTime = startTimeLong != null ? new Date(startTimeLong) : null;
                    Date endTime = endTimeLong != null ? new Date(endTimeLong) : null;

                    BookingActivationStatus activationStatus = getActivationStatus(status);

                    contents.addView(createBookingCard(
                            bookingId, bookingName, category, price,
                            userActivationCode, providerActivationCode,
                            activationStatus, startTime, endTime, bookingFieldId
                    ));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("RealtimeDB", "خطأ أثناء جلب البيانات: " + error.getMessage());
            }
        });
    }

    private BookingActivationStatus getActivationStatus(String status) {
        if (status == null) return BookingActivationStatus.PENDING;

        switch (status) {
            case "user_scanned":
                return BookingActivationStatus.USER_SCANNED;
            case "activated":
                return BookingActivationStatus.ACTIVATED;
            case "completed":
                return BookingActivationStatus.COMPLETED;
            default:
                return BookingActivationStatus.PENDING;
        }
    }

    private CardView createBookingCard(String bookingId, String name, String category,
                                       Double price, String userCode, String providerCode,
                                       BookingActivationStatus status, Date startTime,
                                       Date endTime, String bookingFieldId) {
        CardView card = new CardView(getContext());
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
        cardParams.setMargins(0, 0, 0, dp(12));
        card.setLayoutParams(cardParams);
        card.setRadius(dp(16));
        card.setCardElevation(0);

        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(16));
        bg.setStroke(dp(2), Color.parseColor("#EFEDE9"));
        bg.setColor(Color.WHITE);
        card.setBackground(bg);

        LinearLayout content = new LinearLayout(getContext());
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(16), dp(16), dp(16));

        // Header with name and status badge
        LinearLayout headerRow = new LinearLayout(getContext());
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams headerParams = new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
        headerParams.bottomMargin = dp(12);
        headerRow.setLayoutParams(headerParams);

        TextView titleText = new TextView(getContext());
        titleText.setText(name != null ? name : "حجز");
        titleText.setTextSize(17);
        titleText.setTypeface(ThemeManager.fontBold());
        titleText.setTextColor(Color.parseColor("#4B463D"));
        titleText.setTranslationY(-dpf(1.5f));
        titleText.setLayoutParams(new LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f));

        headerRow.addView(titleText);
        headerRow.addView(createStatusBadge(status));

        content.addView(headerRow);

        // Details section
        LinearLayout detailsBox = new LinearLayout(getContext());
        detailsBox.setOrientation(LinearLayout.VERTICAL);
        detailsBox.setPadding(dp(12), dp(12), dp(12), dp(12));
        GradientDrawable detailsBg = new GradientDrawable();
        detailsBg.setCornerRadius(dp(12));
        detailsBg.setColor(Color.parseColor("#F8F8F8"));
        detailsBox.setBackground(detailsBg);
        LinearLayout.LayoutParams detailsParams = new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
        detailsParams.bottomMargin = dp(12);
        detailsBox.setLayoutParams(detailsParams);

        if (category != null) {
            detailsBox.addView(createDetailRow("📋 النوع", category));
        }
        detailsBox.addView(createDetailRow("💰 السعر",
                String.format(Locale.getDefault(), "%.0f ج.م", price != null ? price : 0)));

        // Time
        if (startTime != null && endTime != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("d MMM, hh:mm a", new Locale("ar"));
            detailsBox.addView(createDetailRow("🕐 من", sdf.format(startTime)));
            detailsBox.addView(createDetailRow("🕐 إلى", sdf.format(endTime)));
        }

        content.addView(detailsBox);

        // Activation Section
        content.addView(createActivationSection(bookingId, userCode, providerCode,
                status, bookingFieldId));

        // Action Buttons
        content.addView(createActionButtons(bookingId, userCode, providerCode,
                status, bookingFieldId));

        card.addView(content);
        return card;
    }

    private LinearLayout createActivationSection(String bookingId, String userCode,
                                                 String providerCode,
                                                 BookingActivationStatus status,
                                                 String bookingFieldId) {
        LinearLayout section = new LinearLayout(getContext());
        section.setOrientation(LinearLayout.VERTICAL);
        section.setPadding(dp(12), dp(12), dp(12), dp(12));

        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(12));

        LinearLayout.LayoutParams sectionParams = new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
        sectionParams.bottomMargin = dp(12);
        section.setLayoutParams(sectionParams);

        // Title and instructions based on status
        TextView title = new TextView(getContext());
        title.setTextSize(14);
        title.setTypeface(ThemeManager.fontBold());
        title.setTranslationY(-dpf(1.5f));
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
        titleParams.bottomMargin = dp(8);
        title.setLayoutParams(titleParams);

        TextView instructions = new TextView(getContext());
        instructions.setTextSize(12);
        instructions.setTypeface(ThemeManager.fontSemiBold());
        instructions.setTranslationY(-dpf(1f));
        LinearLayout.LayoutParams insParams = new LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
        insParams.bottomMargin = dp(12);
        instructions.setLayoutParams(insParams);

        switch (status) {
            case PENDING:
                bg.setColor(Color.parseColor("#E3F2FD"));
                bg.setStroke(dp(1), Color.parseColor("#2196F3"));
                title.setTextColor(Color.parseColor("#1976D2"));
                title.setText("🔐 خطوة 1: التفعيل بواسطة المستخدم");
                instructions.setTextColor(Color.parseColor("#60000000"));
                instructions.setText("اضغط على 'عرض QR Code' لتفعيل حجزك عند مقدم الخدمة");
                break;

            case USER_SCANNED:
                bg.setColor(Color.parseColor("#FFF3E0"));
                bg.setStroke(dp(1), Color.parseColor("#FF9800"));
                title.setTextColor(Color.parseColor("#F57C00"));
                title.setText("🔓 خطوة 2: التفعيل بواسطة مقدم الخدمة");
                instructions.setTextColor(Color.parseColor("#60000000"));
                instructions.setText("يجب على مقدم الخدمة مسح QR Code الثاني لتفعيل الحجز");

                // Show provider QR if available
                if (providerCode != null) {
                    ImageView qrImage = new ImageView(getContext());
                    LinearLayout.LayoutParams qrParams = new LinearLayout.LayoutParams(dp(150), dp(150));
                    qrParams.gravity = Gravity.CENTER;
                    qrParams.topMargin = dp(8);
                    qrParams.bottomMargin = dp(8);
                    qrImage.setLayoutParams(qrParams);

                    Bitmap qrBitmap = generateQRCode(providerCode);
                    if (qrBitmap != null) {
                        qrImage.setImageBitmap(qrBitmap);
                        section.addView(title);
                        section.addView(instructions);
                        section.addView(qrImage);

                        TextView codeText = new TextView(getContext());
                        codeText.setText("كود التفعيل: " + providerCode.substring(0, 8) + "...");
                        codeText.setTextSize(11);
                        codeText.setTypeface(ThemeManager.fontSemiBold());
                        codeText.setTextColor(Color.parseColor("#60000000"));
                        codeText.setGravity(Gravity.CENTER);
                        codeText.setTranslationY(-dpf(1f));
                        section.addView(codeText);
                    }
                }
                break;

            case ACTIVATED:
                bg.setColor(Color.parseColor("#E8F5E9"));
                bg.setStroke(dp(1), Color.parseColor("#4CAF50"));
                title.setTextColor(Color.parseColor("#2E7D32"));
                title.setText("✅ الحجز مفعّل");
                instructions.setTextColor(Color.parseColor("#60000000"));
                instructions.setText("يمكنك الآن استخدام الخدمة. استمتع!");
                break;

            case COMPLETED:
                bg.setColor(Color.parseColor("#F5F5F5"));
                bg.setStroke(dp(1), Color.parseColor("#9E9E9E"));
                title.setTextColor(Color.parseColor("#616161"));
                title.setText("✓ الحجز مكتمل");
                instructions.setTextColor(Color.parseColor("#60000000"));
                instructions.setText("تم إنهاء هذا الحجز");
                break;
        }

        section.setBackground(bg);

        if (status != BookingActivationStatus.USER_SCANNED) {
            section.addView(title);
            section.addView(instructions);
        }

        return section;
    }

    private LinearLayout createActionButtons(String bookingId, String userCode,
                                             String providerCode,
                                             BookingActivationStatus status,
                                             String bookingFieldId) {
        LinearLayout buttonsContainer = new LinearLayout(getContext());
        buttonsContainer.setOrientation(LinearLayout.VERTICAL);

        switch (status) {
            case PENDING:
                // Show user QR button
                buttonsContainer.addView(createButton(
                        "عرض QR Code للتفعيل",
                        Color.parseColor("#2196F3"),
                        Color.WHITE,
                        () -> showUserQRDialog(bookingId, userCode)
                ));
                break;

            case USER_SCANNED:
                // Show scan provider QR button
                buttonsContainer.addView(createButton(
                        "مسح QR Code مقدم الخدمة",
                        Color.parseColor("#FF9800"),
                        Color.WHITE,
                        () -> startQRScanner(bookingId, providerCode)
                ));
                break;

            case ACTIVATED:
                // Show cancel button
                buttonsContainer.addView(createButton(
                        "إلغاء الحجز",
                        Color.parseColor("#F44336"),
                        Color.WHITE,
                        () -> cancelBooking(bookingId, bookingFieldId)
                ));
                break;
        }

        return buttonsContainer;
    }

    private LinearLayout createButton(String text, int bgColor, int textColor, Runnable onClick) {
        LinearLayout btn = new LinearLayout(getContext());
        btn.setOrientation(LinearLayout.HORIZONTAL);
        btn.setGravity(Gravity.CENTER);
        btn.setPadding(0, dp(12), 0, dp(12));

        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(12));
        bg.setColor(bgColor);
        btn.setBackground(bg);

        TextView btnText = new TextView(getContext());
        btnText.setText(text);
        btnText.setTextSize(15);
        btnText.setTypeface(ThemeManager.fontBold());
        btnText.setTextColor(textColor);
        btnText.setTranslationY(-dpf(1.5f));

        btn.addView(btnText);
        btn.setOnClickListener(v -> onClick.run());

        return btn;
    }

    private void showUserQRDialog(String bookingId, String userCode) {
        // Create dialog to show user QR code
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(getContext());

        LinearLayout dialogContent = new LinearLayout(getContext());
        dialogContent.setOrientation(LinearLayout.VERTICAL);
        dialogContent.setGravity(Gravity.CENTER);
        dialogContent.setPadding(dp(24), dp(24), dp(24), dp(24));

        TextView title = new TextView(getContext());
        title.setText("QR Code للتفعيل");
        title.setTextSize(18);
        title.setTypeface(ThemeManager.fontBold());
        title.setTextColor(Color.parseColor("#4B463D"));
        title.setGravity(Gravity.CENTER);
        title.setTranslationY(-dpf(1.5f));
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
        titleParams.bottomMargin = dp(16);
        title.setLayoutParams(titleParams);

        TextView instructions = new TextView(getContext());
        instructions.setText("اعرض هذا الكود لمقدم الخدمة للمسح");
        instructions.setTextSize(14);
        instructions.setTypeface(ThemeManager.fontSemiBold());
        instructions.setTextColor(Color.parseColor("#60000000"));
        instructions.setGravity(Gravity.CENTER);
        instructions.setTranslationY(-dpf(1f));
        LinearLayout.LayoutParams insParams = new LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
        insParams.bottomMargin = dp(16);
        instructions.setLayoutParams(insParams);

        ImageView qrImage = new ImageView(getContext());
        LinearLayout.LayoutParams qrParams = new LinearLayout.LayoutParams(dp(250), dp(250));
        qrParams.bottomMargin = dp(16);
        qrImage.setLayoutParams(qrParams);

        Bitmap qrBitmap = generateQRCode(userCode);
        if (qrBitmap != null) {
            qrImage.setImageBitmap(qrBitmap);
        }

        TextView codeText = new TextView(getContext());
        String safeCode;

        if (userCode == null) {
            safeCode = "غير متوفر";
        } else if (userCode.length() <= 12) {
            safeCode = userCode;
        } else {
            safeCode = userCode.substring(0, 12) + "...";
        }

        codeText.setText("الكود: " + safeCode);        codeText.setTextSize(12);
        codeText.setTypeface(ThemeManager.fontSemiBold());
        codeText.setTextColor(Color.parseColor("#60000000"));
        codeText.setGravity(Gravity.CENTER);
        codeText.setTranslationY(-dpf(1f));

        dialogContent.addView(title);
        dialogContent.addView(instructions);
        dialogContent.addView(qrImage);
        dialogContent.addView(codeText);

        builder.setView(dialogContent);
        builder.setPositiveButton("إغلاق", null);
        builder.show();
    }

    private void startQRScanner(String bookingId, String expectedProviderCode) {
        Intent intent = new Intent(getContext(), QRScannerActivity.class);
        intent.putExtra("bookingId", bookingId);
        intent.putExtra("expectedCode", expectedProviderCode);
        startActivityForResult(intent, 100);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 100 && resultCode == android.app.Activity.RESULT_OK) {
            String scannedCode = data.getStringExtra("scannedCode");
            String bookingId = data.getStringExtra("bookingId");

            if (scannedCode != null && bookingId != null) {
                activateBooking(bookingId, scannedCode);
            }
        }
    }

    private void activateBooking(String bookingId, String scannedProviderCode) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        DatabaseReference bookingRef = realtimeDB.child("bookings")
                .child(user.getUid())
                .child(bookingId);

        bookingRef.child("providerActivationCode").get().addOnSuccessListener(snapshot -> {
            String storedProviderCode = snapshot.getValue(String.class);

            if (storedProviderCode != null && storedProviderCode.equals(scannedProviderCode)) {
                Map<String, Object> updates = new HashMap<>();
                updates.put("activationStatus", "activated");
                updates.put("activatedAt", ServerValue.TIMESTAMP);

                bookingRef.updateChildren(updates)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(getContext(), "✅ تم تفعيل الحجز بنجاح!",
                                    Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(getContext(), "❌ فشل التفعيل",
                                    Toast.LENGTH_SHORT).show();
                        });
            } else {
                Toast.makeText(getContext(), "❌ كود التفعيل غير صحيح",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void cancelBooking(String bookingId, String bookingFieldId) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        new android.app.AlertDialog.Builder(getContext())
                .setTitle("إلغاء الحجز")
                .setMessage("هل أنت متأكد من إلغاء هذا الحجز؟")
                .setPositiveButton("نعم، إلغاء", (dialog, which) -> {
                    realtimeDB.child("bookings")
                            .child(user.getUid())
                            .child(bookingId)
                            .removeValue()
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(getContext(), "تم إلغاء الحجز",
                                        Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(getContext(), "فشل إلغاء الحجز",
                                        Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("لا", null)
                .show();
    }

    private String generateActivationCode(String bookingId, String userId, String salt) {
        try {
            String data = bookingId + userId + salt + System.currentTimeMillis();
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data.getBytes());

            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            return UUID.randomUUID().toString();
        }
    }

    public void performBooking(String bookingDocId, String name, String category,
                               Double price, Long quantity) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(getContext(), "يجب تسجيل الدخول أولاً", Toast.LENGTH_SHORT).show();
            return;
        }

        String bookingId = UUID.randomUUID().toString();
        long now = System.currentTimeMillis();
        long endTime = now + (2 * 60 * 60 * 1000); // 2 hours

        // Generate activation codes
        String userActivationCode = generateActivationCode(bookingId, user.getUid(), "user");
        String providerActivationCode = generateActivationCode(bookingId, user.getUid(), "provider");

        Map<String, Object> userBooking = new HashMap<>();
        userBooking.put("bookingId", bookingId);
        userBooking.put("bookingDocId", bookingDocId);
        userBooking.put("fieldId", fieldId);
        userBooking.put("userId", user.getUid());
        userBooking.put("bookingName", name);
        userBooking.put("category", category);
        userBooking.put("price", price);
        userBooking.put("bookingDate", ServerValue.TIMESTAMP);
        userBooking.put("userActivationCode", userActivationCode);
        userBooking.put("providerActivationCode", providerActivationCode);
        userBooking.put("activationStatus", "pending");

        realtimeDB.child("bookings")
                .child(user.getUid())
                .child(bookingId)
                .setValue(userBooking)
                .addOnSuccessListener(aVoid -> {
                    // Create duration info
                    Map<String, Object> durationData = new HashMap<>();
                    Map<String, Object> startEnd = new HashMap<>();
                    startEnd.put("start", now);
                    startEnd.put("end", endTime);
                    durationData.put("startEnd", startEnd);

                    realtimeDB.child("bookings")
                            .child(user.getUid())
                            .child(bookingId)
                            .child("duration")
                            .setValue(durationData);

                    Toast.makeText(getContext(), "✅ تم الحجز بنجاح!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "❌ فشل الحجز: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
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

    private LinearLayout createStatusBadge(BookingActivationStatus status) {
        LinearLayout badge = new LinearLayout(getContext());
        badge.setOrientation(LinearLayout.HORIZONTAL);
        badge.setGravity(Gravity.CENTER);
        badge.setPadding(dp(10), dp(5), dp(10), dp(5));

        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(16));

        String text;
        int bgColor;
        int textColor;

        switch (status) {
            case PENDING:
                text = "قيد الانتظار";
                bgColor = Color.parseColor("#E3F2FD");
                textColor = Color.parseColor("#1976D2");
                break;
            case USER_SCANNED:
                text = "تم مسح QR المستخدم";
                bgColor = Color.parseColor("#FFF3E0");
                textColor = Color.parseColor("#F57C00");
                break;
            case ACTIVATED:
                text = "مفعّل";
                bgColor = Color.parseColor("#E8F5E9");
                textColor = Color.parseColor("#2E7D32");
                break;
            case COMPLETED:
                text = "مكتمل";
                bgColor = Color.parseColor("#F5F5F5");
                textColor = Color.parseColor("#757575");
                break;
            default:
                text = "—";
                bgColor = Color.parseColor("#F5F5F5");
                textColor = Color.parseColor("#757575");
        }

        bg.setColor(bgColor);
        badge.setBackground(bg);

        TextView badgeText = new TextView(getContext());
        badgeText.setText(text);
        badgeText.setTextSize(12);
        badgeText.setTypeface(ThemeManager.fontBold());
        badgeText.setTextColor(textColor);
        badgeText.setTranslationY(-dpf(1f));

        badge.addView(badgeText);
        return badge;
    }

    private LinearLayout createDetailRow(String label, String value) {
        LinearLayout row = new LinearLayout(getContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(4), 0, dp(4));

        TextView labelText = new TextView(getContext());
        labelText.setText(label);
        labelText.setTextSize(14);
        labelText.setTypeface(ThemeManager.fontSemiBold());
        labelText.setTextColor(Color.parseColor("#804B463D"));
        labelText.setTranslationY(-dpf(1.5f));
        labelText.setLayoutParams(new LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f));

        TextView valueText = new TextView(getContext());
        valueText.setText(value);
        valueText.setTextSize(14);
        valueText.setTypeface(ThemeManager.fontBold());
        valueText.setTextColor(Color.parseColor("#4B463D"));
        valueText.setGravity(Gravity.END);
        valueText.setTranslationY(-dpf(1.5f));
        valueText.setLayoutParams(new LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f));

        row.addView(labelText);
        row.addView(valueText);

        return row;
    }

    private LinearLayout createEmptyState() {
        LinearLayout container = new LinearLayout(getContext());
        container.setOrientation(LinearLayout.VERTICAL);
        container.setGravity(Gravity.CENTER);
        container.setPadding(dp(32), dp(48), dp(32), dp(48));

        ImageView icon = new ImageView(getContext());
        icon.setImageResource(R.drawable.check_badge);
        icon.setColorFilter(Color.parseColor("#C0BBB3"));
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dp(64), dp(64));
        iconParams.bottomMargin = dp(16);
        icon.setLayoutParams(iconParams);

        TextView emptyText = new TextView(getContext());
        emptyText.setText("لا توجد حجوزات حالياً");
        emptyText.setTextSize(16);
        emptyText.setTypeface(ThemeManager.fontBold());
        emptyText.setTextColor(Color.parseColor("#804B463D"));
        emptyText.setGravity(Gravity.CENTER);
        emptyText.setTranslationY(-dpf(1.5f));

        TextView emptySubtext = new TextView(getContext());
        emptySubtext.setText("احجز الآن للبدء!");
        emptySubtext.setTextSize(14);
        emptySubtext.setTypeface(ThemeManager.fontSemiBold());
        emptySubtext.setTextColor(Color.parseColor("#60000000"));
        emptySubtext.setGravity(Gravity.CENTER);
        emptySubtext.setTranslationY(-dpf(1f));
        LinearLayout.LayoutParams subtextParams = new LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
        subtextParams.topMargin = dp(8);
        emptySubtext.setLayoutParams(subtextParams);

        container.addView(icon);
        container.addView(emptyText);
        container.addView(emptySubtext);
        return container;
    }

    private LinearLayout createLoginPrompt() {
        LinearLayout container = new LinearLayout(getContext());
        container.setOrientation(LinearLayout.VERTICAL);
        container.setGravity(Gravity.CENTER);
        container.setPadding(dp(32), dp(48), dp(32), dp(48));

        ImageView icon = new ImageView(getContext());
        icon.setImageResource(R.drawable.check_badge);
        icon.setColorFilter(Color.parseColor("#C0BBB3"));
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dp(64), dp(64));
        iconParams.bottomMargin = dp(16);
        icon.setLayoutParams(iconParams);

        TextView text = new TextView(getContext());
        text.setText("يجب تسجيل الدخول لعرض حجوزاتك");
        text.setTextSize(16);
        text.setTypeface(ThemeManager.fontBold());
        text.setTextColor(Color.parseColor("#804B463D"));
        text.setGravity(Gravity.CENTER);
        text.setTranslationY(-dpf(1.5f));

        container.addView(icon);
        container.addView(text);
        return container;
    }

    private void buildHeader() {
        LinearLayout header = new LinearLayout(getContext());
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(16), dp(12), dp(16), dp(12));
        header.setBackgroundColor(Color.WHITE);

        ImageView backBtn = new ImageView(getContext());
        backBtn.setImageResource(R.drawable.chevron_right);
        backBtn.setRotation(180);
        backBtn.setColorFilter(Color.parseColor("#4B463D"), PorterDuff.Mode.SRC_IN);
        LinearLayout.LayoutParams backParams = new LinearLayout.LayoutParams(dp(32), dp(32));
        backParams.setMarginEnd(dp(12));
        backBtn.setLayoutParams(backParams);
        backBtn.setOnClickListener(v -> {
            if (getContext() instanceof FieldActivity) {
                ((FieldActivity) getContext()).onBackPressed();
            }
        });

        TextView title = new TextView(getContext());
        title.setText("حجوزاتي");
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

        layout.addView(header);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup containerParent, Bundle savedInstanceState) {
        if (getArguments() != null) {
            fieldId = getArguments().getString("newfieldId");
        }

        ThemeManager.setDarkMode(getContext(), false);

        ScrollView scroll = new ScrollView(getContext());
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Color.WHITE);

        layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);

        buildHeader();

        contents = new LinearLayout(getContext());
        contents.setOrientation(LinearLayout.VERTICAL);
        contents.setPadding(dp(16), dp(8), dp(16), dp(16));

        layout.addView(contents);
        scroll.addView(layout);

        ViewCompat.setOnApplyWindowInsetsListener(layout, (v, insets) -> {
            int top = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            int bottom = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom;
            layout.setPadding(0, top, 0, bottom);
            return insets;
        });

        loadData();

        return scroll;
    }

    private int dp(int value) {
        return (int) (value * getContext().getResources().getDisplayMetrics().density);
    }

    private float dpf(float value) {
        return value * getContext().getResources().getDisplayMetrics().density;
    }

    enum BookingActivationStatus {
        PENDING,        // في انتظار مسح QR المستخدم
        USER_SCANNED,   // تم مسح QR المستخدم، في انتظار مسح QR مقدم الخدمة
        ACTIVATED,      // تم التفعيل بالكامل
        COMPLETED       // مكتمل
    }
}