package com.example.modulelaptrinh.utils;

import com.example.modulelaptrinh.models.ChatMessage;
import com.example.modulelaptrinh.models.Note;
import com.example.modulelaptrinh.models.QuizQuestion;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class DataGenerator {

    public static List<Note> generateNotes() {
        List<Note> notes = new ArrayList<>();
        notes.add(new Note("Java - OOP Concepts",
                "Encapsulation, Inheritance, Polymorphism, Abstraction", new Date()));
        notes.add(new Note("Python - List Comprehension",
                "[x**2 for x in range(10)]", new Date()));
        notes.add(new Note("Kotlin - Coroutines",
                "async, await, launch, withContext", new Date()));
        notes.add(new Note("JavaScript - Promises",
                "Promise, async/await, callback", new Date()));
        notes.add(new Note("C++ - Pointers",
                "Pointer arithmetic, smart pointers", new Date()));
        return notes;
    }

    public static List<ChatMessage> generateChatHistory() {
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(new ChatMessage("What is polymorphism in Java?",
                "Polymorphism is the ability to take many forms. In Java, it's achieved through method overloading and overriding.",
                new Date(), "Java"));
        messages.add(new ChatMessage("Explain list comprehension in Python",
                "List comprehension is a concise way to create lists. Syntax: [expression for item in iterable if condition]",
                new Date(), "Python"));
        messages.add(new ChatMessage("What are Kotlin coroutines?",
                "Coroutines are lightweight threads for asynchronous programming in Kotlin.",
                new Date(), "Kotlin"));
        return messages;
    }

    public static List<QuizQuestion> generateQuizQuestions() {
        List<QuizQuestion> questions = new ArrayList<>();

        // FIX: Sửa constructor với Type.MULTIPLE_CHOICE
        questions.add(new QuizQuestion(
                QuizQuestion.Type.MULTIPLE_CHOICE,
                "What is the output of 'print(2**3)' in Python?",
                Arrays.asList("A. 6", "B. 8", "C. 9", "D. 5"),
                "B",
                "2**3 means 2 to the power of 3, which equals 8"
        ));

        questions.add(new QuizQuestion(
                QuizQuestion.Type.MULTIPLE_CHOICE,
                "Which keyword is used to create a class in Java?",
                Arrays.asList("A. class", "B. interface", "C. object", "D. struct"),
                "A",
                "The 'class' keyword is used to create a class in Java"
        ));

        questions.add(new QuizQuestion(
                QuizQuestion.Type.MULTIPLE_CHOICE,
                "What is the correct way to declare a variable in Kotlin that cannot be reassigned?",
                Arrays.asList("A. var", "B. val", "C. const", "D. let"),
                "B",
                "'val' is used for read-only variables in Kotlin"
        ));

        return questions;
    }
}