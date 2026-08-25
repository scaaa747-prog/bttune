package com.bt.bttune.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bt.bttune.innertube.YouTube
import com.bt.bttune.innertube.models.AlbumItem
import com.bt.bttune.innertube.models.ArtistItem
import com.bt.bttune.innertube.models.PlaylistItem
import com.bt.bttune.innertube.utils.completedLibraryPage
import com.bt.bttune.utils.reportException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AccountViewModel @Inject constructor() : ViewModel() {
    val playlists = MutableStateFlow<List<PlaylistItem>?>(null)
    val albums = MutableStateFlow<List<AlbumItem>?>(null)
    val artists = MutableStateFlow<List<ArtistItem>?>(null)

    init {
        viewModelScope.launch {
            YouTube.library("FEmusic_liked_playlists").completedLibraryPage().onSuccess {
                playlists.value = it.items.filterIsInstance<PlaylistItem>()
                    .filterNot { it.id == "SE" }
            }.onFailure {
                reportException(it)
            }
            YouTube.library("FEmusic_liked_albums").completedLibraryPage().onSuccess {
                albums.value = it.items.filterIsInstance<AlbumItem>()
            }.onFailure {
                reportException(it)
            }
            YouTube.library("FEmusic_library_corpus_artists").completedLibraryPage().onSuccess {
                artists.value = it.items.filterIsInstance<ArtistItem>()
            }.onFailure {
                reportException(it)
            }
        }
    }
}
