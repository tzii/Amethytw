package com.github.andreyasadchy.xtra

import android.content.Context
import com.bumptech.glide.Glide
import com.bumptech.glide.Registry
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.integration.okhttp3.OkHttpUrlLoader
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.module.AppGlideModule
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors.fromApplication
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import java.io.InputStream


@GlideModule
class AmethytwGlideModule : AppGlideModule() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    internal interface GlideModuleEntryPoint {
        fun okHttpClient(): OkHttpClient
    }

    override fun isManifestParsingEnabled(): Boolean = false

    override fun registerComponents(context: Context, glide: Glide, registry: Registry) {
        val entryPoint = fromApplication(context.applicationContext, GlideModuleEntryPoint::class.java)
        registry.replace(GlideUrl::class.java, InputStream::class.java, OkHttpUrlLoader.Factory(entryPoint.okHttpClient()))
        super.registerComponents(context, glide, registry)
    }
}
