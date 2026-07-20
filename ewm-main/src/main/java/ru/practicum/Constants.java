package ru.practicum;

import java.time.format.DateTimeFormatter;

public class Constants {
    // Формат даты для API
    public static final String DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";

    // Форматтер даты
    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern(DATE_TIME_FORMAT);

    // Статусы событий
    public static final String EVENT_STATE_PENDING = "PENDING";
    public static final String EVENT_STATE_PUBLISHED = "PUBLISHED";
    public static final String EVENT_STATE_CANCELED = "CANCELED";
    public static final String EVENT_STATE_REJECTED = "REJECTED";

    // Статусы заявок
    public static final String REQUEST_STATUS_PENDING = "PENDING";
    public static final String REQUEST_STATUS_CONFIRMED = "CONFIRMED";
    public static final String REQUEST_STATUS_REJECTED = "REJECTED";
    public static final String REQUEST_STATUS_CANCELED = "CANCELED";

    // Статусы пользователей
    public static final String USER_STATE_ACTIVE = "ACTIVE";
    public static final String USER_STATE_BLOCKED = "BLOCKED";

    // Действия для обновления события
    public static final String STATE_ACTION_SEND_TO_REVIEW = "SEND_TO_REVIEW";
    public static final String STATE_ACTION_CANCEL_REVIEW = "CANCEL_REVIEW";
    public static final String STATE_ACTION_PUBLISH = "PUBLISH";
    public static final String STATE_ACTION_REJECT = "REJECT";

    // Имена приложений для статистики
    public static final String APP_NAME = "ewm-main-service";

    // Сортировка
    public static final String SORT_EVENT_DATE = "EVENT_DATE";
    public static final String SORT_VIEWS = "VIEWS";

    // Ограничения
    public static final int MIN_ANNOTATION_LENGTH = 20;
    public static final int MAX_ANNOTATION_LENGTH = 2000;
    public static final int MIN_DESCRIPTION_LENGTH = 20;
    public static final int MAX_DESCRIPTION_LENGTH = 7000;
    public static final int MIN_TITLE_LENGTH = 3;
    public static final int MAX_TITLE_LENGTH = 120;
    public static final int MAX_CATEGORY_NAME_LENGTH = 255;
    public static final int MAX_USER_NAME_LENGTH = 255;
    public static final int MAX_EMAIL_LENGTH = 512;

    // Часы для проверки даты события
    public static final int MIN_HOURS_BEFORE_EVENT = 2;
}