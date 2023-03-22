package com.raccoongang.course.presentation.outline

import android.content.res.Configuration.UI_MODE_NIGHT_NO
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import com.raccoongang.core.BlockType
import com.raccoongang.core.R
import com.raccoongang.core.UIMessage
import com.raccoongang.core.domain.model.*
import com.raccoongang.core.extension.parcelable
import com.raccoongang.core.presentation.course.CourseViewMode
import com.raccoongang.core.ui.*
import com.raccoongang.core.ui.theme.NewEdxTheme
import com.raccoongang.core.ui.theme.appColors
import com.raccoongang.core.ui.theme.appTypography
import com.raccoongang.course.presentation.CourseRouter
import com.raccoongang.course.presentation.container.CourseContainerFragment
import com.raccoongang.course.presentation.ui.CourseImageHeader
import com.raccoongang.course.presentation.ui.CourseSectionCard
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import java.io.File
import java.util.*

class CourseOutlineFragment : Fragment() {

    private val viewModel by viewModel<CourseOutlineViewModel> {
        parametersOf(requireArguments().getString(ARG_COURSE_ID, ""))
    }
    private val router by inject<CourseRouter>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycle.addObserver(viewModel)
        with(requireArguments()) {
            viewModel.courseImage = getString(ARG_IMAGE, "")
            viewModel.courseTitle = getString(ARG_TITLE, "")
            viewModel.courseCertificate = parcelable(ARG_CERTIFICATE)!!
        }
        viewModel.getCourseData()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            NewEdxTheme {
                val windowSize = rememberWindowSize()

                val uiState by viewModel.uiState.observeAsState()
                val uiMessage by viewModel.uiMessage.observeAsState()
                val refreshing by viewModel.isUpdating.observeAsState(false)

                CourseOutlineScreen(
                    windowSize = windowSize,
                    uiState = uiState!!,
                    courseImage = viewModel.courseImage,
                    courseTitle = viewModel.courseTitle,
                    courseCertificate = viewModel.courseCertificate,
                    uiMessage = uiMessage,
                    refreshing = refreshing,
                    onSwipeRefresh = {
                        viewModel.setIsUpdating()
                        (parentFragment as CourseContainerFragment).updateCourseStructure(true)
                    },
                    hasInternetConnection = viewModel.hasInternetConnection,
                    onReloadClick = {
                        (parentFragment as CourseContainerFragment).updateCourseStructure(false)
                    },
                    onItemClick = { block ->
                        router.navigateToCourseSubsections(
                            requireActivity().supportFragmentManager,
                            courseId = viewModel.courseId,
                            blockId = block.id,
                            title = block.displayName,
                            mode = CourseViewMode.FULL
                        )
                    },
                    onResumeClick = { blockId ->
                        router.navigateToCourseContainer(
                            requireActivity().supportFragmentManager,
                            blockId = blockId,
                            courseId = viewModel.courseId,
                            courseName = viewModel.courseTitle,
                            mode = CourseViewMode.FULL
                        )
                    },
                    onBackClick = {
                        requireActivity().supportFragmentManager.popBackStack()
                    },
                    onDownloadClick = {
                        if (viewModel.isBlockDownloading(it.id)) {
                            viewModel.cancelWork(it.id)
                        } else if (viewModel.isBlockDownloaded(it.id)) {
                            viewModel.removeDownloadedModels(it.id)
                        } else {
                            viewModel.saveDownloadModels(
                                requireContext().externalCacheDir.toString() +
                                        File.separator +
                                        requireContext()
                                            .getString(R.string.app_name)
                                            .replace(Regex("\\s"), "_"), it.id
                            )
                        }
                    }
                )
            }
        }
    }


    companion object {
        private const val ARG_COURSE_ID = "courseId"
        private const val ARG_TITLE = "title"
        private const val ARG_IMAGE = "image"
        private const val ARG_CERTIFICATE = "certificate"
        fun newInstance(
            courseId: String,
            title: String,
            image: String,
            certificate: Certificate
        ): CourseOutlineFragment {
            val fragment = CourseOutlineFragment()
            fragment.arguments = bundleOf(
                ARG_COURSE_ID to courseId,
                ARG_TITLE to title,
                ARG_IMAGE to image,
                ARG_CERTIFICATE to certificate
            )
            return fragment
        }
    }
}


@OptIn(ExperimentalMaterialApi::class)
@Composable
internal fun CourseOutlineScreen(
    windowSize: WindowSize,
    uiState: CourseOutlineUIState,
    courseTitle: String,
    courseImage: String,
    courseCertificate: Certificate,
    uiMessage: UIMessage?,
    refreshing: Boolean,
    hasInternetConnection: Boolean,
    onReloadClick: () -> Unit,
    onSwipeRefresh: () -> Unit,
    onItemClick: (Block) -> Unit,
    onResumeClick: (String) -> Unit,
    onBackClick: () -> Unit,
    onDownloadClick: (Block) -> Unit
) {
    val scaffoldState = rememberScaffoldState()
    val pullRefreshState =
        rememberPullRefreshState(refreshing = refreshing, onRefresh = { onSwipeRefresh() })

    var isInternetConnectionShown by rememberSaveable {
        mutableStateOf(false)
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize(),
        scaffoldState = scaffoldState,
        backgroundColor = MaterialTheme.appColors.background
    ) {

        val screenWidth by remember(key1 = windowSize) {
            mutableStateOf(
                windowSize.windowSizeValue(
                    expanded = Modifier.widthIn(Dp.Unspecified, 560.dp),
                    compact = Modifier.fillMaxWidth()
                )
            )
        }

        val imageHeight by remember(key1 = windowSize) {
            mutableStateOf(
                windowSize.windowSizeValue(
                    expanded = 260.dp,
                    compact = 200.dp
                )
            )
        }

        val listPadding by remember(key1 = windowSize) {
            mutableStateOf(
                windowSize.windowSizeValue(
                    expanded = PaddingValues(
                        start = 6.dp,
                        end = 6.dp,
                        bottom = 24.dp
                    ),
                    compact = PaddingValues(
                        start = 24.dp,
                        end = 24.dp,
                        bottom = 24.dp
                    )
                )
            )
        }

        HandleUIMessage(uiMessage = uiMessage, scaffoldState = scaffoldState)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(it)
                .statusBarsInset(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                screenWidth
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .zIndex(1f),
                    contentAlignment = Alignment.CenterStart
                ) {
                    BackBtn {
                        onBackClick()
                    }
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 56.dp),
                        text = courseTitle,
                        color = MaterialTheme.appColors.textPrimary,
                        style = MaterialTheme.appTypography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }
                Spacer(Modifier.height(6.dp))
                Surface(
                    color = MaterialTheme.appColors.background
                ) {
                    Box(Modifier.pullRefresh(pullRefreshState)) {
                        when (uiState) {
                            is CourseOutlineUIState.Loading -> {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator()
                                }
                            }
                            is CourseOutlineUIState.CourseData -> {
                                Column(
                                    Modifier
                                        .fillMaxSize()
                                ) {
                                    CourseImageHeader(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(imageHeight)
                                            .padding(6.dp),
                                        courseImage = courseImage,
                                        courseCertificate = courseCertificate
                                    )
                                    LazyColumn(
                                        modifier = Modifier.fillMaxWidth(),
                                        contentPadding = listPadding
                                    ) {
                                        items(uiState.blocks) { block ->
                                            if (block.type == BlockType.CHAPTER) {
                                                Text(
                                                    modifier = Modifier.padding(
                                                        top = 36.dp,
                                                        bottom = 8.dp
                                                    ),
                                                    text = block.displayName,
                                                    style = MaterialTheme.appTypography.titleMedium,
                                                    color = MaterialTheme.appColors.textPrimaryVariant
                                                )
                                            } else {
                                                CourseSectionCard(
                                                    block = block,
                                                    downloadedState = uiState.downloadedState[block.id],
                                                    onItemClick = { blockSelected ->
                                                        onItemClick(blockSelected)
                                                    },
                                                    onDownloadClick = onDownloadClick
                                                )
                                                Divider()
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        PullRefreshIndicator(
                            refreshing,
                            pullRefreshState,
                            Modifier.align(Alignment.TopCenter)
                        )
                        if (!isInternetConnectionShown && !hasInternetConnection) {
                            OfflineModeDialog(
                                Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.BottomCenter),
                                onDismissCLick = {
                                    isInternetConnectionShown = true
                                },
                                onReloadClick = {
                                    isInternetConnectionShown = true
                                    onReloadClick()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ResumeCourse(
    block: Block,
    onResumeClick: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.appColors.secondaryVariant)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(id = com.raccoongang.course.R.string.course_resume_unit_title),
                style = MaterialTheme.appTypography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.appColors.textPrimary
            )
            Text(
                text = block.displayName,
                style = MaterialTheme.appTypography.bodyLarge,
                color = MaterialTheme.appColors.textPrimary
            )
        }
        NewEdxOutlinedButton(
            modifier = Modifier,
            borderColor = MaterialTheme.appColors.textFieldBorder,
            textColor = MaterialTheme.appColors.textPrimary,
            text = stringResource(id = com.raccoongang.course.R.string.course_resume_unit_btn),
            onClick = {
                onResumeClick(block.id)
            },
            content = {
                Text(text = stringResource(id = com.raccoongang.course.R.string.course_resume_unit_btn).uppercase())
                Icon(
                    imageVector = Icons.Filled.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier
                        .size(20.dp)
                        .offset(x = 4.dp)
                )
            }
        )
    }
}

@Preview(uiMode = UI_MODE_NIGHT_NO)
@Preview(uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun CourseOutlineScreenPreview() {
    NewEdxTheme {
        CourseOutlineScreen(
            windowSize = WindowSize(WindowType.Compact, WindowType.Compact),
            uiState = CourseOutlineUIState.CourseData(
                listOf(
                    mockSequentialBlock, mockSequentialBlock
                ),
                mapOf(),
                mockChapterBlock
            ),
            courseTitle = "",
            courseImage = "",
            courseCertificate = Certificate(""),
            uiMessage = null,
            refreshing = false,
            hasInternetConnection = true,
            onSwipeRefresh = {},
            onItemClick = {},
            onResumeClick = {},
            onBackClick = {},
            onReloadClick = {},
            onDownloadClick = {}
        )
    }
}

@Preview(uiMode = UI_MODE_NIGHT_NO, device = Devices.NEXUS_9)
@Preview(uiMode = UI_MODE_NIGHT_YES, device = Devices.NEXUS_9)
@Composable
private fun CourseOutlineScreenTabletPreview() {
    NewEdxTheme {
        CourseOutlineScreen(
            windowSize = WindowSize(WindowType.Medium, WindowType.Medium),
            uiState = CourseOutlineUIState.CourseData(
                listOf(
                    mockSequentialBlock, mockSequentialBlock
                ),
                mapOf(),
                mockChapterBlock
            ),
            courseTitle = "",
            courseImage = "",
            courseCertificate = Certificate(""),
            uiMessage = null,
            refreshing = false,
            hasInternetConnection = true,
            onSwipeRefresh = {},
            onItemClick = {},
            onResumeClick = {},
            onBackClick = {},
            onReloadClick = {},
            onDownloadClick = {}
        )
    }
}

@Preview(uiMode = UI_MODE_NIGHT_NO)
@Preview(uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun ResumeCoursePreview() {
    NewEdxTheme {
        ResumeCourse(mockChapterBlock) {}
    }
}

private val mockCourse = EnrolledCourse(
    auditAccessExpires = Date(),
    created = "created",
    certificate = Certificate(""),
    mode = "mode",
    isActive = true,
    course = EnrolledCourseData(
        id = "id",
        name = "Course name",
        number = "",
        org = "Org",
        start = Date(),
        startDisplay = "",
        startType = "",
        end = Date(),
        dynamicUpgradeDeadline = "",
        subscriptionId = "",
        coursewareAccess = CoursewareAccess(
            true,
            "",
            "",
            "",
            "",
            ""
        ),
        media = null,
        courseImage = "",
        courseAbout = "",
        courseSharingUtmParameters = CourseSharingUtmParameters("", ""),
        courseUpdates = "",
        courseHandouts = "",
        discussionUrl = "",
        videoOutline = "",
        isSelfPaced = false
    )
)
private val mockChapterBlock = Block(
    id = "id",
    blockId = "blockId",
    lmsWebUrl = "lmsWebUrl",
    legacyWebUrl = "legacyWebUrl",
    studentViewUrl = "studentViewUrl",
    type = BlockType.CHAPTER,
    displayName = "Chapter",
    graded = false,
    studentViewData = null,
    studentViewMultiDevice = false,
    blockCounts = BlockCounts(1),
    descendants = emptyList(),
    completion = 0.0
)
private val mockSequentialBlock = Block(
    id = "id",
    blockId = "blockId",
    lmsWebUrl = "lmsWebUrl",
    legacyWebUrl = "legacyWebUrl",
    studentViewUrl = "studentViewUrl",
    type = BlockType.SEQUENTIAL,
    displayName = "Sequential",
    graded = false,
    studentViewData = null,
    studentViewMultiDevice = false,
    blockCounts = BlockCounts(1),
    descendants = emptyList(),
    completion = 0.0
)