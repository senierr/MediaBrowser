package com.senierr.media.domain.home

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.splashscreen.SplashScreen
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.senierr.base.support.ui.BaseActivity
import com.senierr.media.R
import com.senierr.media.databinding.ActivityMainBinding

/**
 * 首页
 *
 * @author senierr_zhou
 * @date 2023/07/28
 */
class MainActivity : BaseActivity<ActivityMainBinding>() {

    override fun createViewBinding(layoutInflater: LayoutInflater): ActivityMainBinding {
        return ActivityMainBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        checkPermission({
            initView()
        }, {
            // 不同意权限，退出应用
            finish()
        })
    }

    /**
     * 权限检查
     */
    private fun checkPermission(onGrant: () -> Unit, onFailure: (() -> Unit)? = null) {
        val permissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            if (permissions.values.none { !it }) {
                onGrant.invoke()
            } else {
                onFailure?.invoke()
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_AUDIO,
                Manifest.permission.READ_MEDIA_VIDEO
            ))
        } else {
            permissionLauncher.launch(arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE
            ))
        }
    }

    private fun initView() {
        binding.vpMain.offscreenPageLimit = 2
        binding.vpMain.isUserInputEnabled = false
        binding.vpMain.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                when (position) {
                    0 -> binding.bnvBottom.selectedItemId = R.id.tab_image
                    1 -> binding.bnvBottom.selectedItemId = R.id.tab_audio
                    2 -> binding.bnvBottom.selectedItemId = R.id.tab_video
                    else -> binding.bnvBottom.selectedItemId = R.id.tab_image
                }
            }
        })
        binding.vpMain.adapter = object :FragmentStateAdapter(this) {
            override fun getItemCount(): Int = 3

            override fun createFragment(position: Int): Fragment = when (position) {
                0 -> ImageHomeFragment()
                1 -> AudioHomeFragment()
                2 -> VideoHomeFragment()
                else -> ImageHomeFragment()
            }
        }

        binding.bnvBottom.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.tab_image -> binding.vpMain.setCurrentItem(0, false)
                R.id.tab_audio -> binding.vpMain.setCurrentItem(1, false)
                R.id.tab_video -> binding.vpMain.setCurrentItem(2, false)
            }
            return@setOnItemSelectedListener true
        }
    }
}