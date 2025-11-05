# Digital Wellbeing Android App

A modern Android app for tracking screen time and promoting digital wellness, built with Jetpack Compose and matching the design from `digital-wellbeing-app.html`.

## Features

### Core Functionality
- **Screen Time Tracking**: Monitor daily app usage using Android's UsageStatsManager
- **Dashboard**: View today's screen time with circular progress indicator
- **Most Used Apps**: See your top 3 most-used apps with usage time
- **App Stats**: Detailed breakdown of all app usage with progress towards daily goal
- **Challenge System**: Complete puzzles (Chess, Sudoku, Math) when limits are reached
- **Task Management**: Create and track personal productivity tasks
- **Focus Mode**: Redirect users to productive activities when app limits are reached

### Screens
1. **Dashboard** - Overview of screen time, most used apps, and challenges completed
2. **Stats** - Detailed app usage breakdown
3. **Focus** - Limit reached screen with alternative activities
4. **Tasks** - Personal task list with completion tracking
5. **Chess Puzzle** - Interactive chess challenge
6. **Sudoku** - Sudoku puzzle (placeholder UI)
7. **Math Challenge** - Math problems with number pad

## Architecture

### Tech Stack
- **UI**: Jetpack Compose with Material Design 3
- **Architecture**: MVVM (Model-View-ViewModel)
- **Database**: Room for local data persistence
- **Async**: Kotlin Coroutines & Flow
- **Navigation**: Jetpack Navigation Compose
- **Minimum SDK**: 26 (Android 8.0)
- **Target SDK**: 34 (Android 14)

### Project Structure
```
app/src/main/java/com/example/digitalwellbeing/
├── data/
│   ├── local/          # Room database, DAOs, converters
│   ├── model/          # Data models (entities)
│   └── repository/     # Repository classes
├── ui/
│   ├── components/     # Reusable UI components
│   ├── navigation/     # Navigation setup
│   ├── screens/        # Screen composables
│   │   ├── dashboard/
│   │   ├── stats/
│   │   ├── focus/
│   │   ├── tasks/
│   │   └── challenges/ # Chess, Sudoku, Math
│   ├── theme/          # App theme, colors, typography
│   ├── MainViewModel.kt
│   ├── ViewModelFactory.kt
│   └── DigitalWellbeingMain.kt
├── util/               # Utility classes
├── MainActivity.kt
└── DigitalWellbeingApp.kt
```

### Database Schema
- **app_usage**: App usage data (package name, usage time, date)
- **daily_stats**: Daily aggregated statistics
- **tasks**: User tasks with completion status
- **app_limits**: App time limits
- **challenges**: Completed challenges

## Design

The app follows a minimalist black & white design inspired by the HTML mockup:
- **Colors**: Black (#000000), White (#FFFFFF), Light backgrounds (#FAFAFA)
- **Typography**: System fonts with negative letter spacing
- **Components**: Rounded corners (20dp cards, 12dp buttons)
- **Border**: Subtle 1dp borders with #F0F0F0 color

## Setup & Installation

### Prerequisites
- Android Studio Hedgehog or newer
- JDK 17
- Android SDK 34

### Building the Project
1. Clone/open the project in Android Studio
2. Sync Gradle files
3. Run on an emulator or physical device (API 26+)

### Required Permission
The app requires **Usage Access** permission to track app usage:
1. Go to Settings → Apps → Special app access → Usage access
2. Find "Focus" and enable permission

## Usage

### First Launch
1. Grant Usage Access permission when prompted
2. The app will sync usage data from the current day
3. Explore the dashboard to see your screen time

### Adding Tasks
1. Navigate to the Tasks tab
2. Tap "Add New Task"
3. Enter task title and save
4. Tap tasks to mark them complete

### Completing Challenges
1. When an app limit is reached, the Focus screen appears
2. Choose a challenge (Chess, Sudoku, or Math)
3. Complete the challenge to earn points
4. Track completed challenges on the dashboard

## Future Enhancements

Potential features to add:
- Actual chess engine integration
- Working Sudoku generator and solver
- Math problem generator with difficulty levels
- App blocking enforcement
- Weekly/monthly statistics
- Customizable app limits
- Notifications for app usage
- Dark mode support
- Export usage data
- Widget for home screen

## Notes

- The app currently displays placeholder data for some features
- Challenge screens have basic UI but need game logic implementation
- Usage stats sync on app resume
- Data is stored locally in Room database

## License

This is a demonstration/educational project.
