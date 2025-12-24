package com.example.flowalbum

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

/**
 * 文件夹列表适配器
 * 用于在RecyclerView中显示图片文件夹列表
 */
class FolderAdapter(
    private val folders: List<PhotoFolder>,
    private val onFolderClick: (PhotoFolder) -> Unit
) : RecyclerView.Adapter<FolderAdapter.FolderViewHolder>() {

    inner class FolderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageFolderCover: ImageView = itemView.findViewById(R.id.imageFolderCover)
        val textFolderName: TextView = itemView.findViewById(R.id.textFolderName)
        val textPhotoCount: TextView = itemView.findViewById(R.id.textPhotoCount)

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onFolderClick(folders[position])
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FolderViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_folder_grid, parent, false)
        
        // 设置item宽度，考虑padding和滚动条空间
        // 减去一些空间避免被滚动条遮挡
        val layoutParams = view.layoutParams as ViewGroup.MarginLayoutParams
        val availableWidth = parent.width - parent.paddingStart - parent.paddingEnd - 80 // 减去80dp给滚动条和边距
        layoutParams.width = availableWidth / 3
        view.layoutParams = layoutParams
        
        return FolderViewHolder(view)
    }

    override fun onBindViewHolder(holder: FolderViewHolder, position: Int) {
        val folder = folders[position]
        
        // 设置文件夹名称
        holder.textFolderName.text = folder.getDisplayName()
        
        // 设置图片数量
        holder.textPhotoCount.text = folder.getFormattedCount()
        
        // 加载封面图片
        if (folder.coverPhoto != null) {
            Glide.with(holder.itemView.context)
                .load(folder.coverPhoto.uri)
                .centerCrop()
                .placeholder(R.color.control_background)
                .error(R.color.control_background)
                .into(holder.imageFolderCover)
        } else {
            // 如果没有封面图片，显示默认背景
            holder.imageFolderCover.setImageDrawable(null)
            holder.imageFolderCover.setBackgroundResource(R.color.control_background)
        }
    }

    override fun getItemCount(): Int = folders.size
}