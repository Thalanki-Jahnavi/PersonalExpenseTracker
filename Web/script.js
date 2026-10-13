
let transactions = [];
let editingId = null;
let refreshInProgress = false;

const API_URL = "/api/transactions";

const formSection = document.getElementById("formSection");
const transactionForm = document.getElementById("transactionForm");
const transactionTable = document.getElementById("transactionTable");
const searchInput = document.getElementById("searchInput");

const typeInput = document.getElementById("type");
const categoryInput = document.getElementById("category");
const amountInput = document.getElementById("amount");
const dateInput = document.getElementById("date");
const descriptionInput = document.getElementById("description");

const saveButton = document.getElementById("saveButton");
const formTitle = document.getElementById("formTitle");
const emptyMessage = document.getElementById("emptyMessage");

// Format an amount as Indian currency.
function formatMoney(amount) {
    return new Intl.NumberFormat("en-IN", {
        style: "currency",
        currency: "INR"
    }).format(Number(amount));
}

// Escape text before inserting it into HTML.
function escapeHTML(value) {
    return String(value).replace(/[&<>"']/g, character => {
        const entities = {
            "&": "&amp;",
            "<": "&lt;",
            ">": "&gt;",
            '"': "&quot;",
            "'": "&#39;"
        };

        return entities[character];
    });
}

// Send a request to the Java HTTP server.
async function apiRequest(url, options = {}) {
    const response = await fetch(url, {
        cache: "no-store",
        ...options
    });

    const responseText = await response.text();

    let result = {};

    if (responseText) {
        try {
            result = JSON.parse(responseText);
        } catch {
            throw new Error(
                "Invalid server response. Check whether the Java server is running."
            );
        }
    }

    if (!response.ok) {
        throw new Error(
            result.error || result.message || `Request failed: ${response.status}`
        );
    }

    return result;
}

// Load actual transactions from MySQL through Java.
async function loadTransactions(showError = true) {
    if (refreshInProgress) {
        return;
    }

    refreshInProgress = true;

    try {
        const data = await apiRequest(API_URL);

        if (!Array.isArray(data)) {
            throw new Error("The server did not return a transaction list.");
        }

        transactions = data;
        refreshDashboard();
    } catch (error) {
        console.error("Unable to load transactions:", error);

        if (showError) {
            alert(
                "Unable to load transactions.\n\n" +
                error.message +
                "\n\nMake sure ExpenseHttpServer.java is running and open the page at http://localhost:8080."
            );
        }
    } finally {
        refreshInProgress = false;
    }
}

// Calculate income, expenses and balance from database records.
function updateSummary() {
    const totalIncome = transactions
        .filter(transaction => transaction.type === "INCOME")
        .reduce(
            (total, transaction) => total + Number(transaction.amount),
            0
        );

    const totalExpenses = transactions
        .filter(transaction => transaction.type === "EXPENSE")
        .reduce(
            (total, transaction) => total + Number(transaction.amount),
            0
        );

    const balance = totalIncome - totalExpenses;

    document.getElementById("totalIncome").textContent =
        formatMoney(totalIncome);

    document.getElementById("totalExpenses").textContent =
        formatMoney(totalExpenses);

    document.getElementById("balance").textContent =
        formatMoney(balance);
}

// Display transactions in the dashboard table.
function renderTransactions() {
    const searchText = searchInput.value.trim().toLowerCase();

    const filteredTransactions = transactions.filter(transaction => {
        return (
            String(transaction.category).toLowerCase().includes(searchText) ||
            String(transaction.description).toLowerCase().includes(searchText) ||
            String(transaction.type).toLowerCase().includes(searchText) ||
            String(transaction.date).includes(searchText)
        );
    });

    transactionTable.innerHTML = filteredTransactions.map(transaction => {
        const isIncome = transaction.type === "INCOME";
        const amount = Number(transaction.amount);

        return `
            <tr>
                <td>${escapeHTML(transaction.date)}</td>

                <td>${escapeHTML(transaction.category)}</td>

                <td>${escapeHTML(transaction.description)}</td>

                <td>
                    <span class="type-badge ${
                        isIncome ? "income-badge" : "expense-badge"
                    }">
                        ${isIncome ? "Income" : "Expense"}
                    </span>
                </td>

                <td class="${
                    isIncome ? "income-amount" : "expense-amount"
                }">
                    ${isIncome ? "+" : "-"}${formatMoney(amount)}
                </td>

                <td>
                    <button
                        class="action-button edit-button"
                        data-action="edit"
                        data-id="${transaction.id}">
                        Edit
                    </button>

                    <button
                        class="action-button delete-button"
                        data-action="delete"
                        data-id="${transaction.id}">
                        Delete
                    </button>
                </td>
            </tr>
        `;
    }).join("");

    emptyMessage.style.display =
        filteredTransactions.length === 0 ? "block" : "none";
}

// Refresh the summary and table together.
function refreshDashboard() {
    updateSummary();
    renderTransactions();
}

// Show the transaction form.
function openForm() {
    formSection.style.display = "block";

    formSection.scrollIntoView({
        behavior: "smooth",
        block: "start"
    });
}

// Reset the form and close it.
function resetForm() {
    transactionForm.reset();

    editingId = null;

    formTitle.textContent = "Add Transaction";
    saveButton.textContent = "Save Transaction";
    typeInput.value = "EXPENSE";

    dateInput.value = new Date().toLocaleDateString("en-CA");

    formSection.style.display = "none";
}

// Open the form for a new transaction.
document.getElementById("addButton").addEventListener("click", () => {
    resetForm();
    openForm();
});

// Cancel adding or editing.
document.getElementById("cancelButton").addEventListener("click", () => {
    resetForm();
});

// Save a new transaction or update an existing one in MySQL.
transactionForm.addEventListener("submit", async event => {
    event.preventDefault();

    const wasEditing = editingId !== null;

    const type = typeInput.value;
    const category = categoryInput.value.trim();
    const amount = Number(amountInput.value);
    const date = dateInput.value;
    const description = descriptionInput.value.trim();

    if (
        !category ||
        !description ||
        !date ||
        !Number.isFinite(amount) ||
        amount <= 0
    ) {
        alert("Please enter valid transaction details.");
        return;
    }

    if (category.length > 50 || description.length > 255) {
        alert(
            "Category must be at most 50 characters and description at most 255 characters."
        );
        return;
    }

    const formData = new URLSearchParams({
        type: type,
        category: category,
        amount: String(amount),
        date: date,
        description: description
    });

    saveButton.disabled = true;

    try {
        if (wasEditing) {
            await apiRequest(`${API_URL}/${editingId}`, {
                method: "PUT",
                headers: {
                    "Content-Type": "application/x-www-form-urlencoded"
                },
                body: formData.toString()
            });
        } else {
            await apiRequest(API_URL, {
                method: "POST",
                headers: {
                    "Content-Type": "application/x-www-form-urlencoded"
                },
                body: formData.toString()
            });
        }

        resetForm();

        // Reload records from MySQL after the change.
        await loadTransactions(false);

        alert(
            wasEditing
                ? "Transaction updated successfully!"
                : "Transaction saved successfully!"
        );
    } catch (error) {
        console.error("Saving transaction failed:", error);

        alert("Could not save transaction.\n\n" + error.message);
    } finally {
        saveButton.disabled = false;
    }
});

// Handle Edit and Delete buttons.
transactionTable.addEventListener("click", async event => {
    const button = event.target.closest("button[data-action]");

    if (!button) {
        return;
    }

    const id = Number(button.dataset.id);
    const action = button.dataset.action;

    const transaction = transactions.find(item => item.id === id);

    if (!transaction) {
        alert("Transaction not found. Reloading data.");
        await loadTransactions();
        return;
    }

    // Edit an existing transaction.
    if (action === "edit") {
        editingId = id;

        formTitle.textContent = "Edit Transaction";
        saveButton.textContent = "Update Transaction";

        typeInput.value = transaction.type;
        categoryInput.value = transaction.category;
        amountInput.value = transaction.amount;
        dateInput.value = transaction.date;
        descriptionInput.value = transaction.description;

        openForm();
    }

    // Delete a transaction from MySQL.
    if (action === "delete") {
        const confirmed = confirm(
            "Are you sure you want to permanently delete this transaction?"
        );

        if (!confirmed) {
            return;
        }

        button.disabled = true;

        try {
            await apiRequest(`${API_URL}/${id}`, {
                method: "DELETE"
            });

            await loadTransactions(false);

            alert("Transaction deleted successfully!");
        } catch (error) {
            console.error("Deleting transaction failed:", error);

            alert("Could not delete transaction.\n\n" + error.message);
        } finally {
            button.disabled = false;
        }
    }
});

// Search the currently loaded database records.
searchInput.addEventListener("input", renderTransactions);

// Load data when the page opens.
loadTransactions();

// Check for changes made in other browser sessions every 10 seconds.
setInterval(() => {
    if (
        document.visibilityState === "visible" &&
        formSection.style.display === "none"
    ) {
        loadTransactions(false);
    }
}, 10000);
