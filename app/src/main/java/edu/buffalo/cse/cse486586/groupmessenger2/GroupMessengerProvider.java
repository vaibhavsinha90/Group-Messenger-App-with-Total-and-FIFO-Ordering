package edu.buffalo.cse.cse486586.groupmessenger2;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.util.Log;

import java.io.FileInputStream;
import java.io.FileOutputStream;


public class GroupMessengerProvider extends ContentProvider {
    static final String TAG = GroupMessengerProvider.class.getSimpleName();

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        
        return 0;
    }

    @Override
    public String getType(Uri uri) {
        
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        
        String filename = values.getAsString("key");
        String string = values.getAsString("value");
        FileOutputStream outputStream;

        try {
            outputStream = getContext().getApplicationContext().openFileOutput(filename, Context.MODE_PRIVATE);
            outputStream.write(string.getBytes());
            outputStream.close();
        } catch (Exception e) {
            Log.e(TAG, "File write failed");
        }
        Log.v("inserted", string+"->"+filename);
        //Log.v("insert", values.toString());
        return uri;
    }

    @Override
    public boolean onCreate() {
        
        return false;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        
        return 0;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {
        String[] colnames = {"key", "value"};
        MatrixCursor m = new MatrixCursor(colnames);
        FileInputStream inputStream;

        try {
            inputStream = getContext().getApplicationContext().openFileInput(selection);
            long k= inputStream.getChannel().size();
            byte[] b = new byte[(int)k];
            inputStream.read(b);
            inputStream.close();
            String strval=new String(b);
            m.addRow(new Object[]{selection, strval});
            Log.v("query", selection + " " + strval + "::"+k);
        } catch (Exception e) {
            Log.e(TAG, "File read failed :X");

        }
        return m;
    }
}
