package com.hagzy;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;
import com.hagzy.helpers.ThemeManager;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class QRScannerActivity extends AppCompatActivity {
    private static final int CAMERA_PERMISSION_CODE = 100;
    private PreviewView previewView;
    private ExecutorService cameraExecutor;
    private BarcodeScanner scanner;
    private boolean isScanning = true;

    private String bookingId;
    private String expectedCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ThemeManager.setDarkMode(this, false);

        bookingId = getIntent().getStringExtra("bookingId");
        expectedCode = getIntent().getStringExtra("expectedCode");

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

        // Overlay with instructions
        LinearLayout overlay = new LinearLayout(this);
        overlay.setOrientation(LinearLayout.VERTICAL);
        overlay.setGravity(Gravity.CENTER);
        overlay.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));

        // Top header
        LinearLayout header = createHeader();
        FrameLayout.LayoutParams headerParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
        );
        headerParams.gravity = Gravity.TOP;
        header.setLayoutParams(headerParams);

        // Scanning frame
        FrameLayout scanFrame = new FrameLayout(this);
        FrameLayout.LayoutParams frameParams = new FrameLayout.LayoutParams(dp(280), dp(280));
        frameParams.gravity = Gravity.CENTER;
        scanFrame.setLayoutParams(frameParams);

        // Frame border
        LinearLayout frameBorder = new LinearLayout(this);
        GradientDrawable borderBg = new GradientDrawable();
        borderBg.setStroke(dp(4), Color.WHITE);
        borderBg.setCornerRadius(dp(24));
        frameBorder.setBackground(borderBg);
        frameBorder.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));
        scanFrame.addView(frameBorder);

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
        title.setText("مسح QR Code");
        title.setTextSize(18);
        title.setTypeface(ThemeManager.fontBold());
        title.setTextColor(Color.WHITE);
        title.setTranslationY(-dpf(1.5f));

        header.addView(backBtn);
        header.addView(title);

        return header;
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
        instructions.setText("وجّه الكاميرا نحو QR Code\nللمسح التلقائي");
        instructions.setTextSize(16);
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
                Toast.makeText(this, "Camera permission is required", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindCameraUseCases(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                Log.e("QRScanner", "Error starting camera", e);
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

        imageAnalysis.setAnalyzer(cameraExecutor, imageProxy -> {
            if (!isScanning) {
                imageProxy.close();
                return;
            }

            @androidx.camera.core.ExperimentalGetImage
            android.media.Image mediaImage = imageProxy.getImage();

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
                                    handleScannedCode(rawValue);
                                    break;
                                }
                            }
                        })
                        .addOnFailureListener(e -> {
                            Log.e("QRScanner", "Barcode scanning failed", e);
                        })
                        .addOnCompleteListener(task -> {
                            imageProxy.close();
                        });
            } else {
                imageProxy.close();
            }
        });

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
            Log.e("QRScanner", "Use case binding failed", e);
        }
    }

    private void handleScannedCode(String scannedCode) {
        runOnUiThread(() -> {
            Intent resultIntent = new Intent();
            resultIntent.putExtra("scannedCode", scannedCode);
            resultIntent.putExtra("bookingId", bookingId);
            setResult(Activity.RESULT_OK, resultIntent);

            Toast.makeText(this, "✓ تم المسح بنجاح", Toast.LENGTH_SHORT).show();

            // Delay finish to show toast
            previewView.postDelayed(this::finish, 500);
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
        scanner.close();
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }

    private float dpf(float value) {
        return value * getResources().getDisplayMetrics().density;
    }
}