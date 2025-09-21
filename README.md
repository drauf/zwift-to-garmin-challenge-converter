# 🚴 Zwift to Garmin Challenge Converter

**Make your Zwift rides count for Garmin badges and challenges!**

## 🎯 The Problem

When you complete virtual rides on Zwift, the activities are tagged as coming from "Zwift" rather than a Garmin device. This means they **don't count** towards:
- Garmin Connect badges and challenges
- Monthly distance/time goals
- Garmin device-specific achievements

Even if you use a Garmin watch or bike computer during your Zwift session, the uploaded FIT file is marked as a Zwift activity.

## 💡 The Solution

This tool modifies Zwift FIT files to make them appear as if they came from a **Garmin Edge 840**. After modification:
- ✅ Activities count for Garmin badges and challenges
- ✅ All your ride data is preserved (power, heart rate, cadence, etc.)
- ✅ Zwift's custom data fields are maintained
- ✅ Original files are never modified (creates new files with `_edge840` suffix)

## 🚀 Usage

### Prerequisites
- Java 21 or higher
- Maven (for building from source)

### Build the project:
```bash
mvn clean package
```

### Convert a Zwift FIT file:
```bash
java -jar target/zwift-to-garmin-converter.jar "My Zwift Ride.fit"
```

**Output:** Creates `My Zwift Ride_edge840.fit` that you can upload to Garmin Connect.
