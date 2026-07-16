package com.russert.woodshed.ui.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.russert.woodshed.ui.theme.Amber
import com.russert.woodshed.ui.theme.Cream
import com.russert.woodshed.ui.theme.DarkBrown
import com.russert.woodshed.ui.theme.Theme
import kotlinx.coroutines.launch

private data class OnboardingPage(
    val icon: ImageVector,
    val title: String,
    val description: String,
)

private val pages = listOf(
    OnboardingPage(
        icon = Icons.Default.VideoLibrary,
        title = "Don't Lose the Good Stuff",
        description = "That tune from the workshop. That lick somebody showed you in the parking lot. That version you were sure you'd remember.",
    ),
    OnboardingPage(
        icon = Icons.Default.FiberManualRecord,
        title = "Capture It While It's Fresh",
        description = "When somebody shows you a tune, hit record before it disappears into festival brain.",
    ),
    OnboardingPage(
        icon = Icons.Default.Assignment,
        title = "Keep the Story With It",
        description = "Add the tune name, who played it, who they learned it from, instrument, and any notes while it's still fresh.",
    ),
    OnboardingPage(
        icon = Icons.Default.Search,
        title = "Find It Without Digging",
        description = "No more scrolling through mystery videos trying to remember which Cluck Old Hen this was.",
    ),
    OnboardingPage(
        icon = Icons.Default.Repeat,
        title = "Go Woodshed",
        description = "Slow it down. Loop the hard part. Mark the spot where they show the turnaround. Work it until it sticks.",
    ),
)

@Composable
fun OnboardingScreen(onComplete: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    val isLastPage = pagerState.currentPage == pages.lastIndex

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBrown),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
            ) { index ->
                PageContent(page = pages[index])
            }

            // Dots indicator
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                pages.indices.forEach { index ->
                    val isSelected = index == pagerState.currentPage
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(if (isSelected) 8.dp else 6.dp)
                            .background(
                                color = if (isSelected) Amber else Cream.copy(alpha = 0.3f),
                                shape = CircleShape,
                            ),
                    )
                }
            }

            // Buttons
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Theme.Padding)
                    .padding(bottom = 48.dp, top = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Button(
                    onClick = {
                        if (isLastPage) {
                            onComplete()
                        } else {
                            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(Theme.CornerRadius),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Amber,
                        contentColor = DarkBrown,
                    ),
                ) {
                    Text(
                        text = if (isLastPage) "Get Started" else "Next",
                        fontFamily = FontFamily.Serif,
                        fontSize = 17.sp,
                        modifier = Modifier.padding(vertical = 6.dp),
                    )
                }

                AnimatedVisibility(
                    visible = !isLastPage,
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    TextButton(onClick = onComplete) {
                        Text(
                            "Skip",
                            fontFamily = FontFamily.Serif,
                            fontSize = 14.sp,
                            color = Cream.copy(alpha = 0.4f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PageContent(page: OnboardingPage) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Spacer(Modifier.weight(1f))

        Surface(
            shape = CircleShape,
            color = Amber.copy(alpha = 0.12f),
            modifier = Modifier.size(120.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = page.icon,
                    contentDescription = null,
                    tint = Amber,
                    modifier = Modifier.size(52.dp),
                )
            }
        }

        Spacer(Modifier.height(28.dp))

        Text(
            text = page.title,
            fontFamily = FontFamily.Serif,
            fontSize = 26.sp,
            color = Cream,
            textAlign = TextAlign.Center,
            lineHeight = 32.sp,
        )

        Spacer(Modifier.height(14.dp))

        Text(
            text = page.description,
            fontFamily = FontFamily.Serif,
            fontSize = 16.sp,
            color = Cream.copy(alpha = 0.65f),
            textAlign = TextAlign.Center,
            lineHeight = 24.sp,
        )

        Spacer(Modifier.weight(2f))
    }
}
