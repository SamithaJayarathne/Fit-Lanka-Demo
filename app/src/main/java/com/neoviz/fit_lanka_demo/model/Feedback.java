package com.neoviz.fit_lanka_demo.model;

import com.google.firebase.Timestamp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Feedback {
    private String userId;
    private String userName;
    private float rating;
    private String review;
    private Timestamp timestamp;
}
