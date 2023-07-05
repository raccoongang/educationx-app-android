package com.raccoongang.core.presentation.dialog

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.fragment.app.DialogFragment
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.raccoongang.core.R
import com.raccoongang.core.domain.model.RegistrationField
import com.raccoongang.core.extension.parcelableArrayList
import com.raccoongang.core.ui.SheetContent
import com.raccoongang.core.ui.theme.NewEdxTheme
import com.raccoongang.core.ui.theme.appColors
import com.raccoongang.core.ui.theme.appShapes
import org.koin.androidx.viewmodel.ext.android.viewModel

class SelectBottomDialogFragment : BottomSheetDialogFragment() {

    private val viewModel by viewModel<SelectDialogViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.values = requireArguments().parcelableArrayList(ARG_LIST_VALUES)!!
        setStyle(DialogFragment.STYLE_NORMAL, R.style.BottomSheetDialog)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = ComposeView(requireContext()).apply {
        if (dialog != null && dialog!!.window != null) {
            dialog!!.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            (dialog as? BottomSheetDialog)?.behavior?.apply {
                state = BottomSheetBehavior.STATE_EXPANDED
            }
        }
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            NewEdxTheme {
                val listState = rememberLazyListState()

                var searchValue by rememberSaveable(stateSaver = TextFieldValue.Saver) {
                    mutableStateOf(TextFieldValue())
                }

                Surface(color = androidx.compose.ui.graphics.Color.Transparent) {
                    Box(
                        modifier = Modifier,
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Box(
                            modifier = Modifier
                                .widthIn(max = 640.dp)
                                .fillMaxWidth()
                                .background(
                                    color = MaterialTheme.appColors.background,
                                    shape = MaterialTheme.appShapes.screenBackgroundShape
                                )
                                .clip(MaterialTheme.appShapes.screenBackgroundShape)
                        ) {
                            SheetContent(
                                searchValue = searchValue,
                                expandedList = viewModel.values,
                                onItemClick = { item ->
                                    viewModel.sendCourseEventChanged(item.value)
                                    dismiss()
                                },
                                listState = listState,
                                searchValueChanged = {
                                    searchValue = TextFieldValue(it)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    companion object {
        private const val ARG_LIST_VALUES = "argListValues"

        fun newInstance(
            values: List<RegistrationField.Option>,
        ): SelectBottomDialogFragment {
            val dialog = SelectBottomDialogFragment()
            dialog.arguments = Bundle().apply {
                putParcelableArrayList(ARG_LIST_VALUES, ArrayList(values))
            }
            return dialog
        }
    }

}