
package com.expensetracker;

import java.math.BigDecimal;
import java.time.LocalDate;

public class DatabaseTest {

    public static void main(String[] args) {

        TransactionDAO dao = new TransactionDAO();

        Transaction transaction = new Transaction(
                0,
                "EXPENSE",
                "Food",
                new BigDecimal("250.00"),
                "Lunch",
                LocalDate.now()
        );

        boolean saved = dao.addTransaction(transaction);

        if (saved) {
            System.out.println("Transaction saved to MySQL!");
        } else {
            System.out.println("Transaction could not be saved.");
        }
    }
}
