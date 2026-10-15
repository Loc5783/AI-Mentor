# Science Module

The module is written in Java with Android XML resources.

## Simple Gemini API

All Gemini HTTP logic is in one file:

`src/main/java/com/brightpath/science/data/remote/GeminiApiClient.java`

Only two lines normally need editing:

```java
public static final String API_KEY = "YOUR_KEY";
public static final String MODEL = "gemini-3.5-flash";
```

Chat, adaptive quiz and study planner all reuse this client.
