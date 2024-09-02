package com.tnl.entity;

public class FileRecord {

    private final String fileName;
    private final String importDate;

    public FileRecord(String fileName, String importDate) {
        this.fileName = fileName;
        this.importDate = importDate;
    }

    public String getFileName() {
        return fileName;
    }

    public String getImportDate() {
        return importDate;
    }
}
