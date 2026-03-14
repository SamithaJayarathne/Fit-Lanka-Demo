package com.neoviz.fit_lanka_demo.model;

import java.io.Serializable;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product implements Serializable {
    private String productId;
    private String name;
    private String description;
    private double price;
    private String categoryId;
    private String categoryName;
    private List<String> images;
    private int stockQty;
    private boolean isFeatured;
    private boolean isNewArrival;
    private boolean isTopDiscounted;
    private float rating;
    private Float maxPercentage;
    private List<Attribute> attributes;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Attribute implements Serializable{
        private String type;
        private String name;
        private List<String> values;
    }

}
