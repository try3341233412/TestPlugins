// Use an integer for version numbers
version = 3

cloudstream {
    description = "YouTube search and playback"
    authors = listOf("try3341233412")

    // 0: Down, 1: Ok, 2: Slow, 3: Beta-only
    status = 1

    tvTypes = listOf("Movie")

    requiresResources = false
    language = "en"
}

android {
    buildFeatures {
        buildConfig = true
    }
}
