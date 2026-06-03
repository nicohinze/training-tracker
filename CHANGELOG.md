# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0] - 2026-06-04

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
