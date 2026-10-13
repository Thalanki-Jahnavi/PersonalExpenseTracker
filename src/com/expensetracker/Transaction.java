
package com.expensetracker;

import java.math.BigDecimal;
import java.time.LocalDate;

public class Transaction {

    private int id;
    private String type;
    private String category;
    private BigDecimal amount;
    private String description;
    private LocalDate date;

    public Transaction(int id, String type, String category,
                       BigDecimal amount, String description,
                       LocalDate date) {
        this.id = id;
        this.type = type;
        this.category = category;
        this.amount = amount;
        this.description = description;
        this.date = date;
    }

    public int getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public String getCategory() {
        return category;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getDescription() {
        return description;
    }

    public LocalDate getDate() {
        return date;
    }
}
