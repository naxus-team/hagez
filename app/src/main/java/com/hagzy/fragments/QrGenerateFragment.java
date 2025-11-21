package com.hagzy.fragments;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.view.*;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;
import com.journeyapps.barcodescanner.BarcodeEncoder;
import com.hagzy.helpers.ThemeManager;

public class QrGenerateFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(24), dp(24), dp(24), dp(24));
        layout.setGravity(Gravity.CENTER);
        layout.setBackgroundColor(Color.WHITE);

        // بيانات مبدئية (ممكن تمررها من Bundle أو Firestore)
        String bookingId = "BK-458792";
        String serviceName = "ملعب شباب الأمل";
        String date = "7 نوفمبر 2025";
        String time = "18:00";

        // العنوان
        TextView title = new TextView(requireContext());
        title.setText("رمز الحجز");
        title.setTextSize(20);
        title.setTypeface(ThemeManager.fontBold());
        title.setTextColor(Color.BLACK);
        title.setGravity(Gravity.CENTER);

        // QR Image
        ImageView qrImage = new ImageView(requireContext());
        LinearLayout.LayoutParams qrParams = new LinearLayout.LayoutParams(dp(220), dp(220));
        qrParams.topMargin = dp(24);
        qrImage.setLayoutParams(qrParams);

        Bitmap qrBitmap = generateQRCode(bookingId);
        if (qrBitmap != null) qrImage.setImageBitmap(qrBitmap);

        // معلومات
        TextView info = new TextView(requireContext());
        info.setText(serviceName + "\n" + date + " · " + time + "\nكود: " + bookingId);
        info.setTextSize(14);
        info.setTextColor(Color.parseColor("#555555"));
        info.setGravity(Gravity.CENTER);
        info.setPadding(0, dp(16), 0, 0);

        layout.addView(title);
        layout.addView(qrImage);
        layout.addView(info);

        return layout;
    }

    private Bitmap generateQRCode(String text) {
        try {
            MultiFormatWriter writer = new MultiFormatWriter();
            BitMatrix matrix = writer.encode(text, BarcodeFormat.QR_CODE, 512, 512);
            BarcodeEncoder encoder = new BarcodeEncoder();
            return encoder.createBitmap(matrix);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private int dp(int value) {
        float density = getResources().getDisplayMetrics().density;
        return (int) (value * density);
    }
}
