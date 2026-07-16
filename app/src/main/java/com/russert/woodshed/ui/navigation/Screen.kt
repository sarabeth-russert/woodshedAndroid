package com.russert.woodshed.ui.navigation

sealed class Screen(val route: String) {
    data object Library  : Screen("library")
    data object Record   : Screen("record")
    data object Settings : Screen("settings")
    data object Player   : Screen("player/{recordingId}") {
        fun createRoute(recordingId: String) = "player/$recordingId"
    }
    data object MetadataForm : Screen("metadata_form?videoPath={videoPath}&duration={duration}") {
        fun createRoute(videoPath: String, duration: Double) =
            "metadata_form?videoPath=$videoPath&duration=$duration"
    }
    data object EditRecording : Screen("edit_recording/{recordingId}") {
        fun createRoute(recordingId: String) = "edit_recording/$recordingId"
    }
    data object Onboarding : Screen("onboarding")
}
