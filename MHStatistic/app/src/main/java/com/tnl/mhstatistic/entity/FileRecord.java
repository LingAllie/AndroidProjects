package com.tnl.mhstatistic.entity;

public class FileRecord {

    private String fileName;
    private String importDate;
    private String folderName;
    private String url;

    public FileRecord(String fileName, String importDate, String folderName, String url) {
        this.fileName = fileName;
        this.importDate = importDate;
        this.folderName = folderName;
        this.url = url;
    }

    public String getFileName() {
        return fileName;
    }

    public String getImportDate() {
        return importDate;
    }

    public String getFolderName() {
        return folderName; // Getter for folder name
    }

    public String getUrl() {
        return url;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public void setImportDate(String importDate) {
        this.importDate = importDate;
    }

    public void setFolderName(String folderName) {
        this.folderName = folderName;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
