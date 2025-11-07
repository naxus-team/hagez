package com.naxus.hagez.fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

public class FieldsFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull android.view.LayoutInflater inflater,
                             @Nullable android.view.ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        // 🔹 ScrollView علشان لو البيانات كتير
        ScrollView scrollView = new ScrollView(getContext());
        scrollView.setFillViewport(true);

        // 🔹 Layout عمودي يحتوي على الكروت
        LinearLayout mainLayout = new LinearLayout(getContext());
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setPadding(32, 32, 32, 32);
        mainLayout.setBackgroundColor(Color.WHITE);

        scrollView.addView(mainLayout);

        // 🔹 Firestore instance
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // 🔹 قراءة البيانات من collection "fields"
        db.collection("services")
                .get()
                .addOnSuccessListener((QuerySnapshot queryDocumentSnapshots) -> {
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        // إنشاء كارت لكل ملعب
                        CardView card = new CardView(getContext());
                        card.setCardElevation(8);
                        card.setRadius(24);
                        card.setContentPadding(32, 32, 32, 32);
                        card.setUseCompatPadding(true);

                        // layout داخلي للكارت
                        LinearLayout cardLayout = new LinearLayout(getContext());
                        cardLayout.setOrientation(LinearLayout.VERTICAL);
                        cardLayout.setGravity(Gravity.START);

                        // استخراج البيانات
                        String name = doc.getString("name");
                        String type = doc.getString("type");
                        String description = doc.getString("description");
                        Number price = doc.getDouble("price");
                        Number rating = doc.getDouble("rating");
                        String category = doc.getString("category");

                        // الاسم
                        TextView nameView = new TextView(getContext());
                        nameView.setText(name != null ? name : "No Name");
                        nameView.setTextSize(18);
                        nameView.setTextColor(Color.BLACK);
                        nameView.setGravity(Gravity.START);

                        // النوع
                        TextView typeView = new TextView(getContext());
                        typeView.setText("Type: " + (type != null ? type : "-"));
                        typeView.setTextSize(14);
                        typeView.setTextColor(Color.DKGRAY);

                        // الفئة
                        TextView categoryView = new TextView(getContext());
                        categoryView.setText("Category: " + (category != null ? category : "-"));
                        categoryView.setTextSize(14);
                        categoryView.setTextColor(Color.GRAY);

                        // السعر
                        TextView priceView = new TextView(getContext());
                        priceView.setText("Price: " + (price != null ? price : 0) + " EGP");
                        priceView.setTextSize(14);
                        priceView.setTextColor(Color.rgb(50, 100, 50));

                        // التقييم
                        TextView ratingView = new TextView(getContext());
                        ratingView.setText("Rating: " + (rating != null ? rating : 0));
                        ratingView.setTextSize(14);
                        ratingView.setTextColor(Color.rgb(200, 150, 0));

                        // الوصف
                        TextView descView = new TextView(getContext());
                        descView.setText(description != null ? description : "");
                        descView.setTextSize(13);
                        descView.setTextColor(Color.GRAY);

                        // إضافة العناصر داخل الكارت
                        cardLayout.addView(nameView);
                        cardLayout.addView(typeView);
                        cardLayout.addView(categoryView);
                        cardLayout.addView(priceView);
                        cardLayout.addView(ratingView);
                        cardLayout.addView(descView);

                        card.addView(cardLayout);

                        // إضافة الكارت إلى الصفحة
                        mainLayout.addView(card);

                        // مسافة بين الكروت
                        View spacer = new View(getContext());
                        spacer.setLayoutParams(new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT, 32));
                        mainLayout.addView(spacer);
                    }
                })
                .addOnFailureListener(e -> {
                    TextView error = new TextView(getContext());
                    error.setText("حدث خطأ في تحميل البيانات: " + e.getMessage());
                    error.setTextColor(Color.RED);
                    error.setGravity(Gravity.CENTER);
                    mainLayout.addView(error);
                });

        return scrollView;
    }
}
