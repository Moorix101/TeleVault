package com.moorixlabs.televault;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.github.chrisbanes.photoview.PhotoView;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

public class ImageViewerActivity extends AppCompatActivity {

    private static final String TAG = "ImageViewerActivity";

    public static final String EXTRA_FILE_ID = "file_id";
    public static final String EXTRA_FILE_NAME = "file_name";
    public static final String EXTRA_BOT_TOKEN = "bot_token";

    private PhotoView photoView;
    private LinearLayout loadingLayout;
    private LinearLayout errorLayout;
    private TextView tvLoadingText;
    private TextView tvProgress;
    private TextView tvErrorMessage;
    private ImageButton btnClose;

    private File downloadedImageFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_viewer);

        initializeViews();
        setupClickListeners();

        // Get data from intent
        String fileId = getIntent().getStringExtra(EXTRA_FILE_ID);
        String fileName = getIntent().getStringExtra(EXTRA_FILE_NAME);
        String botToken = getIntent().getStringExtra(EXTRA_BOT_TOKEN);

        if (fileId == null || fileName == null || botToken == null) {
            showError("Missing required information");
            return;
        }

        downloadAndDisplayImage(botToken, fileId, fileName);
    }

    private void initializeViews() {
        photoView = findViewById(R.id.photoView);
        loadingLayout = findViewById(R.id.loadingLayout);
        errorLayout = findViewById(R.id.errorLayout);
        tvLoadingText = findViewById(R.id.tvLoadingText);
        tvProgress = findViewById(R.id.tvProgress);
        tvErrorMessage = findViewById(R.id.tvErrorMessage);
        btnClose = findViewById(R.id.btnClose);
    }

    private void setupClickListeners() {
        btnClose.setOnClickListener(v -> finish());

        // Allow clicking error layout to close
        errorLayout.setOnClickListener(v -> finish());

        // Optional: Double-tap photo to close
        photoView.setOnClickListener(v -> {
            // Single tap does nothing (let PhotoView handle gestures)
        });
    }

    private void downloadAndDisplayImage(String botToken, String fileId, String fileName) {
        showLoading();

        new Thread(() -> {
            TelegramDownloader downloader = new TelegramDownloader(
                    getApplicationContext(),
                    botToken,
                    fileId,
                    fileName,
                    new TelegramDownloader.DownloadCallback() {
                        @Override
                        public void onDownloadProgress(int progress) {
                            runOnUiThread(() -> {
                                tvProgress.setText(progress + "%");
                            });
                        }

                        @Override
                        public void onDownloadSuccess(Uri fileUri) {
                            runOnUiThread(() -> {
                                try {
                                    // Store reference to downloaded file for cleanup
                                    downloadedImageFile = new File(fileUri.getPath());

                                    // Load image from file
                                    displayImage(downloadedImageFile);

                                    hideLoading();
                                    photoView.setVisibility(View.VISIBLE);

                                    Log.i(TAG, "Image loaded successfully: " + fileName);

                                } catch (Exception e) {
                                    Log.e(TAG, "Error displaying image", e);
                                    showError("Failed to display image: " + e.getMessage());
                                }
                            });
                        }

                        @Override
                        public void onDownloadFailed(String error) {
                            runOnUiThread(() -> {
                                Log.e(TAG, "Download failed: " + error);
                                showError("Download failed: " + error);
                            });
                        }
                    }
            );

            downloader.download();
        }).start();
    }

    private void displayImage(File imageFile) throws Exception {
        // Use efficient image loading with downsampling for large images
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;

        // First decode to get dimensions
        BitmapFactory.decodeFile(imageFile.getAbsolutePath(), options);

        // Calculate sample size (downsample if image is too large)
        int maxDimension = 2048; // Max width/height
        int sampleSize = 1;

        if (options.outWidth > maxDimension || options.outHeight > maxDimension) {
            int scale = Math.max(
                    options.outWidth / maxDimension,
                    options.outHeight / maxDimension
            );
            sampleSize = Integer.highestOneBit(scale);
        }

        // Now decode with sample size
        options.inJustDecodeBounds = false;
        options.inSampleSize = sampleSize;
        options.inPreferredConfig = Bitmap.Config.RGB_565; // Use less memory

        Bitmap bitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath(), options);

        if (bitmap != null) {
            photoView.setImageBitmap(bitmap);
            Log.i(TAG, "Bitmap loaded: " + bitmap.getWidth() + "x" + bitmap.getHeight());
        } else {
            throw new Exception("Failed to decode bitmap");
        }
    }

    private void showLoading() {
        loadingLayout.setVisibility(View.VISIBLE);
        photoView.setVisibility(View.GONE);
        errorLayout.setVisibility(View.GONE);
    }

    private void hideLoading() {
        loadingLayout.setVisibility(View.GONE);
    }

    private void showError(String message) {
        loadingLayout.setVisibility(View.GONE);
        photoView.setVisibility(View.GONE);
        errorLayout.setVisibility(View.VISIBLE);
        tvErrorMessage.setText(message);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Clean up: Delete the downloaded image file
        if (downloadedImageFile != null && downloadedImageFile.exists()) {
            if (downloadedImageFile.delete()) {
                Log.i(TAG, "✓ Temp image file deleted: " + downloadedImageFile.getName());
            } else {
                Log.w(TAG, "Failed to delete temp image: " + downloadedImageFile.getName());
            }
        }

        // Also recycle bitmap to free memory
        if (photoView != null && photoView.getDrawable() != null) {
            photoView.setImageDrawable(null);
        }
    }
}