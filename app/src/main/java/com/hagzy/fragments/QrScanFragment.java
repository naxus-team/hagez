package com.hagzy.fragments;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.*;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.google.zxing.ResultPoint;
import com.google.zxing.client.android.BeepManager;
import com.google.zxing.integration.android.IntentIntegrator;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;

import java.util.List;

public class QrScanFragment extends Fragment {

    private DecoratedBarcodeView barcodeView;
    private BeepManager beepManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        barcodeView = new DecoratedBarcodeView(requireContext());
        barcodeView.getBarcodeView().setDecoderFactory(new com.journeyapps.barcodescanner.DefaultDecoderFactory());
        barcodeView.initializeFromIntent(IntentIntegrator.forSupportFragment(this).createScanIntent());
        barcodeView.decodeContinuous(callback);

        beepManager = new BeepManager(requireActivity());
        return barcodeView; // ✅ الآن متوافق لأنه View فعلاً
    }

    private final BarcodeCallback callback = new BarcodeCallback() {
        @Override
        public void barcodeResult(BarcodeResult result) {
            if (result.getText() == null) return;

            beepManager.playBeepSoundAndVibrate();

            Toast.makeText(requireContext(), "📦 الكود: " + result.getText(), Toast.LENGTH_LONG).show();

            // تقدر هنا تعمل navigation أو استدعاء تفاصيل الحجز
            barcodeView.pause();
        }

        @Override
        public void possibleResultPoints(List<ResultPoint> resultPoints) { }
    };

    @Override
    public void onResume() {
        super.onResume();
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, 200);
            return;
        }
        barcodeView.resume();
    }

    @Override
    public void onPause() {
        super.onPause();
        barcodeView.pause();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == 200 && grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            barcodeView.resume();
        } else {
            Toast.makeText(requireContext(), "❌ تم رفض إذن الكاميرا", Toast.LENGTH_SHORT).show();
        }
    }
}
