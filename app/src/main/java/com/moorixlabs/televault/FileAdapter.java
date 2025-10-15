package com.moorixlabs.televault;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class FileAdapter extends RecyclerView.Adapter<FileAdapter.FileViewHolder> {

    private Context context;
    private List<CloudFile> files;
    private SimpleDateFormat dateFormat;
    private FileActionListener actionListener;

    public interface FileActionListener {
        void onDownloadFile(CloudFile file);
        void onDeleteFile(CloudFile file, int position);
        void onShareFileAction(CloudFile file); // Replaces the old onShareFile
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

        // Set file name
        holder.tvFileName.setText(file.getName());

        // Set file size
        holder.tvFileSize.setText(FileUtils.formatFileSize(file.getSize()));

        // Set file date
        holder.tvFileDate.setText(dateFormat.format(new Date(file.getDate())));

        // Set file icon based on extension
        setFileIcon(holder.ivFileIcon, file.getFileExtension());

        // Handle upload progress vs completed status
        if (file.isUploaded()) {
            holder.layoutUploadStatus.setVisibility(View.GONE);
            holder.layoutUploadComplete.setVisibility(View.VISIBLE);
        } else {
            holder.layoutUploadStatus.setVisibility(View.VISIBLE);
            holder.layoutUploadComplete.setVisibility(View.GONE);
            holder.progressBarUpload.setProgress(file.getUploadProgress());
            holder.tvUploadProgress.setText(file.getUploadProgress() + "%");
        }

        // 3-dot menu button click
        holder.btnFileMenu.setOnClickListener(v -> showFileOptionsMenu(file, position));

        // Item click - Show info
        holder.itemView.setOnClickListener(v -> {
            if (file.isUploaded()) {
                Toast.makeText(context,
                        "Tap the ⋮ button for options",
                        Toast.LENGTH_SHORT).show();
            } else {
                int progress = file.getUploadProgress();
                String message = progress > 0 ?
                        "Upload in progress: " + progress + "%" :
                        "Upload starting...";
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
            }
        });

        // Long click - Quick options
        holder.itemView.setOnLongClickListener(v -> {
            showFileOptionsMenu(file, position);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return files.size();
    }

    private void setFileIcon(ImageView imageView, String extension) {
        int iconRes;
        int colorRes;

        switch (extension.toLowerCase()) {
            case "pdf":
                iconRes = android.R.drawable.ic_menu_report_image;
                colorRes = Color.parseColor("#F44336"); // Red
                break;
            case "doc":
            case "docx":
                iconRes = android.R.drawable.ic_menu_edit;
                colorRes = Color.parseColor("#2196F3"); // Blue
                break;
            case "xls":
            case "xlsx":
                iconRes = android.R.drawable.ic_menu_agenda;
                colorRes = Color.parseColor("#4CAF50"); // Green
                break;
            case "ppt":
            case "pptx":
                iconRes = android.R.drawable.ic_menu_slideshow;
                colorRes = Color.parseColor("#FF9800"); // Orange
                break;
            case "jpg":
            case "jpeg":
            case "png":
            case "gif":
            case "bmp":
            case "webp":
                iconRes = android.R.drawable.ic_menu_gallery;
                colorRes = Color.parseColor("#9C27B0"); // Purple
                break;
            case "mp4":
            case "avi":
            case "mkv":
            case "mov":
                iconRes = android.R.drawable.ic_menu_camera;
                colorRes = Color.parseColor("#E91E63"); // Pink
                break;
            case "mp3":
            case "wav":
            case "flac":
            case "aac":
                iconRes = android.R.drawable.ic_lock_silent_mode_off;
                colorRes = Color.parseColor("#00BCD4"); // Cyan
                break;
            case "zip":
            case "rar":
            case "7z":
                iconRes = android.R.drawable.ic_menu_save;
                colorRes = Color.parseColor("#795548"); // Brown
                break;
            case "apk":
                iconRes = android.R.drawable.ic_menu_preferences;
                colorRes = Color.parseColor("#4CAF50"); // Green
                break;
            case "txt":
                iconRes = android.R.drawable.ic_menu_edit;
                colorRes = Color.parseColor("#607D8B"); // Grey
                break;
            default:
                iconRes = android.R.drawable.ic_menu_info_details;
                colorRes = Color.parseColor("#607D8B"); // Grey
                break;
        }

        imageView.setImageResource(iconRes);
        ((View) imageView.getParent()).setBackgroundColor(colorRes);
    }

    private void showFileOptionsMenu(CloudFile file, int position) {
        CharSequence[] options;

        if (file.isUploaded()) {
            // Options for uploaded files
            options = new CharSequence[]{
                    "📥 Download File",
                    "🔗 Share File",         // Renamed from "Share Link"
                    "🗑️ Remove from Vault"
                    // "Copy File Info" is now deleted
            };
        } else {
            // Options for files currently uploading
            options = new CharSequence[]{
                    "⏸️ Cancel Upload (Soon)",
                    "🗑️ Remove from List"
            };
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(file.getName());

        // Add file info subtitle
        String subtitle = FileUtils.formatFileSize(file.getSize()) +
                " • " + dateFormat.format(new Date(file.getDate()));

        if (file.isUploaded()) {
            subtitle += "\n✓ Uploaded to Cloud";
        } else {
            subtitle += "\n⬆️ Uploading " + file.getUploadProgress() + "%";
        }

        //builder.setMessage(subtitle);

        builder.setItems(options, (dialog, which) -> {
            if (file.isUploaded()) {
                handleUploadedFileAction(file, position, which);
            } else {
                handleUploadingFileAction(file, position, which);
            }
        });

        builder.setNegativeButton("Cancel", null);

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void handleUploadedFileAction(CloudFile file, int position, int actionIndex) {
        switch (actionIndex) {
            case 0: // Download
                if (actionListener != null && file.canDownload()) {
                    actionListener.onDownloadFile(file);
                }
                break;

            case 1: // Share File
                if (actionListener != null) {
                    actionListener.onShareFileAction(file);
                }
                break;

            case 2: // Remove from List
                confirmRemoveFile(file, position);
                break;
        }
    }
    private void handleUploadingFileAction(CloudFile file, int position, int actionIndex) {
        switch (actionIndex) {
            case 0: // Cancel Upload
                Toast.makeText(context,
                        "Upload cancellation feature coming soon",
                        Toast.LENGTH_SHORT).show();
                // TODO: Implement upload cancellation
                break;

            case 1: // Remove from List
                confirmRemoveFile(file, position);
                break;
        }
    }



    // Inside confirmRemoveFile method in FileAdapter.java
    private void confirmRemoveFile(CloudFile file, int position) {
        String message;
        if (file.isUploaded()) {
            // This is the new, stronger warning message
            message = "This will permanently delete '" + file.getName() + "' from your Telegram channel and this app.\n\nThis action cannot be undone.";
        } else {
            message = "Remove '" + file.getName() + "' from the app?\n\n" +
                    "The upload is still in progress and will be cancelled."; // Adjusted for clarity
        }

        new AlertDialog.Builder(context)
                .setTitle("Permanently Delete File?") // Changed title
                .setMessage(message)
                .setPositiveButton("Delete", (dialog, which) -> { // Changed button text
                    if (actionListener != null) {
                        actionListener.onDeleteFile(file, position);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    static class FileViewHolder extends RecyclerView.ViewHolder {
        ImageView ivFileIcon;
        TextView tvFileName;
        TextView tvFileSize;
        TextView tvFileDate;
        LinearLayout btnFileMenu;  // Changed from ImageButton to LinearLayout
        LinearLayout layoutUploadStatus;
        LinearLayout layoutUploadComplete;
        ProgressBar progressBarUpload;
        TextView tvUploadProgress;

        public FileViewHolder(@NonNull View itemView) {
            super(itemView);
            ivFileIcon = itemView.findViewById(R.id.ivFileIcon);
            tvFileName = itemView.findViewById(R.id.tvFileName);
            tvFileSize = itemView.findViewById(R.id.tvFileSize);
            tvFileDate = itemView.findViewById(R.id.tvFileDate);
            btnFileMenu = itemView.findViewById(R.id.btnFileMenu);
            layoutUploadStatus = itemView.findViewById(R.id.layoutUploadStatus);
            layoutUploadComplete = itemView.findViewById(R.id.layoutUploadComplete);
            progressBarUpload = itemView.findViewById(R.id.progressBarUpload);
            tvUploadProgress = itemView.findViewById(R.id.tvUploadProgress);
        }
    }
}