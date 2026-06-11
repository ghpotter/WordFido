package com.gregoryhpotter.textlistscanner.di

import android.content.Context
import android.content.SharedPreferences
import androidx.room.Room
import com.gregoryhpotter.textlistscanner.data.db.AppDatabase
import com.gregoryhpotter.textlistscanner.feedback.AndroidAudioFeedbackProvider
import com.gregoryhpotter.textlistscanner.feedback.AndroidHapticFeedbackProvider
import com.gregoryhpotter.textlistscanner.feedback.AudioFeedbackProvider
import com.gregoryhpotter.textlistscanner.feedback.FeedbackManager
import com.gregoryhpotter.textlistscanner.feedback.HapticFeedbackProvider
import com.gregoryhpotter.textlistscanner.matcher.WordMatcher
import com.gregoryhpotter.textlistscanner.ocr.OcrResultProcessor
import com.gregoryhpotter.textlistscanner.ui.camera.CoordinateMapper
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideWordMatcher(): WordMatcher = WordMatcher()

    @Provides
    @Singleton
    fun provideOcrResultProcessor(wordMatcher: WordMatcher): OcrResultProcessor =
        OcrResultProcessor(wordMatcher)

    @Provides
    @Singleton
    fun provideCoordinateMapper(): CoordinateMapper = CoordinateMapper()

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "word_fido.db").build()

    @Provides
    @Singleton
    fun provideHapticFeedbackProvider(
        @ApplicationContext context: Context
    ): HapticFeedbackProvider = AndroidHapticFeedbackProvider(context)

    @Provides
    @Singleton
    fun provideAudioFeedbackProvider(): AudioFeedbackProvider =
        AndroidAudioFeedbackProvider()

    @Provides
    @Singleton
    fun provideFeedbackManager(
        hapticProvider: HapticFeedbackProvider,
        audioProvider: AudioFeedbackProvider
    ): FeedbackManager = FeedbackManager(hapticProvider, audioProvider)

    @Provides
    @Singleton
    fun provideSharedPreferences(@ApplicationContext context: Context): SharedPreferences =
        context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
}
