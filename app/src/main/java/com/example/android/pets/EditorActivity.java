/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.android.pets;

import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.NavUtils;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.example.android.pets.data.PetContract.PetEntry;
import com.example.android.pets.data.PetDbHelper;

/**
 * Allows user to create a new pet or edit an existing one.
 */
public class EditorActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final int PET_LOADER_ID = 0;

    private Uri petUri;

    /**
     * EditText field to enter the pet's name
     */
    private EditText mNameEditText;

    /**
     * EditText field to enter the pet's breed
     */
    private EditText mBreedEditText;

    /**
     * EditText field to enter the pet's weight
     */
    private EditText mWeightEditText;

    /**
     * EditText field to enter the pet's gender
     */
    private Spinner mGenderSpinner;

    /**
     * Gender of the pet. The possible valid values are in the PetContract.java file:
     * {@link PetEntry#GENDER_UNKNOWN}, {@link PetEntry#GENDER_MALE}, or
     * {@link PetEntry#GENDER_FEMALE}.
     */
    private int mGender = PetEntry.GENDER_UNKNOWN;

    PetDbHelper dbHelper;

    private boolean mPetHasChanged = false;

    private View.OnTouchListener mTouchListener = new View.OnTouchListener() {

        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {

            mPetHasChanged = true;
            return false;

        }

    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor);

        // Find all relevant views that we will need to read user input from
        mNameEditText = findViewById(R.id.edit_pet_name);
        mBreedEditText = findViewById(R.id.edit_pet_breed);
        mWeightEditText = findViewById(R.id.edit_pet_weight);
        mGenderSpinner = findViewById(R.id.spinner_gender);

        mNameEditText.setOnTouchListener(mTouchListener);
        mBreedEditText.setOnTouchListener(mTouchListener);
        mWeightEditText.setOnTouchListener(mTouchListener);
        mGenderSpinner.setOnTouchListener(mTouchListener);

        setupSpinner();

        dbHelper = new PetDbHelper(this);

        Intent intent = getIntent();
        petUri = intent.getData();

        if (petUri == null) {

            setTitle(R.string.editor_activity_title_new_pet);

            // Invalidate the options menu, so the "Delete" menu option can be hidden.
            // (It doesn't make sense to delete a pet that hasn't been created yet.)
            invalidateOptionsMenu();

        } else {

            setTitle(R.string.editor_activity_title_edit_pet);
            getSupportLoaderManager().initLoader(PET_LOADER_ID, null, this);

        }

    }

    /**
     * Setup the dropdown spinner that allows the user to select the gender of the pet.
     */
    private void setupSpinner() {

        // Create adapter for spinner. The list options are from the String array it will use
        // the spinner will use the default layout
        ArrayAdapter genderSpinnerAdapter = ArrayAdapter.createFromResource(this,
                R.array.array_gender_options, android.R.layout.simple_spinner_item);

        // Specify dropdown layout style - simple list view with 1 item per line
        genderSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_dropdown_item_1line);

        // Apply the adapter to the spinner
        mGenderSpinner.setAdapter(genderSpinnerAdapter);

        // Set the integer mSelected to the constant values
        mGenderSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                String selection = (String) parent.getItemAtPosition(position);
                if (!TextUtils.isEmpty(selection)) {

                    if (selection.equals(getString(R.string.gender_male))) {

                        mGender = PetEntry.GENDER_MALE; // Male

                    } else if (selection.equals(getString(R.string.gender_female))) {

                        mGender = PetEntry.GENDER_FEMALE; // Female

                    } else {

                        mGender = PetEntry.GENDER_UNKNOWN; // Unknown

                    }

                }

            }

            // Because AdapterView is an abstract class, onNothingSelected must be defined
            @Override
            public void onNothingSelected(AdapterView<?> parent) {

                mGender = PetEntry.GENDER_UNKNOWN; // Unknown

            }

        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Inflate the menu options from the res/menu/menu_editor.xml file.
        // This adds menu items to the app bar.
        getMenuInflater().inflate(R.menu.menu_editor, menu);
        return true;

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        // User clicked on a menu option in the app bar overflow menu
        switch (item.getItemId()) {

            // Respond to a click on the "Save" menu option
            case R.id.action_save:
                String name = mNameEditText.getText().toString();
                String breed = mBreedEditText.getText().toString();
                String weight = mWeightEditText.getText().toString();
                savePet(name, breed, mGender, weight);
                finish();
                return true;

            // Respond to a click on the "Delete" menu option
            case R.id.action_delete:
                showDeleteConfirmationDialog();
                return true;

            // Respond to a click on the "Up" arrow button in the app bar
            case android.R.id.home:
                // If the pet hasn't changed, continue with navigating up to parent activity
                // which is the {@link CatalogActivity}.
                if (!mPetHasChanged) {

                    NavUtils.navigateUpFromSameTask(EditorActivity.this);
                    return true;

                }

                // Otherwise if there are unsaved changes, setup a dialog to warn the user.
                // Create a click listener to handle the user confirming that
                // changes should be discarded.
                DialogInterface.OnClickListener discardButtonClickListener =
                        new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {

                                // User clicked "Discard" button, navigate to parent activity.
                                NavUtils.navigateUpFromSameTask(EditorActivity.this);

                            }

                        };

                // Show a dialog that notifies the user they have unsaved changes
                showUnsavedChangesDialog(discardButtonClickListener);
                return true;

        }
        return super.onOptionsItemSelected(item);

    }

    public void savePet(String name, String breed, int mGender, String weight) {

        if (petUri == null && TextUtils.isEmpty(name) && TextUtils.isEmpty(breed) &&
                TextUtils.isEmpty(weight) && mGender == PetEntry.GENDER_UNKNOWN) {

            return;

        }

        // Create a ContentValues object where column names are the keys,
        // and pet attributes from the editor are the values.
        ContentValues values = new ContentValues();
        values.put(PetEntry.COLUMN_PET_NAME, name);
        values.put(PetEntry.COLUMN_PET_BREED, breed);
        values.put(PetEntry.COLUMN_PET_GENDER, mGender);
        if (!TextUtils.isEmpty(weight)) {

            values.put(PetEntry.COLUMN_PET_WEIGHT, weight);

        } else {

            values.put(PetEntry.COLUMN_PET_WEIGHT, 0);

        }

        if (petUri == null) {

            // Insert a new pet into the provider, returning the content URI for the new pet.
            Uri newUri = getContentResolver().insert(PetEntry.CONTENT_URI, values);

            // Show a toast message depending on whether or not the insertion was successful
            if (newUri == null) {

                // If the new content URI is null, then there was an error with insertion.
                Toast.makeText(this, getString(R.string.editor_save_pet_failed),
                        Toast.LENGTH_SHORT).show();

            } else {

                // Otherwise, the insertion was successful and we can display a toast.
                Toast.makeText(this, getString(R.string.editor_save_pet_successful),
                        Toast.LENGTH_SHORT).show();

            }

        } else {

            int numberUpdated = getContentResolver().update(petUri, values, null, null);

            if (numberUpdated == 0) {

                // If the new content URI is null, then there was an error with insertion.
                Toast.makeText(this, getString(R.string.editor_save_pet_failed),
                        Toast.LENGTH_SHORT).show();

            } else {

                // Otherwise, the insertion was successful and we can display a toast.
                Toast.makeText(this, getString(R.string.editor_save_pet_successful),
                        Toast.LENGTH_SHORT).show();

            }

        }

    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {

        String[] projection = {

                PetEntry._ID,
                PetEntry.COLUMN_PET_NAME,
                PetEntry.COLUMN_PET_BREED,
                PetEntry.COLUMN_PET_GENDER,
                PetEntry.COLUMN_PET_WEIGHT

        };

        return new CursorLoader(this, petUri, projection, null, null, null);

    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {

        if (data.moveToFirst()) {

            String name = data.getString(data.getColumnIndex(PetEntry.COLUMN_PET_NAME));
            String breed = data.getString(data.getColumnIndex(PetEntry.COLUMN_PET_BREED));
            mGender = data.getInt(data.getColumnIndex(PetEntry.COLUMN_PET_GENDER));
            int weight = data.getInt(data.getColumnIndex(PetEntry.COLUMN_PET_WEIGHT));

            mNameEditText.setText(name);
            mBreedEditText.setText(breed);
            switch (mGender) {

                case PetEntry.GENDER_MALE:
                    mGenderSpinner.setSelection(1);
                    break;

                case PetEntry.GENDER_FEMALE:
                    mGenderSpinner.setSelection(2);
                    break;

                default:
                    mGenderSpinner.setSelection(0);
                    break;

            }
            mWeightEditText.setText(Integer.toString(weight));

        }

    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

        mNameEditText.setText(null);
        mBreedEditText.setText(null);
        mGenderSpinner.setSelection(-1);
        mWeightEditText.setText(null);

    }

    private void showUnsavedChangesDialog(DialogInterface.OnClickListener discardButtonClickListener) {

        // Create an AlertDialog.Builder and set the message, and click listeners
        // for the positive and negative buttons on the dialog.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.unsaved_changes_dialog_msg);
        builder.setPositiveButton(R.string.discard, discardButtonClickListener);
        builder.setNegativeButton(R.string.keep_editing, new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int id) {

                // User clicked the "Keep editing" button, so dismiss the dialog
                // and continue editing the pet.
                if (dialog != null) {

                    dialog.dismiss();

                }

            }

        });

        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();

    }

    @Override
    public void onBackPressed() {

        // If the pet hasn't changed, continue with handling back button press
        if (!mPetHasChanged) {

            super.onBackPressed();
            return;

        }

        // Otherwise if there are unsaved changes, setup a dialog to warn the user.
        // Create a click listener to handle the user confirming that changes should be discarded.
        DialogInterface.OnClickListener discardButtonClickListener = new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

                // User clicked "Discard" button, close the current activity.
                finish();

            }

        };

        // Show dialog that there are unsaved changes
        showUnsavedChangesDialog(discardButtonClickListener);

    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {

        super.onPrepareOptionsMenu(menu);
        // If this is a new pet, hide the "Delete" menu item.
        if (petUri == null) {

            MenuItem menuItem = menu.findItem(R.id.action_delete);
            menuItem.setVisible(false);

        }
        return true;

    }

    private void showDeleteConfirmationDialog() {

        // Create an AlertDialog.Builder and set the message, and click listeners
        // for the postivie and negative buttons on the dialog.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.delete_dialog_msg);
        builder.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Delete" button, so delete the pet.
                deletePet();

            }

        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int id) {

                // User clicked the "Cancel" button, so dismiss the dialog
                // and continue editing the pet.
                if (dialog != null) {

                    dialog.dismiss();

                }

            }

        });

        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();

    }

    /**
     * Perform the deletion of the pet in the database.
     */
    private void deletePet() {

        if (petUri != null) {

            int numberDeleted = getContentResolver().delete(petUri, null, null);

            if (numberDeleted == 0) {

                // If the new content URI is null, then there was an error with insertion.
                Toast.makeText(this, getString(R.string.editor_delete_pet_failed),
                        Toast.LENGTH_SHORT).show();

            } else {

                // Otherwise, the insertion was successful and we can display a toast.
                Toast.makeText(this, getString(R.string.editor_delete_pet_successful),
                        Toast.LENGTH_SHORT).show();

            }

            finish();

        }

    }

}