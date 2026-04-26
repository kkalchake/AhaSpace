package com.kkalchake.enlightenment.service;

public interface AiProvider {
    String chat(String message);
    String getModelName();
}
