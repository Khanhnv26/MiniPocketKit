package com.pocketkit.backend.service;



public interface OcrEngine {
    String extractText(byte[] imageBytes, String contentType, String originalFileName);
}
