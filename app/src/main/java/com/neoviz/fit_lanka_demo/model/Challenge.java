package com.neoviz.fit_lanka_demo.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Challenge {
    private String title;
    private String description;
    private int targetSteps;
    private double rewardCoins;
    private String date;


}
