# Zwift FIT File Author Changer

This tool modifies FIT files to change the device information to show they came from a Garmin Edge 840.

## Usage

### Build the project:
```bash
mvn package
```

### Run the tool:
```bash
# Using Maven exec plugin
mvn exec:java -Dexec.mainClass="org.example.MainKt" -Dexec.args="input.fit"

# Or using the jar file directly
java -cp target/classes:target/dependency/* org.example.MainKt input.fit
```

### Input and Output:
- **Input**: Any FIT file (e.g., `activity.fit`, `workout.fit`)
- **Output**: Modified FIT file with `_edge840` suffix (e.g., `activity_edge840.fit`)

The tool will:
1. Load the input FIT file
2. Modify device information to set manufacturer as Garmin and product as Edge 840
3. Save the modified file with a new name (original file is preserved)

## Example:
```bash
mvn exec:java -Dexec.mainClass="org.example.MainKt" -Dexec.args="my_zwift_ride.fit"
```

This will create `my_zwift_ride_edge840.fit` with the modified device information.
