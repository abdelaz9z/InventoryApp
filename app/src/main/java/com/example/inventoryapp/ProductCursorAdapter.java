package com.example.inventoryapp;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.inventoryapp.data.ProductContract;
import com.example.inventoryapp.data.ProductContract.ProductEntry;

import java.text.NumberFormat;

public class ProductCursorAdapter extends CursorAdapter {

    private static final String TAG = ProductCursorAdapter.class.getSimpleName();

    ProductCursorAdapter(Context context, Cursor c) {
        super(context, c, 0);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return LayoutInflater.from(context).inflate(R.layout.list_item_product, parent, false);
    }

    @Override
    public void bindView(View view, final Context context, Cursor cursor) {
        // Find individual views that we want to modify in the list item layout
        ImageView productImageView = view.findViewById(R.id.image_product);
        TextView productNameTextView = view.findViewById(R.id.text_product_name);
        TextView modelNumberTextView = view.findViewById(R.id.text_model_number);
        TextView priceTextView = view.findViewById(R.id.text_price);
        TextView quantityTextView = view.findViewById(R.id.text_quantity);
        ImageButton saleImageButton = view.findViewById(R.id.image_button_sale);

        // Find the columns of product attributes that we're interested in
        final int idColumnIndex = cursor.getColumnIndex(ProductEntry._ID);
        int productImageColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_IMAGE);
        int productNameColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_NAME);
        int modelNumberColumnIndex = cursor.getColumnIndex(ProductContract.ProductEntry.COLUMN_MODEL_NO);
        int priceColumnIndex = cursor.getColumnIndex(ProductContract.ProductEntry.COLUMN_PRICE);
        final int quantityColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_QUANTITY);

        final int id = cursor.getInt(idColumnIndex);
        String productImage = cursor.getString(productImageColumnIndex);
        String productName = cursor.getString(productNameColumnIndex);
        String modelNumber = cursor.getString(modelNumberColumnIndex);
        int price = cursor.getInt(priceColumnIndex);
        final int quantity = cursor.getInt(quantityColumnIndex);

        Uri productImageUri = Uri.parse(productImage);

        if (TextUtils.isEmpty(modelNumber)) {
            modelNumberTextView.setVisibility(View.GONE);
        }

        productImageView.setImageURI(productImageUri);
        productNameTextView.setText(productName);
        modelNumberTextView.setText(modelNumber);
        priceTextView.setText(NumberFormat.getCurrencyInstance().format(price));

        String q = String.valueOf(quantity);
        Spannable spannable = new SpannableString("QTY " + q);
        spannable.setSpan(new ForegroundColorSpan(Color.parseColor("#757575")), 0, "QTY ".length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannable.setSpan(new ForegroundColorSpan(Color.parseColor("#BDBDBD")), "QTY ".length(), "QTY ".length() + q.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        quantityTextView.setText(spannable);

        saleImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Uri productUri = ContentUris.withAppendedId(ProductEntry.CONTENT_URI, id);
                adjustProductQuantity(context, productUri, quantity);
            }
        });
    }

    /**
     * This method reduced product stock by 1
     *
     * @param context                - Activity context
     * @param productUri             - Uri used to update the stock of a specific product in the ListView
     * @param currentQuantityInStock - current stock of that specific product
     */
    private void adjustProductQuantity(Context context, Uri productUri, int currentQuantityInStock) {

        // Subtract 1 from current value if quantity of product >= 1
        int newQuantityValue = (currentQuantityInStock >= 1) ? currentQuantityInStock - 1 : 0;

        if (currentQuantityInStock == 0) {
            Toast.makeText(context.getApplicationContext(), "Product is out of stock!", Toast.LENGTH_SHORT).show();
        }

        // Update table by using new value of quantity
        ContentValues contentValues = new ContentValues();
        contentValues.put(ProductEntry.COLUMN_QUANTITY, newQuantityValue);
        int numRowsUpdated = context.getContentResolver().update(productUri, contentValues, null, null);
        if (numRowsUpdated > 0) {
            // Show error message in Logs with info about pass update.
            Log.i(TAG, "Item has been sold");
        } else {
            Toast.makeText(context.getApplicationContext(), "No available product in stock", Toast.LENGTH_SHORT).show();
            // Show error message in Logs with info about fail update.
            Log.e(TAG, "Issue with upload value of quantity");
        }


    }
}
