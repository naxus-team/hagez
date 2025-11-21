package com.hagzy;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.hagzy.helpers.ThemeManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class InboxActivity extends AppCompatActivity {
    FirebaseFirestore db;
    private RecyclerView recyclerView;
    private InboxAdapter adapter;
    private List<InboxMessage> messagesList = new ArrayList<>();
    private LinearLayout root;
    private LinearLayout emptyStateContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ThemeManager.setDarkMode(this, false);

        db = FirebaseFirestore.getInstance();

        buildUI();
        loadInboxMessages();
    }

    private void buildUI() {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.WHITE);

        // Header
        root.addView(createHeader());

        // RecyclerView
        recyclerView = new RecyclerView(this);
        recyclerView.setLayoutParams(new LinearLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT));
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setPadding(dp(16), dp(8), dp(16), dp(16));

        adapter = new InboxAdapter(messagesList);
        recyclerView.setAdapter(adapter);

        root.addView(recyclerView);

        // Empty state (hidden initially)
        emptyStateContainer = createEmptyState();
        emptyStateContainer.setVisibility(View.GONE);
        root.addView(emptyStateContainer);

        setContentView(root);
    }

    private LinearLayout createHeader() {
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(16), dp(12), dp(16), dp(12));
        header.setBackgroundColor(Color.WHITE);

        // Back button
        ImageView backBtn = new ImageView(this);
        backBtn.setImageResource(R.drawable.chevron_right);
        backBtn.setRotation(180);
        backBtn.setColorFilter(Color.parseColor("#4B463D"), PorterDuff.Mode.SRC_IN);
        LinearLayout.LayoutParams backParams = new LinearLayout.LayoutParams(dp(32), dp(32));
        backParams.setMarginEnd(dp(12));
        backBtn.setLayoutParams(backParams);
        backBtn.setOnClickListener(v -> finish());

        // Title
        TextView title = new TextView(this);
        title.setText("الإشعارات");
        title.setTextSize(18);
        title.setTypeface(ThemeManager.fontBold());
        title.setTextColor(Color.parseColor("#4B463D"));
        title.setTranslationY(-dpf(1.5f));

        header.addView(backBtn);
        header.addView(title);

        // Add shadow
        header.setElevation(dp(4));

        return header;
    }

    private LinearLayout createEmptyState() {
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setGravity(Gravity.CENTER);
        container.setPadding(dp(32), dp(48), dp(32), dp(48));
        container.setLayoutParams(new LinearLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT));

        // Icon
        ImageView icon = new ImageView(this);
        icon.setImageResource(R.drawable.check_badge);
        icon.setColorFilter(Color.parseColor("#C0BBB3"));
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dp(80), dp(80));
        iconParams.bottomMargin = dp(16);
        icon.setLayoutParams(iconParams);

        // Text
        TextView emptyText = new TextView(this);
        emptyText.setText("لا توجد إشعارات");
        emptyText.setTextSize(18);
        emptyText.setTypeface(ThemeManager.fontBold());
        emptyText.setTextColor(Color.parseColor("#804B463D"));
        emptyText.setGravity(Gravity.CENTER);
        emptyText.setTranslationY(-dpf(1.5f));

        TextView emptySubtext = new TextView(this);
        emptySubtext.setText("ستظهر هنا جميع الإشعارات المتعلقة بحجوزاتك");
        emptySubtext.setTextSize(14);
        emptySubtext.setTypeface(ThemeManager.fontSemiBold());
        emptySubtext.setTextColor(Color.parseColor("#80000000"));
        emptySubtext.setGravity(Gravity.CENTER);
        emptySubtext.setTranslationY(-dpf(1.5f));
        LinearLayout.LayoutParams subtextParams = new LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
        subtextParams.topMargin = dp(8);
        emptySubtext.setLayoutParams(subtextParams);

        container.addView(icon);
        container.addView(emptyText);
        container.addView(emptySubtext);

        return container;
    }

    private void loadInboxMessages() {
        showSkeletonLoading();

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "الرجاء تسجيل الدخول", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = user.getUid();

        // جلب الرسائل من collection "inbox" أو "notifications"
        db.collection("inbox")
                .whereEqualTo("userId", uid)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    // حذف skeleton
                    int skeletonCount = messagesList.size();
                    messagesList.clear();
                    adapter.notifyItemRangeRemoved(0, skeletonCount);

                    if (querySnapshot.isEmpty()) {
                        showEmptyState();
                        return;
                    }

                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        String messageId = doc.getId();
                        String fieldId = doc.getString("fieldId");
                        String bookingId = doc.getString("bookingId");
                        String title = doc.getString("title");
                        String message = doc.getString("message");
                        String type = doc.getString("type"); // "booking_confirmed", "booking_cancelled", etc.
                        Boolean isRead = doc.getBoolean("isRead");
                        Timestamp timestamp = doc.getTimestamp("timestamp");

                        // جلب معلومات الملعب
                        if (fieldId != null) {
                            loadFieldData(fieldId, bookingId, title, message, type,
                                    isRead != null && isRead, timestamp, messageId);
                        } else {
                            // رسالة بدون ملعب
                            InboxMessage item = new InboxMessage(
                                    messageId,
                                    null,
                                    null,
                                    title,
                                    message,
                                    type,
                                    isRead != null && isRead,
                                    timestamp,
                                    fieldId,
                                    bookingId
                            );
                            messagesList.add(item);
                            adapter.notifyItemInserted(messagesList.size() - 1);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("InboxActivity", "Error loading inbox", e);
                    Toast.makeText(this, "فشل تحميل الإشعارات", Toast.LENGTH_SHORT).show();

                    int skeletonCount = messagesList.size();
                    messagesList.clear();
                    adapter.notifyItemRangeRemoved(0, skeletonCount);
                    showEmptyState();
                });
    }

    private void loadFieldData(String fieldId, String bookingId, String title,
                               String message, String type, boolean isRead,
                               Timestamp timestamp, String messageId) {
        db.collection("fields")
                .document(fieldId)
                .get()
                .addOnSuccessListener(fieldDoc -> {
                    String fieldName = null;
                    if (fieldDoc.exists()) {
                        Map<String, Object> meta = (Map<String, Object>) fieldDoc.get("meta");
                        fieldName = meta != null && meta.get("name") != null
                                ? meta.get("name").toString()
                                : null;
                    }

                    String finalFieldName = fieldName;

                    // جلب صورة الملعب
                    db.collection("fields")
                            .document(fieldId)
                            .collection("images")
                            .limit(1)
                            .get()
                            .addOnSuccessListener(imagesSnapshot -> {
                                String imageUrl = null;
                                if (!imagesSnapshot.isEmpty()) {
                                    imageUrl = imagesSnapshot.getDocuments()
                                            .get(0).getString("url");
                                }

                                InboxMessage item = new InboxMessage(
                                        messageId,
                                        finalFieldName,
                                        imageUrl,
                                        title,
                                        message,
                                        type,
                                        isRead,
                                        timestamp,
                                        fieldId,
                                        bookingId
                                );

                                messagesList.add(item);
                                adapter.notifyItemInserted(messagesList.size() - 1);
                            });
                });
    }

    private void showSkeletonLoading() {
        if (!messagesList.isEmpty()) {
            int oldSize = messagesList.size();
            messagesList.clear();
            adapter.notifyItemRangeRemoved(0, oldSize);
        }

        for (int i = 0; i < 5; i++) {
            messagesList.add(new InboxMessage(true));
            adapter.notifyItemInserted(i);
        }

        emptyStateContainer.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);
    }

    private void showEmptyState() {
        emptyStateContainer.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }

    private float dpf(float value) {
        return value * getResources().getDisplayMetrics().density;
    }

    // ============================
    // InboxMessage Model
    // ============================
    public static class InboxMessage {
        public String messageId;
        public String fieldName;
        public String imageUrl;
        public String title;
        public String message;
        public String type;
        public boolean isRead;
        public Timestamp timestamp;
        public String fieldId;
        public String bookingId;
        public boolean isSkeleton;

        public InboxMessage(boolean isSkeleton) {
            this.isSkeleton = isSkeleton;
        }

        public InboxMessage(String messageId, String fieldName, String imageUrl,
                            String title, String message, String type,
                            boolean isRead, Timestamp timestamp,
                            String fieldId, String bookingId) {
            this.messageId = messageId;
            this.fieldName = fieldName;
            this.imageUrl = imageUrl;
            this.title = title;
            this.message = message;
            this.type = type;
            this.isRead = isRead;
            this.timestamp = timestamp;
            this.fieldId = fieldId;
            this.bookingId = bookingId;
            this.isSkeleton = false;
        }
    }

    // ============================
    // RecyclerView Adapter
    // ============================
    private class InboxAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private final int VIEW_TYPE_SKELETON = 0;
        private final int VIEW_TYPE_MESSAGE = 1;

        private List<InboxMessage> items;

        public InboxAdapter(List<InboxMessage> items) {
            this.items = items;
        }

        @Override
        public int getItemViewType(int position) {
            return items.get(position).isSkeleton ? VIEW_TYPE_SKELETON : VIEW_TYPE_MESSAGE;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            if (viewType == VIEW_TYPE_SKELETON) {
                return new SkeletonViewHolder(createSkeletonView());
            } else {
                return new MessageViewHolder(createMessageCard());
            }
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            if (holder instanceof MessageViewHolder) {
                ((MessageViewHolder) holder).bind(items.get(position));
            }
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        private View createSkeletonView() {
            CardView card = new CardView(InboxActivity.this);
            GradientDrawable bg = new GradientDrawable();
            bg.setCornerRadius(dp(16));
            bg.setColor(Color.parseColor("#F0F0F0"));
            card.setBackground(bg);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(MATCH_PARENT, dp(90));
            params.setMargins(0, dp(4), 0, dp(4));
            card.setLayoutParams(params);

            LinearLayout content = new LinearLayout(InboxActivity.this);
            content.setOrientation(LinearLayout.HORIZONTAL);
            content.setPadding(dp(12), dp(12), dp(12), dp(12));

            // Skeleton circle
            View circle = new View(InboxActivity.this);
            circle.setLayoutParams(new LinearLayout.LayoutParams(dp(50), dp(50)));
            GradientDrawable circleBg = new GradientDrawable();
            circleBg.setShape(GradientDrawable.OVAL);
            circleBg.setColor(Color.parseColor("#E0E0E0"));
            circle.setBackground(circleBg);

            // Skeleton lines
            LinearLayout textColumn = new LinearLayout(InboxActivity.this);
            textColumn.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams columnParams = new LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f);
            columnParams.setMarginStart(dp(12));
            textColumn.setLayoutParams(columnParams);

            View line1 = new View(InboxActivity.this);
            LinearLayout.LayoutParams line1Params = new LinearLayout.LayoutParams(MATCH_PARENT, dp(16));
            line1Params.bottomMargin = dp(8);
            line1.setLayoutParams(line1Params);
            line1.setBackgroundColor(Color.parseColor("#E0E0E0"));

            View line2 = new View(InboxActivity.this);
            line2.setLayoutParams(new LinearLayout.LayoutParams(MATCH_PARENT, dp(12)));
            line2.setBackgroundColor(Color.parseColor("#EEEEEE"));

            textColumn.addView(line1);
            textColumn.addView(line2);

            content.addView(circle);
            content.addView(textColumn);
            card.addView(content);

            return card;
        }

        private CardView createMessageCard() {
            CardView card = new CardView(InboxActivity.this);
            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
            cardParams.setMargins(0, dp(4), 0, dp(4));
            card.setLayoutParams(cardParams);
            card.setRadius(dp(16));
            card.setCardElevation(0);

            return card;
        }

        class SkeletonViewHolder extends RecyclerView.ViewHolder {
            public SkeletonViewHolder(@NonNull View itemView) {
                super(itemView);

                ObjectAnimator fadeAnim = ObjectAnimator.ofFloat(itemView, "alpha", 0.5f, 1f, 0.5f);
                fadeAnim.setDuration(1200);
                fadeAnim.setRepeatCount(ValueAnimator.INFINITE);
                fadeAnim.setRepeatMode(ValueAnimator.REVERSE);
                fadeAnim.start();
            }
        }

        class MessageViewHolder extends RecyclerView.ViewHolder {
            ImageView fieldImage;
            TextView titleText, messageText, timeText;
            View unreadIndicator;
            CardView card;

            public MessageViewHolder(@NonNull View itemView) {
                super(itemView);

                card = (CardView) itemView;

                LinearLayout content = new LinearLayout(InboxActivity.this);
                content.setOrientation(LinearLayout.HORIZONTAL);
                content.setGravity(Gravity.CENTER_VERTICAL);
                content.setPadding(dp(12), dp(12), dp(12), dp(12));

                // Field image
                fieldImage = new ImageView(InboxActivity.this);
                LinearLayout.LayoutParams imgParams = new LinearLayout.LayoutParams(dp(50), dp(50));
                imgParams.setMarginEnd(dp(12));
                fieldImage.setLayoutParams(imgParams);

                GradientDrawable imgBg = new GradientDrawable();
                imgBg.setShape(GradientDrawable.OVAL);
                imgBg.setColor(Color.parseColor("#F0F0F0"));
                fieldImage.setBackground(imgBg);
                fieldImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
                fieldImage.setClipToOutline(true);

                // Text column
                LinearLayout textColumn = new LinearLayout(InboxActivity.this);
                textColumn.setOrientation(LinearLayout.VERTICAL);
                textColumn.setLayoutParams(new LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f));

                titleText = new TextView(InboxActivity.this);
                titleText.setTextSize(15);
                titleText.setTypeface(ThemeManager.fontBold());
                titleText.setTextColor(Color.parseColor("#4B463D"));
                titleText.setTranslationY(-dpf(1.5f));
                titleText.setMaxLines(1);

                messageText = new TextView(InboxActivity.this);
                messageText.setTextSize(13);
                messageText.setTypeface(ThemeManager.fontSemiBold());
                messageText.setTextColor(Color.parseColor("#804B463D"));
                messageText.setTranslationY(-dpf(1f));
                messageText.setMaxLines(2);
                LinearLayout.LayoutParams msgParams = new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
                msgParams.topMargin = dp(4);
                messageText.setLayoutParams(msgParams);

                timeText = new TextView(InboxActivity.this);
                timeText.setTextSize(11);
                timeText.setTypeface(ThemeManager.fontSemiBold());
                timeText.setTextColor(Color.parseColor("#60000000"));
                timeText.setTranslationY(-dpf(1f));
                LinearLayout.LayoutParams timeParams = new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
                timeParams.topMargin = dp(4);
                timeText.setLayoutParams(timeParams);

                textColumn.addView(titleText);
                textColumn.addView(messageText);
                textColumn.addView(timeText);

                // Unread indicator
                unreadIndicator = new View(InboxActivity.this);
                LinearLayout.LayoutParams indicatorParams = new LinearLayout.LayoutParams(dp(10), dp(10));
                indicatorParams.setMarginStart(dp(8));
                unreadIndicator.setLayoutParams(indicatorParams);

                GradientDrawable indicatorBg = new GradientDrawable();
                indicatorBg.setShape(GradientDrawable.OVAL);
                indicatorBg.setColor(Color.parseColor("#4CAF50"));
                unreadIndicator.setBackground(indicatorBg);

                content.addView(fieldImage);
                content.addView(textColumn);
                content.addView(unreadIndicator);

                card.addView(content);
            }

            public void bind(InboxMessage item) {
                titleText.setText(item.title != null ? item.title : "إشعار جديد");
                messageText.setText(item.message != null ? item.message : "");

                if (item.timestamp != null) {
                    timeText.setText(formatTimeAgo(item.timestamp.toDate()));
                } else {
                    timeText.setText("");
                }

                // Show/hide unread indicator
                unreadIndicator.setVisibility(item.isRead ? View.GONE : View.VISIBLE);

                // Set background based on read status
                GradientDrawable bg = new GradientDrawable();
                bg.setCornerRadius(dp(16));
                if (item.isRead) {
                    bg.setColor(Color.WHITE);
                    bg.setStroke(dp(1), Color.parseColor("#F0F0F0"));
                } else {
                    bg.setColor(Color.parseColor("#F8F9FA"));
                    bg.setStroke(dp(2), Color.parseColor("#E8F5E9"));
                }
                card.setBackground(bg);

                // Load image
                if (item.imageUrl != null) {
                    Glide.with(InboxActivity.this)
                            .load(item.imageUrl)
                            .into(fieldImage);
                } else {
                    fieldImage.setImageResource(R.drawable.check_badge);
                    fieldImage.setColorFilter(Color.parseColor("#C0BBB3"));
                }

                // Click listener
                card.setOnClickListener(v -> {
                    if (item.bookingId != null) {
                        // Mark as read
                        markAsRead(item.messageId);

                        // Open booking details
                        Intent intent = new Intent(InboxActivity.this, MyBookingActivity.class);
                        intent.putExtra("bookingId", item.bookingId);
                        startActivity(intent);
                    }
                });
            }
        }
    }

    private void markAsRead(String messageId) {
        if (messageId != null) {
            db.collection("inbox")
                    .document(messageId)
                    .update("isRead", true)
                    .addOnSuccessListener(aVoid -> {
                        // Update UI
                        for (InboxMessage msg : messagesList) {
                            if (msg.messageId != null && msg.messageId.equals(messageId)) {
                                msg.isRead = true;
                                adapter.notifyDataSetChanged();
                                break;
                            }
                        }
                    });
        }
    }

    private String formatTimeAgo(Date date) {
        long diff = System.currentTimeMillis() - date.getTime();
        long seconds = diff / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (days > 0) {
            if (days == 1) return "أمس";
            if (days < 7) return "منذ " + days + " أيام";
            SimpleDateFormat sdf = new SimpleDateFormat("d MMM", new Locale("ar"));
            return sdf.format(date);
        } else if (hours > 0) {
            return "منذ " + hours + " ساعة";
        } else if (minutes > 0) {
            return "منذ " + minutes + " دقيقة";
        } else {
            return "الآن";
        }
    }
}