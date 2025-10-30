<!-- f34f7b08-df00-4bed-ab9c-25a847853b42 6dbfd3de-2e50-4edd-b6b8-b80388637dcd -->
# Simple Screen Time Tracker - From Scratch

## What We're Building

A minimal Flutter app that shows:

1. **Total screen time today**
2. **List of apps used today** with time spent on each
3. That's it - nothing else

## Technical Approach

### Use UsageStatsManager with queryUsageStats (Not queryEvents)

**Why this approach**:

- `queryUsageStats()` returns pre-aggregated data from Android's own tracking
- Android already calculates accurate screen time - we just read it
- More reliable than manually processing events
- Used by Digital Wellbeing internally
- Less prone to implementation bugs

**How it works**:

```kotlin
// Get today's start time (midnight)
val calendar = Calendar.getInstance()
calendar.set(Calendar.HOUR_OF_DAY, 0)
calendar.set(Calendar.MINUTE, 0)
val startTime = calendar.timeInMillis

// Query usage stats for today
val usageStatsList = usageStatsManager.queryUsageStats(
    UsageStatsManager.INTERVAL_DAILY, 
    startTime, 
    System.currentTimeMillis()
)

// Each UsageStats object has:
// - packageName: the app
// - totalTimeInForeground: accurate time in foreground (milliseconds)
```

## Implementation Plan

### 1. Android Native Code (Kotlin)

**File**: `android/app/src/main/kotlin/.../UsageStatsHandler.kt`

Simple handler that:

- Checks for PACKAGE_USAGE_STATS permission
- Opens settings to grant permission
- Queries today's usage stats using `queryUsageStats()`
- Returns list with: packageName, appName, timeInForeground

### 2. Flutter Side

**Minimal structure**:

```
lib/
  main.dart                    # App entry point
  permission_screen.dart       # Request permission if not granted
  dashboard_screen.dart        # Show today's stats
  usage_stats_service.dart     # Platform channel to Kotlin
  models/app_usage.dart        # Simple data model
```

### 3. UI Design

**Permission Screen**:

- Explanation why permission is needed
- Button to open settings
- Auto-check when returning to app

**Dashboard Screen**:

- Big card showing total time today
- List of apps sorted by usage time
- Pull to refresh
- That's all

## Complete File Structure

```
usage_guardian/
├── android/
│   └── app/src/main/kotlin/.../
│       ├── MainActivity.kt           # Simple FlutterActivity
│       └── UsageStatsHandler.kt      # New: handles all native logic
├── lib/
│   ├── main.dart                     # New: simple app setup
│   ├── permission_screen.dart        # New: permission UI
│   ├── dashboard_screen.dart         # New: main screen
│   ├── usage_stats_service.dart      # New: platform channel
│   └── models/
│       └── app_usage.dart            # New: data model
└── pubspec.yaml                      # Minimal dependencies
```

## Dependencies

**Minimal - only what's needed**:

```yaml
dependencies:
  flutter:
    sdk: flutter
  cupertino_icons: ^1.0.8
```

No state management libraries, no databases, no charts - pure simplicity.

## Key Implementation Details

### UsageStatsHandler.kt (Complete Logic)

```kotlin
class UsageStatsHandler(private val context: Context) {
    
    fun hasPermission(): Boolean {
        // Check if PACKAGE_USAGE_STATS is granted
    }
    
    fun openSettings() {
        // Open Settings.ACTION_USAGE_ACCESS_SETTINGS
    }
    
    fun getTodayUsage(): List<Map<String, Any>> {
        val usageStatsManager = context.getSystemService(...)
        
        // Today's midnight
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startTime = calendar.timeInMillis
        val endTime = System.currentTimeMillis()
        
        // Query aggregated stats
        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startTime,
            endTime
        )
        
        // Filter and return apps with usage > 0
        return stats
            .filter { it.totalTimeInForeground > 0 }
            .map { 
                mapOf(
                    "packageName" to it.packageName,
                    "appName" to getAppName(it.packageName),
                    "timeInForeground" to it.totalTimeInForeground
                )
            }
            .sortedByDescending { it["timeInForeground"] as Long }
    }
}
```

### dashboard_screen.dart (UI)

Simple stateless widget:

- FutureBuilder to load data
- Total time at top
- ListView of apps
- RefreshIndicator to reload

## Why This Approach is Accurate

1. **Uses Android's own calculations**: We're reading what Android already computed
2. **No manual event processing**: Fewer chances for bugs
3. **Proven method**: This is what many tracking apps use
4. **Simple = Less bugs**: Minimal code means fewer places for errors

## What We're NOT Building

- ❌ No usage history (past days)
- ❌ No charts/graphs
- ❌ No usage limits
- ❌ No notifications
- ❌ No database
- ❌ No complex state management
- ❌ No analytics

Just today's screen time. Period.

## Testing

Compare with Digital Wellbeing for the same day:

1. Use phone normally
2. Check both apps at end of day
3. Should match within 1-5% margin

### To-dos

- [ ] Create UsageStatsHandler.kt with queryUsageStats implementation
- [ ] Create usage_stats_service.dart with MethodChannel to communicate with Kotlin
- [ ] Create app_usage.dart model class
- [ ] Build permission_screen.dart to handle permission request
- [ ] Build dashboard_screen.dart with total time and app list
- [ ] Create main.dart with minimal app setup and routing