
package com.expensetracker;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExpenseHttpServer {

    private static final int PORT = 8080;
    private static final TransactionDAO dao = new TransactionDAO();
    private static Path webRoot;

    public static void main(String[] args) throws IOException {
        webRoot = findWebRoot();

        HttpServer server = HttpServer.create(
                new InetSocketAddress("127.0.0.1", PORT), 0);

        server.createContext("/api/transactions", ExpenseHttpServer::handleTransactions);
        server.createContext("/", ExpenseHttpServer::serveStaticFiles);

        server.start();

        System.out.println("Expense Tracker web server is running.");
        System.out.println("Open http://localhost:" + PORT);
        System.out.println("Serving files from: " + webRoot.toAbsolutePath());
    }

    private static Path findWebRoot() {
        Path first = Path.of("Web", "src");
        Path second = Path.of("Web");

        if (Files.isDirectory(first) && Files.exists(first.resolve("index.html"))) {
            return first.toAbsolutePath().normalize();
        }

        if (Files.isDirectory(second) && Files.exists(second.resolve("index.html"))) {
            return second.toAbsolutePath().normalize();
        }

        throw new IllegalStateException(
                "Cannot find index.html. Put index.html, style.css, and script.js " +
                        "inside the Web/src folder (or Web folder).");
    }

    private static void handleTransactions(HttpExchange exchange) throws IOException {
        try {
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();
            String basePath = "/api/transactions";

            if ("GET".equals(method) && basePath.equals(path)) {
                sendJson(exchange, 200, transactionsToJson(dao.getAllTransactions()));
                return;
            }

            if ("POST".equals(method) && basePath.equals(path)) {
                Map<String, String> form = readForm(exchange);
                Transaction transaction = transactionFromForm(form, 0);

                boolean saved = dao.addTransaction(transaction);

                if (saved) {
                    sendJson(exchange, 201, "{\"message\":\"Transaction saved\"}");
                } else {
                    sendError(exchange, 500, "Could not save transaction.");
                }
                return;
            }

            if (path.startsWith(basePath + "/")) {
                String idText = path.substring((basePath + "/").length());

                if (idText.isBlank() || idText.contains("/")) {
                    sendError(exchange, 400, "Invalid transaction ID.");
                    return;
                }

                int id;
                try {
                    id = Integer.parseInt(idText);
                    if (id <= 0) {
                        throw new NumberFormatException();
                    }
                } catch (NumberFormatException e) {
                    sendError(exchange, 400, "Invalid transaction ID.");
                    return;
                }

                if ("PUT".equals(method)) {
                    Map<String, String> form = readForm(exchange);
                    Transaction transaction = transactionFromForm(form, id);

                    if (dao.updateTransaction(transaction)) {
                        sendJson(exchange, 200, "{\"message\":\"Transaction updated\"}");
                    } else {
                        sendError(exchange, 404,
                                "Transaction not found or could not be updated.");
                    }
                    return;
                }

                if ("DELETE".equals(method)) {
                    if (dao.deleteTransaction(id)) {
                        sendJson(exchange, 200, "{\"message\":\"Transaction deleted\"}");
                    } else {
                        sendError(exchange, 404, "Transaction not found.");
                    }
                    return;
                }
            }

            sendError(exchange, 405, "Unsupported API request.");

        } catch (IllegalArgumentException e) {
            sendError(exchange, 400, e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            sendError(exchange, 500, "Server error. Check the IntelliJ Run console.");
        } finally {
            exchange.close();
        }
    }

    private static Transaction transactionFromForm(
            Map<String, String> form, int id) {

        String type = required(form, "type").toUpperCase();
        String category = required(form, "category");
        String description = required(form, "description");
        String amountText = required(form, "amount");
        String dateText = required(form, "date");

        if (!type.equals("INCOME") && !type.equals("EXPENSE")) {
            throw new IllegalArgumentException(
                    "Type must be INCOME or EXPENSE.");
        }

        if (category.length() > 50) {
            throw new IllegalArgumentException(
                    "Category must be 50 characters or fewer.");
        }

        if (description.length() > 255) {
            throw new IllegalArgumentException(
                    "Description must be 255 characters or fewer.");
        }

        BigDecimal amount;
        LocalDate date;

        try {
            amount = new BigDecimal(amountText);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Enter a valid amount.");
        }

        if (amount.compareTo(BigDecimal.ZERO) <= 0 ||
                amount.precision() - amount.scale() > 10 ||
                amount.scale() > 2) {
            throw new IllegalArgumentException(
                    "Amount must be positive and have at most two decimal places.");
        }

        try {
            date = LocalDate.parse(dateText);
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Enter a valid date in YYYY-MM-DD format.");
        }

        return new Transaction(
                id, type, category, amount, description, date);
    }

    private static String required(Map<String, String> form, String key) {
        String value = form.get(key);

        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "The field '" + key + "' is required.");
        }

        return value.trim();
    }

    private static Map<String, String> readForm(HttpExchange exchange)
            throws IOException {

        String contentType = exchange.getRequestHeaders()
                .getFirst("Content-Type");

        if (contentType == null ||
                !contentType.toLowerCase().startsWith(
                        "application/x-www-form-urlencoded")) {
            throw new IllegalArgumentException(
                    "Expected form-encoded data.");
        }

        String body;
        try (InputStream input = exchange.getRequestBody()) {
            body = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }

        Map<String, String> form = new HashMap<>();

        if (body.isBlank()) {
            return form;
        }

        for (String pair : body.split("&")) {
            String[] parts = pair.split("=", 2);

            String key = URLDecoder.decode(parts[0], StandardCharsets.UTF_8);
            String value = parts.length > 1
                    ? URLDecoder.decode(parts[1], StandardCharsets.UTF_8)
                    : "";

            form.put(key, value);
        }

        return form;
    }

    private static String transactionsToJson(List<Transaction> transactions) {
        StringBuilder json = new StringBuilder("[");
        boolean first = true;

        for (Transaction transaction : transactions) {
            if (!first) {
                json.append(",");
            }
            first = false;

            json.append("{")
                    .append("\"id\":").append(transaction.getId()).append(",")
                    .append("\"type\":\"").append(escapeJson(transaction.getType())).append("\",")
                    .append("\"category\":\"").append(escapeJson(transaction.getCategory())).append("\",")
                    .append("\"amount\":").append(transaction.getAmount().toPlainString()).append(",")
                    .append("\"description\":\"").append(escapeJson(transaction.getDescription())).append("\",")
                    .append("\"date\":\"").append(transaction.getDate()).append("\"")
                    .append("}");
        }

        return json.append("]").toString();
    }

    private static String escapeJson(String value) {
        StringBuilder escaped = new StringBuilder();

        for (char c : value.toCharArray()) {
            switch (c) {
                case '"' -> escaped.append("\\\"");
                case '\\' -> escaped.append("\\\\");
                case '\b' -> escaped.append("\\b");
                case '\f' -> escaped.append("\\f");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                default -> {
                    if (c < 0x20) {
                        escaped.append(String.format("\\u%04x", (int) c));
                    } else {
                        escaped.append(c);
                    }
                }
            }
        }

        return escaped.toString();
    }

    private static void serveStaticFiles(HttpExchange exchange)
            throws IOException {

        try {
            if (!"GET".equals(exchange.getRequestMethod())) {
                sendError(exchange, 405, "Method not allowed.");
                return;
            }

            String requestPath = exchange.getRequestURI().getPath();

            if (requestPath.equals("/")) {
                requestPath = "/index.html";
            }

            Path file = webRoot.resolve(requestPath.substring(1))
                    .normalize();

            if (!file.startsWith(webRoot) || !Files.isRegularFile(file)) {
                sendError(exchange, 404, "File not found.");
                return;
            }

            String contentType = contentType(file);
            byte[] bytes = Files.readAllBytes(file);

            exchange.getResponseHeaders().set("Content-Type", contentType);
            exchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
            exchange.sendResponseHeaders(200, bytes.length);

            try (OutputStream output = exchange.getResponseBody()) {
                output.write(bytes);
            }

        } finally {
            exchange.close();
        }
    }

    private static String contentType(Path file) {
        String name = file.getFileName().toString().toLowerCase();

        if (name.endsWith(".html")) {
            return "text/html; charset=UTF-8";
        }
        if (name.endsWith(".css")) {
            return "text/css; charset=UTF-8";
        }
        if (name.endsWith(".js")) {
            return "application/javascript; charset=UTF-8";
        }

        return "application/octet-stream";
    }

    private static void sendJson(
            HttpExchange exchange, int status, String json) throws IOException {

        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set(
                "Content-Type", "application/json; charset=UTF-8");

        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.sendResponseHeaders(status, bytes.length);

        try (OutputStream output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }

    private static void sendError(
            HttpExchange exchange, int status, String message) throws IOException {

        String json = "{\"error\":\"" + escapeJson(message) + "\"}";
        sendJson(exchange, status, json);
    }
}
