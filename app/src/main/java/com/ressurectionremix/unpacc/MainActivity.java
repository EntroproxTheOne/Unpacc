package com.ressurectionremix.unpacc;

import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.content.ClipboardManager;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.SparseArray;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.text.MessageFormat;
import java.util.Objects;

import at.huber.youtubeExtractor.VideoMeta;
import at.huber.youtubeExtractor.YouTubeExtractor;
import at.huber.youtubeExtractor.YtFile;

public class MainActivity extends AppCompatActivity {

    @SuppressLint("StaticFieldLeak")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Apply dynamic color theme if available (Android 12+)
        setDynamicColors();

        // Initialize views
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        MaterialButton downloadBtn = findViewById(R.id.downloadBtn);
        TextInputEditText urlInput = findViewById(R.id.urlET);
        MaterialButton pasteBtn = findViewById(R.id.pasteBtn);
        ImageView thumbnail = findViewById(R.id.thumbnail);
        TextView title = findViewById(R.id.titleTV);
        TextView channelName = findViewById(R.id.channelName);
        TextView videoLength = findViewById(R.id.videoLength);
        TextView viewCount = findViewById(R.id.viewCount);
        RecyclerView recyclerView = findViewById(R.id.recycler);

        // Settings Button
        MaterialButton settingsBtn = findViewById(R.id.settingsBtn);
        settingsBtn.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
        });

        // Set up paste button listener
        pasteBtn.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard != null && clipboard.hasPrimaryClip()) {
                ClipData clip = clipboard.getPrimaryClip();
                ClipData.Item item = clip.getItemAt(0);
                CharSequence pasteData = item.getText();
                if (pasteData != null) {
                    urlInput.setText(pasteData);
                    Toast.makeText(this, "Pasted: " + pasteData, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, R.string.clipboard_no_text, Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, R.string.clipboard_empty, Toast.LENGTH_SHORT).show();
            }
        });

        // Download button logic
        downloadBtn.setOnClickListener(view -> {
            String videoUrl = Objects.requireNonNull(urlInput.getText()).toString().trim();
            if (videoUrl.isEmpty()) {
                Toast.makeText(this, R.string.enter_valid_url, Toast.LENGTH_SHORT).show();
                return;
            }

            new YouTubeExtractor(this) {
                @Override
                public void onExtractionComplete(SparseArray<YtFile> files, VideoMeta videoMeta) {
                    if (files == null) {
                        Toast.makeText(MainActivity.this, R.string.extraction_failed, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Set video metadata
                    title.setText(MessageFormat.format(getString(R.string.title_format), videoMeta.getTitle()));
                    channelName.setText(MessageFormat.format(getString(R.string.author_format), videoMeta.getAuthor()));
                    videoLength.setText(MessageFormat.format(getString(R.string.duration_format), getDuration(videoMeta.getVideoLength())));
                    viewCount.setText(MessageFormat.format(getString(R.string.views_format), videoMeta.getViewCount()));

                    // Load thumbnail using Glide
                    String imageUrl = videoMeta.getMaxResImageUrl().replace("http", "https");
                    Glide.with(getApplicationContext()).load(imageUrl).fitCenter().into(thumbnail);

                    // Filter files to show only 360p or higher + audio-only formats
                    SparseArray<YtFile> filteredFiles = new SparseArray<>();
                    for (int i = 0; i < files.size(); i++) {
                        YtFile ytFile = files.get(files.keyAt(i));
                        if (ytFile.getFormat().getHeight() == -1 || ytFile.getFormat().getHeight() >= 360) {
                            filteredFiles.put(files.keyAt(i), ytFile);
                        }
                    }

                    // Setup adapter
                    ResultAdapter adapter = new ResultAdapter(MainActivity.this, filteredFiles);
                    recyclerView.setAdapter(adapter);

                    // Handle item click
                    adapter.setOnItemClickListener(ytFile -> {
                        String filename = videoMeta.getTitle() + "." + ytFile.getFormat().getExt();
                        filename = filename.replaceAll("[\\\\><\"|*?%:#/]", ""); // sanitize
                        download(ytFile.getUrl(), videoMeta.getTitle(), filename);
                    });
                }
            }.extract(videoUrl, false, false);
        });
    }

    private void setDynamicColors() {
        getDelegate().setLocalNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
    }

    public String getDuration(long duration) {
        final int MINUTES_IN_AN_HOUR = 60;
        final int SECONDS_IN_A_MINUTE = 60;
        int seconds = (int) (duration % SECONDS_IN_A_MINUTE);
        int totalMinutes = (int) (duration / SECONDS_IN_A_MINUTE);
        int minutes = totalMinutes % MINUTES_IN_AN_HOUR;
        int hours = totalMinutes / MINUTES_IN_AN_HOUR;
        return hours + ":" + minutes + ":" + seconds;
    }

    private void download(String youtubeDlUrl, String downloadTitle, String fileName) {
        Uri uri = Uri.parse(youtubeDlUrl);
        DownloadManager.Request request = new DownloadManager.Request(uri);
        request.setTitle(downloadTitle);
        request.allowScanningByMediaScanner();
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);
        DownloadManager manager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        if (manager != null) {
            manager.enqueue(request);
            Toast.makeText(this, R.string.downloading, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, R.string.download_manager_unavailable, Toast.LENGTH_SHORT).show();
        }
    }
}