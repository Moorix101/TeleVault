package com.moorixlabs.televault;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;

import java.util.Locale;

public class FileUtils {

    /**
     * Get file name from URI
     */
    public static String getFileName(Context context, Uri uri) {
        String fileName = "unknown";

        if (uri.getScheme() != null && uri.getScheme().equals("content")) {
            try (Cursor cursor = context.getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (nameIndex != -1) {
                        fileName = cursor.getString(nameIndex);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (fileName.equals("unknown")) {
            String path = uri.getPath();
            if (path != null) {
                int lastSlash = path.lastIndexOf('/');
                if (lastSlash != -1) {
                    fileName = path.substring(lastSlash + 1);
                }
            }
        }

        return fileName;
    }

    /**
     * Get file size from URI
     */
    public static long getFileSize(Context context, Uri uri) {
        long fileSize = 0;

        if (uri.getScheme() != null && uri.getScheme().equals("content")) {
            try (Cursor cursor = context.getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
                    if (sizeIndex != -1) {
                        fileSize = cursor.getLong(sizeIndex);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return fileSize;
    }

    /**
     * Format file size to human readable format
     */
    public static String formatFileSize(long size) {
        if (size <= 0) return "0 B";

        final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));

        return String.format(Locale.getDefault(), "%.1f %s",
                size / Math.pow(1024, digitGroups),
                units[digitGroups]);
    }

    /**
     * Get MIME type from file extension
     */
    public static String getMimeType(String fileName) {
        String extension = "";
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot > 0) {
            extension = fileName.substring(lastDot + 1).toLowerCase();
        }

        switch (extension) {
            case "pdf":
                return "application/pdf";
            case "doc":
            case "docx":
                return "application/msword";
            case "xls":
            case "xlsx":
                return "application/vnd.ms-excel";
            case "ppt":
            case "pptx":
                return "application/vnd.ms-powerpoint";
            case "jpg":
            case "jpeg":
                return "image/jpeg";
            case "png":
                return "image/png";
            case "gif":
                return "image/gif";
            case "mp4":
                return "video/mp4";
            case "mp3":
                return "audio/mpeg";
            case "zip":
                return "application/zip";
            case "txt":
                return "text/plain";
            default:
                return "application/octet-stream";
        }
    }

    /**
     * Check if file is an image
     */
    public static boolean isImage(String fileName) {
        String ext = getFileExtension(fileName);
        return ext.equals("jpg") || ext.equals("jpeg") || ext.equals("png") ||
                ext.equals("gif") || ext.equals("bmp") || ext.equals("webp");
    }

    /**
     * Check if file is a video
     */
    public static boolean isVideo(String fileName) {
        String ext = getFileExtension(fileName);
        return ext.equals("mp4") || ext.equals("avi") || ext.equals("mkv") ||
                ext.equals("mov") || ext.equals("wmv") || ext.equals("flv");
    }

    /**
     * Check if file is audio
     */
    public static boolean isAudio(String fileName) {
        String ext = getFileExtension(fileName);
        return ext.equals("mp3") || ext.equals("wav") || ext.equals("flac") ||
                ext.equals("aac") || ext.equals("ogg") || ext.equals("m4a");
    }

    /**
     * Get file extension
     */
    public static String getFileExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot > 0 && lastDot < fileName.length() - 1) {
            return fileName.substring(lastDot + 1).toLowerCase();
        }
        return "";
    }
}