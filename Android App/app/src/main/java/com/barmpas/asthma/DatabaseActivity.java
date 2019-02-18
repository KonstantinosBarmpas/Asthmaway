package com.barmpas.asthma;

import android.app.AlertDialog;
import android.app.LoaderManager;
import android.content.Context;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import com.barmpas.asthma.data.SessionContract;

import java.util.Calendar;

/*
 * The database activity. Reads from the SQLite Database and displays the results using an Adapter.
 * The reading is perfomed using a cursor.
 */

public class DatabaseActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final int URL_LOADER = 0;
    SimpleCursorAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sql_layout);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        //The displayed views
        String[] columns = {
                SessionContract.SessionEntry.COLUMN_ID,
                SessionContract.SessionEntry.COLUMN_ERROR
        };

        int[] views = {
                R.id.date_sql_text,
                R.id.error_sql_text
        };

        //Setting the adapter
        mAdapter = new SimpleCursorAdapter(this, R.layout.list_item_favourites, null, columns, views, 0) {

            @Override
            public void bindView(View view, Context context, final Cursor cursor) {
                super.bindView(view, context, cursor);

                //Setting each individual view of the adapter.
                TextView error = (TextView) view.findViewById(R.id.error_sql_text);
                final String error_str = cursor.getString(cursor.getColumnIndex("ain"));
                int valid = 0;
                String temp[] = error_str.split("/");
                for (int i = 0; i < temp.length; i++) {
                    if (temp[i].equals("0")) {
                        valid++;
                    }
                }

                //Set the progress bar
                ProgressBar pb = (ProgressBar) view.findViewById(R.id.pb);
                pb.setMax(100);
                pb.setProgress(valid * 100 / temp.length);

                //Set the sub-views to display correct data
                error.setText("Correct presses: " + String.valueOf(valid) + "/" + String.valueOf(temp.length));
                Calendar cl = Calendar.getInstance();
                cl.setTimeInMillis(cursor.getLong(
                        cursor.getColumnIndex("id")));
                TextView date_text = (TextView) view.findViewById(R.id.date_sql_text);
                final String date = cl.get(Calendar.DAY_OF_MONTH) + "/" + String.valueOf(cl.get(Calendar.MONTH) + 1) + "/" + cl.get(Calendar.YEAR);
                date_text.setText("Date: " + date);
                final String breathes_str = cursor.getString(cursor.getColumnIndex("bin"));

                //On click pass data to the Graph activity
                view.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(getBaseContext(), GraphActivity.class);
                        intent.putExtra("BREATHES", breathes_str);
                        intent.putExtra("DATE", date);
                        intent.putExtra("ERROR", error_str);
                        startActivity(intent);
                    }
                });
            }
        };

        //Activating the cursor and the adapter
        ListView favListView = (ListView) findViewById(R.id.empty_view_favourites);
        favListView.setAdapter(mAdapter);
        View emptyView = findViewById(R.id.empty_view_favourites);
        favListView.setEmptyView(emptyView);
        getLoaderManager().initLoader(URL_LOADER, null, this);
    }


    //Menus to allow delete the SQlite database.
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.sql_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.delete_all:
                new AlertDialog.Builder(this)
                        .setTitle("Delete")
                        .setMessage("Do you want to delete")
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {

                            public void onClick(DialogInterface dialog, int whichButton) {
                                getContentResolver().delete(SessionContract.SessionEntry.CONTENT_URI, null, null);
                            }
                        })
                        .setNegativeButton(android.R.string.no, null).show();
                return true;
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);

        }
    }

    //Creating the Cursor
    @Override
    public Loader<Cursor> onCreateLoader(int loaderID, Bundle bundle) {

        switch (loaderID) {
            case URL_LOADER:
                String[] projection = {
                        SessionContract.SessionEntry._ID,
                        SessionContract.SessionEntry.COLUMN_ID,
                        SessionContract.SessionEntry.COLUMN_BREATHES_IN,
                        SessionContract.SessionEntry.COLUMN_ERROR
                };
                return new CursorLoader(
                        this,
                        SessionContract.SessionEntry.CONTENT_URI,
                        projection,
                        null,
                        null,
                        null
                );
            default:
                return null;
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        mAdapter.swapCursor(cursor);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mAdapter.swapCursor(null);
    }
}
