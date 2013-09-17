package com.iwobanas.screenrecorder;

import android.app.Activity;
import android.app.ListActivity;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Collections;

public class DirectoryChooserActivity extends ListActivity {

    private File dir;
    private ArrayList<FileWrapper> items = new ArrayList<FileWrapper>();
    private ArrayAdapter<FileWrapper> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        truncateTittleAtMiddle();

        setContentView(R.layout.directory_chooser);

        Button selectButton = (Button) findViewById(R.id.select_button);

        selectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent result = new Intent(null, Uri.fromFile(dir));
                setResult(Activity.RESULT_OK, result);
                finish();
            }
        });

        adapter = new ArrayAdapter<FileWrapper>(
                this,
                android.R.layout.simple_selectable_list_item,
                items
        );

        setDir(new File(getIntent().getData().getPath()));

        setListAdapter(adapter);
    }

    private void truncateTittleAtMiddle() {
        try {
            final int actionBarTitle = Resources.getSystem().getIdentifier("action_bar_title", "id", "android");
            final TextView title = (TextView) getWindow().findViewById(actionBarTitle);
            if (title != null) {
                title.setEllipsize(TextUtils.TruncateAt.MIDDLE);
            }
        } catch (Throwable ignored) {
        }
    }

    private void setDir(File dir) {
        this.dir = dir;
        setTitle(dir.getAbsolutePath());

        items.clear();
        items.addAll(listWrapped(dir));
        adapter.notifyDataSetChanged();
    }

    private ArrayList<FileWrapper> listWrapped(File dir) {
        ArrayList<FileWrapper> items = new ArrayList<FileWrapper>();

        if (dir.getParentFile() != null) {
            items.add(new FileWrapper(dir.getParentFile(), ".."));
        }

        File[] files = dir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return pathname.isDirectory();
            }
        });

        if (files != null) {
            for (File file : files) {
                items.add(new FileWrapper(file));
            }
        }

        Collections.sort(items);

        return items;
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        FileWrapper item = (FileWrapper) l.getItemAtPosition(position);
        setDir(item.getFile());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.directory_chooser, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.directory_chooser_refresh:
                setDir(dir);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

}
