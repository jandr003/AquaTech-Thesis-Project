package com.example.aquatech;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class AvatarAdapter extends RecyclerView.Adapter<AvatarAdapter.AvatarViewHolder> {

    private final List<AvatarModel> avatarList;
    private final OnAvatarClickListener listener;

    public interface OnAvatarClickListener {
        void onAvatarClick(AvatarModel avatar);
    }

    public AvatarAdapter(List<AvatarModel> avatarList, OnAvatarClickListener listener) {
        this.avatarList = avatarList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public AvatarViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_avatar, parent, false);
        return new AvatarViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AvatarViewHolder holder, int position) {
        AvatarModel avatar = avatarList.get(position);
        holder.ivAvatar.setImageResource(avatar.getImageResId());
        holder.itemView.setOnClickListener(v -> listener.onAvatarClick(avatar));
    }

    @Override
    public int getItemCount() {
        return avatarList.size();
    }

    static class AvatarViewHolder extends RecyclerView.ViewHolder {
        ImageView ivAvatar;

        public AvatarViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.ivAvatarItem);
        }
    }
}
