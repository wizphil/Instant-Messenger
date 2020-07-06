package com.wizphil.instantmessenger.persistence.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@AllArgsConstructor
@Builder
@Data
public class UserSettings {
    private boolean compactView;
    private boolean soundDisabled;
    private boolean minimizeToTray;
    private boolean keepOpenWindows;
    private int fontSize;

    public static UserSettings defaultSettings() {
        return UserSettings.builder()
                .keepOpenWindows(true)
                .fontSize(12)
                .build();
    }
}
