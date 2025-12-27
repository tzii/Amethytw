package com.github.andreyasadchy.xtra.di

import android.app.Application
import android.net.http.HttpEngine
import android.os.Build
import android.os.ext.SdkExtensions
import android.util.Log
import androidx.annotation.OptIn
import com.github.andreyasadchy.xtra.BuildConfig
import com.github.andreyasadchy.xtra.R
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.chromium.net.CronetEngine
import org.chromium.net.CronetProvider
import org.chromium.net.QuicOptions
import org.chromium.net.RequestFinishedInfo
import org.conscrypt.Conscrypt
import java.security.KeyStore
import java.security.Security
import java.security.cert.CertificateFactory
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.inject.Singleton
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

@Module
@InstallIn(SingletonComponent::class)
class ThystTVModule {

    @Singleton
    @Provides
    fun providesHttpEngine(application: Application): HttpEngine? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && SdkExtensions.getExtensionVersion(Build.VERSION_CODES.S) >= 7) {
            HttpEngine.Builder(application).apply {
                addQuicHint("gql.twitch.tv", 443, 443)
                addQuicHint("www.twitch.tv", 443, 443)
                addQuicHint("7tv.io", 443, 443)
                addQuicHint("cdn.7tv.app", 443, 443)
                addQuicHint("api.betterttv.net", 443, 443)
            }.build()
        } else {
            null
        }
    }

    @Singleton
    @Provides
    @OptIn(QuicOptions.Experimental::class)
    fun providesCronetEngine(application: Application): CronetEngine? {
        return if (CronetProvider.getAllProviders(application).any { it.isEnabled }) {
            CronetEngine.Builder(application).apply {
                val userAgent = "Cronet/" + defaultUserAgent.substringAfter("Cronet/", "").substringBefore(')')
                setUserAgent(userAgent)
                setQuicOptions(QuicOptions.builder().setHandshakeUserAgent(userAgent).build())
                addQuicHint("gql.twitch.tv", 443, 443)
                addQuicHint("www.twitch.tv", 443, 443)
                addQuicHint("7tv.io", 443, 443)
                addQuicHint("cdn.7tv.app", 443, 443)
                addQuicHint("api.betterttv.net", 443, 443)
            }.build().also {
                if (BuildConfig.DEBUG) {
                    it.addRequestFinishedListener(object : RequestFinishedInfo.Listener(Executors.newSingleThreadExecutor()) {
                        override fun onRequestFinished(requestInfo: RequestFinishedInfo) {
                            requestInfo.responseInfo?.let {
                                Log.i("Cronet", "${it.httpStatusCode} ${it.negotiatedProtocol} ${it.url}")
                                it.allHeadersAsList?.forEach {
                                    Log.i("Cronet", "${it.key}: ${it.value}")
                                }
                            }
                        }
                    })
                }
            }
        } else {
            null
        }
    }

    @Singleton
    @Provides
    fun providesCronetExecutor(): ExecutorService {
        return Executors.newCachedThreadPool()
    }

    @Singleton
    @Provides
    fun providesOkHttpClient(trustManager: X509TrustManager?): OkHttpClient {
        val builder = OkHttpClient.Builder().apply {
            if (BuildConfig.DEBUG) {
                addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY })
            }
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                val conscrypt = Conscrypt.newProvider()
                Security.insertProviderAt(conscrypt, 1)
            }
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N && trustManager != null) {
                val sslContext = SSLContext.getInstance("TLSv1.3")
                sslContext.init(null, arrayOf(trustManager), null)
                sslSocketFactory(sslContext.socketFactory, trustManager)
            }
        }
        return builder.build()
    }

    @Singleton
    @Provides
    fun providesTrustManager(application: Application): X509TrustManager? {
        return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            val keyStore = KeyStore.getInstance(KeyStore.getDefaultType())
            keyStore.load(null, null)
            var count = 0
            val certificateFactory = CertificateFactory.getInstance("X.509")
            application.resources.openRawResource(R.raw.isrgrootx1).use {
                val certificate = certificateFactory.generateCertificate(it)
                keyStore.setCertificateEntry("cert_0", certificate)
                count += 1
            }
            val defaultTrustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
            defaultTrustManagerFactory.init(null as KeyStore?)
            val defaultTrustManager = defaultTrustManagerFactory.trustManagers.first() as X509TrustManager
            defaultTrustManager.acceptedIssuers.forEach {
                keyStore.setCertificateEntry("cert_$count", it)
                count += 1
            }
            val trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
            trustManagerFactory.init(keyStore)
            trustManagerFactory.trustManagers.first() as X509TrustManager
        } else {
            null
        }
    }

    @Singleton
    @Provides
    fun providesJsonInstance(): Json {
        return Json { ignoreUnknownKeys = true }
    }
}
