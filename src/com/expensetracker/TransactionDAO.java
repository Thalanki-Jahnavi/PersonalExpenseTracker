
package com.expensetracker;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.Date;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TransactionDAO {

    // 1. Add a transaction
    public boolean addTransaction(Transaction transaction) {
        String sql = "INSERT INTO transactions " +
                "(type, category, amount, description, transaction_date) " +
                "VALUES (?, ?, ?, ?, ?)";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, transaction.getType());
            statement.setString(2, transaction.getCategory());
            statement.setBigDecimal(3, transaction.getAmount());
            statement.setString(4, transaction.getDescription());
            statement.setDate(5, Date.valueOf(transaction.getDate()));

            return statement.executeUpdate() > 0;

        } catch (SQLException e) {
            System.out.println("Error saving transaction: " + e.getMessage());
            return false;
        }
    }

    // 2. View all transactions
    public List<Transaction> getAllTransactions() {
        List<Transaction> transactions = new ArrayList<>();

        String sql = "SELECT * FROM transactions ORDER BY transaction_date DESC, id DESC";

        try (Connection connection = DBConnection.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {

            while (resultSet.next()) {
                Transaction transaction = new Transaction(
                        resultSet.getInt("id"),
                        resultSet.getString("type"),
                        resultSet.getString("category"),
                        resultSet.getBigDecimal("amount"),
                        resultSet.getString("description"),
                        resultSet.getDate("transaction_date").toLocalDate()
                );

                transactions.add(transaction);
            }

        } catch (SQLException e) {
            System.out.println("Error retrieving transactions: " + e.getMessage());
        }

        return transactions;
    }

    // 3. Calculate total income or expenses
    public BigDecimal getTotal(String type) {
        String sql = "SELECT COALESCE(SUM(amount), 0) AS total " +
                "FROM transactions WHERE type = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, type);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getBigDecimal("total");
                }
            }

        } catch (SQLException e) {
            System.out.println("Error calculating total: " + e.getMessage());
        }

        return BigDecimal.ZERO;
    }

    // 4. Original update method for the console application
    public boolean updateTransaction(int id, String category,
                                     BigDecimal amount, String description) {
        String sql = "UPDATE transactions " +
                "SET category = ?, amount = ?, description = ? WHERE id = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, category);
            statement.setBigDecimal(2, amount);
            statement.setString(3, description);
            statement.setInt(4, id);

            return statement.executeUpdate() > 0;

        } catch (SQLException e) {
            System.out.println("Error updating transaction: " + e.getMessage());
            return false;
        }
    }

    // 5. Full update method for the web application
    public boolean updateTransaction(Transaction transaction) {
        String sql = "UPDATE transactions SET type = ?, category = ?, " +
                "amount = ?, description = ?, transaction_date = ? WHERE id = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, transaction.getType());
            statement.setString(2, transaction.getCategory());
            statement.setBigDecimal(3, transaction.getAmount());
            statement.setString(4, transaction.getDescription());
            statement.setDate(5, Date.valueOf(transaction.getDate()));
            statement.setInt(6, transaction.getId());

            return statement.executeUpdate() > 0;

        } catch (SQLException e) {
            System.out.println("Error updating transaction: " + e.getMessage());
            return false;
        }
    }

    // 6. Delete a transaction
    public boolean deleteTransaction(int id) {
        String sql = "DELETE FROM transactions WHERE id = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, id);
            return statement.executeUpdate() > 0;

        } catch (SQLException e) {
            System.out.println("Error deleting transaction: " + e.getMessage());
            return false;
        }
    }

    // 7. Calculate monthly income or expenses
    public BigDecimal getMonthlyTotal(String type, YearMonth month) {
        String sql = "SELECT COALESCE(SUM(amount), 0) AS total " +
                "FROM transactions WHERE type = ? " +
                "AND transaction_date >= ? AND transaction_date < ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, type);
            statement.setDate(2, Date.valueOf(month.atDay(1)));
            statement.setDate(3, Date.valueOf(month.plusMonths(1).atDay(1)));

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getBigDecimal("total");
                }
            }

        } catch (SQLException e) {
            System.out.println("Error calculating monthly total: " + e.getMessage());
        }

        return BigDecimal.ZERO;
    }

    // 8. Calculate expense totals for each category in a month
    public Map<String, BigDecimal> getCategoryExpenses(YearMonth month) {
        Map<String, BigDecimal> categoryTotals = new LinkedHashMap<>();

        String sql = "SELECT category, SUM(amount) AS total " +
                "FROM transactions WHERE type = 'EXPENSE' " +
                "AND transaction_date >= ? AND transaction_date < ? " +
                "GROUP BY category ORDER BY category";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setDate(1, Date.valueOf(month.atDay(1)));
            statement.setDate(2, Date.valueOf(month.plusMonths(1).atDay(1)));

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    categoryTotals.put(
                            resultSet.getString("category"),
                            resultSet.getBigDecimal("total")
                    );
                }
            }

        } catch (SQLException e) {
            System.out.println("Error retrieving category report: " + e.getMessage());
        }

        return categoryTotals;
    }
}
