package org.example.dto;

import jakarta.validation.constraints.NotBlank;

// No userId/vehicleId/transactionType/totalAmount/createdAt here: those can't change after creation.
public class TransactionUpdateRequest {

    @NotBlank(message = "status is required")
    private String status;

    private String paymentMethod;

    private String receiptUrl;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getReceiptUrl() {
        return receiptUrl;
    }

    public void setReceiptUrl(String receiptUrl) {
        this.receiptUrl = receiptUrl;
    }
}
