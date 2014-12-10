package com.poofycow.utils.logger;

public enum LogLevel {
    FATAL("\u001B[30m\u001B[41m"),
    SEVERE("\u001B[31m"),
    WARNING("\u001B[33m"),
    INFO("\u001B[36m"),
    DEBUG("\u001B[32m");
    
    private String consoleColor;
    
    private LogLevel(String consoleColor) {
        this.consoleColor = consoleColor;
    }
    
    String getConsoleColor() {
        return this.consoleColor;
    }
}
