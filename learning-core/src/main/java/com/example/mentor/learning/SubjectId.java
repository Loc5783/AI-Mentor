package com.example.mentor.learning;

/** Stable curriculum identifiers, independent of translated labels and legacy database names. */
public enum SubjectId {
  MATH("math", "Toán", "Mathematics"),
  LITERATURE("literature", "Ngữ văn", "Literature"),
  ENGLISH("english", "Tiếng Anh", "English"),
  HISTORY("history", "Lịch sử", "History"),
  GEOGRAPHY("geography", "Địa lý", "Geography"),
  CHEMISTRY("chemistry", "Hóa học", "Chemistry"),
  PHYSICS("physics", "Vật lý", "Physics"),
  BIOLOGY("biology", "Sinh học", "Biology"),
  ECONOMICS_LAW("economics_law", "Giáo dục kinh tế và pháp luật", "Economics and Law");

  public final String wireId;
  private final String vietnamese, english;

  SubjectId(String wireId, String vietnamese, String english) {
    this.wireId = wireId;
    this.vietnamese = vietnamese;
    this.english = english;
  }

  public String label(String language) {
    return "en".equals(language) ? english : vietnamese;
  }

  public static SubjectId fromWireId(String value) {
    for (SubjectId id : values()) if (id.wireId.equals(value)) return id;
    throw new IllegalArgumentException("Unknown subject identifier");
  }
}
