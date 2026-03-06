package com.neoviz.fit_lanka_demo.model;

import com.google.firebase.Timestamp;

import java.util.Date;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class User {
    private String uid;
    private String name;
    private String email;
    private String contactNumber;
    private String profileImageUrl;
    private UserRole role;
    private Timestamp createdAt;
    private boolean isActive;
    private int dailyStepTarget;
    private int currentDaySteps;
    private long allTimeSteps;
    private double fitCoins;
    private double weight;
    private double height;
    private List<Address> addresses;
    private List<DailySteps> dailySteps;
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class Address {
        private String addressType;
        private String addressId;
        private String addressLine1;
        private String addressLine2;
        private String city;
        private String state;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class DailySteps {
        private String id;
        private long stepsCount;
        private long timestamp;

    }

}
