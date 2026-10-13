
package com.expensetracker;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) {

        Scanner sc = new Scanner(System.in);
        TransactionDAO dao = new TransactionDAO();

        boolean running = true;

        while (running) {

            System.out.println("\n===== PERSONAL EXPENSE TRACKER =====");
            System.out.println("1. Add Transaction");
            System.out.println("2. View All Transactions");
            System.out.println("3. View Total Income");
            System.out.println("4. View Total Expenses");
            System.out.println("5. View Balance");
            System.out.println("6. Update Transaction");
            System.out.println("7. Delete Transaction");
            System.out.println("8. Exit");
            System.out.println("9. Monthly Report");
            System.out.println("10. Category-wise Expense Report");
            System.out.print("Enter your choice: ");

            String choice = sc.nextLine().trim();

            switch (choice) {

                case "1": {
                    System.out.println("1. Income");
                    System.out.println("2. Expense");
                    System.out.print("Choose transaction type: ");

                    String typeChoice = sc.nextLine().trim();
                    String type;

                    if (typeChoice.equals("1")) {
                        type = "INCOME";
                    } else if (typeChoice.equals("2")) {
                        type = "EXPENSE";
                    } else {
                        System.out.println("Invalid transaction type!");
                        break;
                    }

                    System.out.print("Enter category: ");
                    String category = sc.nextLine().trim();

                    if (category.isEmpty()) {
                        System.out.println("Category cannot be empty!");
                        break;
                    }

                    System.out.print("Enter amount: ");
                    BigDecimal amount;

                    try {
                        amount = new BigDecimal(sc.nextLine().trim());

                        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                            System.out.println(
                                    "Amount must be greater than zero!");
                            break;
                        }
                    } catch (NumberFormatException e) {
                        System.out.println("Please enter a valid amount!");
                        break;
                    }

                    System.out.print("Enter description: ");
                    String description = sc.nextLine().trim();

                    if (description.isEmpty()) {
                        System.out.println(
                                "Description cannot be empty!");
                        break;
                    }

                    Transaction transaction = new Transaction(
                            0, type, category, amount,
                            description, LocalDate.now()
                    );

                    if (dao.addTransaction(transaction)) {
                        System.out.println(
                                "Transaction saved successfully!");
                    } else {
                        System.out.println(
                                "Failed to save transaction.");
                    }
                    break;
                }

                case "2": {
                    List<Transaction> transactions =
                            dao.getAllTransactions();

                    if (transactions.isEmpty()) {
                        System.out.println("No transactions found.");
                    } else {
                        for (Transaction t : transactions) {
                            System.out.println("\nID: " + t.getId());
                            System.out.println("Type: " + t.getType());
                            System.out.println(
                                    "Category: " + t.getCategory());
                            System.out.println(
                                    "Amount: Rs. " + t.getAmount());
                            System.out.println(
                                    "Description: " + t.getDescription());
                            System.out.println("Date: " + t.getDate());
                        }
                    }
                    break;
                }

                case "3":
                    System.out.println("Total Income: Rs. "
                            + dao.getTotal("INCOME"));
                    break;

                case "4":
                    System.out.println("Total Expenses: Rs. "
                            + dao.getTotal("EXPENSE"));
                    break;

                case "5": {
                    BigDecimal income = dao.getTotal("INCOME");
                    BigDecimal expenses = dao.getTotal("EXPENSE");

                    System.out.println("Total Income: Rs. " + income);
                    System.out.println("Total Expenses: Rs. " + expenses);
                    System.out.println("Remaining Balance: Rs. "
                            + income.subtract(expenses));
                    break;
                }

                case "6": {
                    System.out.print("Enter transaction ID to update: ");
                    int updateId;

                    try {
                        updateId = Integer.parseInt(
                                sc.nextLine().trim());

                        if (updateId <= 0) {
                            System.out.println("Invalid transaction ID!");
                            break;
                        }
                    } catch (NumberFormatException e) {
                        System.out.println("Invalid transaction ID!");
                        break;
                    }

                    System.out.print("Enter new category: ");
                    String newCategory = sc.nextLine().trim();

                    System.out.print("Enter new amount: ");
                    BigDecimal newAmount;

                    try {
                        newAmount = new BigDecimal(
                                sc.nextLine().trim());

                        if (newAmount.compareTo(BigDecimal.ZERO) <= 0) {
                            System.out.println(
                                    "Amount must be greater than zero!");
                            break;
                        }
                    } catch (NumberFormatException e) {
                        System.out.println("Invalid amount!");
                        break;
                    }

                    System.out.print("Enter new description: ");
                    String newDescription = sc.nextLine().trim();

                    if (newCategory.isEmpty()
                            || newDescription.isEmpty()) {
                        System.out.println(
                                "Category and description cannot be empty!");
                        break;
                    }

                    if (dao.updateTransaction(
                            updateId, newCategory,
                            newAmount, newDescription)) {
                        System.out.println(
                                "Transaction updated successfully!");
                    } else {
                        System.out.println(
                                "Transaction not found or update failed.");
                    }
                    break;
                }

                case "7": {
                    System.out.print("Enter transaction ID to delete: ");
                    int deleteId;

                    try {
                        deleteId = Integer.parseInt(
                                sc.nextLine().trim());

                        if (deleteId <= 0) {
                            System.out.println("Invalid transaction ID!");
                            break;
                        }
                    } catch (NumberFormatException e) {
                        System.out.println("Invalid transaction ID!");
                        break;
                    }

                    System.out.print(
                            "Are you sure? Enter Y to confirm: ");
                    String confirmation = sc.nextLine().trim();

                    if (confirmation.equalsIgnoreCase("Y")) {
                        if (dao.deleteTransaction(deleteId)) {
                            System.out.println(
                                    "Transaction deleted successfully!");
                        } else {
                            System.out.println(
                                    "Transaction not found or deletion failed.");
                        }
                    } else {
                        System.out.println("Deletion cancelled.");
                    }
                    break;
                }

                case "8":
                    running = false;
                    System.out.println(
                            "Thank you for using Expense Tracker!");
                    break;

                case "9":
                    showMonthlyReport(sc, dao);
                    break;

                case "10":
                    showCategoryReport(sc, dao);
                    break;

                default:
                    System.out.println(
                            "Invalid choice! Please select 1-10.");
            }
        }

        sc.close();
    }

    // Display income, expenses and balance for a selected month
    private static void showMonthlyReport(
            Scanner sc, TransactionDAO dao) {

        YearMonth month = readMonth(sc);

        if (month == null) {
            return;
        }

        BigDecimal income =
                dao.getMonthlyTotal("INCOME", month);

        BigDecimal expenses =
                dao.getMonthlyTotal("EXPENSE", month);

        System.out.println("\n===== MONTHLY REPORT =====");
        System.out.println("Month: " + month);
        System.out.println("Total Income: Rs. " + income);
        System.out.println("Total Expenses: Rs. " + expenses);
        System.out.println("Monthly Balance: Rs. "
                + income.subtract(expenses));
    }

    // Display expense totals grouped by category
    private static void showCategoryReport(
            Scanner sc, TransactionDAO dao) {

        YearMonth month = readMonth(sc);

        if (month == null) {
            return;
        }

        Map<String, BigDecimal> categoryTotals =
                dao.getCategoryExpenses(month);

        System.out.println("\n===== CATEGORY-WISE EXPENSE REPORT =====");
        System.out.println("Month: " + month);

        if (categoryTotals.isEmpty()) {
            System.out.println("No expenses found for this month.");
            return;
        }

        for (Map.Entry<String, BigDecimal> entry
                : categoryTotals.entrySet()) {

            System.out.println(entry.getKey() + ": Rs. "
                    + entry.getValue());
        }
    }

    // Read and validate a month in yyyy-MM format
    private static YearMonth readMonth(Scanner sc) {

        System.out.print("Enter month (yyyy-MM, e.g. 2026-10): ");
        String input = sc.nextLine().trim();

        try {
            return YearMonth.parse(input);
        } catch (DateTimeParseException e) {
            System.out.println(
                    "Invalid month! Use the format yyyy-MM.");
            return null;
        }
    }
}
