# Programming module — Java 11 compatibility

Updated on 1 August 2026.

## Changes

- Changed `programming/build.gradle` source and target compatibility from Java 17 to Java 11.
- Replaced Java text blocks (`"""`) in `ProgrammingChatActivity` and `QuizGenerator` with `String.join("\n", ...)`, preserving the prompt text and trailing newline.
- Kept all Gemini keys, models, dependencies, resources, and other modules unchanged.

## Important

The Programming source now targets Java 11. Android Studio/Gradle itself may still require a newer JDK to run the Android Gradle Plugin; that runtime requirement is separate from the Java language level used by this module.
