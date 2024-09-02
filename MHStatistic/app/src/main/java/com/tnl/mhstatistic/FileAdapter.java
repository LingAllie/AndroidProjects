package com.tnl.mhstatistic;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class FileAdapter extends RecyclerView.Adapter<FileAdapter.FileViewHolder> {

    private final List<FileRecord> fileRecords;

    public FileAdapter() {
        this.fileRecords = new ArrayList<>();
    }

    @NonNull
    @Override
    public FileViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.custom_layout_file, parent, false);
        return new FileViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FileViewHolder holder, int position) {
        FileRecord fileRecord = fileRecords.get(position);
        holder.textViewFileName.setText(fileRecord.getFileName());
        holder.textViewImportDate.setText(fileRecord.getImportDate());
    }

    @Override
    public int getItemCount() {
        return fileRecords.size();
    }

    public void addFileRecord(String fileName, String importDate) {
        fileRecords.add(new FileRecord(fileName, importDate));
        notifyDataSetChanged();
    }

    static class FileViewHolder extends RecyclerView.ViewHolder {
        TextView textViewFileName;
        TextView textViewImportDate;

        FileViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewFileName = itemView.findViewById(R.id.textViewFileName);
            textViewImportDate = itemView.findViewById(R.id.textViewDate);
        }
    }

    static class FileRecord {
        private final String fileName;
        private final String importDate;

        FileRecord(String fileName, String importDate) {
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
}
