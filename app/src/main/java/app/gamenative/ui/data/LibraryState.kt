package app.gamenative.ui.data

import app.gamenative.PrefManager
import app.gamenative.data.LibraryItem
import app.gamenative.ui.enums.AppFilter
import java.util.EnumSet

data class LibraryState(
    val appInfoSortType: EnumSet<AppFilter> = PrefManager.libraryFilter,
    val appInfoList: List<LibraryItem> = emptyList(),
    val modalBottomSheet: Boolean = false,

    val isSearching: Boolean = false,
    val searchQuery: String = "",
)
