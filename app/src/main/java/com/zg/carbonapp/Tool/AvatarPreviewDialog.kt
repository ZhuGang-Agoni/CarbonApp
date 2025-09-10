package com.zg.carbonapp.Tool

import android.app.Dialog
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.fragment.app.DialogFragment
import com.bumptech.glide.Glide
import com.zg.carbonapp.Dao.ProductType
import com.zg.carbonapp.Dao.User
import com.zg.carbonapp.Dao.VirtualProduct
import com.zg.carbonapp.R
import com.zg.carbonapp.databinding.DialogAvatarPreviewBinding

class AvatarPreviewDialog(
    private val user: User,
    private val product: VirtualProduct
) : DialogFragment() {

    private lateinit var binding: DialogAvatarPreviewBinding

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        return dialog
    }

    override fun onStart() {
        super.onStart()
        // 确保对话框尺寸固定
        val size = (300 * resources.displayMetrics.density).toInt()
        dialog?.window?.setLayout(size, size)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DialogAvatarPreviewBinding.inflate(inflater, container, false)

        Log.d("AvatarPreview", "Dialog created")
        Log.d("AvatarPreview", "Product type: ${product.type}")

        loadUserAvatar()
        showDecorations()
        return binding.root
    }

    private fun loadUserAvatar() {
        try {
            if (user.userAvatar.isNotEmpty()) {
                Glide.with(this)
                    .load(Uri.parse(user.userAvatar))
                    .error(R.drawable.default_avatar)
                    .into(binding.userAvatar)
            } else {
                binding.userAvatar.setImageResource(R.drawable.default_avatar)
            }
        } catch (e: Exception) {
            Log.e("AvatarPreview", "Error loading avatar: ${e.message}")
            binding.userAvatar.setImageResource(R.drawable.default_avatar)
        }
    }

    private fun showDecorations() {
        // 确保头像始终可见
        binding.userAvatar.visibility = View.VISIBLE

        // 隐藏所有装饰
        binding.avatarFrame.visibility = View.GONE
        binding.avatarAccessory.visibility = View.GONE

        Log.d("AvatarPreview", "Showing decorations for type: ${product.type}")

        when (product.type) {
            ProductType.AVATAR_FRAME -> {
                binding.avatarFrame.visibility = View.VISIBLE
                binding.avatarFrame.setImageResource(product.unlockRes)
                binding.avatarFrame.bringToFront()
                Log.d("AvatarPreview", "Frame resource: ${product.unlockRes}")
            }
            ProductType.AVATAR_ITEM -> {
                binding.avatarAccessory.visibility = View.VISIBLE
                binding.avatarAccessory.setImageResource(product.iconRes)

                // 调整层级关系
                binding.userAvatar.bringToFront()
                binding.avatarAccessory.bringToFront()
                Log.d("AvatarPreview", "Accessory resource: ${product.iconRes}")
            }
            else -> {
                Log.d("AvatarPreview", "Unknown product type")
            }
        }

        // 调试视图位置
        binding.root.post {
            Log.d("AvatarPreview", "Avatar position: ${binding.userAvatar.left}, ${binding.userAvatar.top}")
            Log.d("AvatarPreview", "Accessory position: ${binding.avatarAccessory.left}, ${binding.avatarAccessory.top}")
        }
    }

    companion object {
        const val TAG = "AvatarPreviewDialog"
    }
}