# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- The rest timer keeps running in the background, and an active workout is shown in a notification with the remaining rest

### Fixed

- Reset all stats now also clears completion history instead of leaving orphaned records
- Rapidly adding exercises no longer produces duplicate order indices due to a race condition
- Loading a workout to run it now reads the workout and its exercises atomically
- The JSON export now shows an error instead of "Workouts exported" when the file could not be written
- The JSON import/export no longer read or write the file on the main thread
- The JSON import now rejects files with blank names, non-positive sets or amounts, or negative pauses or durations
- The rest timer vibration no longer leaks the activity, e.g. after a screen rotation during an active workout
- An active workout is no longer lost when the app is closed in the background, but continues where it left off
- The rest timer no longer drifts and keeps counting correctly while the app is in the background
- Exercises can no longer be saved with a rest that is too large, which was silently saved as no rest
- A finished workout is now always recorded, even when the screen is left immediately after the last set
- A database can no longer be created twice when it is first accessed from multiple threads at once
- The JSON import/export is no longer cancelled and its result message no longer lost when the screen is rotated

## [1.3.0] - 2026-09-25

### Added

- Workout completion history is now included in JSON export/import
- The text alignment of the month and weekday labels in the activity graph was improved
- Tapping a square in the activity graph highlights it and shows which workouts were completed that day

## [1.2.0] - 2026-09-03

### Added

- A vibration effect was added that is active in the last 3 seconds when resting
- Each workout can be assigned a colour, chosen from a predefined palette
- An activity graph screen shows completed workouts over time, colour-coded per workout
- An about screen was added

## [1.1.0] - 2026-08-05

### Added

- The total runtime across all workouts is displayed on the workout list screen
- Active workouts can be paused and resumed during exercising and resting via a pause button in the top app bar
- Workout data can be exported as JSON and imported from a JSON file via the Import/Export menu in the top app bar
- A "Reset all stats" option in the overflow menu resets completion counts and total time for all workouts

## [1.0.0] - 2026-06-05

### Added

- Workout CRUD, exercise management with reordering, and a rest timer with a coroutine-based state machine were added
- The total set count is displayed on the workout list to give a quick overview of workout volume
- Exercise type support was added to distinguish between reps-based and time-based exercises
- A configurable intensity level was added to each exercise
- The total run-time is displayed during active workouts so the user can track how long they have been exercising
- A rename button was added to allow renaming workouts directly from the list
- The screen is kept on during active workouts to prevent the device from sleeping mid-session
- Compose screenshot testing was added
- An app icon was added to the launcher
