package com.example

import android.content.Context
import android.content.ComponentName
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AppAccent(val primary: Color, val secondary: Color, val container: Color, val onContainer: Color) {
    CYAN(Color(0xFF22D3EE), Color(0xFF6366F1), Color(0xFFD1E4FF), Color(0xFF00315C)), // Default
    EMERALD(Color(0xFF34D399), Color(0xFF059669), Color(0xFFD1FAE5), Color(0xFF064E3B)),
    ROSE(Color(0xFFFB7185), Color(0xFFE11D48), Color(0xFFFFE4E6), Color(0xFF881337)),
    AMBER(Color(0xFFFBBF24), Color(0xFFD97706), Color(0xFFFEF3C7), Color(0xFF78350F)),
    PURPLE(Color(0xFFC084FC), Color(0xFF7E22CE), Color(0xFFF3E8FF), Color(0xFF3B0764))
}

data class PlayerState(
    val tracks: List<AudioTrack> = emptyList(),
    val currentTrack: AudioTrack? = null,
    val isPlaying: Boolean = false,
    val isReady: Boolean = false,
    val currentAccent: AppAccent = AppAccent.CYAN
)

class PlayerViewModel(
    private val repository: AudioRepository,
    private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlayerState())
    val uiState: StateFlow<PlayerState> = _uiState.asStateFlow()

    private var mediaController: MediaController? = null

    init {
        initializeController()
    }

    private fun initializeController() {
        val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        
        controllerFuture.addListener({
            try {
                mediaController = controllerFuture.get()
                setupControllerListener()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(context))
    }

    private fun setupControllerListener() {
        val controller = mediaController ?: return
        
        controller.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _uiState.update { it.copy(isPlaying = isPlaying) }
            }
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                super.onMediaItemTransition(mediaItem, reason)
                if (mediaItem != null) {
                    val trackId = mediaItem.mediaId.toLongOrNull()
                    val track = _uiState.value.tracks.find { it.id == trackId }
                    _uiState.update { it.copy(currentTrack = track) }
                } else {
                    _uiState.update { it.copy(currentTrack = null) }
                }
            }
        })
        
        // If tracks were loaded before controller was ready, set them now
        setMediaItemsToController()
    }

    fun loadTracks() {
        viewModelScope.launch {
            val tracks = repository.getAudioFiles()
            _uiState.update { it.copy(tracks = tracks, isReady = true) }
            setMediaItemsToController()
        }
    }

    private fun setMediaItemsToController() {
        val controller = mediaController ?: return
        val currentTracks = _uiState.value.tracks
        
        if (currentTracks.isEmpty()) return
        
        val mediaItems = currentTracks.map { track ->
            MediaItem.Builder()
                .setMediaId(track.id.toString())
                .setUri(track.uri)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(track.title)
                        .setArtist(track.artist)
                        .setIsPlayable(true)
                        .build()
                )
                .build()
        }
        controller.setMediaItems(mediaItems)
        controller.prepare()
    }

    fun togglePlayPause() {
        val controller = mediaController ?: return
        if (controller.isPlaying) {
            controller.pause()
        } else {
            controller.play()
        }
    }
    
    fun nextTrack() {
        val controller = mediaController ?: return
        if (controller.hasNextMediaItem()) {
            controller.seekToNext()
            controller.play()
        }
    }
    
    fun previousTrack() {
        val controller = mediaController ?: return
        if (controller.hasPreviousMediaItem()) {
            controller.seekToPrevious()
            controller.play()
        }
    }
    
    fun playTrack(track: AudioTrack) {
        val controller = mediaController ?: return
        val index = _uiState.value.tracks.indexOf(track)
        if (index != -1) {
            controller.seekTo(index, 0L)
            controller.play()
        }
    }
    
    fun cycleAccent() {
        val accents = AppAccent.values()
        val nextIndex = (_uiState.value.currentAccent.ordinal + 1) % accents.size
        _uiState.update { it.copy(currentAccent = accents[nextIndex]) }
    }

    override fun onCleared() {
        super.onCleared()
        mediaController?.release()
    }
}

class PlayerViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PlayerViewModel::class.java)) {
            val repository = AudioRepository(context)
            @Suppress("UNCHECKED_CAST")
            return PlayerViewModel(repository, context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
