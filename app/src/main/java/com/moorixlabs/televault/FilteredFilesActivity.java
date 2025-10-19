package com.moorixlabs.televault;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class FilteredFilesActivity extends AppCompatActivity implements FileAdapter.FileActionListener {

    public static final String EXTRA_FILE_TYPE = "file_type";
    public static final String TYPE_IMAGE = "IMAGE";
    public static final String TYPE_VIDEO = "VIDEO";
    public static final String TYPE_PDF = "PDF";
    public static final String TYPE_OTHER = "OTHER";

    private String botToken;
    private String chatId;

    private static final String FILES_DATABASE = "televault_files.txt";
    private static final String APP_FOLDER = "TeleVault";
    private static final String ZARCHIVER_PACKAGE_NAME = "ru.zdevs.zarchiver";

    private RecyclerView recyclerViewFiles;
    private TextView tvFilteredTitle;
    private ImageButton btnBack, btnSort;

    private FileAdapter fileAdapter;
    private List<CloudFile> allCloudFiles;
    private List<CloudFile> filteredFiles;
    private String currentFilterType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_filtered_files);

        botToken = SettingsUtils.getBotToken(this);
        chatId = SettingsUtils.getChatId(this);

        currentFilterType = getIntent().getStringExtra(EXTRA_FILE_TYPE);
        if (currentFilterType == null) {
            finish();
            return;
        }

        initializeViews();
        setupRecyclerView();
        setupClickListeners();

        loadAndFilterFiles();
    }

    private void initializeViews() {
        recyclerViewFiles = findViewById(R.id.recyclerViewFilteredFiles);
        tvFilteredTitle = findViewById(R.id.tvFilteredTitle);
        btnBack = findViewById(R.id.btnBack);
        btnSort = findViewById(R.id.btnSortFiltered);

        String typeDisplay;
        switch (currentFilterType) {
            case TYPE_IMAGE:
                typeDisplay = getString(R.string.images);
                break;
            case TYPE_VIDEO:
                typeDisplay = getString(R.string.videos);
                break;
            case TYPE_PDF:
                typeDisplay = getString(R.string.pdfs);
                break;
            case TYPE_OTHER:
                typeDisplay = getString(R.string.other);
                break;
            default:
                typeDisplay = "Files";
        }
        tvFilteredTitle.setText(getString(R.string.vault_title_format, typeDisplay));
    }

    private void setupRecyclerView() {
        allCloudFiles = new ArrayList<>();
        filteredFiles = new ArrayList<>();
        fileAdapter = new FileAdapter(this, filteredFiles, this);

        recyclerViewFiles.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewFiles.setAdapter(fileAdapter);
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());
        btnSort.setOnClickListener(v -> showSortDialog());
    }

    // ==================== FILTERING & LOADING ====================

    private void loadAndFilterFiles() {
        if (!hasStoragePermission()) {
            Toast.makeText(this, R.string.error_loading_files, Toast.LENGTH_LONG).show();
            return;
        }

        allCloudFiles.clear();
        filteredFiles.clear();

        File dbFile = getDatabaseFile();
        if (dbFile.exists()) {
            try (FileInputStream fis = new FileInputStream(dbFile);
                 BufferedReader reader = new BufferedReader(new InputStreamReader(fis))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    CloudFile file = parseFileLine(line.split("\\|"));
                    if (file != null) allCloudFiles.add(file);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        for (CloudFile file : allCloudFiles) {
            String fileName = file.getName();
            boolean matches = false;

            if (currentFilterType.equals(TYPE_IMAGE) && FileUtils.isImage(fileName)) {
                matches = true;
            } else if (currentFilterType.equals(TYPE_VIDEO) && FileUtils.isVideo(fileName)) {
                matches = true;
            } else if (currentFilterType.equals(TYPE_PDF) && "pdf".equals(file.getFileExtension())) {
                matches = true;
            } else if (currentFilterType.equals(TYPE_OTHER) &&
                    !FileUtils.isImage(fileName) && !FileUtils.isVideo(fileName) && !"pdf".equals(file.getFileExtension())) {
                matches = true;
            }

            if (matches) {
                filteredFiles.add(file);
            }
        }

        filteredFiles.sort((f1, f2) -> Long.compare(f2.getDate(), f1.getDate()));
        fileAdapter.notifyDataSetChanged();

        if (filteredFiles.isEmpty()) {
            String filterName;
            switch (currentFilterType) {
                case TYPE_IMAGE: filterName = getString(R.string.images); break;
                case TYPE_VIDEO: filterName = getString(R.string.videos); break;
                case TYPE_PDF: filterName = getString(R.string.pdfs); break;
                default: filterName = getString(R.string.other); break;
            }
            Toast.makeText(this, getString(R.string.filtered_empty_message, filterName), Toast.LENGTH_LONG).show();
        }
    }

    // Utility methods (copied from MainActivity)
    private File getAppFolder() {
        File documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
        return new File(documentsDir, APP_FOLDER);
    }

    private File getDatabaseFile() {
        return new File(getAppFolder(), FILES_DATABASE);
    }

    private boolean hasStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager();
        } else {
            return true;
        }
    }

    private CloudFile parseFileLine(String[] parts) {
        try {
            if (parts.length >= 8) {
                return new CloudFile(parts[0], parts[1], Long.parseLong(parts[2]), Long.parseLong(parts[3]),
                        parts[4], Boolean.parseBoolean(parts[5]), parts[6], parts[7]);
            } else if (parts.length == 6) {
                return new CloudFile(parts[0], parts[1], Long.parseLong(parts[2]), Long.parseLong(parts[3]),
                        parts[4], Boolean.parseBoolean(parts[5]));
            }
        } catch (Exception e) {
            Log.e("FilteredActivity", "Error parsing file line: " + e.getMessage());
        }
        return null;
    }

    // ==================== ACTIONS (Delegated from FileAdapter) ====================

    @Override
    public void onDownloadFile(CloudFile file) {
        downloadFromTelegram(file);
    }

    @Override
    public void onDeleteFile(CloudFile file, int position) {
        deleteFileFromVault(file, position);
    }

    @Override
    public void onShareFileAction(CloudFile file) {
        shareDownloadedFile(file);
    }

    // ==================== CORE LOGIC (Copied/Adapted from MainActivity) ====================

    private void deleteFileFromVault(CloudFile file, int position) {
        if (!file.isUploaded() || file.getMessageId() == null || file.getMessageId().isEmpty()) {
            filteredFiles.remove(position);
            fileAdapter.notifyItemRemoved(position);
            deleteFileFromDatabase(file);
            Snackbar.make(findViewById(android.R.id.content),
                    getString(R.string.removed_local_snackbar, file.getName()),
                    Snackbar.LENGTH_SHORT).show();
            return;
        }

        if (!SettingsUtils.isConfigured(this)) {
            Toast.makeText(this, R.string.settings_required_message, Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog progressDialog = new AlertDialog.Builder(this)
                .setTitle(R.string.deletion_dialog_title)
                .setMessage(R.string.deletion_dialog_message)
                .setCancelable(false)
                .create();
        progressDialog.show();

        new Thread(() -> {
            TelegramDeleter deleter = new TelegramDeleter(
                    botToken, chatId, file.getMessageId(),
                    new TelegramDeleter.DeleteCallback() {
                        @Override
                        public void onDeleteSuccess() {
                            runOnUiThread(() -> {
                                progressDialog.dismiss();
                                filteredFiles.remove(position);
                                fileAdapter.notifyItemRemoved(position);
                                deleteFileFromDatabase(file);
                                Snackbar.make(findViewById(android.R.id.content),
                                        getString(R.string.delete_success_snackbar, file.getName()),
                                        Snackbar.LENGTH_LONG).show();
                            });
                        }

                        @Override
                        public void onDeleteFailed(String error) {
                            runOnUiThread(() -> {
                                progressDialog.dismiss();
                                new AlertDialog.Builder(FilteredFilesActivity.this)
                                        .setTitle(R.string.deletion_failed_title)
                                        .setMessage(getString(R.string.deletion_failed_message, error))
                                        .setPositiveButton(R.string.ok, null)
                                        .show();
                            });
                        }
                    }
            );
            deleter.delete();
        }).start();
    }

    private void downloadFromTelegram(CloudFile file) {
        if (!SettingsUtils.isConfigured(this)) {
            Toast.makeText(this, R.string.settings_required_message, Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog progressDialog = new AlertDialog.Builder(this)
                .setTitle(R.string.downloading_dialog_title)
                .setMessage(getString(R.string.downloading_dialog_message, file.getName(), 0))
                .setCancelable(false)
                .create();
        progressDialog.show();

        new Thread(() -> {
            TelegramDownloader downloader = new TelegramDownloader(
                    getApplicationContext(), botToken, file.getFileId(), file.getName(),
                    new TelegramDownloader.DownloadCallback() {
                        @Override
                        public void onDownloadProgress(int progress) {
                            runOnUiThread(() -> progressDialog.setMessage(getString(R.string.downloading_dialog_message, file.getName(), progress)));
                        }

                        @Override
                        public void onDownloadSuccess(Uri fileUri) {
                            runOnUiThread(() -> {
                                progressDialog.dismiss();
                                new AlertDialog.Builder(FilteredFilesActivity.this)
                                        .setTitle(R.string.download_complete_title)
                                        .setMessage(getString(R.string.download_complete_message, file.getName()))
                                        .setPositiveButton(R.string.open_file, (dialog, which) -> openDownloadedFile(fileUri, file.getName()))
                                        .setNegativeButton(R.string.open_folder, (dialog, which) -> openDownloadFolder())
                                        .setNeutralButton(R.string.cancel, null)
                                        .show();
                            });
                        }

                        @Override
                        public void onDownloadFailed(String error) {
                            runOnUiThread(() -> {
                                progressDialog.dismiss();
                                new AlertDialog.Builder(FilteredFilesActivity.this)
                                        .setTitle(R.string.download_failed)
                                        .setMessage(getString(R.string.download_failed, error))
                                        .setPositiveButton(R.string.ok, null)
                                        .show();
                            });
                        }
                    }
            );
            downloader.download();
        }).start();
    }

    private void shareDownloadedFile(CloudFile file) {
        if (!SettingsUtils.isConfigured(this)) {
            Toast.makeText(this, R.string.settings_required_message, Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog progressDialog = new AlertDialog.Builder(this)
                .setTitle(R.string.share_preparing_title)
                .setMessage(getString(R.string.downloading_dialog_message, file.getName(), 0))
                .setCancelable(false)
                .create();
        progressDialog.show();

        new Thread(() -> {
            TelegramDownloader downloader = new TelegramDownloader(
                    getApplicationContext(), botToken, file.getFileId(), file.getName(),
                    new TelegramDownloader.DownloadCallback() {
                        @Override
                        public void onDownloadProgress(int progress) {}

                        @Override
                        public void onDownloadSuccess(Uri fileUri) {
                            runOnUiThread(() -> {
                                progressDialog.dismiss();
                                try {
                                    File fileToShare = new File(fileUri.getPath());
                                    Uri contentUri = FileProvider.getUriForFile(FilteredFilesActivity.this, getPackageName() + ".fileprovider", fileToShare);
                                    Intent shareIntent = new Intent(Intent.ACTION_SEND);
                                    shareIntent.setType(FileUtils.getMimeType(file.getName()));
                                    shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
                                    shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                    startActivity(Intent.createChooser(shareIntent, getString(R.string.menu_share_file)));
                                    scheduleTempFileDeletion(fileToShare);
                                } catch (Exception e) {
                                    Toast.makeText(FilteredFilesActivity.this, getString(R.string.error_sharing_file, e.getMessage()), Toast.LENGTH_LONG).show();
                                }
                            });
                        }

                        @Override
                        public void onDownloadFailed(String error) {
                            runOnUiThread(() -> {
                                progressDialog.dismiss();
                                new AlertDialog.Builder(FilteredFilesActivity.this)
                                        .setTitle(R.string.share_failed_title)
                                        .setMessage(getString(R.string.share_failed_message, error))
                                        .setPositiveButton(R.string.ok, null)
                                        .show();
                            });
                        }
                    }
            );
            downloader.download();
        }).start();
    }

    private void openDownloadedFile(Uri fileUri, String fileName) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            if (fileUri.getScheme() != null && fileUri.getScheme().equals("file")) {
                File file = new File(fileUri.getPath());
                Uri contentUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file);
                intent.setDataAndType(contentUri, FileUtils.getMimeType(fileName));
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } else {
                intent.setDataAndType(fileUri, FileUtils.getMimeType(fileName));
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            }
            startActivity(Intent.createChooser(intent, getString(R.string.open_file)));
        } catch (Exception e) {
            Toast.makeText(this, R.string.error_no_app_to_open, Toast.LENGTH_SHORT).show();
        }
    }

    private void openDownloadFolder() {
        File downloadFolder = new File(getAppFolder(), "Download");
        if (!downloadFolder.exists()) {
            Toast.makeText(this, R.string.download_folder_not_found, Toast.LENGTH_SHORT).show();
            return;
        }
        Uri folderUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", downloadFolder);
        Intent zarchiverIntent = new Intent(Intent.ACTION_VIEW);
        zarchiverIntent.setData(folderUri);
        zarchiverIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        zarchiverIntent.setPackage(ZARCHIVER_PACKAGE_NAME);

        if (zarchiverIntent.resolveActivity(getPackageManager()) != null) {
            startActivity(zarchiverIntent);
        } else {
            Toast.makeText(this, R.string.zarchiver_not_found, Toast.LENGTH_LONG).show();
            Intent generalIntent = new Intent(Intent.ACTION_VIEW);
            generalIntent.setData(folderUri);
            generalIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            Intent chooser = Intent.createChooser(generalIntent, getString(R.string.open_folder));
            if (chooser.resolveActivity(getPackageManager()) != null) {
                startActivity(chooser);
            } else {
                showFolderPathDialog(downloadFolder);
            }
        }
    }

    private void showFolderPathDialog(File downloadFolder) {
        String folderPath = downloadFolder.getAbsolutePath();
        new AlertDialog.Builder(this)
                .setTitle(R.string.download_folder_dialog_title)
                .setMessage(getString(R.string.download_folder_path_message, folderPath))
                .setPositiveButton(R.string.copy_path, (dialog, which) -> {
                    android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    android.content.ClipData clip = android.content.ClipData.newPlainText("Folder Path", folderPath);
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(this, R.string.path_copied, Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(R.string.ok, null)
                .show();
    }

    @Override
    public void onCancelUpload(CloudFile file, int position) {
        // "Cancelling" from this screen just removes it from the list and database.
        // The main activity's upload will eventually stop on its own.
        filteredFiles.remove(position);
        fileAdapter.notifyItemRemoved(position);
        deleteFileFromDatabase(file);

        Snackbar.make(findViewById(android.R.id.content),
                getString(R.string.removed_local_snackbar, file.getName()),
                Snackbar.LENGTH_SHORT).show();
    }

    private void scheduleTempFileDeletion(File file) {
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            if (file != null && file.exists()) {
                if (file.delete()) {
                    android.util.Log.i("FileCleanup", "Temp share file deleted: " + file.getName());
                } else {
                    android.util.Log.w("FileCleanup", "Failed to delete temp file: " + file.getName());
                }
            }
        }, 15000);
    }

    private void deleteFileFromDatabase(CloudFile file) {
        if (!hasStoragePermission()) return;
        List<CloudFile> filesToRewrite = new ArrayList<>();
        File dbFile = getDatabaseFile();
        if (dbFile.exists()) {
            try (FileInputStream fis = new FileInputStream(dbFile);
                 BufferedReader reader = new BufferedReader(new InputStreamReader(fis))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split("\\|");
                    if (parts.length >= 6 && !parts[0].equals(file.getId())) {
                        CloudFile existingFile = parseFileLine(parts);
                        if (existingFile != null) filesToRewrite.add(existingFile);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        rewriteDatabase(filesToRewrite);
    }

    private void rewriteDatabase(List<CloudFile> files) {
        if (!hasStoragePermission()) return;
        try {
            File dbFile = getDatabaseFile();
            try (FileOutputStream fos = new FileOutputStream(dbFile, false)) {
                for (CloudFile file : files) {
                    String line = String.format("%s|%s|%d|%d|%s|%b|%s|%s\n",
                            file.getId(), file.getName(), file.getSize(), file.getDate(),
                            file.getPath(), file.isUploaded(), file.getFileId(), file.getMessageId());
                    fos.write(line.getBytes());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showSortDialog() {
        String[] options = {
                getString(R.string.sort_name_asc),
                getString(R.string.sort_name_desc),
                getString(R.string.sort_newest),
                getString(R.string.sort_oldest),
                getString(R.string.sort_largest),
                getString(R.string.sort_smallest)
        };
        new AlertDialog.Builder(this)
                .setTitle(R.string.sort_files)
                .setItems(options, (dialog, which) -> sortFiles(which))
                .show();
    }

    private void sortFiles(int sortType) {
        switch (sortType) {
            case 0: filteredFiles.sort((f1, f2) -> f1.getName().compareToIgnoreCase(f2.getName())); break;
            case 1: filteredFiles.sort((f1, f2) -> f2.getName().compareToIgnoreCase(f1.getName())); break;
            case 2: filteredFiles.sort((f1, f2) -> Long.compare(f2.getDate(), f1.getDate())); break;
            case 3: filteredFiles.sort((f1, f2) -> Long.compare(f1.getDate(), f2.getDate())); break;
            case 4: filteredFiles.sort((f1, f2) -> Long.compare(f2.getSize(), f1.getSize())); break;
            case 5: filteredFiles.sort((f1, f2) -> Long.compare(f1.getSize(), f2.getSize())); break;
        }
        fileAdapter.notifyDataSetChanged();
    }
}