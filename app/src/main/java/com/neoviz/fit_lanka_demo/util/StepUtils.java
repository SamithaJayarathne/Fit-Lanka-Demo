package com.neoviz.fit_lanka_demo.util;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Transaction;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class StepUtils {

    public static void updateLeaderboardSteps(String userId, long newlyDetectedSteps) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference userRef = db.collection("users").document(userId);

        // Transaction එකක් පාවිච්චි කරමු - එතකොට දත්ත එකවර update වෙද්දී අවුල් වෙන්නේ නැහැ
        db.runTransaction((Transaction.Function<Void>) transaction -> {
            DocumentSnapshot snapshot = transaction.get(userRef);

            Calendar now = Calendar.getInstance();
            int currentWeek = now.get(Calendar.WEEK_OF_YEAR);
            int currentMonth = now.get(Calendar.MONTH);

            long currentWeekly = snapshot.contains("weeklySteps") ? snapshot.getLong("weeklySteps") : 0;
            long currentMonthly = snapshot.contains("monthlySteps") ? snapshot.getLong("monthlySteps") : 0;
            int lastWeek = snapshot.contains("lastUpdateWeek") ? snapshot.getLong("lastUpdateWeek").intValue() : -1;
            int lastMonth = snapshot.contains("lastUpdateMonth") ? snapshot.getLong("lastUpdateMonth").intValue() : -1;

            // සතිය Reset Logic
            if (currentWeek != lastWeek) {
                currentWeekly = newlyDetectedSteps;
            } else {
                currentWeekly += newlyDetectedSteps;
            }

            // මාසය Reset Logic
            if (currentMonth != lastMonth) {
                currentMonthly = newlyDetectedSteps;
            } else {
                currentMonthly += newlyDetectedSteps;
            }

            // Data Update එක
            Map<String, Object> updates = new HashMap<>();
            updates.put("weeklySteps", currentWeekly);
            updates.put("monthlySteps", currentMonthly);
            updates.put("lastUpdateWeek", currentWeek);
            updates.put("lastUpdateMonth", currentMonth);


            transaction.update(userRef, updates);
            return null;
        });
    }
}