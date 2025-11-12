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
import io.github.lauramiron.nextuptv.ui.deeplinktest.LaunchMethod
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

    /**
     * Add deeplink test row for a specific streaming service.
     * Shows multiple launch methods for a single title.
     */
    private fun addDeepLinkTestRow(
        rowsAdapter: ArrayObjectAdapter,
        service: StreamingService,
        titleName: String,
        externalId: String,
        headerId: Long
    ) {
        // Create test cards for different launch methods
        val tests = listOf(
            DeepLinkItem(
                methodName = "HTTPS + Package",
                service = service,
                externalId = externalId,
                titleName = titleName,
                launchMethod = LaunchMethod.HTTPS_WITH_PACKAGE
            ),
            DeepLinkItem(
                methodName = "HTTPS Only",
                service = service,
                externalId = externalId,
                titleName = titleName,
                launchMethod = LaunchMethod.HTTPS_NO_PACKAGE
            ),
            DeepLinkItem(
                methodName = "Custom Scheme",
                service = service,
                externalId = externalId,
                titleName = titleName,
                launchMethod = LaunchMethod.CUSTOM_SCHEME
            ),
            DeepLinkItem(
                methodName = "Web Fallback",
                service = service,
                externalId = externalId,
                titleName = titleName,
                launchMethod = LaunchMethod.WEB_FALLBACK
            )
        )

        val cardPresenter = DeepLinkTestCardPresenter()
        val rowAdapter = ArrayObjectAdapter(cardPresenter).apply {
            tests.forEach { add(it) }
        }

        val header = HeaderItem(headerId, "${service.id.uppercase()} Deeplink Test: $titleName")
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

        // Test Deeplinks rows - one per service with example titles
        addDeepLinkTestRow(
            rowsAdapter = rowsAdapter,
            service = StreamingService.NETFLIX,
            titleName = "Stranger Things",
            externalId = "80057281",
            headerId = 2000L
        )

        addDeepLinkTestRow(
            rowsAdapter = rowsAdapter,
            service = StreamingService.APPLE,
            titleName = "Severance",
            externalId = "umc.cmc.1srk2goyh2q2zdxcx605w8vtx",
            headerId = 2001L
        )

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
                    DeeplinkTester.launch(requireContext(), item)
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

    companion object {
        private val TAG = "MainFragment"

        private val BACKGROUND_UPDATE_DELAY = 300
    }
}