package com.neoviz.fit_lanka_demo.model;

import com.google.firebase.Timestamp;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WishlistItem implements Serializable {
    private String productId;
    private String productName;
    private String productImage;
    private double price;
    private String categoryName;
    private Timestamp addedAt;
}
