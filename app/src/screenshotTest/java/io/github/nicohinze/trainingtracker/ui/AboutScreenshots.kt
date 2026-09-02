package io.github.nicohinze.trainingtracker.ui

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.android.tools.screenshot.PreviewTest
import io.github.nicohinze.trainingtracker.ui.theme.TrainingTrackerTheme

@PreviewTest
@PreviewLightDark
@Composable
private fun AboutScreenScreenshot() {
    TrainingTrackerTheme(dynamicColor = false) {
        Surface {
            AboutScreen(
                onBack = {},
            )
        }
    }
}
