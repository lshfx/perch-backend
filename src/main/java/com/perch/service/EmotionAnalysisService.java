package com.perch.service;

/**
 * @Author: lsh
 * @Date: 2026/02/26/20:51
 * @Description:
 */
public interface EmotionAnalysisService {

    void analyzeAndSaveEmotionAsync(Long messageId, String userContent);
}
