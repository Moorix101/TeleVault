package com.moorixlabs.televault;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog; // Stays androidx
import androidx.appcompat.app.AppCompatActivity; // NEW IMPORT
import androidx.recyclerview.widget.RecyclerView;

import java.io.File; // NEW IMPORT
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.transition.Transition;
import android.content.Intent;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider; // NEW IMPORT
import android.os.Handler; // NEW IMPORT
import android.os.Looper; // NEW IMPORT
import android.util.Log; // NEW IMPORT

public class FileAdapter extends RecyclerView.Adapter<FileAdapter.FileViewHolder> {

    private final Context context;
    private final List<CloudFile> files;
    private final SimpleDateFormat dateFormat;
    private final FileActionListener actionListener;

    public interface FileActionListener {
        void onDownloadFile(CloudFile file);
        void onDeleteFile(CloudFile file, int position);
        void onShareFileAction(CloudFile file);
        void onCancelUpload(CloudFile file, int position);
    }

    public FileAdapter(Context context, List<CloudFile> files, FileActionListener actionListener) {
        this.context = context;
        this.files = files;
        this.actionListener = actionListener;
        this.dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
    }

    @NonNull
    @Override
    public FileViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_file, parent, false);
        return new FileViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FileViewHolder holder, int position) {
        CloudFile file = files.get(position);

        // Set basic info
        holder.tvFileName.setText(file.getName());
        holder.tvFileSize.setText(FileUtils.formatFileSize(file.getSize()));
        holder.tvFileDate.setText(dateFormat.format(new Date(file.getDate())));

        // Set file icon/thumbnail
        setFileIcon(holder.ivFileIcon, file);

        // Simple upload status - just show "Uploading..." or hide it
        if (file.isUploaded()) {
            // File is uploaded - hide upload status, show cloud status
            holder.layoutUploadStatus.setVisibility(View.GONE);
            holder.layoutCloudStatus.setVisibility(View.VISIBLE);
        } else {
            // File is uploading - show upload status, hide cloud status
            holder.layoutCloudStatus.setVisibility(View.GONE);
            holder.layoutUploadStatus.setVisibility(View.VISIBLE);
        }

        // Menu button click
        holder.btnFileMenu.setOnClickListener(v -> showFileOptionsMenu(file, position));

        // Item click - Show image viewer for uploaded images
        holder.itemView.setOnClickListener(v -> {
            if (file.isUploaded()) {
                // Check if it's an image file
                if (FileUtils.isImage(file.getName())) {
                    showImageViewerDialog(file);

                    // --- NEW BLOCK ---
                } else if (FileUtils.isVideo(file.getName())) {
                    showVideoViewerDialog(file);
                    // --- END NEW BLOCK ---

                } else {
                    Toast.makeText(context, R.string.tap_for_options, Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(context, "Uploading...", Toast.LENGTH_SHORT).show();
            }
        });

        // Long click
        holder.itemView.setOnLongClickListener(v -> {
            showFileOptionsMenu(file, position);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return files.size();
    }

    /**
     * Loads a thumbnail if the file is an image/video.
     * Otherwise, loads a specific icon based on the file type.
     */
    private void setFileIcon(ImageView imageView, CloudFile file) {
        String fileName = file.getName();
        String fileExtension = file.getFileExtension();
        String localPath = file.getPath();
        View iconContainer = (View) imageView.getParent();

        // ADD LOGGING HERE - This will help you debug
        android.util.Log.d("FileAdapter", "=== Loading File Icon ===");
        android.util.Log.d("FileAdapter", "File Name: " + fileName);
        android.util.Log.d("FileAdapter", "Extension: " + fileExtension);
        android.util.Log.d("FileAdapter", "Local Path: " + localPath);
        android.util.Log.d("FileAdapter", "Is Image: " + FileUtils.isImage(fileName));
        android.util.Log.d("FileAdapter", "Is Video: " + FileUtils.isVideo(fileName));

        // 1. Handle Images and Videos with Thumbnails
        if (localPath != null && !localPath.isEmpty() &&
                (FileUtils.isImage(fileName) || FileUtils.isVideo(fileName))) {

            android.util.Log.d("FileAdapter", "Loading THUMBNAIL for: " + fileName);

            try {
                // Clear background color to let the thumbnail fill the space
                iconContainer.setBackgroundColor(Color.TRANSPARENT);

                // Clear any color filter/tint
                imageView.setColorFilter(null);

                Uri fileUri = Uri.parse(localPath);

                android.util.Log.d("FileAdapter", "Parsed URI: " + fileUri.toString());

                Glide.with(context)
                        .load(fileUri)
                        .centerCrop()
                        .placeholder(R.drawable.ic_file_general)
                        .error(R.drawable.ic_file_general)
                        .listener(new com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable>() {
                            @Override
                            public boolean onLoadFailed(@androidx.annotation.Nullable com.bumptech.glide.load.engine.GlideException e, Object model, com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target, boolean isFirstResource) {
                                android.util.Log.e("FileAdapter", "Glide FAILED to load: " + fileName);
                                if (e != null) {
                                    android.util.Log.e("FileAdapter", "Error: " + e.getMessage());
                                    e.logRootCauses("FileAdapter");
                                }
                                return false;
                            }

                            @Override
                            public boolean onResourceReady(android.graphics.drawable.Drawable resource, Object model, com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target, com.bumptech.glide.load.DataSource dataSource, boolean isFirstResource) {
                                android.util.Log.d("FileAdapter", "Glide SUCCESS loading: " + fileName);
                                return false;
                            }
                        })
                        .override(200, 200) // Add size limit for better performance
                        .into(imageView);
                return;
            } catch (Exception e) {
                // If thumbnail loading fails, fall through to icon display
                android.util.Log.e("FileAdapter", "Error loading thumbnail: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            android.util.Log.d("FileAdapter", "NOT a media file, showing ICON instead");
        }

        // 2. Handle other file types with icons and colored backgrounds
        int iconRes;
        int colorRes;

        switch (fileExtension.toLowerCase()) {
            case "pdf":
                iconRes = R.drawable.ic_pdf;
                colorRes = Color.parseColor("#F44336");
                break;
            case "doc":
            case "docx":
                iconRes = android.R.drawable.ic_menu_edit;
                colorRes = Color.parseColor("#2196F3");
                break;
            case "xls":
            case "xlsx":
                iconRes = android.R.drawable.ic_menu_agenda;
                colorRes = Color.parseColor("#4CAF50");
                break;
            case "ppt":
            case "pptx":
                iconRes = android.R.drawable.ic_menu_slideshow;
                colorRes = Color.parseColor("#FF9800");
                break;
            case "mp3":
            case "wav":
            case "flac":
            case "aac":
                iconRes = android.R.drawable.ic_lock_silent_mode_off;
                colorRes = Color.parseColor("#00BCD4");
                break;
            case "zip":
            case "rar":
            case "7z":
                iconRes = android.R.drawable.ic_menu_save;
                colorRes = Color.parseColor("#795548");
                break;
            case "apk":
                iconRes = android.R.drawable.ic_menu_preferences;
                colorRes = Color.parseColor("#4CAF50");
                break;
            default:
                iconRes = R.drawable.ic_file_general;
                colorRes = Color.parseColor("#607D8B");
                break;
        }

        // Set the background color for the icon
        iconContainer.setBackgroundColor(colorRes);

        // Clear any tint first
        imageView.setColorFilter(null);

        // Load the icon drawable and apply white tint
        Glide.with(context)
                .load(iconRes)
                .into(new com.bumptech.glide.request.target.CustomTarget<android.graphics.drawable.Drawable>() {

                    public void onResourceReady(@androidx.annotation.NonNull android.graphics.drawable.Drawable resource, @androidx.annotation.Nullable com.bumptech.glide.load.DataSource dataSource, @androidx.annotation.Nullable com.bumptech.glide.request.transition.Transition<? super android.graphics.drawable.Drawable> transition) {
                        resource.setTint(Color.WHITE); // Apply white tint to icons only
                        imageView.setImageDrawable(resource);
                    }

                    @Override
                    public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {

                    }

                    @Override
                    public void onLoadCleared(@androidx.annotation.Nullable android.graphics.drawable.Drawable placeholder) {
                        imageView.setImageDrawable(placeholder);
                    }
                });
    }

    private void showFileOptionsMenu(CloudFile file, int position) {
        CharSequence[] options = file.isUploaded() ?
                new CharSequence[]{
                        context.getString(R.string.menu_download_file),
                        context.getString(R.string.menu_share_file),
                        context.getString(R.string.menu_remove_vault)
                } :
                new CharSequence[]{
                        context.getString(R.string.menu_cancel_upload),
                        context.getString(R.string.menu_remove_list)
                };

        AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.AppTheme_AlertDialogTheme);

        builder.setTitle(file.getName());
        builder.setItems(options, (dialog, which) -> {
            if (file.isUploaded()) {
                handleUploadedFileAction(file, position, which);
            } else {
                handleUploadingFileAction(file, position, which);
            }
        });
        builder.setNegativeButton(R.string.cancel, null).show();
    }

    private void handleUploadedFileAction(CloudFile file, int position, int actionIndex) {
        switch (actionIndex) {
            case 0: // Download
                if (actionListener != null && file.canDownload()) actionListener.onDownloadFile(file);
                break;
            case 1: // Share File
                if (actionListener != null) actionListener.onShareFileAction(file);
                break;
            case 2: // Remove
                confirmRemoveFile(file, position);
                break;
        }
    }

    private void handleUploadingFileAction(CloudFile file, int position, int actionIndex) {
        switch (actionIndex) {
            case 0: // Cancel Upload
                if (actionListener != null) {
                    actionListener.onCancelUpload(file, position);
                }
                break;
            case 1: // Remove from List
                confirmRemoveFile(file, position);
                break;
        }
    }

    private void confirmRemoveFile(CloudFile file, int position) {
        String message = file.isUploaded() ?
                context.getString(R.string.confirm_delete_uploaded, file.getName()) :
                context.getString(R.string.confirm_delete_unuploaded, file.getName());

        new AlertDialog.Builder(context, R.style.AppTheme_AlertDialogTheme)
                .setTitle(R.string.confirm_delete_title)
                .setMessage(message)
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    if (actionListener != null) {
                        actionListener.onDeleteFile(file, position);
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    /**
     * Show dialog to confirm image viewing
     */
    private void showImageViewerDialog(CloudFile file) {
        if (!file.canDownload()) {
            Toast.makeText(context, R.string.file_not_available, Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(context, R.style.AppTheme_AlertDialogTheme)
                .setTitle(R.string.view_image_dialog_title)
                .setMessage(R.string.view_image_dialog_message)
                .setPositiveButton(R.string.view_image_yes, (dialog, which) -> {
                    openImageViewer(file);
                })
                .setNegativeButton(R.string.view_image_no, null)
                .show();
    }

    /**
     * Launch ImageViewerActivity
     */
    private void openImageViewer(CloudFile file) {
        Toast.makeText(context, R.string.starting_image_viewer, Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(context, ImageViewerActivity.class);
        intent.putExtra(ImageViewerActivity.EXTRA_FILE_ID, file.getFileId());
        intent.putExtra(ImageViewerActivity.EXTRA_FILE_NAME, file.getName());
        intent.putExtra(ImageViewerActivity.EXTRA_BOT_TOKEN, SettingsUtils.getBotToken(context));
        context.startActivity(intent);
    }

    // ==================== VIDEO METHODS (MODIFIED) ====================

    /**
     * Show dialog to confirm video viewing
     */
    private void showVideoViewerDialog(CloudFile file) {
        if (!file.canDownload()) {
            Toast.makeText(context, R.string.file_not_available, Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(context, R.style.AppTheme_AlertDialogTheme)
                .setTitle(R.string.view_video_dialog_title) // Uses string
                .setMessage(R.string.view_video_dialog_message) // Uses string
                .setPositiveButton(R.string.view_image_yes, (dialog, which) -> {
                    // *** MODIFIED: Calls the new download method ***
                    downloadAndLaunchVideo(file);
                })
                .setNegativeButton(R.string.view_image_no, null)
                .show();
    }

    /**
     * Replaces openVideoViewer.
     * Downloads the video and then launches an ACTION_VIEW intent.
     */
    private void downloadAndLaunchVideo(CloudFile file) {
        // 1. Show a progress dialog (reusing strings from MainActivity)
        final AlertDialog progressDialog = new AlertDialog.Builder(context, R.style.AppTheme_AlertDialogTheme)
                .setTitle(R.string.downloading_dialog_title)
                .setMessage(context.getString(R.string.downloading_dialog_message, file.getName(), 0))
                .setCancelable(false)
                .create();
        progressDialog.show();

        String botToken = SettingsUtils.getBotToken(context);

        // 2. Start the download
        new Thread(() -> {
            TelegramDownloader downloader = new TelegramDownloader(
                    context.getApplicationContext(),
                    botToken,
                    file.getFileId(),
                    file.getName(),
                    new TelegramDownloader.DownloadCallback() {
                        @Override
                        public void onDownloadProgress(int progress) {
                            // Update progress dialog
                            if (context instanceof AppCompatActivity) {
                                ((AppCompatActivity) context).runOnUiThread(() ->
                                        progressDialog.setMessage(context.getString(R.string.downloading_dialog_message, file.getName(), progress))
                                );
                            }
                        }

                        @Override
                        public void onDownloadSuccess(Uri fileUri) {
                            if (context instanceof AppCompatActivity) {
                                ((AppCompatActivity) context).runOnUiThread(() -> {
                                    progressDialog.dismiss();
                                    try {
                                        File downloadedFile = new File(fileUri.getPath());

                                        // 3. Get a content:// URI from FileProvider
                                        Uri contentUri = FileProvider.getUriForFile(
                                                context,
                                                context.getPackageName() + ".fileprovider",
                                                downloadedFile
                                        );

                                        // 4. Create and launch the ACTION_VIEW intent
                                        Intent viewIntent = new Intent(Intent.ACTION_VIEW);
                                        viewIntent.setDataAndType(contentUri, FileUtils.getMimeType(file.getName()));
                                        viewIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                                        // Use string from MainActivity
                                        context.startActivity(Intent.createChooser(viewIntent, context.getString(R.string.open_file)));

                                        // 5. Schedule the temp file for deletion
                                        scheduleTempFileDeletion(downloadedFile);

                                    } catch (Exception e) {
                                        Log.e("FileAdapter", "Error launching video player", e);
                                        // Use string from MainActivity
                                        Toast.makeText(context, R.string.error_no_app_to_open, Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                        }

                        @Override
                        public void onDownloadFailed(String error) {
                            if (context instanceof AppCompatActivity) {
                                ((AppCompatActivity) context).runOnUiThread(() -> {
                                    progressDialog.dismiss();
                                    // Use strings from MainActivity
                                    new AlertDialog.Builder(context, R.style.AppTheme_AlertDialogTheme)
                                            .setTitle(R.string.download_failed)
                                            .setMessage(context.getString(R.string.download_failed, error))
                                            .setPositiveButton(R.string.ok, null)
                                            .show();
                                });
                            }
                        }
                    }
            );
            downloader.download();
        }).start();
    }

    /**
     * Helper method to delete the temp file after a delay.
     * (Copied from MainActivity)
     */
    private void scheduleTempFileDeletion(File file) {
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            if (file != null && file.exists()) {
                if (file.delete()) {
                    android.util.Log.i("FileAdapter", "Temp video file deleted: " + file.getName());
                } else {
                    android.util.Log.w("FileAdapter", "Failed to delete temp video: " + file.getName());
                }
            }
        }, 15000); // 15-second delay
    }

    // ==================== VIEW HOLDER ====================

    static class FileViewHolder extends RecyclerView.ViewHolder {
        ImageView ivFileIcon;
        TextView tvFileName, tvFileSize, tvFileDate, tvUploadProgress;
        View btnFileMenu;
        LinearLayout layoutUploadStatus, layoutCloudStatus;
        ProgressBar progressBarUpload;

        public FileViewHolder(@NonNull View itemView) {
            super(itemView);
            ivFileIcon = itemView.findViewById(R.id.ivFileIcon);
            tvFileName = itemView.findViewById(R.id.tvFileName);
            tvFileSize = itemView.findViewById(R.id.tvFileSize);
            tvFileDate = itemView.findViewById(R.id.tvFileDate);
            btnFileMenu = itemView.findViewById(R.id.btnFileMenu);
            layoutUploadStatus = itemView.findViewById(R.id.layoutUploadStatus);
            layoutCloudStatus = itemView.findViewById(R.id.layoutCloudStatus);
        }
    }
}