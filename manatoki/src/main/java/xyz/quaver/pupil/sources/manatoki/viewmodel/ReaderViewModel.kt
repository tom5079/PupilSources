package xyz.quaver.pupil.sources.manatoki.viewmodel

import android.app.Application
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import xyz.quaver.io.FileX
import xyz.quaver.pupil.sources.base.composables.ReaderBaseViewModel
import xyz.quaver.pupil.sources.base.composables.ReaderItem
import xyz.quaver.pupil.sources.manatoki.FavoriteDao
import xyz.quaver.pupil.sources.manatoki.HistoryDao
import xyz.quaver.pupil.sources.manatoki.networking.ManatokiHttpClient
import xyz.quaver.pupil.sources.manatoki.networking.MangaListing
import xyz.quaver.pupil.sources.manatoki.networking.ReaderInfo

class ReaderViewModel(
    private val app: Application,
    private val client: ManatokiHttpClient,
    private val favoriteDao: FavoriteDao,
    private val historyDao: HistoryDao
) : ReaderBaseViewModel() {
    var readerInfo by mutableStateOf<ReaderInfo?>(null)
        private set

    var mangaListing: MangaListing? by mutableStateOf(null)
        private set

    var isFavorite by mutableStateOf(false)
        private set

    fun load(itemID: String) = viewModelScope.launch {
        val readerInfo = client.getItem(itemID)

        check(readerInfo is ReaderInfo)

        this@ReaderViewModel.readerInfo = readerInfo

        updateHistory()

        items = client.images(readerInfo).map { (image, progress) ->
            ReaderItem(FileX(app, image), progress)
        }
    }

    fun toggleFavorite() {

    }

    fun updateHistory(currentPage: Int = 0) {
        val itemID = readerInfo?.itemID ?: return
        val parent = readerInfo?.listingItemID ?: return

        viewModelScope.launch {
            historyDao.insert(itemID, parent, currentPage)
        }
    }

    fun loadList(itemID: String) {
        viewModelScope.launch {
            val listing = client.getItem(itemID)

            check(listing is MangaListing)

            mangaListing = listing
        }
    }
}