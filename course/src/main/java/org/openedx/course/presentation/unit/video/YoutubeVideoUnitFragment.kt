package org.openedx.course.presentation.unit.video

import android.os.Bundle
import android.view.LayoutInflater
import android.view.OrientationEventListener
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.utils.YouTubePlayerTracker
import com.pierfrancescosoffritti.androidyoutubeplayer.core.ui.DefaultPlayerUiController
import org.openedx.core.extension.computeWindowSizeClasses
import org.openedx.core.extension.objectToString
import org.openedx.core.extension.stringToObject
import org.openedx.core.presentation.dialog.SelectBottomDialogFragment
import org.openedx.core.ui.WindowSize
import org.openedx.core.ui.theme.OpenEdXTheme
import org.openedx.core.ui.theme.appColors
import org.openedx.core.utils.LocaleUtils
import org.openedx.course.R
import org.openedx.course.databinding.FragmentYoutubeVideoUnitBinding
import org.openedx.course.presentation.CourseRouter
import org.openedx.course.presentation.ui.ConnectionErrorView
import org.openedx.course.presentation.ui.VideoRotateView
import org.openedx.course.presentation.ui.VideoSubtitles
import org.openedx.course.presentation.ui.VideoTitle
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class YoutubeVideoUnitFragment : Fragment(R.layout.fragment_youtube_video_unit) {

    private val viewModel by viewModel<VideoUnitViewModel> {
        parametersOf(requireArguments().getString(ARG_COURSE_ID, ""))
    }
    private val router by inject<CourseRouter>()
    private var _binding: FragmentYoutubeVideoUnitBinding? = null
    private val binding get() = _binding!!

    private var windowSize: WindowSize? = null
    private var orientationListener: OrientationEventListener? = null
    private var _youTubePlayer: YouTubePlayer? = null

    private var blockId = ""

    private var isPlayerInitialized = false

    private val youtubeTrackerListener = YouTubePlayerTracker()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        windowSize = computeWindowSizeClasses()
        lifecycle.addObserver(viewModel)
        requireArguments().apply {
            viewModel.videoUrl = getString(ARG_VIDEO_URL, "")
            viewModel.transcripts = stringToObject<Map<String, String>>(
                getString(ARG_TRANSCRIPT_URL, "")
            ) ?: emptyMap()
            blockId = getString(ARG_BLOCK_ID, "")
        }
        viewModel.downloadSubtitles()
        orientationListener = object : OrientationEventListener(requireActivity()) {
            override fun onOrientationChanged(orientation: Int) {
                if (windowSize?.isTablet != true) {
                    if (orientation in 75..300) {
                        if (!viewModel.fullscreenHandled) {
                            router.navigateToFullScreenYoutubeVideo(
                                requireActivity().supportFragmentManager,
                                viewModel.videoUrl,
                                viewModel.getCurrentVideoTime(),
                                blockId,
                                viewModel.courseId
                            )
                            viewModel.fullscreenHandled = true
                        }
                    } else {
                        viewModel.fullscreenHandled = false
                    }
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentYoutubeVideoUnitBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.cvRotateHelper.setContent {
            OpenEdXTheme {
                VideoRotateView()
            }
        }

        binding.cvVideoTitle.setContent {
            OpenEdXTheme {
                VideoTitle(text = requireArguments().getString(ARG_TITLE) ?: "")
            }
        }

        binding.connectionError.setContent {
            OpenEdXTheme {
                ConnectionErrorView(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.appColors.background)
                ) {
                    binding.connectionError.isVisible = !viewModel.hasInternetConnection
                }
            }
        }

        binding.subtitles.setContent {
            OpenEdXTheme {
                val state = rememberLazyListState()
                val currentIndex by viewModel.currentIndex.collectAsState(0)
                val transcriptObject by viewModel.transcriptObject.observeAsState()
                VideoSubtitles(
                    listState = state,
                    timedTextObject = transcriptObject,
                    subtitleLanguage = LocaleUtils.getDisplayLanguage(viewModel.transcriptLanguage),
                    showSubtitleLanguage = viewModel.transcripts.size > 1,
                    currentIndex = currentIndex,
                    onSettingsClick = {
                        _youTubePlayer?.pause()
                        val dialog =
                            SelectBottomDialogFragment.newInstance(
                                LocaleUtils.getLanguages(viewModel.transcripts.keys.toList())
                            )
                        dialog.show(
                            requireActivity().supportFragmentManager,
                            SelectBottomDialogFragment::class.simpleName
                        )
                    }
                )
            }
        }

        binding.connectionError.isVisible = !viewModel.hasInternetConnection

        lifecycle.addObserver(binding.youtubePlayerView)

        val options = IFramePlayerOptions.Builder()
            .controls(0)
            .rel(0)
            .build()

        val listener = object : AbstractYouTubePlayerListener() {
            override fun onCurrentSecond(youTubePlayer: YouTubePlayer, second: Float) {
                super.onCurrentSecond(youTubePlayer, second)
                viewModel.setCurrentVideoTime((second * 1000f).toLong())
                val completePercentage = second / youtubeTrackerListener.videoDuration
                if (completePercentage >= 0.8f) {
                    viewModel.markBlockCompleted(blockId)
                }
            }

            override fun onReady(youTubePlayer: YouTubePlayer) {
                super.onReady(youTubePlayer)
                _youTubePlayer = youTubePlayer
                if (_binding != null) {
                    val defPlayerUiController = DefaultPlayerUiController(
                        binding.youtubePlayerView,
                        youTubePlayer
                    )
                    defPlayerUiController.setFullScreenButtonClickListener {
                        viewModel.fullscreenHandled = true
                        router.navigateToFullScreenYoutubeVideo(
                            requireActivity().supportFragmentManager,
                            viewModel.videoUrl,
                            viewModel.getCurrentVideoTime(),
                            blockId,
                            viewModel.courseId
                        )
                    }
                    binding.youtubePlayerView.setCustomPlayerUi(defPlayerUiController.rootView)
                }

                val videoId = viewModel.videoUrl.split("watch?v=")[1]
                youTubePlayer.cueVideo(videoId, viewModel.getCurrentVideoTime().toFloat() / 1000)
                youTubePlayer.addListener(youtubeTrackerListener)
            }
        }

        if (!isPlayerInitialized) {
            binding.youtubePlayerView.initialize(listener, options)
            isPlayerInitialized = true
        }

        viewModel.isPopUpViewShow.observe(viewLifecycleOwner) {
            if (windowSize?.isTablet != true) {
                binding.cvRotateHelper.isVisible = it
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (orientationListener?.canDetectOrientation() == true) {
            orientationListener?.enable()
        }
    }

    override fun onPause() {
        super.onPause()
        _youTubePlayer?.pause()
        orientationListener?.disable()
    }

    override fun onDestroyView() {
        isPlayerInitialized = false
        _youTubePlayer = null
        super.onDestroyView()
        _binding = null
    }

    companion object {

        private const val ARG_VIDEO_URL = "videoUrl"
        private const val ARG_TRANSCRIPT_URL = "transcriptUrl"
        private const val ARG_BLOCK_ID = "blockId"
        private const val ARG_COURSE_ID = "courseId"
        private const val ARG_TITLE = "blockTitle"

        fun newInstance(
            blockId: String,
            courseId: String,
            videoUrl: String,
            transcriptsUrl: Map<String, String>,
            blockTitle: String
        ): YoutubeVideoUnitFragment {
            val fragment = YoutubeVideoUnitFragment()
            fragment.arguments = bundleOf(
                ARG_VIDEO_URL to videoUrl,
                ARG_TRANSCRIPT_URL to objectToString(transcriptsUrl),
                ARG_BLOCK_ID to blockId,
                ARG_COURSE_ID to courseId,
                ARG_TITLE to blockTitle
            )
            return fragment
        }
    }
}
