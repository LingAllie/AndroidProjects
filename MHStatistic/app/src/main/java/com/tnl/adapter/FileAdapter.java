package com.tnl.adapter;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.tnl.entity.FileRecord;
import com.tnl.mhstatistic.R;
import com.tnl.shared.CustomLongClickListener;

import java.util.ArrayList;
import java.util.List;

public class FileAdapter extends RecyclerView.Adapter<FileAdapter.FileViewHolder> {

    private List<FileRecord> fileRecords;
    private OnFileLongClickListener onFileLongClickListener;

    public FileAdapter(OnFileLongClickListener onFileLongClickListener) {
        this.fileRecords = new ArrayList<>();
        this.onFileLongClickListener = onFileLongClickListener;
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

        holder.itemView.setOnTouchListener(new CustomLongClickListener(v -> {
            onFileLongClickListener.onFileLongClick(fileRecord);
            return true;
        }));
    }

    @Override
    public int getItemCount() {
        return fileRecords.size();
    }

    @SuppressLint("NotifyDataSetChanged")
    public void updateFileList(List<FileRecord> newFileList) {
        if (fileRecords == null) {
            fileRecords = new ArrayList<>();
        } else {
            fileRecords.clear();
        }

        if (newFileList != null) {
            fileRecords.addAll(newFileList);
        }

        notifyDataSetChanged();
    }

    @SuppressLint("NotifyDataSetChanged")
    public void addFileRecord(FileRecord fileRecord) {
        // Prevent duplicate addition
        boolean exists = fileRecords.stream()
                .anyMatch(existingFile -> existingFile.getFileName().equals(fileRecord.getFileName()));

        if (!exists) {
            fileRecords.add(fileRecord);
            notifyDataSetChanged();
        }
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

    public interface OnFileLongClickListener {
        void onFileLongClick(FileRecord fileRecord);
    }

}
