package com.example.aquatech;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class SharedMediaAdapter extends RecyclerView.Adapter<SharedMediaAdapter.ViewHolder> {

    private final List<ChatMessage> mediaList;
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
                if (isDoc) {
                    try {
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uriStr));
                        context.startActivity(intent);
                    } catch (Exception e) {
                        Toast.makeText(context, "Opening document...", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    showFullScreenImageDialog(context, uriStr);
                }
            }
        });
    }

    private void showFullScreenImageDialog(Context ctx, String imageUrl) {
        Dialog dialog = new Dialog(ctx, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        FrameLayout layout = new FrameLayout(ctx);
        layout.setBackgroundColor(Color.BLACK);
        layout.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        ImageView imageView = new ImageView(ctx);
        imageView.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);

        Glide.with(ctx)
                .load(imageUrl)
                .placeholder(R.drawable.maintenance_technician1)
                .into(imageView);

        ImageView btnClose = new ImageView(ctx);
        int size = (int) (40 * ctx.getResources().getDisplayMetrics().density);
        int margin = (int) (16 * ctx.getResources().getDisplayMetrics().density);
        FrameLayout.LayoutParams closeParams = new FrameLayout.LayoutParams(size, size);
        closeParams.gravity = Gravity.TOP | Gravity.END;
        closeParams.setMargins(margin, margin + 24, margin, margin);
        btnClose.setLayoutParams(closeParams);
        btnClose.setImageResource(R.drawable.exit);
        btnClose.setColorFilter(Color.WHITE);
        btnClose.setPadding(8, 8, 8, 8);
        btnClose.setOnClickListener(v -> dialog.dismiss());

        layout.addView(imageView);
        layout.addView(btnClose);

        imageView.setOnClickListener(v -> dialog.dismiss());

        dialog.setContentView(layout);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.BLACK));
        }
        dialog.show();
    }

    @Override
    public int getItemCount() {
        return mediaList != null ? mediaList.size() : 0;
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
