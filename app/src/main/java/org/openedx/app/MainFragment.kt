package org.openedx.app

import android.os.Bundle
import android.view.View
import androidx.viewpager2.widget.ViewPager2
import androidx.fragment.app.Fragment
import org.openedx.core.presentation.global.viewBinding
import org.openedx.dashboard.presentation.DashboardFragment
import org.openedx.discovery.presentation.DiscoveryFragment
import org.openedx.app.adapter.MainNavigationFragmentAdapter
import org.openedx.profile.presentation.profile.ProfileFragment
import org.koin.android.ext.android.inject
import org.openedx.app.databinding.FragmentMainBinding

class MainFragment : Fragment(R.layout.fragment_main) {

    private val binding by viewBinding(FragmentMainBinding::bind)
    private val analytics by inject<AppAnalytics>()

    private lateinit var adapter: MainNavigationFragmentAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViewPager()

        binding.bottomNavView.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.fragmentHome -> {
                    analytics.discoveryTabClickedEvent()
                    binding.viewPager.setCurrentItem(0, false)
                }
                R.id.fragmentDashboard -> {
                    analytics.dashboardTabClickedEvent()
                    binding.viewPager.setCurrentItem(1, false)
                }
                R.id.fragmentPrograms -> {
                    analytics.programsTabClickedEvent()
                    binding.viewPager.setCurrentItem(2, false)
                }
                R.id.fragmentProfile -> {
                    analytics.profileTabClickedEvent()
                    binding.viewPager.setCurrentItem(3, false)
                }
            }
            true
        }
    }

    private fun initViewPager() {
        binding.viewPager.orientation = ViewPager2.ORIENTATION_HORIZONTAL
        binding.viewPager.offscreenPageLimit = 4
        adapter = MainNavigationFragmentAdapter(this).apply {
            addFragment(DiscoveryFragment())
            addFragment(DashboardFragment())
            addFragment(InDevelopmentFragment())
            addFragment(ProfileFragment())
        }
        binding.viewPager.adapter = adapter
        binding.viewPager.isUserInputEnabled = false
    }
}