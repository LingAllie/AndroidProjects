package com.tnl.mhstatistic.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.tnl.mhstatistic.R;
import com.tnl.mhstatistic.shared.CustomLongClickListener;

import java.util.List;

public class FolderAdapter extends RecyclerView.Adapter<FolderAdapter.ViewHolder> {

    private List<String> folderList;
    private OnFolderClickListener onFolderClickListener;
    private OnFolderLongClickListener onFolderLongClickListener;

    public FolderAdapter(List<String> folderList, OnFolderClickListener clickListener, OnFolderLongClickListener longClickListener) {
        this.folderList = folderList;
        this.onFolderClickListener = clickListener;
        this.onFolderLongClickListener = longClickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.custom_layout_folder, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String folderName = folderList.get(position);
        holder.folderNameTextView.setText(folderName);
        holder.itemView.setOnClickListener(v -> onFolderClickListener.onFolderClick(folderName));
        holder.itemView.setOnTouchListener(new CustomLongClickListener(v -> {
            onFolderLongClickListener.onFolderLongClick(folderName, position);
            return true;
        }));
    }

    @Override
    public int getItemCount() {
        return folderList.size();
    }

    public void updateFolders(List<String> newFolderList) {
        this.folderList = newFolderList;
        notifyDataSetChanged();
    }

    public interface OnFolderClickListener {
        void onFolderClick(String folderName);
    }

    public interface OnFolderLongClickListener {
        void onFolderLongClick(String folderName, int position);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView folderNameTextView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            folderNameTextView = itemView.findViewById(R.id.folderName);
        }
    }
}
