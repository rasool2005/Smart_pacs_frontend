package com.simats.smartpcas

import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.activity.enableEdgeToEdge
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.viewpager2.widget.ViewPager2

class MainActivity : BaseActivity() {

    private lateinit var onboardingAdapter: OnboardingAdapter
    private lateinit var indicatorContainer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupOnboarding()
        setupIndicators()
        setCurrentIndicator(0)

        findViewById<Button>(R.id.btnSignUp).setOnClickListener {
            markOnboardingSeen()
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        findViewById<Button>(R.id.btnLogin).setOnClickListener {
            markOnboardingSeen()
            startActivity(Intent(this, HospitalSelectionActivity::class.java))
        }
    }

    private fun markOnboardingSeen() {
        SessionManager(this).setHasSeenOnboarding(true)
    }

    private fun setupOnboarding() {
        onboardingAdapter = OnboardingAdapter(
            listOf(
                OnboardingItem(
                    "Smart PACS AI",
                    "AI-powered medical imaging platform for faster and more accurate diagnosis",
                    R.drawable.ic_brain
                ),
                OnboardingItem(
                    "Instant Analysis",
                    "Deep learning algorithms to detect anomalies in X-rays, CT scans, and MRI",
                    R.drawable.ic_ai_chat
                ),
                OnboardingItem(
                    "Welcome Back",
                    "Login to continue your diagnosis",
                    R.drawable.ic_brain
                )
            )
        )
        val viewPager = findViewById<ViewPager2>(R.id.viewPager)
        viewPager.adapter = onboardingAdapter
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                setCurrentIndicator(position)
            }
        })
    }

    private fun setupIndicators() {
        indicatorContainer = findViewById(R.id.indicatorContainer)
        val indicators = arrayOfNulls<ImageView>(onboardingAdapter.itemCount)
        val layoutParams: LinearLayout.LayoutParams =
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        layoutParams.setMargins(8, 0, 8, 0)
        for (i in indicators.indices) {
            indicators[i] = ImageView(applicationContext)
            indicators[i]?.apply {
                setImageDrawable(
                    ContextCompat.getDrawable(
                        applicationContext,
                        R.drawable.bg_notification_dot
                    )
                )
                this.layoutParams = layoutParams
            }
            indicatorContainer.addView(indicators[i])
        }
    }

    private fun setCurrentIndicator(index: Int) {
        val childCount = indicatorContainer.childCount
        for (i in 0 until childCount) {
            val imageView = indicatorContainer.getChildAt(i) as ImageView
            if (i == index) {
                imageView.alpha = 1f
                imageView.scaleX = 1.2f
                imageView.scaleY = 1.2f
            } else {
                imageView.alpha = 0.4f
                imageView.scaleX = 1f
                imageView.scaleY = 1f
            }
        }
    }
}
