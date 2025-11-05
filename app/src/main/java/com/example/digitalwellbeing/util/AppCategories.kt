package com.example.digitalwellbeing.util

import com.example.digitalwellbeing.data.model.AppUsageInfo

enum class AppCategory(val displayName: String, val emoji: String) {
    SOCIAL_MEDIA("Social Media", "💬"),
    ENTERTAINMENT("Entertainment", "🎬"),
    GAMES("Games", "🎮"),
    PRODUCTIVITY("Productivity", "📊"),
    COMMUNICATION("Communication", "📞"),
    SHOPPING("Shopping", "🛒"),
    NEWS("News & Reading", "📰"),
    UTILITIES("Utilities", "🔧"),
    OTHER("Other", "📱")
}

object AppCategorizer {

    private val categoryMappings = mapOf(
        // Social Media
        AppCategory.SOCIAL_MEDIA to listOf(
            "facebook", "instagram", "twitter", "tiktok", "snapchat", "linkedin",
            "reddit", "pinterest", "tumblr", "whatsapp", "telegram", "discord",
            "messenger", "wechat", "signal", "viber", "line", "kakao"
        ),

        // Entertainment
        AppCategory.ENTERTAINMENT to listOf(
            "youtube", "netflix", "prime video", "hulu", "disney", "hbo",
            "spotify", "apple music", "soundcloud", "pandora", "deezer", "tidal",
            "twitch", "vimeo", "dailymotion"
        ),

        // Games
        AppCategory.GAMES to listOf(
            "game", "play", "clash", "candy", "pokemon", "minecraft", "pubg",
            "roblox", "fortnite", "among", "genshin", "league"
        ),

        // Productivity
        AppCategory.PRODUCTIVITY to listOf(
            "office", "word", "excel", "powerpoint", "docs", "sheets", "slides",
            "notion", "evernote", "onenote", "trello", "asana", "monday",
            "todoist", "calendar", "drive", "dropbox", "onedrive", "box"
        ),

        // Communication
        AppCategory.COMMUNICATION to listOf(
            "gmail", "outlook", "mail", "email", "zoom", "teams", "meet",
            "skype", "slack", "webex", "gotomeeting"
        ),

        // Shopping
        AppCategory.SHOPPING to listOf(
            "amazon", "ebay", "walmart", "target", "aliexpress", "etsy",
            "shopify", "wish", "mercari", "poshmark", "shop"
        ),

        // News & Reading
        AppCategory.NEWS to listOf(
            "news", "medium", "flipboard", "feedly", "pocket", "instapaper",
            "kindle", "wattpad", "goodreads", "audible", "cnn", "bbc", "nyt"
        ),

        // Utilities
        AppCategory.UTILITIES to listOf(
            "settings", "calculator", "camera", "gallery", "photos", "files",
            "clock", "weather", "maps", "navigation", "translate", "flashlight"
        )
    )

    /**
     * Categorize an app based on its package name and app name
     */
    fun categorizeApp(packageName: String, appName: String): AppCategory {
        val searchText = "$packageName $appName".lowercase()

        for ((category, keywords) in categoryMappings) {
            for (keyword in keywords) {
                if (searchText.contains(keyword)) {
                    return category
                }
            }
        }

        return AppCategory.OTHER
    }

    /**
     * Group apps by category
     */
    fun groupAppsByCategory(apps: List<AppUsageInfo>): Map<AppCategory, List<AppUsageInfo>> {
        return apps.groupBy { app ->
            categorizeApp(app.packageName, app.appName)
        }.toSortedMap(compareBy { it.ordinal })
    }

    /**
     * Get total usage time for a category
     */
    fun getCategoryUsageTime(apps: List<AppUsageInfo>, category: AppCategory): Long {
        return apps
            .filter { categorizeApp(it.packageName, it.appName) == category }
            .sumOf { it.usageTimeMillis }
    }
}
