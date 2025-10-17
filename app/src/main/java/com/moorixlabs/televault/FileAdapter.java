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

    private final Context context;
    private final List<CloudFile> files;
    private final SimpleDateFormat dateFormat;
    private final FileActionListener actionListener;

    public interface FileActionListener {
        void onDownloadFile(CloudFile file);
        void onDeleteFile(CloudFile file, int position);
        void onShareFileAction(CloudFile file);
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

        holder.tvFileName.setText(file.getName());
        holder.tvFileSize.setText(FileUtils.formatFileSize(file.getSize()));
        holder.tvFileDate.setText(dateFormat.format(new Date(file.getDate())));
        setFileIcon(holder.ivFileIcon, file.getFileExtension());

        if (file.isUploaded()) {
            holder.layoutUploadStatus.setVisibility(View.GONE);
            // Use the correct layout for the cloud status icon
            holder.layoutCloudStatus.setVisibility(View.VISIBLE);
        } else {
            holder.layoutCloudStatus.setVisibility(View.GONE);
            holder.layoutUploadStatus.setVisibility(View.VISIBLE);
            holder.progressBarUpload.setProgress(file.getUploadProgress());
            holder.tvUploadProgress.setText(file.getUploadProgress() + "%");
        }

        holder.btnFileMenu.setOnClickListener(v -> showFileOptionsMenu(file, position));
        holder.itemView.setOnClickListener(v -> {
            if (file.isUploaded()) {
                Toast.makeText(context, "Tap the ⋮ button for options", Toast.LENGTH_SHORT).show();
            } else {
                int progress = file.getUploadProgress();
                String message = progress > 0 ? "Upload in progress: " + progress + "%" : "Upload starting...";
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
            }
        });
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
            case "pdf": iconRes = android.R.drawable.ic_menu_report_image; colorRes = Color.parseColor("#F44336"); break;
            case "doc": case "docx": iconRes = android.R.drawable.ic_menu_edit; colorRes = Color.parseColor("#2196F3"); break;
            case "xls": case "xlsx": iconRes = android.R.drawable.ic_menu_agenda; colorRes = Color.parseColor("#4CAF50"); break;
            case "ppt": case "pptx": iconRes = android.R.drawable.ic_menu_slideshow; colorRes = Color.parseColor("#FF9800"); break;
            case "jpg": case "jpeg": case "png": case "gif": case "bmp": case "webp": iconRes = android.R.drawable.ic_menu_gallery; colorRes = Color.parseColor("#9C27B0"); break;
            case "mp4": case "avi": case "mkv": case "mov": iconRes = android.R.drawable.ic_menu_camera; colorRes = Color.parseColor("#E91E63"); break;
            case "mp3": case "wav": case "flac": case "aac": iconRes = android.R.drawable.ic_lock_silent_mode_off; colorRes = Color.parseColor("#00BCD4"); break;
            case "zip": case "rar": case "7z": iconRes = android.R.drawable.ic_menu_save; colorRes = Color.parseColor("#795548"); break;
            case "apk": iconRes = android.R.drawable.ic_menu_preferences; colorRes = Color.parseColor("#4CAF50"); break;
            default: iconRes = android.R.drawable.ic_menu_info_details; colorRes = Color.parseColor("#607D8B"); break;
        }
        imageView.setImageResource(iconRes);
        ((View) imageView.getParent()).setBackgroundColor(colorRes);
    }

    private void showFileOptionsMenu(CloudFile file, int position) {
        CharSequence[] options = file.isUploaded() ?
                new CharSequence[]{"📥 Download File", "🔗 Share File", "🗑️ Remove from Vault"} :
                new CharSequence[]{"⏸️ Cancel Upload (Soon)", "🗑️ Remove from List"};

        // APPLY THE CUSTOM THEME HERE
        AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.AppTheme_AlertDialogTheme);

        builder.setTitle(file.getName());
        builder.setItems(options, (dialog, which) -> {
            if (file.isUploaded()) {
                handleUploadedFileAction(file, position, which);
            } else {
                handleUploadingFileAction(file, position, which);
            }
        });
        builder.setNegativeButton("Cancel", null).show();
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
                Toast.makeText(context, "Upload cancellation coming soon", Toast.LENGTH_SHORT).show();
                break;
            case 1: // Remove from List
                confirmRemoveFile(file, position);
                break;
        }
    }

    private void confirmRemoveFile(CloudFile file, int position) {
        String message = file.isUploaded() ?
                "This will permanently delete '" + file.getName() + "' from your Telegram channel and this app.\n\nThis action cannot be undone." :
                "Remove '" + file.getName() + "' from the app? The upload will be cancelled.";

        new AlertDialog.Builder(context, R.style.AppTheme_AlertDialogTheme)
                .setTitle("Permanently Delete File?")
                .setMessage(message)
                .setPositiveButton("Delete", (dialog, which) -> {
                    if (actionListener != null) {
                        actionListener.onDeleteFile(file, position);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    static class FileViewHolder extends RecyclerView.ViewHolder {
        ImageView ivFileIcon;
        TextView tvFileName, tvFileSize, tvFileDate, tvUploadProgress;
        LinearLayout btnFileMenu, layoutUploadStatus, layoutCloudStatus; // Corrected
        ProgressBar progressBarUpload;

        public FileViewHolder(@NonNull View itemView) {
            super(itemView);
            ivFileIcon = itemView.findViewById(R.id.ivFileIcon);
            tvFileName = itemView.findViewById(R.id.tvFileName);
            tvFileSize = itemView.findViewById(R.id.tvFileSize);
            tvFileDate = itemView.findViewById(R.id.tvFileDate);
            btnFileMenu = itemView.findViewById(R.id.btnFileMenu);
            layoutUploadStatus = itemView.findViewById(R.id.layoutUploadStatus);
            // This now points to the correct small cloud icon layout
            layoutCloudStatus = itemView.findViewById(R.id.layoutCloudStatus);
            progressBarUpload = itemView.findViewById(R.id.progressBarUpload);
            tvUploadProgress = itemView.findViewById(R.id.tvUploadProgress);
        }
    }
}