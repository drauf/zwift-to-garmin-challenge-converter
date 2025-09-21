> [!WARNING]
> This repository contains a lot of LLM generated code and documentation. :)

# 🚴 Zwift to Garmin challenge converter

**Make your Zwift rides count for Garmin badges and challenges!**

## The problem

Zwift activities are tagged as coming from "Zwift" rather than a Garmin device, so they **don't count** towards Garmin Connect badges, challenges, or monthly goals.

## The solution

This tool modifies Zwift FIT files to appear as if they came from a **Garmin Edge 840**:
- ✅ Activities count for Garmin badges and challenges
- ✅ All ride data preserved (power, heart rate, cadence, etc.)
- ✅ Creates new files with `_edge840` suffix (originals untouched)

## Usage

### Convert files:
```bash
# Single file
mvn exec:java -Dexec.args="'My Zwift Ride.fit'"

# Entire directory
mvn exec:java -Dexec.args="/path/to/zwift/activities"

# Verbose output
mvn exec:java -Dexec.args="-v 'My Zwift Ride.fit'"
```

**Manually upload the `*_edge840.fit` files to Garmin Connect.**

## Requirements

- Java 21 or higher
- Maven
