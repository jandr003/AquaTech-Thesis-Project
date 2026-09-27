package com.example.aquatech;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class SharedMediaAdapter extends RecyclerView.Adapter<SharedMediaAdapter.ViewHolder> {

    private List<ChatMessage> mediaList;
    private Context context;

    public SharedMediaAdapter(List<ChatMessage> mediaList) {
        this.mediaList = mediaList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        View view = LayoutInflater.from(context).inflate(R.layout.item_shared_media, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ChatMessage msg = mediaList.get(position);
        String uriStr = msg.getFileUri();
        String name = msg.getFileName();

        boolean isDoc = name != null && name.toLowerCase().endsWith(".pdf");

        if (isDoc) {
            holder.ivThumb.setVisibility(View.GONE);
            holder.docOverlay.setVisibility(View.VISIBLE);
            holder.tvName.setText(name != null ? name : "Document.pdf");
        } else {
            holder.docOverlay.setVisibility(View.GONE);
            holder.ivThumb.setVisibility(View.VISIBLE);
            if (uriStr != null && !uriStr.isEmpty()) {
                Glide.with(context)
                        .load(uriStr)
                        .centerCrop()
                        .placeholder(R.drawable.maintenance_technician1)
                        .into(holder.ivThumb);
            }
        }

        holder.itemView.setOnClickListener(v -> {
            if (uriStr != null && !uriStr.isEmpty()) {
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uriStr));
                    context.startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(context, "Opening file...", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return mediaList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivThumb;
        LinearLayout docOverlay;
        TextView tvName;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivThumb = itemView.findViewById(R.id.ivSharedMediaThumb);
            docOverlay = itemView.findViewById(R.id.sharedMediaDocOverlay);
            tvName = itemView.findViewById(R.id.tvSharedMediaName);
        }
    }
}