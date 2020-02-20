package com.example.inventoryapp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NavUtils;
import androidx.core.content.ContextCompat;

import com.example.inventoryapp.data.ProductContract;
import com.example.inventoryapp.data.ProductContract.ProductEntry;


public class EditorActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final int EXISTING_INVENTORY_LOADER = 0;
    private int mGender = ProductContract.ProductEntry.GRADE_UNKNOWN;

    private Uri mCurrentProductUri;
    private Uri mImageUri;

    private EditText mProductNameEditText;
    private EditText mModelNoEditText;
    private EditText mPriceEditText;
    private EditText mQuantityEditText;
    private ImageView mProductImageView;
    private TextView mHintPhotoTextView;
    private Spinner mProductGradeSpinner;
    private EditText mSupplierNameEditText;
    private EditText mSupplierPhoneEditText;
    private EditText mSupplierEmailEditText;
    private ImageButton mDecrementImageButton;
    private ImageButton mIncrementImageButton;
    private ImageButton mCallImageButton;
    private ImageButton mEmailImageButton;
    private View emptyCommunication;

    private int mQuantity = 0;

    // BOOLEAN status for required fields,TRUE if these fields have been populated
    private boolean hasAllRequiredValues = false;
    private boolean mProductHasChanged = false;

    /**
     * OnTouchListener that listens for any user touches on a View, implying that they are modifying
     * the emptyCommunication, and we change the mProductHasChanged boolean to true.
     */
    private View.OnTouchListener mTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            mProductHasChanged = true;
            return false;
        }
    };

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor);

        // Examine the intent that was used to launch this activity,
        // in order to figure out if we're creating a new product or editing an existing one.
        Intent intent = getIntent();
        mCurrentProductUri = intent.getData();

        // Find all relevant views that we will need to read user input from
        init();

        // If the intent DOES NOT contain a product content URI, then we know that we are
        // creating a new product.
        if (mCurrentProductUri == null) {
            setTitle("Add inventory");
            mProductImageView.setImageResource(R.drawable.ic_wallpaper);
            mHintPhotoTextView.setText("Tap to add a photo");
            emptyCommunication.setVisibility(View.GONE);

            // Invalidate the options menu, so the "Delete" menu option can be hidden.
            // (It doesn't make sense to delete a product that hasn't been created yet.)
            invalidateOptionsMenu();
        } else {
            setTitle("Edit inventory");
            mHintPhotoTextView.setText("Tap to change a photo");
            emptyCommunication.setVisibility(View.VISIBLE);

            getLoaderManager().initLoader(EXISTING_INVENTORY_LOADER, null, this);
        }

        // Setup OnTouchListeners on all the input fields, so we can determine if the user
        // has touched or modified them. This will let us know if there are unsaved changes
        // or not, if the user tries to leave the editor without saving.
        mProductNameEditText.setOnTouchListener(mTouchListener);
        mModelNoEditText.setOnTouchListener(mTouchListener);
        mPriceEditText.setOnTouchListener(mTouchListener);
        mQuantityEditText.setOnTouchListener(mTouchListener);
        mProductImageView.setOnTouchListener(mTouchListener);
        mProductGradeSpinner.setOnTouchListener(mTouchListener);
        mSupplierNameEditText.setOnTouchListener(mTouchListener);
        mSupplierPhoneEditText.setOnTouchListener(mTouchListener);
        mSupplierEmailEditText.setOnTouchListener(mTouchListener);
        mCallImageButton.setOnTouchListener(mTouchListener);
        mEmailImageButton.setOnTouchListener(mTouchListener);
        mDecrementImageButton.setOnTouchListener(mTouchListener);
        mIncrementImageButton.setOnTouchListener(mTouchListener);

        mDecrementImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mQuantity <= 0) {
                    mQuantity = 1;
                }
                mQuantity--;
                mQuantityEditText.setText(String.valueOf(mQuantity));
            }
        });

        mIncrementImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mQuantity <= 0) {
                    mQuantity = 0;
                }
                mQuantity++;
                mQuantityEditText.setText(String.valueOf(mQuantity));
            }
        });

        mCallImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String phoneNumber = mSupplierPhoneEditText.getText().toString();
                mackPhoneCall(EditorActivity.this, phoneNumber, EditorActivity.this);
            }
        });

        mEmailImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendEmail();
            }
        });

        mProductImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                trySelector();
                mProductHasChanged = true;
            }
        });

        setupSpinner();
    }

    @Override
    public void onBackPressed() {

        if (!mProductHasChanged) {
            super.onBackPressed();
            return;
        }

        DialogInterface.OnClickListener onClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                finish();
            }
        };

        showUnsavedChangesDialog(onClickListener);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_editor, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        if (mCurrentProductUri == null) {
            MenuItem menuItem = menu.findItem(R.id.item_delete);
            menuItem.setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.item_insert:
                saveProduct();
                if (hasAllRequiredValues) {
                    // Exit activity
                    finish();
                }
                break;
            case R.id.item_delete:
                showDeleteConfirmationDialog();
                break;
            case android.R.id.home:
                if (!mProductHasChanged) {
                    NavUtils.navigateUpFromSameTask(EditorActivity.this);
                    break;
                }

                DialogInterface.OnClickListener discardButtonClickListener = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        NavUtils.navigateUpFromSameTask(EditorActivity.this);
                    }
                };
                showUnsavedChangesDialog(discardButtonClickListener);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case 1:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openSelector();
                }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 0 && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                mImageUri = data.getData();

                mProductImageView.setImageURI(mImageUri);
                mProductImageView.invalidate();
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int id, @Nullable Bundle args) {
        // Since the editor shows all product attributes, define a projection that contains
        // all columns from the product table
        String[] projection = {
                ProductContract.ProductEntry.COLUMN_MODEL_NO,
                ProductContract.ProductEntry.COLUMN_PRODUCT_NAME,
                ProductEntry.COLUMN_MODEL_NO,
                ProductContract.ProductEntry.COLUMN_PRICE,
                ProductContract.ProductEntry.COLUMN_QUANTITY,
                ProductEntry.COLUMN_PRODUCT_IMAGE,
                ProductEntry.COLUMN_PRODUCT_GRADE,
                ProductEntry.COLUMN_SUPPLIER_NAME,
                ProductContract.ProductEntry.COLUMN_SUPPLIER_PHONE,
                ProductEntry.COLUMN_SUPPLIER_EMAIL
        };

        // This loader will execute the ContentProvider's query method on a background thread
        return new CursorLoader(this,
                mCurrentProductUri,
                projection,
                null,
                null,
                null
        );
    }

    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor cursor) {

        if (cursor == null || cursor.getCount() < 1) {
            return;
        }

        if (cursor.moveToFirst()) {
            int productNameColumnIndex = cursor.getColumnIndex(ProductContract.ProductEntry.COLUMN_PRODUCT_NAME);
            int modelNoColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_MODEL_NO);
            int priceColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRICE);
            int quantityColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_QUANTITY);
            int productImageColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_IMAGE);
            int productGradeColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_GRADE);
            int supplierNameColumnIndex = cursor.getColumnIndex(ProductContract.ProductEntry.COLUMN_SUPPLIER_NAME);
            int supplierPhoneColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_SUPPLIER_PHONE);
            int supplierEmailColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_SUPPLIER_EMAIL);

            String productNameString = cursor.getString(productNameColumnIndex);
            String modelNoString = cursor.getString(modelNoColumnIndex);
            String priceInt = Integer.toString(cursor.getInt(priceColumnIndex));
            String quantityInt = Integer.toString(cursor.getInt(quantityColumnIndex));
            String productImageString = cursor.getString(productImageColumnIndex);
            int productGradeInt = cursor.getInt(productGradeColumnIndex);
            String supplierNameString = cursor.getString(supplierNameColumnIndex);
            String supplierPhoneInt = Integer.toString(cursor.getInt(supplierPhoneColumnIndex));
            String supplierEmailString = cursor.getString(supplierEmailColumnIndex);
            mImageUri = Uri.parse(productImageString);

            // Update the views on the screen with the values from the database
            mProductNameEditText.setText(productNameString);
            mModelNoEditText.setText(modelNoString);
            mPriceEditText.setText(priceInt);
            mQuantityEditText.setText(quantityInt);
            mProductImageView.setImageURI(mImageUri);
            mSupplierNameEditText.setText(supplierNameString);
            mSupplierPhoneEditText.setText(supplierPhoneInt);
            mSupplierEmailEditText.setText(supplierEmailString);

            // Set product grade
            switch (productGradeInt) {
                case ProductEntry.GRADE_NEW:
                    mProductGradeSpinner.setSelection(ProductEntry.GRADE_NEW);
                    break;
                case ProductEntry.GRADE_USED:
                    mProductGradeSpinner.setSelection(ProductEntry.GRADE_USED);
                    break;
                default:
                    mProductGradeSpinner.setSelection(ProductContract.ProductEntry.GRADE_UNKNOWN);
                    break;
            }
        }

    }

    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> loader) {
        // If the loader is invalidated, clear out all the data from the input fields.
        mProductNameEditText.setText("");
        mModelNoEditText.setText("");
        mPriceEditText.setText("");
        mQuantityEditText.setText("");
        mProductImageView.setImageResource(R.drawable.ic_wallpaper);
        mProductGradeSpinner.setSelection(ProductContract.ProductEntry.GRADE_UNKNOWN);
        mSupplierNameEditText.setText("");
        mSupplierPhoneEditText.setText("");
        mSupplierEmailEditText.setText("");
    }

    private void init() {
        mProductNameEditText = findViewById(R.id.edit_product_name);
        mModelNoEditText = findViewById(R.id.edit_model_no);
        mPriceEditText = findViewById(R.id.edit_price);
        mQuantityEditText = findViewById(R.id.edit_quantity);
        mProductImageView = findViewById(R.id.product_image);
        mHintPhotoTextView = findViewById(R.id.text_hint_photo);
        mProductGradeSpinner = findViewById(R.id.spinner_product_grade);
        mSupplierNameEditText = findViewById(R.id.edit_supplier_name);
        mSupplierPhoneEditText = findViewById(R.id.edit_supplier_phone);
        mSupplierEmailEditText = findViewById(R.id.edit_supplier_email);
        mCallImageButton = findViewById(R.id.image_button_call);
        mEmailImageButton = findViewById(R.id.image_button_email);
        mDecrementImageButton = findViewById(R.id.image_button_decrement);
        mIncrementImageButton = findViewById(R.id.image_button_increment);
        emptyCommunication = findViewById(R.id.empty_communication);
    }

    private void setupSpinner() {
        ArrayAdapter genderSpinnerAdapter = ArrayAdapter.createFromResource(
                this,
                R.array.array_product_grade_option,
                android.R.layout.simple_spinner_item);

        genderSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        mProductGradeSpinner.setAdapter(genderSpinnerAdapter);
        mProductGradeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selection = (String) parent.getItemAtPosition(position);
                if (selection.equals(getString(R.string.product_grade_new))) {
                    mGender = ProductEntry.GRADE_NEW;
                } else if (selection.equals(getString(R.string.product_grade_used))) {
                    mGender = ProductEntry.GRADE_USED;
                } else {
                    mGender = ProductEntry.GRADE_UNKNOWN;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                mGender = ProductEntry.GRADE_UNKNOWN;
            }
        });

    }

    private boolean saveProduct() {

        String productName = mProductNameEditText.getText().toString().trim();
        String modelNo = mModelNoEditText.getText().toString().trim();
        String price = mPriceEditText.getText().toString().trim();
        String quantity = mQuantityEditText.getText().toString().trim();
        String supplierName = mSupplierNameEditText.getText().toString().trim();
        String supplierPhone = mSupplierPhoneEditText.getText().toString().trim();
        String supplierEmail = mSupplierEmailEditText.getText().toString().trim();

        // Check if this is supposed to be a new product
        // and check if all the fields in the editor are blank
        if (mCurrentProductUri == null &&
                TextUtils.isEmpty(productName) &&
                TextUtils.isEmpty(modelNo) &&
                TextUtils.isEmpty(price) &&
                TextUtils.isEmpty(quantity) &&
                mImageUri == null &&
                mGender == ProductEntry.GRADE_UNKNOWN &&
                TextUtils.isEmpty(supplierName) &&
                TextUtils.isEmpty(supplierPhone) &&
                TextUtils.isEmpty(supplierEmail)) {

            return hasAllRequiredValues = true;
        }

        // Create a ContentValues object where column names are the keys,
        // and product attributes from the editor are the values.
        ContentValues values = new ContentValues();

        // REQUIRED VALUES
        // Validation section
        if (TextUtils.isEmpty(productName)) {
            Toast.makeText(this, "Product name is required", Toast.LENGTH_SHORT).show();
            return hasAllRequiredValues;
        } else {
            values.put(ProductEntry.COLUMN_PRODUCT_NAME, productName);
        }

        if (TextUtils.isEmpty(modelNo)) {
            Toast.makeText(this, "Model no. is required", Toast.LENGTH_SHORT).show();
            return hasAllRequiredValues;
        } else {
            values.put(ProductContract.ProductEntry.COLUMN_MODEL_NO, modelNo);
        }

        if (TextUtils.isEmpty(price)) {
            Toast.makeText(this, "Price is required", Toast.LENGTH_SHORT).show();
            return hasAllRequiredValues;
        } else {
            values.put(ProductContract.ProductEntry.COLUMN_PRICE, price);
        }

        if (TextUtils.isEmpty(quantity)) {
            Toast.makeText(this, "Quantity is required", Toast.LENGTH_SHORT).show();
            return hasAllRequiredValues;
        } else {
            values.put(ProductEntry.COLUMN_QUANTITY, quantity);
        }

        if (mImageUri == null) {
            Toast.makeText(this, "Image is required", Toast.LENGTH_SHORT).show();
            return hasAllRequiredValues;
        } else {
            values.put(ProductEntry.COLUMN_PRODUCT_IMAGE, mImageUri.toString());
        }

        if (TextUtils.isEmpty(String.valueOf(mGender))) {
            Toast.makeText(this, "Product grade is required", Toast.LENGTH_SHORT).show();
            return hasAllRequiredValues;
        } else {
            values.put(ProductEntry.COLUMN_PRODUCT_GRADE, mGender);
        }

        if (TextUtils.isEmpty(supplierName)) {
            Toast.makeText(this, "Supplier name is required", Toast.LENGTH_SHORT).show();
            return hasAllRequiredValues;
        } else {
            values.put(ProductEntry.COLUMN_SUPPLIER_NAME, supplierName);
        }

        if (TextUtils.isEmpty(supplierPhone)) {
            Toast.makeText(this, "Supplier phone is required", Toast.LENGTH_SHORT).show();
            return hasAllRequiredValues;
        } else {
            values.put(ProductEntry.COLUMN_SUPPLIER_PHONE, supplierPhone);
        }

        // OPTIONAL VALUES
        values.put(ProductContract.ProductEntry.COLUMN_SUPPLIER_EMAIL, supplierEmail);

        if (mCurrentProductUri == null) {
            Uri rowInsert = getContentResolver().insert(ProductEntry.CONTENT_URI, values);
            if (rowInsert == null) {
                Toast.makeText(this, "Error with saving item", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Item saved", Toast.LENGTH_SHORT).show();
            }
        } else {
            int rowsAffected = getContentResolver().update(mCurrentProductUri, values, null, null);
            if (rowsAffected == 0) {
                Toast.makeText(this, "Error with updating item", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Item updated", Toast.LENGTH_SHORT).show();
            }
        }

        return hasAllRequiredValues = true;
    }

    private void deleteProduct() {
        if (mCurrentProductUri != null) {
            int rowDelete = getContentResolver().delete(mCurrentProductUri, null, null);
            if (rowDelete == 0) {
                Toast.makeText(this, "Error with deleting inventory", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Inventory deleted", Toast.LENGTH_SHORT).show();
            }
            finish();
        }
    }

    private void showUnsavedChangesDialog(DialogInterface.OnClickListener discardButtonClickListener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Discard your changes and quit editing?");
        builder.setPositiveButton("Discard", discardButtonClickListener);
        builder.setNegativeButton("Keep Editing", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    private void showDeleteConfirmationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Delete this inventory?");
        builder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                deleteProduct();
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    private void trySelector() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
            return;
        }
        openSelector();
    }

    private void openSelector() {
        Intent intent;
        if (Build.VERSION.SDK_INT < 19) {
            intent = new Intent(Intent.ACTION_GET_CONTENT);
        } else {
            intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
        }
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, "Select image"), 0);
    }

    private void sendEmail() {
        Intent intent = new Intent(android.content.Intent.ACTION_SENDTO);
        intent.setType("text/plain");
        intent.setData(Uri.parse("mailto:" + mSupplierEmailEditText.getText().toString().trim()));
        intent.putExtra(android.content.Intent.EXTRA_SUBJECT, "New order: " +
                mProductNameEditText.getText().toString().trim() +
                " " + mModelNoEditText.getText().toString().trim());
        String message = "We need to make a new order of: " +
                mProductNameEditText.getText().toString().trim() +
                " " +
                mModelNoEditText.getText().toString().trim() + "." +
                "\n" +
                "Please confirm that you can send to us ___ pcs." +
                "\n" +
                "\n" +
                "Best regards," + "\n" +
                "_________________";
        intent.putExtra(android.content.Intent.EXTRA_TEXT, message);
        startActivity(intent);
    }

    public static void mackPhoneCall(Context context, String phoneNumber, Activity activity) {
        if (androidx.core.content.ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            androidx.core.app.ActivityCompat.requestPermissions((Activity) context, new String[]{Manifest.permission.CALL_PHONE}, 1);
        } else {
            String dial = "tel:" + phoneNumber;
            activity.startActivity(new Intent(Intent.ACTION_CALL, Uri.parse(dial)));
        }
    }

}
