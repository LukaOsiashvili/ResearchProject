package com.example.phoneapp;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.widget.Button;

public class RecommendationEngine {
    private Context context;
    private Button youtubeButton;

    public RecommendationEngine(Context context, Button youtubeButton) {
        this.context = context;
        this.youtubeButton = youtubeButton;
    }

    public String getRecommendation(EmotionalState state) {
        switch (state) {
            case ANXIOUS:
                return getAnxietyRecommendation();
            case STRESSED:
                return getStressRecommendation();
            case CALM:
                return getCalmnessRecommendation();
            case NORMAL:
                return "Everything looks good! Keep going!";
            default:
                return "Gathering more data...";
        }
    }

    private String getAnxietyRecommendation() {
        // Provide calming recommendations
        String recommendation = "Try this calming breathing exercise:\n";
        recommendation += "1. Breathe in for 4 seconds\n";
        recommendation += "2. Hold for 4 seconds\n";
        recommendation += "3. Breathe out for 4 seconds\n\n";
        recommendation += "Would you like to listen to calming music?";

        // Optionally open YouTube with calming music search results
        showYouTubeButton("calming music playlist");

        return recommendation;
    }

    private String getStressRecommendation() {
        // Provide stress-relief recommendations
        String recommendation = "Take a short break:\n";
        recommendation += "1. Stand up and stretch\n";
        recommendation += "2. Drink some water\n";
        recommendation += "Would you like to watch something funny?";

        // Optionally open YouTube with funny videos search results
        showYouTubeButton("funny cats");

        return recommendation;
    }

    private String getCalmnessRecommendation() {
        return "You seem relaxed! This is a good time for focused work or meditation.";
    }

    private void showYouTubeButton(final String searchQuery) {
        youtubeButton.setVisibility(View.VISIBLE);
        youtubeButton.setOnClickListener(v -> openYouTube(searchQuery));
    }

    private void hideYouTubeButton() {
        youtubeButton.setVisibility(View.GONE);
    }
    private void openYouTube(String searchQuery) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse("https://www.youtube.com/results?search_query=" +
                Uri.encode(searchQuery)));
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }
}