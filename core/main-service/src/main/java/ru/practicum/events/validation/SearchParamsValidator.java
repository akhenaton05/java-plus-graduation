package ru.practicum.events.validation;

import jakarta.validation.ConstraintViolationException;
import ru.practicum.config.DateConfig;
import ru.practicum.events.dto.SearchEventsParams;

import java.time.LocalDateTime;
import java.util.Objects;

public class SearchParamsValidator {

    public static void validateSearchParams(SearchEventsParams se) throws ConstraintViolationException {
        String dateErrorMessage = "RangeStart not appointed but rangeEnd is earlier as now or " +
                "Date of rangeEnd is earlier as date of rangeStart";
        Long minCatId = se.getCategories().stream()
                .min(Long::compareTo)
                .orElse(null);
        if (Objects.nonNull(minCatId) && minCatId < 1L) {
            throw new ConstraintViolationException("Invalid categories list", null);
        }
        LocalDateTime start =
                (Objects.isNull(se.getRangeStart())) ? LocalDateTime.now() :
                        LocalDateTime.parse(se.getRangeStart(), DateConfig.FORMATTER);
        LocalDateTime end =
                (Objects.nonNull(se.getRangeEnd())) ? LocalDateTime.parse(se.getRangeEnd(), DateConfig.FORMATTER) : null;
        if (Objects.nonNull(end) && !end.isAfter(start))
            throw new ConstraintViolationException(dateErrorMessage, null);

    }
}