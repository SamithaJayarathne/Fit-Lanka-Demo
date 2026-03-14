package com.neoviz.fit_lanka_demo.model;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Order {
    private String orderId;
    private String userId;
    private double totalAmount;
    private String status;
    private com.google.firebase.Timestamp orderDate;
    private String paymentMethod;
    private Address shippingAddress;
    private Address billingAddress;
    private String paymentStatus;
    private List<OrderItem> orderItems;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class OrderItem {
        private String productId;
        private String productName;
        private String productImage;
        private int quantity;
        private double unitPrice;
        private List<Attribute> attributes;

        @Data
        @AllArgsConstructor
        @NoArgsConstructor
        public static class Attribute {
            private String name;
            private String value;
        }
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class Address {
        private String name;
        private String email;
        private String contactNo;
        private String addressLine1;
        private String addressLine2;
        private String city;
        private String postalCode;
    }
}