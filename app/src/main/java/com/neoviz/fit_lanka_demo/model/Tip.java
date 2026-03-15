package com.neoviz.fit_lanka_demo.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Tip {
    private String id;
    private String title;
    private String description;
    private String iconType;
}