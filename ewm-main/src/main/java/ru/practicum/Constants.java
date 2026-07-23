package ru.practicum;

import java.time.format.DateTimeFormatter;

public class Constants {

    // Формат даты
    public static final String DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    public static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern(DATE_TIME_FORMAT);

    // Имя приложения для статистики
    public static final String APP_NAME = "ewm-main-service";

    // Сортировка
    public static final String SORT_EVENT_DATE = "EVENT_DATE";

    // Действия для обновления события (это НЕ статусы, это действия из запросов)
    public static final String STATE_ACTION_SEND_TO_REVIEW = "SEND_TO_REVIEW";
    public static final String STATE_ACTION_CANCEL_REVIEW = "CANCEL_REVIEW";
    public static final String STATE_ACTION_PUBLISH = "PUBLISH_EVENT";
    public static final String STATE_ACTION_REJECT = "REJECT_EVENT";

    // Ограничения полей
    public static final int MIN_ANNOTATION_LENGTH = 20;
    public static final int MAX_ANNOTATION_LENGTH = 2000;
    public static final int MIN_DESCRIPTION_LENGTH = 20;
    public static final int MAX_DESCRIPTION_LENGTH = 7000;
    public static final int MIN_TITLE_LENGTH = 3;
    public static final int MAX_TITLE_LENGTH = 120;
    public static final int MAX_CATEGORY_NAME_LENGTH = 255;
    public static final int MAX_USER_NAME_LENGTH = 255;
    public static final int MAX_EMAIL_LENGTH = 512;

    // Минимальное время до события
    public static final int MIN_HOURS_BEFORE_EVENT = 2;

    private Constants() {
    }
}