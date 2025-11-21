package com.hagzy.helpers;

import android.content.Context;
import org.json.JSONObject;
import java.io.InputStream;

public class TranslationManager {

    private static JSONObject translations;
    private static String currentLang = "ar_EG";

    public static void load(Context context, String lang){
        currentLang = lang;
        try {
            InputStream is = context.getAssets().open("langs/"+lang + ".json"); // en_US.json, ar_EG.json
            byte[] buffer = new byte[is.available()];
            is.read(buffer);
            is.close();
            translations = new JSONObject(new String(buffer, "UTF-8"));
            android.util.Log.d("TranslationManager", "Loaded language file: " + lang + ", keys: " + translations.length());

        } catch (Exception e){
            e.printStackTrace();
            translations = new JSONObject(); // fallback
            android.util.Log.d("TranslationManager", "Loaded language file: " + lang + ", keys: " + e.getMessage());
        }
    }

    public static String t(String key) {
        if (translations == null || key == null || key.isEmpty()) return key;

        try {
            // تقسيم المفتاح حسب الفراغ لتحديد "شجرة" كل جزء
            String[] parts = key.split(" ");

            StringBuilder result = new StringBuilder();

            for (String part : parts) {
                String[] subParts = part.split("\\."); // تقسيم كل شجرة حسب النقطة
                JSONObject obj = translations;
                Object value = null;

                for (String subKey : subParts) {
                    if (obj != null && obj.has(subKey)) {
                        value = obj.get(subKey);
                        if (value instanceof JSONObject) {
                            obj = (JSONObject) value;
                        } else {
                            obj = null;
                        }
                    } else {
                        value = subKey; // fallback لو المفتاح مش موجود
                        obj = null;
                        break;
                    }
                }

                if (value instanceof String) {
                    result.append(value).append(" ");
                } else {
                    result.append(part).append(" "); // fallback آمن
                }
            }

            return result.toString().trim();

        } catch (Exception e) {
            return key;
        }
    }


    public static String getCurrentLang(){ return currentLang; }
}
