package com.github.andreyasadchy.xtra.ui.chat

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil3.imageLoader
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.request.target
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.github.andreyasadchy.xtra.BuildConfig
import com.github.andreyasadchy.xtra.R
import com.github.andreyasadchy.xtra.model.chat.Emote

class EmotesAdapter(
    private val fragment: Fragment,
    private val clickListener: (Emote) -> Unit,
    private val emoteQuality: String,
    private val imageLibrary: String?,
) : ListAdapter<Emote, RecyclerView.ViewHolder>(
    object : DiffUtil.ItemCallback<Emote>() {
        override fun areItemsTheSame(oldItem: Emote, newItem: Emote): Boolean {
            return oldItem.name == newItem.name
        }

        override fun areContentsTheSame(oldItem: Emote, newItem: Emote): Boolean {
            return true
        }
    }
) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return object : RecyclerView.ViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.fragment_emotes_list_item, parent, false)
        ) {}
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val emote = getItem(position)
        if (imageLibrary == "0" || (imageLibrary == "1" && !emote.format.equals("webp", true))) {
            fragment.requireContext().imageLoader.enqueue(
                ImageRequest.Builder(fragment.requireContext()).apply {
                    data(
                        when (emoteQuality) {
                            "4" -> emote.url4x ?: emote.url3x ?: emote.url2x ?: emote.url1x
                            "3" -> emote.url3x ?: emote.url2x ?: emote.url1x
                            "2" -> emote.url2x ?: emote.url1x
                            else -> emote.url1x
                        }
                    )
                    if (emote.thirdParty) {
                        httpHeaders(NetworkHeaders.Builder().apply {
                            add("User-Agent", "Amethytw/" + BuildConfig.VERSION_NAME)
                        }.build())
                    }
                    crossfade(true)
                    target(holder.itemView as ImageView)
                }.build()
            )
        } else {
            Glide.with(fragment)
                .load(
                    when (emoteQuality) {
                        "4" -> emote.url4x ?: emote.url3x ?: emote.url2x ?: emote.url1x
                        "3" -> emote.url3x ?: emote.url2x ?: emote.url1x
                        "2" -> emote.url2x ?: emote.url1x
                        else -> emote.url1x
                    }
                )
                .diskCacheStrategy(DiskCacheStrategy.DATA)
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(holder.itemView as ImageView)
        }
        holder.itemView.setOnClickListener { clickListener(emote) }
    }
}
