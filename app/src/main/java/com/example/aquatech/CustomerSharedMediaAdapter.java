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

public class CustomerSharedMediaAdapter extends RecyclerView.Adapter<CustomerSharedMediaAdapter.ViewHolder> {

    private final List<ChatMessage> mediaList;
    private Context context;

    public CustomerSharedMediaAdapter(List<ChatMessage> mediaList) {
        this.mediaList = mediaList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        View view = LayoutInflater.from(context).inflate(R.layout.item_cust_shared_media, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (mediaList == null || position >= mediaList.size()) {
            return;
        }

        ChatMessage msg = mediaList.get(position);
        if (msg == null) {
            return;
        }

        String uriStr = msg.getFileUri();
        if (uriStr == null || uriStr.isEmpty()) {
            uriStr = msg.getFileUrl();
        }

        String name = msg.getFileName();
        boolean isDoc = name != null && name.toLowerCase().endsWith(".pdf");

        if (isDoc) {
            if (holder.ivThumb != null) {
                holder.ivThumb.setVisibility(View.GONE);
            }
            if (holder.docOverlay != null) {
                holder.docOverlay.setVisibility(View.VISIBLE);
            }
            if (holder.tvName != null) {
                holder.tvName.setText(name != null ? name : "Attachment.pdf");
            }
        } else {
            if (holder.docOverlay != null) {
                holder.docOverlay.setVisibility(View.GONE);
            }
            if (holder.ivThumb != null) {
                holder.ivThumb.setVisibility(View.VISIBLE);
                if (uriStr != null && !uriStr.isEmpty() && context != null) {
                    Glide.with(context)
                            .load(uriStr)
                            .centerCrop()
                            .placeholder(R.drawable.ic_gallery_image_vector)
                            .into(holder.ivThumb);
                }
            }
        }

        final String finalUriStr = uriStr;
        holder.itemView.setOnClickListener(v -> {
            if (finalUriStr != null && !finalUriStr.isEmpty()) {
                if (isDoc) {
                    try {
                        if (context != null) {
                            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(finalUriStr));
                            context.startActivity(intent);
                        }
                    } catch (Exception e) {
                        if (context != null) {
                            Toast.makeText(context, "Opening document...", Toast.LENGTH_SHORT).show();
                        }
                    }
                } else if (context != null) {
                    showFullScreenImageDialog(context, finalUriStr);
                }
            }
        });
    }

    private void showFullScreenImageDialog(Context ctx, String imageUrl) {
        if (ctx == null) {
            return;
        }

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
                .placeholder(R.drawable.ic_gallery_image_vector)
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
        if (mediaList != null) {
            return mediaList.size();
        }
        return 0;
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
