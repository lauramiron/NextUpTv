package io.github.lauramiron.nextuptv.ui.main

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.leanback.app.BackgroundManager
import androidx.leanback.app.BrowseSupportFragment
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.HeaderItem
import androidx.leanback.widget.ListRow
import androidx.leanback.widget.ListRowPresenter
import androidx.leanback.widget.OnItemViewClickedListener
import androidx.leanback.widget.OnItemViewSelectedListener
import androidx.leanback.widget.Presenter
import androidx.leanback.widget.Row
import androidx.leanback.widget.RowPresenter
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import io.github.lauramiron.nextuptv.AppSource
import io.github.lauramiron.nextuptv.NextUpTvApplication
import io.github.lauramiron.nextuptv.R
import io.github.lauramiron.nextuptv.ResumeSource
import io.github.lauramiron.nextuptv.data.LibraryRepository
import io.github.lauramiron.nextuptv.data.local.entity.StreamingService
import io.github.lauramiron.nextuptv.data.mappers.toMovieItem
import io.github.lauramiron.nextuptv.ui.app.AppCardPresenter
import io.github.lauramiron.nextuptv.ui.app.AppItem
import io.github.lauramiron.nextuptv.ui.common.CardPresenter
import io.github.lauramiron.nextuptv.ui.common.StreamingLauncher
import io.github.lauramiron.nextuptv.ui.deeplinktest.DeepLinkItem
import io.github.lauramiron.nextuptv.ui.deeplinktest.DeepLinkTestCardPresenter
import io.github.lauramiron.nextuptv.ui.deeplinktest.DeeplinkTester
import io.github.lauramiron.nextuptv.ui.details.MovieItem
import io.github.lauramiron.nextuptv.ui.resume.ResumeCardPresenter
import kotlinx.coroutines.launch
import java.util.Timer
import java.util.TimerTask

/**
 * Loads a grid of cards with movies to browse.
 */
class MainFragment : BrowseSupportFragment() {

    private val mHandler = Handler(Looper.myLooper()!!)
    private lateinit var mBackgroundManager: BackgroundManager
    private var mDefaultBackground: Drawable? = null
    private lateinit var mMetrics: DisplayMetrics
    private var mBackgroundTimer: Timer? = null
    private var mBackgroundUri: String? = null
    private lateinit var repository: LibraryRepository

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        Log.i(TAG, "onCreate")
        super.onActivityCreated(savedInstanceState)

        // Initialize repository
        repository = NextUpTvApplication.getRepository(requireContext())

        prepareBackgroundManager()

        setupUIElements()

        loadRows()

        setupEventListeners()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy: " + mBackgroundTimer?.toString())
        mBackgroundTimer?.cancel()
    }

    private fun prepareBackgroundManager() {

        mBackgroundManager = BackgroundManager.getInstance(activity)
        mBackgroundManager.attach(requireActivity().window)
        mDefaultBackground = ContextCompat.getDrawable(requireActivity(), R.drawable.default_background)
        mMetrics = DisplayMetrics()
        requireActivity().windowManager.defaultDisplay.getMetrics(mMetrics)
    }

    private fun setupUIElements() {
        title = getString(R.string.browse_title)
        // over title
        headersState = HEADERS_ENABLED
        isHeadersTransitionOnBackEnabled = true

        // set fastLane (or headers) background color
        brandColor = ContextCompat.getColor(requireActivity(), R.color.fastlane_background)
        // set search icon color
        searchAffordanceColor = ContextCompat.getColor(requireActivity(), R.color.search_opaque)
    }

    private fun addDeepLinkTestRow(rowsAdapter: ArrayObjectAdapter) {
        // Replace IDs with ones you want to test
        val tests = listOf(
            DeepLinkItem("Stranger Things", "80077368"),
            DeepLinkItem("Dark", "80114790"),
            DeepLinkItem("Black Mirror", "81716301")
        )

        val cardPresenter = DeepLinkTestCardPresenter()
        val rowAdapter = ArrayObjectAdapter(cardPresenter).apply {
            tests.forEach { add(it) }
        }

        val header = HeaderItem(1000L, "Netflix Deep Link Tests")
        rowsAdapter.add(ListRow(header, rowAdapter))
    }

    private fun addResumeRow(rowsAdapter: ArrayObjectAdapter) {
        val resumeEntries = ResumeSource().load(requireContext())
        val presenter = ResumeCardPresenter()

        if (resumeEntries.isEmpty()) return

        val resumeAdapter = ArrayObjectAdapter(presenter).apply {
            resumeEntries.forEach { add(it) }
        }
        val header = HeaderItem(1L, "Resume Watching")
        rowsAdapter.add(ListRow(header, resumeAdapter))
    }

    private fun loadRows() {
        val rowsAdapter = ArrayObjectAdapter(ListRowPresenter())

        val appsSource = AppSource()
        val apps = appsSource.loadApps(requireContext())
        val appsRowsAdapter = ArrayObjectAdapter(AppCardPresenter())
        apps.forEach { appsRowsAdapter.add(it) }

        val header = HeaderItem(0L, "Apps")
        rowsAdapter.add(ListRow(header, appsRowsAdapter))

        // Resume Watching Row
//        addResumeRow(rowsAdapter)

        // Top Shows rows for each streaming service
        addTopShowsRows(rowsAdapter)

        // Test Deeplinks row
        addDeepLinkTestRow(rowsAdapter)


//        val gridHeader = HeaderItem(NUM_ROWS.toLong(), "PREFERENCES")
//
//        val mGridPresenter = GridItemPresenter()
//        val gridRowAdapter = ArrayObjectAdapter(mGridPresenter)
//        gridRowAdapter.add(resources.getString(R.string.grid_view))
//        gridRowAdapter.add(getString(R.string.error_fragment))
//        gridRowAdapter.add(resources.getString(R.string.personal_settings))
//        rowsAdapter.add(ListRow(gridHeader, gridRowAdapter))

        adapter = rowsAdapter
    }

    private fun addTopShowsRows(rowsAdapter: ArrayObjectAdapter) {
        // List of streaming services to display
        val services = listOf(
            StreamingService.NETFLIX to "Top on Netflix",
            StreamingService.PRIME to "Top on Prime Video",
            StreamingService.DISNEY to "Top on Disney+",
            StreamingService.APPLE to "Top on Apple TV+",
            StreamingService.HBO to "Top on HBO Max"
        )

        services.forEachIndexed { index, (service, title) ->
            val headerId = 100L + index
            val header = HeaderItem(headerId, title)
            val cardPresenter = CardPresenter()
            val listRowAdapter = ArrayObjectAdapter(cardPresenter)

            // Add empty row first, will be populated asynchronously
            rowsAdapter.add(ListRow(header, listRowAdapter))

            // Load top shows asynchronously
            lifecycleScope.launch {
                try {
                    val titlesWithExternalIds = repository.topTitlesWithExternalIds(service)
                    val movieItems = titlesWithExternalIds.map { it.toMovieItem(service) }

                    // Update adapter on main thread
                    mHandler.post {
                        movieItems.forEach { listRowAdapter.add(it) }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error loading top shows for $service", e)
                }
            }
        }
    }

    private fun setupEventListeners() {
        setOnSearchClickedListener {
            Toast.makeText(requireActivity(), "Implement your own in-app search", Toast.LENGTH_LONG)
                .show()
        }

        onItemViewClickedListener = ItemViewClickedListener()
        onItemViewSelectedListener = ItemViewSelectedListener()
    }

    private inner class ItemViewClickedListener : OnItemViewClickedListener {
        override fun onItemClicked(
            itemViewHolder: Presenter.ViewHolder,
            item: Any,
            rowViewHolder: RowPresenter.ViewHolder,
            row: Row
        ) {
            when (item) {

                // Launch streaming content
                is MovieItem -> {
                    val videoUrl = item.videoUrl
                    if (videoUrl != null) {
                        StreamingLauncher.launch(requireContext(), videoUrl)
                    } else {
                        Toast.makeText(requireContext(),
                            "No video URL available for ${item.title}", Toast.LENGTH_SHORT).show()
                    }
                }

                // If your adapter stores a custom AppEntry
                is AppItem -> {
                    val pm = requireContext().packageManager
                    val launch = pm.getLeanbackLaunchIntentForPackage(item.packageName)
                        ?: pm.getLaunchIntentForPackage(item.packageName)
                    if (launch != null) {
//                        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(launch)
                    } else {
                        Toast.makeText(requireContext(),
                            "Can't launch ${item.label}", Toast.LENGTH_SHORT).show()
                    }
                }

                is DeepLinkItem -> {
                    DeeplinkTester.launch(requireContext(), item.netflixId)
                }
            }
        }
    }

    private inner class ItemViewSelectedListener : OnItemViewSelectedListener {
        override fun onItemSelected(itemViewHolder: Presenter.ViewHolder?, item: Any?,
                                    rowViewHolder: RowPresenter.ViewHolder, row: Row
        ) {
            if (item is MovieItem) {
                mBackgroundUri = item.backgroundImageUrl
                startBackgroundTimer()
            }
        }
    }

    private fun updateBackground(uri: String?) {
        val width = mMetrics.widthPixels
        val height = mMetrics.heightPixels
        Glide.with(requireActivity())
                .load(uri)
                .centerCrop()
                .error(mDefaultBackground)
                .into<SimpleTarget<Drawable>>(
                        object : SimpleTarget<Drawable>(width, height) {
                            override fun onResourceReady(drawable: Drawable,
                                                         transition: Transition<in Drawable>?) {
                                mBackgroundManager.drawable = drawable
                            }
                        })
        mBackgroundTimer?.cancel()
    }

    private fun startBackgroundTimer() {
        mBackgroundTimer?.cancel()
        mBackgroundTimer = Timer()
        mBackgroundTimer?.schedule(UpdateBackgroundTask(), BACKGROUND_UPDATE_DELAY.toLong())
    }

    private inner class UpdateBackgroundTask : TimerTask() {

        override fun run() {
            mHandler.post { updateBackground(mBackgroundUri) }
        }
    }

//    private inner class GridItemPresenter : Presenter() {
//        override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
//            val view = TextView(parent.context)
//            view.layoutParams = ViewGroup.LayoutParams(GRID_ITEM_WIDTH, GRID_ITEM_HEIGHT)
//            view.isFocusable = true
//            view.isFocusableInTouchMode = true
//            view.setBackgroundColor(ContextCompat.getColor(activity!!, R.color.default_background))
//            view.setTextColor(Color.WHITE)
//            view.gravity = Gravity.CENTER
//            return ViewHolder(view)
//        }
//
//        override fun onBindViewHolder(viewHolder: ViewHolder, item: Any) {
//            (viewHolder.view as TextView).text = item as String
//        }
//
//        override fun onUnbindViewHolder(viewHolder: ViewHolder) {}
//    }

    companion object {
        private val TAG = "MainFragment"

        private val BACKGROUND_UPDATE_DELAY = 300
        private val GRID_ITEM_WIDTH = 200
        private val GRID_ITEM_HEIGHT = 200
        private val NUM_ROWS = 6
        private val NUM_COLS = 15
    }
}