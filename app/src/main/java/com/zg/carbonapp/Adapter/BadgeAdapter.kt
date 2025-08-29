package com.zg.carbonapp.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.checkbox.MaterialCheckBox
import com.zg.carbonapp.Dao.VirtualProduct
import com.zg.carbonapp.MMKV.UserAssetsManager
import com.zg.carbonapp.databinding.ItemBadgeBinding

class BadgeAdapter(
    private val badges: List<VirtualProduct>,
    private val currentBadgeId: Int,
    private val onItemClick: (VirtualProduct) -> Unit
) : RecyclerView.Adapter<BadgeAdapter.BadgeViewHolder>() {

    inner class BadgeViewHolder(val binding: ItemBadgeBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BadgeViewHolder {
        return BadgeViewHolder(
            ItemBadgeBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: BadgeViewHolder, position: Int) {
        val badge = badges[position]
        val isSelected = badge.id == currentBadgeId

        with(holder.binding) {
            badgeIcon.setImageResource(badge.iconRes)
            badgeName.text = badge.name
            badgeDesc.text = badge.description
            cbSelected.isChecked = isSelected
            cbSelected.visibility = if (isSelected) View.VISIBLE else View.GONE

            root.setOnClickListener { onItemClick(badge) }
        }
    }

    override fun getItemCount() = badges.size
}