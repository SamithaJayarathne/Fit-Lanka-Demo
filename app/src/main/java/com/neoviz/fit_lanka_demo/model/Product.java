package com.neoviz.fit_lanka_demo.model;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {
    private String productId;
    private String name;
    private String description;
    private double price;
    private String categoryId;
    private List<String> images;
    private int stockQty;
    private boolean isFeatured;
    private boolean isNewArrival;
    private boolean isTopDiscounted;
    private List<Attribute> attributes;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Attribute{
        private String type;
        private String name;
        private List<String> values;
    }

}
