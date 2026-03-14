package com.neoviz.fit_lanka_demo.model;

import com.google.firebase.Timestamp;

import java.io.Serializable;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CartItem implements Serializable {

    private String productId;
    private int quantity;
    private List<CartItem.SelectedAttribute> attributes;
    private Timestamp addedAt;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class SelectedAttribute implements Serializable{
        private String attributeName;
        private String attributeValue;
    }
}
