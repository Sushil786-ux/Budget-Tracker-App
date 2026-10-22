package com.example.shoppingbudgettrackerapp;

import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.text.NumberFormat;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    EditText etBudget, etItemName, etPrice, etQuantity;

    TextView tvBudget, tvSpent, tvRemaining, tvPercentage;

    Button btnSetBudget, btnAddItem, btnClear;

    ProgressBar progressBudget;

    LinearLayout itemsContainer;

    DatabaseHelper databaseHelper;

    double totalBudget = 0;
    double totalSpent = 0;

    NumberFormat currencyFormat =
            NumberFormat.getCurrencyInstance(new Locale("en", "IN"));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        databaseHelper = new DatabaseHelper(this);

        etBudget = findViewById(R.id.etBudget);
        etItemName = findViewById(R.id.etItemName);
        etPrice = findViewById(R.id.etPrice);
        etQuantity = findViewById(R.id.etQuantity);

        tvBudget = findViewById(R.id.tvBudget);
        tvSpent = findViewById(R.id.tvSpent);
        tvRemaining = findViewById(R.id.tvRemaining);
        tvPercentage = findViewById(R.id.tvPercentage);

        btnSetBudget = findViewById(R.id.btnSetBudget);
        btnAddItem = findViewById(R.id.btnAddItem);
        btnClear = findViewById(R.id.btnClear);

        progressBudget = findViewById(R.id.progressBudget);
        itemsContainer = findViewById(R.id.itemsContainer);

        totalBudget = databaseHelper.getBudget();

        btnSetBudget.setOnClickListener(v -> setBudget());

        btnAddItem.setOnClickListener(v -> addItem());

        btnClear.setOnClickListener(v -> confirmClearAll());

        refreshData();
    }

    private void confirmClearAll() {
        new AlertDialog.Builder(this)
                .setTitle("Clear Shopping List")
                .setMessage(
                        "Are you sure you want to delete all shopping items?"
                )
                .setPositiveButton("Yes, Clear", (dialog, which) -> {

                    databaseHelper.clearAllItems();

                    refreshData();

                    Toast.makeText(
                            MainActivity.this,
                            "Shopping list cleared",
                            Toast.LENGTH_SHORT
                    ).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (databaseHelper != null) {
            databaseHelper.close();
        }
    }

    private void addItem() {
        String name = etItemName.getText().toString().trim();
        String priceText = etPrice.getText().toString().trim();
        String quantityText = etQuantity.getText().toString().trim();

        if (name.isEmpty()) {
            etItemName.setError("Enter item name");
            return;
        }

        if (priceText.isEmpty()) {
            etPrice.setError("Enter item price");
            return;
        }

        if (quantityText.isEmpty()) {
            etQuantity.setError("Enter quantity");
            return;
        }

        try {
            double price = Double.parseDouble(priceText);
            int quantity = Integer.parseInt(quantityText);

            if (!Double.isFinite(price) || price < 0) {
                etPrice.setError("Enter a valid price");
                return;
            }

            if (quantity <= 0) {
                etQuantity.setError("Quantity must be at least 1");
                return;
            }

            if (price > 0 && quantity > Double.MAX_VALUE / price) {
                etPrice.setError("Amount is too large");
                return;
            }

            double itemTotal = price * quantity;

            if (!Double.isFinite(itemTotal)
                    || !Double.isFinite(totalSpent + itemTotal)) {
                etPrice.setError("Total amount is too large");
                return;
            }

            boolean inserted = databaseHelper.addItem(
                    name,
                    price,
                    quantity
            );

            if (inserted) {

                etItemName.setText("");
                etPrice.setText("");
                etQuantity.setText("");

                refreshData();

                Toast.makeText(
                        this,
                        "Item added successfully!",
                        Toast.LENGTH_SHORT
                ).show();

            } else {

                Toast.makeText(
                        this,
                        "Unable to add item",
                        Toast.LENGTH_SHORT
                ).show();
            }

        } catch (NumberFormatException e) {
            Toast.makeText(
                    this,
                    "Enter valid price and quantity",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    private void setBudget() {
        String budgetText = etBudget.getText().toString().trim();

        if (budgetText.isEmpty()) {
            etBudget.setError("Enter your budget");
            return;
        }

        try {
            double budget = Double.parseDouble(budgetText);

            if (!Double.isFinite(budget) || budget < 0) {
                etBudget.setError("Enter a valid budget");
                return;
            }

            totalBudget = budget;

            databaseHelper.setBudget(totalBudget);

            etBudget.setText("");

            updateSummary();

            Toast.makeText(
                    this,
                    "Budget saved successfully!",
                    Toast.LENGTH_SHORT
            ).show();

        } catch (NumberFormatException e) {
            etBudget.setError("Enter a valid amount");
        }
    }

    private void updateSummary() {
        double remaining = totalBudget - totalSpent;

        tvBudget.setText(currencyFormat.format(totalBudget));

        tvSpent.setText(currencyFormat.format(totalSpent));

        tvRemaining.setText(currencyFormat.format(remaining));

        if (remaining < 0) {
            tvRemaining.setTextColor(Color.RED);
        } else {
            tvRemaining.setTextColor(Color.rgb(21, 128, 61));
        }

        int percentage = 0;

        if (totalBudget > 0) {

            double used = (totalSpent / totalBudget) * 100;

            percentage = (int) Math.min(100, Math.round(used));

            tvPercentage.setText(
                    String.format(
                            Locale.getDefault(),
                            "%.1f%% budget used",
                            used
                    )
            );

            if (used > 100) {
                tvPercentage.setText(
                        String.format(
                                Locale.getDefault(),
                                "%.1f%% budget used — Budget exceeded!",
                                used
                        )
                );
            }

        } else {

            tvPercentage.setText(
                    totalSpent > 0
                            ? "No budget set — add a budget to track usage"
                            : "0% budget used"
            );
        }

        progressBudget.setProgress(percentage);

        if (totalBudget > 0 && totalSpent > totalBudget) {
            progressBudget.setProgressTintList(
                    android.content.res.ColorStateList.valueOf(Color.RED)
            );
        } else {
            progressBudget.setProgressTintList(
                    android.content.res.ColorStateList.valueOf(
                            Color.rgb(22, 163, 74)
                    )
            );
        }
    }

    private void refreshData() {
        itemsContainer.removeAllViews();

        totalSpent = 0;

        Cursor cursor = databaseHelper.getAllItems();

        try {

            while (cursor.moveToNext()) {

                int id = cursor.getInt(0);
                String name = cursor.getString(1);
                double price = cursor.getDouble(2);
                int quantity = cursor.getInt(3);

                double itemTotal = price * quantity;

                totalSpent += itemTotal;

                addItemView(id, name, price, quantity, itemTotal);
            }

        } finally {
            cursor.close();
        }

        updateSummary();
    }

    private void addItemView(
            int id,
            String name,
            double price,
            int quantity,
            double itemTotal) {

        LinearLayout card = new LinearLayout(this);

        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(16, 14, 16, 14);
        card.setBackgroundColor(Color.WHITE);

        LinearLayout.LayoutParams cardParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        cardParams.setMargins(0, 0, 0, 12);

        itemsContainer.addView(card, cardParams);

        TextView tvName = new TextView(this);

        tvName.setText(name);
        tvName.setTextSize(18);
        tvName.setTextColor(Color.rgb(23, 37, 84));
        tvName.setTypeface(null, Typeface.BOLD);

        card.addView(tvName);

        TextView tvDetails = new TextView(this);

        tvDetails.setText(
                "Price: " + currencyFormat.format(price) +
                        "\nQuantity: " + quantity +
                        "\nTotal: " + currencyFormat.format(itemTotal)
        );

        tvDetails.setTextSize(15);
        tvDetails.setTextColor(Color.DKGRAY);
        tvDetails.setPadding(0, 8, 0, 8);

        card.addView(tvDetails);

        Button btnDelete = new Button(this);

        btnDelete.setText("Delete Item");
        btnDelete.setTextColor(Color.WHITE);
        btnDelete.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(
                        Color.rgb(220, 38, 38)
                )
        );

        LinearLayout.LayoutParams buttonParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        buttonParams.gravity = Gravity.END;

        card.addView(btnDelete, buttonParams);

        btnDelete.setOnClickListener(v -> {

            new AlertDialog.Builder(MainActivity.this)
                    .setTitle("Delete Item")
                    .setMessage("Do you want to delete " + name + "?")
                    .setPositiveButton("Delete", (dialog, which) -> {

                        boolean deleted =
                                databaseHelper.deleteItem(id);

                        if (deleted) {
                            refreshData();

                            Toast.makeText(
                                    MainActivity.this,
                                    "Item deleted",
                                    Toast.LENGTH_SHORT
                            ).show();
                        } else {
                            Toast.makeText(
                                    MainActivity.this,
                                    "Unable to delete item",
                                    Toast.LENGTH_SHORT
                            ).show();
                        }

                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });
        
    }
}