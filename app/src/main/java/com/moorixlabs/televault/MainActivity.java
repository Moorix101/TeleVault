package com.moorixlabs.televault;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.util.Map; // --- NEW IMPORT ---
import java.util.HashMap; // --- NEW IMPORT ---

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private static final String ZARCHIVER_PACKAGE_NAME = "ru.zdevs.zarchiver";

    private String botToken;
    private String chatId;

    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final String FILES_DATABASE = "televault_files.txt";
    private static final String APP_FOLDER = "TeleVault";
    private static final int RECENT_FILES_LIMIT = 10;

    private RecyclerView recyclerViewFiles;
    private LinearLayout emptyStateLayout;
    private TextView tvStorageInfo;
    private FloatingActionButton fabAddFile;
    private ImageButton btnSort, btnSettings;

    private TextView tvTotalImages, tvTotalVideos, tvTotalPDFs, tvTotalOther;
    private CardView cardImages, cardVideos, cardPDFs, cardOther;

    private FileAdapter fileAdapter;
    private List<CloudFile> recentFiles;
    private List<CloudFile> allCloudFiles;

    private ActivityResultLauncher<Intent> filePickerLauncher;
    private ActivityResultLauncher<Intent> allFilesPermissionLauncher;
    private ActivityResultLauncher<Intent> settingsLauncher;

    private boolean isCheckingConfiguration = false;
    private boolean hasShownConfigAlert = false;
    private Map<String, TelegramUploader> activeUploads = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeViews();
        setupActivityLaunchers();
        setupRecyclerView();
        setupClickListeners();

        checkConfigurationAndProceed();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!isCheckingConfiguration) {
            checkConfigurationAndProceed();
        }
    }

    private void initializeViews() {
        recyclerViewFiles = findViewById(R.id.recyclerViewFiles);
        emptyStateLayout = findViewById(R.id.emptyStateLayout);
        tvStorageInfo = findViewById(R.id.tvStorageInfo);
        fabAddFile = findViewById(R.id.fabAddFile);
        btnSort = findViewById(R.id.btnSort);
        btnSettings = findViewById(R.id.btnSettings);

        tvTotalImages = findViewById(R.id.tvTotalImages);
        tvTotalVideos = findViewById(R.id.tvTotalVideos);
        tvTotalPDFs = findViewById(R.id.tvTotalPDFs);
        tvTotalOther = findViewById(R.id.tvTotalOther);

        cardImages = findViewById(R.id.cardImages);
        cardVideos = findViewById(R.id.cardVideos);
        cardPDFs = findViewById(R.id.cardPDFs);
        cardOther = findViewById(R.id.cardOther);
    }

    private void checkConfigurationAndProceed() {
        isCheckingConfiguration = true;

        botToken = SettingsUtils.getBotToken(this);
        chatId = SettingsUtils.getChatId(this);

        if (!SettingsUtils.isConfigured(this)) {
            if (!hasShownConfigAlert) {
                hasShownConfigAlert = true;
                showConfigurationAlert();
            }
            fabAddFile.setEnabled(false);
            btnSort.setEnabled(false);
            if (recentFiles != null) {
                recentFiles.clear();
                fileAdapter.notifyDataSetChanged();
            }
            updateUI();
            isCheckingConfiguration = false;
        } else {
            hasShownConfigAlert = false;
            fabAddFile.setEnabled(true);
            btnSort.setEnabled(true);
            checkAndRequestPermissions();
            isCheckingConfiguration = false;
        }
    }

    private void showConfigurationAlert() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.settings_required_title)
                .setMessage(R.string.settings_required_message)
                .setPositiveButton(R.string.go_to_settings, (dialog, which) -> {
                    Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                    settingsLauncher.launch(intent);
                })
                .setCancelable(false)
                .show();
    }

    private void setupRecyclerView() {
        allCloudFiles = new ArrayList<>();
        recentFiles = new ArrayList<>();
        fileAdapter = new FileAdapter(this, recentFiles, new FileAdapter.FileActionListener() {
            @Override
            public void onDownloadFile(CloudFile file) {
                downloadFromTelegram(file);
            }

            @Override
            public void onShareFileAction(CloudFile file) {
                shareDownloadedFile(file);
            }

            @Override
            public void onDeleteFile(CloudFile file, int position) {
                deleteFileFromVault(file, position);
            }
            @Override
            public void onCancelUpload(CloudFile file, int position) {
                android.util.Log.d("MainActivity", "Cancel requested for: " + file.getName());
                TelegramUploader uploader = activeUploads.get(file.getId());
                if (uploader != null) {
                    uploader.cancel();
                    // The onUploadCancelled callback will handle UI removal
                } else {
                    // Uploader not found, maybe already finished or failed? Just remove it.
                    android.util.Log.w("MainActivity", "Uploader not found for cancellation. Removing manually.");
                    handleUploadFailure(file, "Upload cancelled.");
                }
            }
        });

        recyclerViewFiles.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewFiles.setAdapter(fileAdapter);
    }

    private void setupActivityLaunchers() {
        filePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri fileUri = result.getData().getData();
                        if (fileUri != null) {
                            handleSelectedFile(fileUri);
                        }
                    }
                }
        );

        allFilesPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        if (Environment.isExternalStorageManager()) {
                            Toast.makeText(this, R.string.permission_granted_toast, Toast.LENGTH_SHORT).show();
                            initializeAppFolder();
                            loadFilesFromDatabase();
                            updateUI();
                        } else {
                            showPermissionDeniedDialog();
                        }
                    }
                }
        );

        settingsLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    // onResume handles the check
                }
        );
    }

    private void setupClickListeners() {
        fabAddFile.setOnClickListener(v -> {
            if (!SettingsUtils.isConfigured(this)) {
                showConfigurationAlert();
            } else if (!hasStoragePermission()) {
                checkAndRequestPermissions();
            } else {
                openFilePicker();
            }
        });

        btnSort.setOnClickListener(v -> showSortDialog());

        btnSettings.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            settingsLauncher.launch(intent);
        });

        cardImages.setOnClickListener(v -> openFilteredActivity(FilteredFilesActivity.TYPE_IMAGE));
        cardVideos.setOnClickListener(v -> openFilteredActivity(FilteredFilesActivity.TYPE_VIDEO));
        cardPDFs.setOnClickListener(v -> openFilteredActivity(FilteredFilesActivity.TYPE_PDF));
        cardOther.setOnClickListener(v -> openFilteredActivity(FilteredFilesActivity.TYPE_OTHER));
    }

    private void openFilteredActivity(String fileType) {
        if (!SettingsUtils.isConfigured(this)) {
            showConfigurationAlert();
            return;
        }
        if (!hasStoragePermission()) {
            Toast.makeText(this, R.string.storage_permission_required_toast, Toast.LENGTH_SHORT).show();
            checkAndRequestPermissions();
            return;
        }

        Intent intent = new Intent(MainActivity.this, FilteredFilesActivity.class);
        intent.putExtra(FilteredFilesActivity.EXTRA_FILE_TYPE, fileType);
        startActivity(intent);
    }

    // ==================== STORAGE LOCATION ====================

    private File getAppFolder() {
        File documentsDir = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO) {
            documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
        }
        return new File(documentsDir, APP_FOLDER);
    }

    private File getDatabaseFile() {
        return new File(getAppFolder(), FILES_DATABASE);
    }

    private void initializeAppFolder() {
        try {
            File appFolder = getAppFolder();
            if (!appFolder.exists()) {
                boolean created = appFolder.mkdirs();
                if (created) {
                    Toast.makeText(this,
                            getString(R.string.database_location_toast, appFolder.getAbsolutePath()),
                            Toast.LENGTH_LONG).show();
                }
            }

            File downloadFolder = new File(appFolder, "Download");
            if (!downloadFolder.exists()) {
                downloadFolder.mkdirs();
            }
        } catch (Exception e) {
            Toast.makeText(this,
                    getString(R.string.error_creating_folder, e.getMessage()),
                    Toast.LENGTH_SHORT).show();
        }
    }

    // ==================== PERMISSION HANDLING ====================

    private void checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                showAllFilesAccessDialog();
            } else {
                initializeAppFolder();
                loadFilesFromDatabase();
                updateUI();
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!hasStoragePermission()) {
                requestStoragePermissions();
            } else {
                initializeAppFolder();
                loadFilesFromDatabase();
                updateUI();
            }
        } else {
            initializeAppFolder();
            loadFilesFromDatabase();
            updateUI();
        }
    }

    private boolean hasStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager();
        } else {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                            == PackageManager.PERMISSION_GRANTED;
        }
    }

    private void showAllFilesAccessDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.storage_permission_required)
                .setMessage(R.string.permission_rationale)
                .setPositiveButton(R.string.grant_permission, (dialog, which) -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                        intent.setData(Uri.parse("package:" + getPackageName()));
                        allFilesPermissionLauncher.launch(intent);
                    }
                })
                .setNegativeButton(R.string.cancel, (dialog, which) ->
                        Toast.makeText(this, R.string.permission_denied_toast, Toast.LENGTH_SHORT).show())
                .setCancelable(false)
                .show();
    }

    private void requestStoragePermissions() {
        ActivityCompat.requestPermissions(this,
                new String[]{
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                }, PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, R.string.permission_granted_toast, Toast.LENGTH_SHORT).show();
                initializeAppFolder();
                loadFilesFromDatabase();
                updateUI();
            } else {
                showPermissionDeniedDialog();
            }
        }
    }

    private void showPermissionDeniedDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.permission_denied_title)
                .setMessage(R.string.permission_denied_message)
                .setPositiveButton(R.string.open_settings, (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    intent.setData(Uri.parse("package:" + getPackageName()));
                    startActivity(intent);
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    // ==================== FILE HANDLING ====================

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);

        try {
            filePickerLauncher.launch(intent);
        } catch (Exception e) {
            Toast.makeText(this, R.string.error_file_picker, Toast.LENGTH_SHORT).show();
        }
    }
    private void handleSelectedFile(Uri fileUri) {
        if (!SettingsUtils.isConfigured(this)) {
            showConfigurationAlert();
            return;
        }

        try {
            // Take persistable permission
            try {
                getContentResolver().takePersistableUriPermission(
                        fileUri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                );
                android.util.Log.d("MainActivity", "✓ Persistable permission granted for: " + fileUri);
            } catch (SecurityException e) {
                android.util.Log.w("MainActivity", "Could not take persistable permission: " + e.getMessage());
            }

            String fileName = FileUtils.getFileName(this, fileUri);
            long fileSize = FileUtils.getFileSize(this, fileUri);

            if (fileSize == 0) {
                Toast.makeText(this, R.string.error_empty_file, Toast.LENGTH_SHORT).show();
                return;
            }

            if (fileSize > 50 * 1024 * 1024) {
                Toast.makeText(this, R.string.error_file_too_large, Toast.LENGTH_LONG).show();
                return;
            }

            CloudFile cloudFile = new CloudFile(
                    UUID.randomUUID().toString(),
                    fileName,
                    fileSize,
                    System.currentTimeMillis(),
                    fileUri.toString(),
                    false
            );

            allCloudFiles.add(0, cloudFile);
            recentFiles.add(0, cloudFile);

            if (recentFiles.size() > RECENT_FILES_LIMIT) {
                recentFiles.remove(RECENT_FILES_LIMIT);
            }

            fileAdapter.notifyItemInserted(0);
            recyclerViewFiles.smoothScrollToPosition(0);
            updateUI();
            saveFileToDatabase(cloudFile);

            Snackbar.make(findViewById(android.R.id.content),
                    getString(R.string.uploading, fileName),
                    Snackbar.LENGTH_SHORT).show();

            uploadToTelegram(cloudFile, fileUri);

        } catch (Exception e) {
            Toast.makeText(this, getString(R.string.error_add_file, e.getMessage()), Toast.LENGTH_SHORT).show();
        }
    }

    private void uploadToTelegram(CloudFile cloudFile, Uri fileUri) {
        new Thread(() -> {
            TelegramUploader uploader = new TelegramUploader(
                    getApplicationContext(), botToken, chatId, fileUri, cloudFile.getName(),
                    new TelegramUploader.UploadCallback() {
                        // ... (onUploadProgress is unchanged)

                        @Override
                        public void onUploadProgress(int progress) {
                            runOnUiThread(() -> {
                                android.util.Log.d("MainActivity", "📤 Upload progress: " + progress + "% for " + cloudFile.getName());
                                cloudFile.setUploadProgress(progress);
                            });
                        }

                        @Override
                        public void onUploadSuccess(String fileId, String messageId) {
                            runOnUiThread(() -> {
                                activeUploads.remove(cloudFile.getId()); // --- CLEANUP MAP ---

                                android.util.Log.d("MainActivity", "✅ Upload complete: " + cloudFile.getName());

                                // ... (rest of the method is unchanged)
                                cloudFile.setUploaded(true);
                                cloudFile.setUploadProgress(100);
                                cloudFile.setFileId(fileId);
                                cloudFile.setMessageId(messageId);
                                updateFileInDatabase(cloudFile);
                                loadFilesFromDatabase();
                                updateUI();
                                Snackbar.make(findViewById(android.R.id.content),
                                        getString(R.string.upload_success_snackbar, cloudFile.getName()),
                                        Snackbar.LENGTH_LONG).show();
                            });
                        }

                        @Override
                        public void onUploadFailed(String error) {
                            runOnUiThread(() -> {
                                activeUploads.remove(cloudFile.getId()); // --- CLEANUP MAP ---

                                new AlertDialog.Builder(MainActivity.this)
                                        .setTitle(R.string.upload_failed_dialog_title)
                                        .setMessage(getString(R.string.upload_failed_dialog_message, cloudFile.getName(), error))
                                        .setPositiveButton(R.string.ok, (dialog, which) -> {
                                            handleUploadFailure(cloudFile, null); // Call helper
                                        })
                                        .show();
                            });
                        }

                        // --- NEW CALLBACK IMPLEMENTATION ---
                        @Override
                        public void onUploadCancelled() {
                            runOnUiThread(() -> {
                                android.util.Log.d("MainActivity", "Upload cancelled for " + cloudFile.getName());
                                activeUploads.remove(cloudFile.getId()); // Clean up map
                                handleUploadFailure(cloudFile, "Upload cancelled for " + cloudFile.getName());
                            });
                        }
                        // --- END NEW CALLBACK IMPLEMENTATION ---
                    }
            );

            // --- STORE THE UPLOADER ---
            activeUploads.put(cloudFile.getId(), uploader);
            // --- START THE UPLOAD ---
            uploader.upload();

        }).start();
    }
    private void handleUploadFailure(CloudFile cloudFile, String snackbarMessage) {
        // 1. Remove the file from the database FIRST
        deleteFileFromDatabase(cloudFile);

        // 2. Force the app to re-read the (now updated) database file
        //    This removes the cancelled/failed item from allCloudFiles and recentFiles lists
        loadFilesFromDatabase();

        // 3. Update the UI based on the freshly loaded data
        updateUI(); // This already calls notifyDataSetChanged()

        // 4. Show a message if provided
        if (snackbarMessage != null) {
            Snackbar.make(findViewById(android.R.id.content), snackbarMessage, Snackbar.LENGTH_SHORT).show();
        }
    }
    private void downloadFromTelegram(CloudFile file) {
        if (!file.canDownload()) {
            Toast.makeText(this, R.string.file_not_available, Toast.LENGTH_SHORT).show();
            return;
        }

        if (!hasStoragePermission()) {
            Toast.makeText(this, R.string.storage_permission_required_toast, Toast.LENGTH_SHORT).show();
            checkAndRequestPermissions();
            return;
        }

        if (!SettingsUtils.isConfigured(this)) {
            showConfigurationAlert();
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
                                new AlertDialog.Builder(MainActivity.this)
                                        .setTitle(R.string.download_complete_title)
                                        .setMessage(getString(R.string.download_complete_message, file.getName()))
                                        .setPositiveButton(R.string.open_file, (dialog, which) -> openDownloadedFile(fileUri, file.getName()))
                                        .setNegativeButton(R.string.open_folder, (dialog, which) -> openDownloadFolder())
                                        .setNeutralButton(R.string.cancel, null)
                                        .show();
                            });
                        }

                        @SuppressLint("StringFormatInvalid")
                        @Override
                        public void onDownloadFailed(String error) {
                            runOnUiThread(() -> {
                                progressDialog.dismiss();
                                new AlertDialog.Builder(MainActivity.this)
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

    // ==================== DATABASE OPERATIONS (UNMODIFIED) ====================

    private void saveFileToDatabase(CloudFile file) {
        if (!hasStoragePermission()) return;
        try {
            initializeAppFolder();
            File dbFile = getDatabaseFile();
            try (FileOutputStream fos = new FileOutputStream(dbFile, true)) {
                String line = String.format("%s|%s|%d|%d|%s|%b|%s|%s\n",
                        file.getId(), file.getName(), file.getSize(), file.getDate(),
                        file.getPath(), file.isUploaded(), file.getFileId(), file.getMessageId());
                fos.write(line.getBytes());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateFileInDatabase(CloudFile file) {
        if (!hasStoragePermission()) return;
        List<CloudFile> allFiles = new ArrayList<>();
        File dbFile = getDatabaseFile();
        if (dbFile.exists()) {
            try (FileInputStream fis = new FileInputStream(dbFile);
                 BufferedReader reader = new BufferedReader(new InputStreamReader(fis))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split("\\|");
                    if (parts.length >= 6) {
                        if (parts[0].equals(file.getId())) {
                            allFiles.add(file);
                        } else {
                            CloudFile existingFile = parseFileLine(parts);
                            if (existingFile != null) allFiles.add(existingFile);
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        rewriteDatabase(allFiles);
    }

    private void deleteFileFromDatabase(CloudFile file) {
        if (!hasStoragePermission()) return;
        List<CloudFile> allFiles = new ArrayList<>();
        File dbFile = getDatabaseFile();
        if (dbFile.exists()) {
            try (FileInputStream fis = new FileInputStream(dbFile);
                 BufferedReader reader = new BufferedReader(new InputStreamReader(fis))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split("\\|");
                    if (parts.length >= 6 && !parts[0].equals(file.getId())) {
                        CloudFile existingFile = parseFileLine(parts);
                        if (existingFile != null) allFiles.add(existingFile);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        rewriteDatabase(allFiles);
    }

    private void rewriteDatabase(List<CloudFile> files) {
        if (!hasStoragePermission()) return;
        try {
            initializeAppFolder();
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

    private void loadFilesFromDatabase() {
        if (!hasStoragePermission()) return;
        allCloudFiles.clear();
        recentFiles.clear();

        File dbFile = getDatabaseFile();
        if (!dbFile.exists()) return;

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

        allCloudFiles.sort((f1, f2) -> Long.compare(f2.getDate(), f1.getDate()));

        int limit = Math.min(RECENT_FILES_LIMIT, allCloudFiles.size());
        for (int i = 0; i < limit; i++) {
            recentFiles.add(allCloudFiles.get(i));
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
            e.printStackTrace();
        }
        return null;
    }

    // ==================== UI & SORTING ====================

    public void updateUI() {
        int imageCount = 0;
        int videoCount = 0;
        int pdfCount = 0;
        int otherCount = 0;
        long totalSize = 0;

        for (CloudFile file : allCloudFiles) {
            totalSize += file.getSize();
            String extension = file.getFileExtension();
            if (FileUtils.isImage(file.getName())) {
                imageCount++;
            } else if (FileUtils.isVideo(file.getName())) {
                videoCount++;
            } else if ("pdf".equals(extension)) {
                pdfCount++;
            } else {
                otherCount++;
            }
        }

        tvTotalImages.setText(String.valueOf(imageCount));
        tvTotalVideos.setText(String.valueOf(videoCount));
        tvTotalPDFs.setText(String.valueOf(pdfCount));
        tvTotalOther.setText(String.valueOf(otherCount));

        if (allCloudFiles.isEmpty()) {
            emptyStateLayout.setVisibility(View.VISIBLE);
            recyclerViewFiles.setVisibility(View.GONE);
            tvStorageInfo.setText(getString(R.string.storage_info_format, 0, "0 B"));
        } else {
            emptyStateLayout.setVisibility(View.GONE);
            recyclerViewFiles.setVisibility(View.VISIBLE);
            String sizeStr = FileUtils.formatFileSize(totalSize);
            tvStorageInfo.setText(getString(R.string.storage_info_format, allCloudFiles.size(), sizeStr));
        }
        fileAdapter.notifyDataSetChanged();
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
            case 0: recentFiles.sort((f1, f2) -> f1.getName().compareToIgnoreCase(f2.getName())); break;
            case 1: recentFiles.sort((f1, f2) -> f2.getName().compareToIgnoreCase(f1.getName())); break;
            case 2: recentFiles.sort((f1, f2) -> Long.compare(f2.getDate(), f1.getDate())); break;
            case 3: recentFiles.sort((f1, f2) -> Long.compare(f1.getDate(), f2.getDate())); break;
            case 4: recentFiles.sort((f1, f2) -> Long.compare(f2.getSize(), f1.getSize())); break;
            case 5: recentFiles.sort((f1, f2) -> Long.compare(f1.getSize(), f2.getSize())); break;
        }
        fileAdapter.notifyDataSetChanged();
    }

    private void shareDownloadedFile(CloudFile file) {
        if (!file.canDownload()) {
            Toast.makeText(this, R.string.file_not_available, Toast.LENGTH_SHORT).show();
            return;
        }
        if (!hasStoragePermission()) {
            checkAndRequestPermissions();
            return;
        }
        if (!SettingsUtils.isConfigured(this)) {
            showConfigurationAlert();
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
                                    Uri contentUri = FileProvider.getUriForFile(MainActivity.this, getPackageName() + ".fileprovider", fileToShare);
                                    Intent shareIntent = new Intent(Intent.ACTION_SEND);
                                    shareIntent.setType(FileUtils.getMimeType(file.getName()));
                                    shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
                                    shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                    startActivity(Intent.createChooser(shareIntent, getString(R.string.menu_share_file)));
                                    scheduleTempFileDeletion(fileToShare);
                                } catch (Exception e) {
                                    Toast.makeText(MainActivity.this, getString(R.string.error_sharing_file, e.getMessage()), Toast.LENGTH_LONG).show();
                                }
                            });
                        }

                        @Override
                        public void onDownloadFailed(String error) {
                            runOnUiThread(() -> {
                                progressDialog.dismiss();
                                new AlertDialog.Builder(MainActivity.this)
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

    private void deleteFileFromVault(CloudFile file, int position) {
        if (!file.isUploaded() || file.getMessageId() == null || file.getMessageId().isEmpty()) {

            // --- MODIFIED TO USE HELPER ---
            handleUploadFailure(file, getString(R.string.removed_local_snackbar, file.getName()));
            // --- END MODIFICATION ---

            return;
        }

        if (!SettingsUtils.isConfigured(this)) {
            showConfigurationAlert();
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
                                recentFiles.remove(position);
                                fileAdapter.notifyItemRemoved(position);
                                allCloudFiles.remove(file);
                                deleteFileFromDatabase(file);
                                updateUI();
                                Snackbar.make(findViewById(android.R.id.content),
                                        getString(R.string.delete_success_snackbar, file.getName()),
                                        Snackbar.LENGTH_LONG).show();
                            });
                        }

                        @Override
                        public void onDeleteFailed(String error) {
                            runOnUiThread(() -> {
                                progressDialog.dismiss();
                                new AlertDialog.Builder(MainActivity.this)
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
}