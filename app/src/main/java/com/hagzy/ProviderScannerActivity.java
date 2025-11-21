package com.hagzy;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.media.Image;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;
import com.hagzy.helpers.ThemeManager;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Activity لمقدم الخدمة لمسح QR Code الخاص بالمستخدم
 * وعرض QR Code الخاص بمقدم الخدمة للتفعيل النهائي
 */
public class ProviderScannerActivity extends AppCompatActivity {
    private static final int CAMERA_PERMISSION_CODE = 100;
    private PreviewView previewView;
    private ExecutorService cameraExecutor;
    private BarcodeScanner scanner;
    private boolean isScanning = true;
    private DatabaseReference realtimeDB;
    private ProcessCameraProvider cameraProvider;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ThemeManager.setDarkMode(this, false);
        realtimeDB = FirebaseDatabase.getInstance().getReference();

        buildUI();

        cameraExecutor = Executors.newSingleThreadExecutor();

        BarcodeScannerOptions options = new BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                .build();
        scanner = BarcodeScanning.getClient(options);

        if (checkCameraPermission()) {
            startCamera();
        } else {
            requestCameraPermission();
        }
    }

    private void buildUI() {
        FrameLayout root = new FrameLayout(this);
        root.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));
        root.setBackgroundColor(Color.BLACK);

        // Camera preview
        previewView = new PreviewView(this);
        previewView.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));
        root.addView(previewView);

        // Top header
        LinearLayout header = createHeader();
        FrameLayout.LayoutParams headerParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
        );
        headerParams.gravity = Gravity.TOP;
        header.setLayoutParams(headerParams);

        // Scanning frame with corner indicators
        FrameLayout scanFrame = createScanFrame();
        FrameLayout.LayoutParams frameParams = new FrameLayout.LayoutParams(dp(280), dp(280));
        frameParams.gravity = Gravity.CENTER;
        scanFrame.setLayoutParams(frameParams);

        // Instructions box
        LinearLayout instructionsBox = createInstructionsBox();
        FrameLayout.LayoutParams insParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
        );
        insParams.gravity = Gravity.BOTTOM;
        insParams.bottomMargin = dp(48);
        insParams.leftMargin = dp(32);
        insParams.rightMargin = dp(32);
        instructionsBox.setLayoutParams(insParams);

        root.addView(header);
        root.addView(scanFrame);
        root.addView(instructionsBox);

        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            int top = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            int bottom = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom;
            header.setPadding(0, top, 0, 0);
            insParams.bottomMargin = dp(48) + bottom;
            return insets;
        });

        setContentView(root);
    }

    private LinearLayout createHeader() {
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(16), dp(12), dp(16), dp(12));
        header.setBackgroundColor(Color.parseColor("#CC000000"));

        // Back button
        LinearLayout backBtn = new LinearLayout(this);
        backBtn.setGravity(Gravity.CENTER);
        GradientDrawable backBg = new GradientDrawable();
        backBg.setCornerRadius(dp(8));
        backBg.setColor(Color.parseColor("#40FFFFFF"));
        backBtn.setBackground(backBg);
        LinearLayout.LayoutParams backParams = new LinearLayout.LayoutParams(dp(40), dp(40));
        backParams.setMarginEnd(dp(12));
        backBtn.setLayoutParams(backParams);

        ImageView backIcon = new ImageView(this);
        backIcon.setImageResource(R.drawable.chevron_right);
        backIcon.setRotation(180);
        backIcon.setColorFilter(Color.WHITE);
        backIcon.setLayoutParams(new LinearLayout.LayoutParams(dp(24), dp(24)));
        backBtn.addView(backIcon);

        backBtn.setOnClickListener(v -> finish());

        // Title
        TextView title = new TextView(this);
        title.setText("مسح كود المستخدم");
        title.setTextSize(18);
        title.setTypeface(ThemeManager.fontBold());
        title.setTextColor(Color.WHITE);
        title.setTranslationY(-dpf(1.5f));

        header.addView(backBtn);
        header.addView(title);

        return header;
    }

    private FrameLayout createScanFrame() {
        FrameLayout scanFrame = new FrameLayout(this);

        // Border frame
        LinearLayout frameBorder = new LinearLayout(this);
        GradientDrawable borderBg = new GradientDrawable();
        borderBg.setStroke(dp(3), Color.parseColor("#4CAF50"));
        borderBg.setCornerRadius(dp(24));
        frameBorder.setBackground(borderBg);
        frameBorder.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));
        scanFrame.addView(frameBorder);

        // Corner indicators
        scanFrame.addView(createCorner(Gravity.TOP | Gravity.START));
        scanFrame.addView(createCorner(Gravity.TOP | Gravity.END));
        scanFrame.addView(createCorner(Gravity.BOTTOM | Gravity.START));
        scanFrame.addView(createCorner(Gravity.BOTTOM | Gravity.END));

        return scanFrame;
    }

    private View createCorner(int gravity) {
        View corner = new View(this);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(dp(40), dp(40));
        params.gravity = gravity;

        if ((gravity & Gravity.TOP) != 0) params.topMargin = -dp(3);
        if ((gravity & Gravity.BOTTOM) != 0) params.bottomMargin = -dp(3);
        if ((gravity & Gravity.START) != 0) params.leftMargin = -dp(3);
        if ((gravity & Gravity.END) != 0) params.rightMargin = -dp(3);

        corner.setLayoutParams(params);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor("#4CAF50"));
        bg.setCornerRadius(dp(8));
        corner.setBackground(bg);

        return corner;
    }

    private LinearLayout createInstructionsBox() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        box.setPadding(dp(20), dp(16), dp(20), dp(16));

        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(16));
        bg.setColor(Color.parseColor("#E0FFFFFF"));
        box.setBackground(bg);

        TextView instructions = new TextView(this);
        instructions.setText("وجّه الكاميرا نحو QR Code المستخدم\nللمسح التلقائي والتفعيل");
        instructions.setTextSize(15);
        instructions.setTypeface(ThemeManager.fontBold());
        instructions.setTextColor(Color.parseColor("#4B463D"));
        instructions.setGravity(Gravity.CENTER);
        instructions.setLineSpacing(dp(4), 1.0f);
        instructions.setTranslationY(-dpf(1.5f));

        box.addView(instructions);
        return box;
    }

    private boolean checkCameraPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.CAMERA},
                CAMERA_PERMISSION_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                Toast.makeText(this, "يجب السماح بإذن الكاميرا", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                bindCameraUseCases(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                Log.e("ProviderScanner", "Error starting camera", e);
                Toast.makeText(this, "فشل تشغيل الكاميرا", Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    @OptIn(markerClass = ExperimentalGetImage.class)
    private void bindCameraUseCases(@NonNull ProcessCameraProvider cameraProvider) {
        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        imageAnalysis.setAnalyzer(cameraExecutor, this::analyzeImage);

        CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

        try {
            cameraProvider.unbindAll();
            Camera camera = cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview,
                    imageAnalysis
            );
        } catch (Exception e) {
            Log.e("ProviderScanner", "Use case binding failed", e);
        }
    }

    @androidx.camera.core.ExperimentalGetImage
    private void analyzeImage(@NonNull ImageProxy imageProxy) {
        if (!isScanning) {
            imageProxy.close();
            return;
        }

        Image mediaImage = imageProxy.getImage();

        if (mediaImage != null) {
            InputImage image = InputImage.fromMediaImage(
                    mediaImage,
                    imageProxy.getImageInfo().getRotationDegrees()
            );

            scanner.process(image)
                    .addOnSuccessListener(barcodes -> {
                        for (Barcode barcode : barcodes) {
                            String rawValue = barcode.getRawValue();
                            if (rawValue != null && isScanning) {
                                isScanning = false;
                                handleScannedUserCode(rawValue);
                                break;
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e("ProviderScanner", "Barcode scanning failed", e);
                    })
                    .addOnCompleteListener(task -> {
                        imageProxy.close();
                    });
        } else {
            imageProxy.close();
        }
    }

    private void handleScannedUserCode(String userActivationCode) {
        runOnUiThread(() -> {
            // Show loading
            Toast.makeText(this, "جاري التحقق من الكود...", Toast.LENGTH_SHORT).show();

            // Search for booking with this user activation code
            searchBookingByUserCode(userActivationCode);
        });
    }

    private void searchBookingByUserCode(String userActivationCode) {
        realtimeDB.child("bookings")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        boolean found = false;

                        for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                            String userId = userSnapshot.getKey();

                            for (DataSnapshot bookingSnapshot : userSnapshot.getChildren()) {
                                String storedUserCode = bookingSnapshot.child("userActivationCode")
                                        .getValue(String.class);

                                if (storedUserCode != null && storedUserCode.equals(userActivationCode)) {
                                    found = true;
                                    String bookingId = bookingSnapshot.getKey();
                                    String providerCode = bookingSnapshot.child("providerActivationCode")
                                            .getValue(String.class);
                                    String bookingName = bookingSnapshot.child("bookingName")
                                            .getValue(String.class);
                                    String currentStatus = bookingSnapshot.child("activationStatus")
                                            .getValue(String.class);

                                    // Check if already scanned
                                    if ("user_scanned".equals(currentStatus) ||
                                            "activated".equals(currentStatus)) {
                                        runOnUiThread(() -> {
                                            Toast.makeText(ProviderScannerActivity.this,
                                                    "⚠️ هذا الحجز تم مسحه مسبقاً",
                                                    Toast.LENGTH_LONG).show();
                                            isScanning = true;
                                        });
                                        return;
                                    }

                                    // Update status to user_scanned
                                    updateBookingStatus(userId, bookingId, providerCode, bookingName);
                                    return;
                                }
                            }
                        }

                        if (!found) {
                            runOnUiThread(() -> {
                                Toast.makeText(ProviderScannerActivity.this,
                                        "❌ كود غير صحيح أو منتهي الصلاحية",
                                        Toast.LENGTH_LONG).show();
                                isScanning = true;
                            });
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        runOnUiThread(() -> {
                            Toast.makeText(ProviderScannerActivity.this,
                                    "خطأ في قاعدة البيانات: " + error.getMessage(),
                                    Toast.LENGTH_LONG).show();
                            isScanning = true;
                        });
                    }
                });
    }

    private void updateBookingStatus(String userId, String bookingId,
                                     String providerCode, String bookingName) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("activationStatus", "user_scanned");
        updates.put("userScannedAt", ServerValue.TIMESTAMP);

        realtimeDB.child("bookings")
                .child(userId)
                .child(bookingId)
                .updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    runOnUiThread(() -> {
                        // Pause camera
                        if (cameraProvider != null) {
                            cameraProvider.unbindAll();
                        }

                        // Show provider QR code
                        showProviderQRDialog(providerCode, bookingName);
                    });
                })
                .addOnFailureListener(e -> {
                    runOnUiThread(() -> {
                        Toast.makeText(ProviderScannerActivity.this,
                                "❌ فشل تحديث الحجز",
                                Toast.LENGTH_SHORT).show();
                        isScanning = true;
                    });
                    Log.e("ProviderScanner", "Update failed", e);
                });
    }

    private void showProviderQRDialog(String providerCode, String bookingName) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        LinearLayout dialogContent = new LinearLayout(this);
        dialogContent.setOrientation(LinearLayout.VERTICAL);
        dialogContent.setGravity(Gravity.CENTER);
        dialogContent.setPadding(dp(24), dp(24), dp(24), dp(24));
        dialogContent.setBackgroundColor(Color.WHITE);

        // Success icon
        TextView successIcon = new TextView(this);
        successIcon.setText("✓");
        successIcon.setTextSize(56);
        successIcon.setTextColor(Color.parseColor("#4CAF50"));
        successIcon.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        iconParams.bottomMargin = dp(16);
        successIcon.setLayoutParams(iconParams);

        TextView title = new TextView(this);
        title.setText("تم المسح بنجاح!");
        title.setTextSize(22);
        title.setTypeface(ThemeManager.fontBold());
        title.setTextColor(Color.parseColor("#4B463D"));
        title.setGravity(Gravity.CENTER);
        title.setTranslationY(-dpf(2f));
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        titleParams.bottomMargin = dp(8);
        title.setLayoutParams(titleParams);

        TextView subtitle = new TextView(this);
        subtitle.setText("كود التفعيل النهائي");
        subtitle.setTextSize(16);
        subtitle.setTypeface(ThemeManager.fontSemiBold());
        subtitle.setTextColor(Color.parseColor("#804B463D"));
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setTranslationY(-dpf(1.5f));
        LinearLayout.LayoutParams subtitleParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        subtitleParams.bottomMargin = dp(16);
        subtitle.setLayoutParams(subtitleParams);

        TextView instructions = new TextView(this);
        instructions.setText("اعرض هذا الكود للمستخدم\nليقوم بمسحه لتفعيل الحجز");
        instructions.setTextSize(14);
        instructions.setTypeface(ThemeManager.fontSemiBold());
        instructions.setTextColor(Color.parseColor("#60000000"));
        instructions.setGravity(Gravity.CENTER);
        instructions.setLineSpacing(dp(4), 1.0f);
        instructions.setTranslationY(-dpf(1f));
        LinearLayout.LayoutParams insParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        insParams.bottomMargin = dp(16);
        instructions.setLayoutParams(insParams);

        // Booking name
        if (bookingName != null && !bookingName.isEmpty()) {
            LinearLayout bookingBox = new LinearLayout(this);
            bookingBox.setOrientation(LinearLayout.HORIZONTAL);
            bookingBox.setGravity(Gravity.CENTER);
            bookingBox.setPadding(dp(12), dp(8), dp(12), dp(8));

            GradientDrawable bookingBg = new GradientDrawable();
            bookingBg.setCornerRadius(dp(8));
            bookingBg.setColor(Color.parseColor("#E8F5E9"));
            bookingBox.setBackground(bookingBg);

            LinearLayout.LayoutParams bookingParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            bookingParams.bottomMargin = dp(16);
            bookingBox.setLayoutParams(bookingParams);

            TextView bookingText = new TextView(this);
            bookingText.setText("📋 " + bookingName);
            bookingText.setTextSize(15);
            bookingText.setTypeface(ThemeManager.fontBold());
            bookingText.setTextColor(Color.parseColor("#2E7D32"));
            bookingText.setTranslationY(-dpf(1.5f));

            bookingBox.addView(bookingText);
            dialogContent.addView(bookingBox);
        }

        // QR Code with border
        FrameLayout qrContainer = new FrameLayout(this);
        LinearLayout.LayoutParams qrContainerParams = new LinearLayout.LayoutParams(dp(300), dp(300));
        qrContainerParams.bottomMargin = dp(16);
        qrContainer.setLayoutParams(qrContainerParams);

        GradientDrawable qrBorder = new GradientDrawable();
        qrBorder.setCornerRadius(dp(16));
        qrBorder.setStroke(dp(4), Color.parseColor("#4CAF50"));
        qrBorder.setColor(Color.WHITE);
        qrContainer.setBackground(qrBorder);
        qrContainer.setPadding(dp(16), dp(16), dp(16), dp(16));

        ImageView qrImage = new ImageView(this);
        FrameLayout.LayoutParams qrParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        );
        qrImage.setLayoutParams(qrParams);

        Bitmap qrBitmap = generateQRCode(providerCode);
        if (qrBitmap != null) {
            qrImage.setImageBitmap(qrBitmap);
        }

        qrContainer.addView(qrImage);

        TextView codeHint = new TextView(this);
        codeHint.setText("احتفظ بهذه الشاشة حتى يتم المسح");
        codeHint.setTextSize(12);
        codeHint.setTypeface(ThemeManager.fontBold());
        codeHint.setTextColor(Color.parseColor("#FF9800"));
        codeHint.setGravity(Gravity.CENTER);
        codeHint.setTranslationY(-dpf(1f));

        dialogContent.addView(successIcon);
        dialogContent.addView(title);
        dialogContent.addView(subtitle);
        dialogContent.addView(instructions);
        dialogContent.addView(qrContainer);
        dialogContent.addView(codeHint);

        builder.setView(dialogContent);
        builder.setPositiveButton("تم", (dialog, which) -> finish());
        builder.setNegativeButton("مسح آخر", (dialog, which) -> {
            isScanning = true;
            if (cameraProvider != null) {
                bindCameraUseCases(cameraProvider);
            }
        });
        builder.setCancelable(false);
        builder.show();
    }

    private Bitmap generateQRCode(String content) {
        try {
            BitMatrix bitMatrix = new MultiFormatWriter().encode(
                    content,
                    BarcodeFormat.QR_CODE,
                    600, 600
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
        if (scanner != null) {
            scanner.close();
        }
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }

    private float dpf(float value) {
        return value * getResources().getDisplayMetrics().density;
    }
}