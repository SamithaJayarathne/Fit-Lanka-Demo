package com.neoviz.fit_lanka_demo.model;

import com.google.firebase.Timestamp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Notification {
    private String id;
    private String title;
    private String message;
    private Timestamp timestamp;
    private String type;
    private boolean read;
}
