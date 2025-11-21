package com.hagzy.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.util.Log;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.functions.FirebaseFunctions;
import com.hagzy.MainActivity;
import com.hagzy.R;
import com.hagzy.helpers.ThemeManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class WalletLayout {

    private final Context context;
    private FrameLayout root;
    private LinearLayout contentLayout;
    private TextView balanceAmount;
    private LinearLayout transactionsContainer;

    private DatabaseReference realtimeDB;
    private FirebaseAuth mAuth;
    private FirebaseFunctions functions;
    private ValueEventListener walletListener;
    private ValueEventListener transactionsListener;

    private double currentBalance = 0.0;

    public static class TransactionItem {
        public String id;
        public String type; // deposit, withdrawal, booking_payment, refund
        public String title;
        public String date;
        public double amount;
        public boolean isIncome;
        public String status; // pending, completed, failed
        public long timestamp;

        public TransactionItem(String id, String type, String title, String date,
                               double amount, boolean isIncome, String status, long timestamp) {
            this.id = id;
            this.type = type;
            this.title = title;
            this.date = date;
            this.amount = amount;
            this.isIncome = isIncome;
            this.status = status;
            this.timestamp = timestamp;
        }
    }

    public WalletLayout(Context context) {
        this.context = context;
        mAuth = FirebaseAuth.getInstance();
        realtimeDB = FirebaseDatabase.getInstance().getReference();
        functions = FirebaseFunctions.getInstance();
        buildLayout();
        loadWalletData();
    }

    private void buildLayout() {
        root = new FrameLayout(context);
        root.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));
        root.setBackgroundColor(Color.WHITE);

        contentLayout = new LinearLayout(context);
        contentLayout.setOrientation(LinearLayout.VERTICAL);
        contentLayout.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));

        root.addView(contentLayout);

        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            int top = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            int bottom = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom;
            contentLayout.setPadding(0, top, 0, bottom);
            return insets;
        });

        buildHeader();
        buildBody();
    }

    private void buildHeader() {
        LinearLayout header = new LinearLayout(context);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(16), dp(12), dp(16), dp(12));
        header.setBackgroundColor(Color.WHITE);

        // Back button
        ImageView backBtn = new ImageView(context);
        backBtn.setImageResource(R.drawable.chevron_right);
        backBtn.setRotation(180);
        backBtn.setColorFilter(Color.parseColor("#4B463D"));
        LinearLayout.LayoutParams backParams = new LinearLayout.LayoutParams(dp(32), dp(32));
        backParams.setMarginEnd(dp(12));
        backBtn.setLayoutParams(backParams);
        backBtn.setOnClickListener(v -> {
            if (context instanceof MainActivity) {
                ((MainActivity) context).onBackPressed();
            }
        });

        // Title
        TextView title = new TextView(context);
        title.setText("💳 المحفظة");
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

        contentLayout.addView(header);
    }

    private void buildBody() {
        // Balance Card
        contentLayout.addView(createBalanceCard());

        // Action Buttons
        contentLayout.addView(createActionButtons());

        // Transactions ScrollView
        ScrollView scrollView = new ScrollView(context);
        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
        );
        scrollView.setLayoutParams(scrollParams);
        scrollView.setVerticalScrollBarEnabled(false);

        transactionsContainer = new LinearLayout(context);
        transactionsContainer.setOrientation(LinearLayout.VERTICAL);
        transactionsContainer.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        transactionsContainer.setPadding(dp(16), dp(16), dp(16), dp(16));

        scrollView.addView(transactionsContainer);
        contentLayout.addView(scrollView);
    }

    private LinearLayout createBalanceCard() {
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER);
        card.setPadding(dp(24), dp(24), dp(24), dp(24));

        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(20));
        bg.setColors(new int[]{
                Color.parseColor("#667eea"),
                Color.parseColor("#764ba2")
        });
        bg.setGradientType(GradientDrawable.LINEAR_GRADIENT);
        bg.setOrientation(GradientDrawable.Orientation.BR_TL);
        card.setBackground(bg);

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(dp(16), dp(16), dp(16), 0);
        card.setLayoutParams(cardParams);

        TextView balanceLabel = new TextView(context);
        balanceLabel.setText("الرصيد المتاح");
        balanceLabel.setTextSize(14);
        balanceLabel.setTypeface(ThemeManager.fontSemiBold());
        balanceLabel.setTextColor(Color.parseColor("#E0FFFFFF"));
        balanceLabel.setTranslationY(-dpf(1f));

        balanceAmount = new TextView(context);
        balanceAmount.setText("0.00 ج.م");
        balanceAmount.setTextSize(32);
        balanceAmount.setTextColor(Color.WHITE);
        balanceAmount.setTypeface(ThemeManager.fontBold());
        balanceAmount.setTranslationY(-dpf(2.5f));
        LinearLayout.LayoutParams amountParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        amountParams.topMargin = dp(8);
        balanceAmount.setLayoutParams(amountParams);

        card.addView(balanceLabel);
        card.addView(balanceAmount);

        return card;
    }

    private LinearLayout createActionButtons() {
        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.HORIZONTAL);
        container.setGravity(Gravity.CENTER);
        container.setPadding(dp(16), dp(16), dp(16), dp(8));

        LinearLayout.LayoutParams containerParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        container.setLayoutParams(containerParams);

        // Deposit Button
        container.addView(createActionButton(
                "💰 إيداع",
                Color.parseColor("#4CAF50"),
                () -> showDepositDialog()
        ));

        // Withdraw Button (optional - for future)
        container.addView(createActionButton(
                "💸 سحب",
                Color.parseColor("#FF9800"),
                () -> Toast.makeText(context, "قريباً", Toast.LENGTH_SHORT).show()
        ));

        return container;
    }

    private LinearLayout createActionButton(String text, int color, Runnable onClick) {
        LinearLayout btn = new LinearLayout(context);
        btn.setOrientation(LinearLayout.VERTICAL);
        btn.setGravity(Gravity.CENTER);
        btn.setPadding(dp(16), dp(12), dp(16), dp(12));

        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f
        );
        btnParams.setMargins(dp(4), 0, dp(4), 0);
        btn.setLayoutParams(btnParams);

        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(12));
        bg.setColor(color);
        btn.setBackground(bg);

        TextView btnText = new TextView(context);
        btnText.setText(text);
        btnText.setTextSize(15);
        btnText.setTypeface(ThemeManager.fontBold());
        btnText.setTextColor(Color.WHITE);
        btnText.setTranslationY(-dpf(1.5f));

        btn.addView(btnText);
        btn.setOnClickListener(v -> onClick.run());

        return btn;
    }

    private void showDepositDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("💰 إيداع في المحفظة");

        LinearLayout dialogLayout = new LinearLayout(context);
        dialogLayout.setOrientation(LinearLayout.VERTICAL);
        dialogLayout.setPadding(dp(20), dp(10), dp(20), dp(10));

        TextView label = new TextView(context);
        label.setText("المبلغ (ج.م):");
        label.setTextSize(14);
        label.setTypeface(ThemeManager.fontSemiBold());
        label.setTextColor(Color.parseColor("#4B463D"));
        label.setTranslationY(-dpf(1.5f));

        EditText amountInput = new EditText(context);
        amountInput.setHint("100");
        amountInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER |
                android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        amountInput.setTextSize(16);
        amountInput.setPadding(dp(12), dp(12), dp(12), dp(12));

        GradientDrawable inputBg = new GradientDrawable();
        inputBg.setCornerRadius(dp(8));
        inputBg.setStroke(dp(2), Color.parseColor("#E0E0E0"));
        inputBg.setColor(Color.WHITE);
        amountInput.setBackground(inputBg);

        LinearLayout.LayoutParams inputParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        inputParams.topMargin = dp(8);
        inputParams.bottomMargin = dp(8);
        amountInput.setLayoutParams(inputParams);

        TextView note = new TextView(context);
        note.setText("الحد الأدنى: 10 ج.م");
        note.setTextSize(12);
        note.setTypeface(ThemeManager.fontSemiBold());
        note.setTextColor(Color.parseColor("#999999"));
        note.setTranslationY(-dpf(1f));

        dialogLayout.addView(label);
        dialogLayout.addView(amountInput);
        dialogLayout.addView(note);

        builder.setView(dialogLayout);
        builder.setPositiveButton("متابعة", (dialog, which) -> {
            String amountStr = amountInput.getText().toString().trim();
            if (amountStr.isEmpty()) {
                Toast.makeText(context, "⚠️ أدخل المبلغ", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                double amount = Double.parseDouble(amountStr);
                if (amount < 10) {
                    Toast.makeText(context, "⚠️ الحد الأدنى 10 ج.م", Toast.LENGTH_SHORT).show();
                    return;
                }
                initiatePaymobDeposit(amount);
            } catch (NumberFormatException e) {
                Toast.makeText(context, "⚠️ مبلغ غير صحيح", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("إلغاء", null);
        builder.show();
    }

    private void initiatePaymobDeposit(double amount) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Toast.makeText(context, "⚠️ يجب تسجيل الدخول", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(context, "جاري إنشاء رابط الدفع...", Toast.LENGTH_SHORT).show();

        Map<String, Object> data = new HashMap<>();
        data.put("amount", amount);
        data.put("userId", user.getUid());
        data.put("userEmail", user.getEmail());
        data.put("userName", user.getDisplayName());

        functions.getHttpsCallable("createPaymobPayment")
                .call(data)
                .addOnSuccessListener(result -> {
                    Map<String, Object> response = (Map<String, Object>) result.getData();
                    String paymentUrl = (String) response.get("payment_url");
                    String transactionId = (String) response.get("transaction_id");

                    if (paymentUrl != null) {
                        // Save pending transaction
                        savePendingTransaction(transactionId, amount);

                        // Open payment URL
                        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(paymentUrl));
                        context.startActivity(browserIntent);

                        Toast.makeText(context, "✓ تم فتح صفحة الدفع", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(context, "❌ فشل إنشاء رابط الدفع", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(context, "❌ خطأ: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("Wallet", "Paymob error", e);
                });
    }

    private void savePendingTransaction(String transactionId, double amount) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        Map<String, Object> transaction = new HashMap<>();
        transaction.put("type", "deposit");
        transaction.put("amount", amount);
        transaction.put("status", "pending");
        transaction.put("transactionId", transactionId);
        transaction.put("timestamp", ServerValue.TIMESTAMP);
        transaction.put("title", "إيداع في المحفظة");

        realtimeDB.child("wallets")
                .child(user.getUid())
                .child("transactions")
                .push()
                .setValue(transaction);
    }

    private void loadWalletData() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            showLoginPrompt();
            return;
        }

        String userId = user.getUid();

        // Load balance
        walletListener = realtimeDB.child("wallets")
                .child(userId)
                .child("balance")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Double balance = snapshot.getValue(Double.class);
                        currentBalance = balance != null ? balance : 0.0;
                        updateBalanceUI();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("Wallet", "Error loading balance", error.toException());
                    }
                });

        // Load transactions
        transactionsListener = realtimeDB.child("wallets")
                .child(userId)
                .child("transactions")
                .orderByChild("timestamp")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        List<TransactionItem> transactions = new ArrayList<>();

                        for (DataSnapshot txSnapshot : snapshot.getChildren()) {
                            String id = txSnapshot.getKey();
                            String type = txSnapshot.child("type").getValue(String.class);
                            Double amount = txSnapshot.child("amount").getValue(Double.class);
                            String status = txSnapshot.child("status").getValue(String.class);
                            Long timestamp = txSnapshot.child("timestamp").getValue(Long.class);
                            String title = txSnapshot.child("title").getValue(String.class);

                            if (amount != null && timestamp != null) {
                                boolean isIncome = "deposit".equals(type) || "refund".equals(type);

                                SimpleDateFormat sdf = new SimpleDateFormat("d MMM yyyy, hh:mm a",
                                        new Locale("ar"));
                                String date = sdf.format(new Date(timestamp));

                                transactions.add(new TransactionItem(
                                        id, type, title, date, amount, isIncome, status, timestamp
                                ));
                            }
                        }

                        // Sort by timestamp (newest first)
                        transactions.sort((t1, t2) -> Long.compare(t2.timestamp, t1.timestamp));

                        displayTransactions(transactions);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("Wallet", "Error loading transactions", error.toException());
                    }
                });
    }

    private void updateBalanceUI() {
        balanceAmount.setText(String.format(Locale.getDefault(), "%.2f ج.م", currentBalance));
    }

    private void displayTransactions(List<TransactionItem> transactions) {
        transactionsContainer.removeAllViews();

        if (transactions.isEmpty()) {
            transactionsContainer.addView(createEmptyState());
            return;
        }

        TextView sectionTitle = new TextView(context);
        sectionTitle.setText("📋 سجل العمليات");
        sectionTitle.setTextSize(16);
        sectionTitle.setTypeface(ThemeManager.fontBold());
        sectionTitle.setTextColor(Color.parseColor("#4B463D"));
        sectionTitle.setTranslationY(-dpf(1.5f));
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        titleParams.bottomMargin = dp(12);
        sectionTitle.setLayoutParams(titleParams);
        transactionsContainer.addView(sectionTitle);

        for (TransactionItem item : transactions) {
            transactionsContainer.addView(createTransactionCard(item));
        }
    }

    private LinearLayout createTransactionCard(TransactionItem item) {
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(16), dp(16), dp(16), dp(16));

        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(12));
        bg.setStroke(dp(2), Color.parseColor("#EFEDE9"));
        bg.setColor(Color.WHITE);
        card.setBackground(bg);

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        cardParams.bottomMargin = dp(8);
        card.setLayoutParams(cardParams);

        // Icon
        TextView icon = new TextView(context);
        icon.setText(getTransactionIcon(item.type));
        icon.setTextSize(24);
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dp(40), dp(40));
        iconParams.setMarginEnd(dp(12));
        icon.setLayoutParams(iconParams);
        icon.setGravity(Gravity.CENTER);

        // Details
        LinearLayout details = new LinearLayout(context);
        details.setOrientation(LinearLayout.VERTICAL);
        details.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView title = new TextView(context);
        title.setText(item.title != null ? item.title : getTransactionTitle(item.type));
        title.setTextSize(15);
        title.setTypeface(ThemeManager.fontBold());
        title.setTextColor(Color.parseColor("#4B463D"));
        title.setTranslationY(-dpf(1.5f));

        TextView date = new TextView(context);
        date.setText(item.date);
        date.setTextSize(12);
        date.setTypeface(ThemeManager.fontSemiBold());
        date.setTextColor(Color.parseColor("#999999"));
        date.setTranslationY(-dpf(1f));
        LinearLayout.LayoutParams dateParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        dateParams.topMargin = dp(2);
        date.setLayoutParams(dateParams);

        details.addView(title);
        details.addView(date);

        // Amount with status
        LinearLayout amountContainer = new LinearLayout(context);
        amountContainer.setOrientation(LinearLayout.VERTICAL);
        amountContainer.setGravity(Gravity.END);

        TextView amount = new TextView(context);
        amount.setText((item.isIncome ? "+ " : "- ") +
                String.format(Locale.getDefault(), "%.2f ج.م", item.amount));
        amount.setTextSize(16);
        amount.setTypeface(ThemeManager.fontBold());
        amount.setTextColor(item.isIncome ?
                Color.parseColor("#4CAF50") : Color.parseColor("#F44336"));
        amount.setTranslationY(-dpf(1.5f));

        if ("pending".equals(item.status)) {
            TextView statusBadge = new TextView(context);
            statusBadge.setText("⏳ قيد المعالجة");
            statusBadge.setTextSize(10);
            statusBadge.setTypeface(ThemeManager.fontSemiBold());
            statusBadge.setTextColor(Color.parseColor("#FF9800"));
            statusBadge.setTranslationY(-dpf(1f));
            LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            statusParams.topMargin = dp(2);
            statusBadge.setLayoutParams(statusParams);
            amountContainer.addView(statusBadge);
        }

        amountContainer.addView(amount, 0);

        card.addView(icon);
        card.addView(details);
        card.addView(amountContainer);

        return card;
    }

    private String getTransactionIcon(String type) {
        switch (type) {
            case "deposit": return "💰";
            case "withdrawal": return "💸";
            case "booking_payment": return "🎫";
            case "refund": return "↩️";
            default: return "💳";
        }
    }

    private String getTransactionTitle(String type) {
        switch (type) {
            case "deposit": return "إيداع في المحفظة";
            case "withdrawal": return "سحب من المحفظة";
            case "booking_payment": return "دفع حجز";
            case "refund": return "استرجاع مبلغ";
            default: return "عملية";
        }
    }

    private LinearLayout createEmptyState() {
        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setGravity(Gravity.CENTER);
        container.setPadding(dp(32), dp(48), dp(32), dp(48));

        TextView icon = new TextView(context);
        icon.setText("💳");
        icon.setTextSize(48);
        icon.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        iconParams.bottomMargin = dp(16);
        icon.setLayoutParams(iconParams);

        TextView text = new TextView(context);
        text.setText("لا توجد عمليات بعد");
        text.setTextSize(16);
        text.setTypeface(ThemeManager.fontBold());
        text.setTextColor(Color.parseColor("#804B463D"));
        text.setGravity(Gravity.CENTER);
        text.setTranslationY(-dpf(1.5f));

        container.addView(icon);
        container.addView(text);

        return container;
    }

    private void showLoginPrompt() {
        transactionsContainer.removeAllViews();

        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setGravity(Gravity.CENTER);
        container.setPadding(dp(32), dp(64), dp(32), dp(64));

        TextView text = new TextView(context);
        text.setText("يجب تسجيل الدخول\nلعرض محفظتك");
        text.setTextSize(18);
        text.setTypeface(ThemeManager.fontBold());
        text.setTextColor(Color.parseColor("#804B463D"));
        text.setGravity(Gravity.CENTER);
        text.setTranslationY(-dpf(1.5f));
        text.setLineSpacing(dp(4), 1.0f);

        container.addView(text);
        transactionsContainer.addView(container);
    }

    public void cleanup() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            if (walletListener != null) {
                realtimeDB.child("wallets").child(user.getUid()).child("balance")
                        .removeEventListener(walletListener);
            }
            if (transactionsListener != null) {
                realtimeDB.child("wallets").child(user.getUid()).child("transactions")
                        .removeEventListener(transactionsListener);
            }
        }
    }

    private int dp(int value) {
        return (int) (value * context.getResources().getDisplayMetrics().density);
    }

    private float dpf(float value) {
        return value * context.getResources().getDisplayMetrics().density;
    }

    public FrameLayout getView() {
        return root;
    }
}