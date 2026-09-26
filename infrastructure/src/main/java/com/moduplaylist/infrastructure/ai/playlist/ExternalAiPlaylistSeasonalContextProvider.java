
package com.moduplaylist.infrastructure.ai.playlist;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moduplaylist.core.playlist.ai.AiPlaylistSeasonalContext;
import com.moduplaylist.core.playlist.ai.AiPlaylistSeasonalContextProvider;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.time.DayOfWeek;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ExternalAiPlaylistSeasonalContextProvider implements AiPlaylistSeasonalContextProvider {

  private static final String HOLIDAY_API =
      "https://date.nager.at/api/v3/PublicHolidays/%d/KR";

  private static final String WEATHER_API =
      "https://api.open-meteo.com/v1/forecast"
          + "?latitude=37.5665&longitude=126.9780"
          + "&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max"
          + "&timezone=Asia%2FSeoul&forecast_days=7";

  private final HttpClient httpClient;
  private final ObjectMapper objectMapper;

  public ExternalAiPlaylistSeasonalContextProvider(ObjectMapper objectMapper) {
    this.httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .build();
    this.objectMapper = objectMapper;
  }

  @Override
  public AiPlaylistSeasonalContext getContext(LocalDate date) {
    String holidaySummary = fetchHolidaySummary(date);
    String weatherSummary = fetchWeatherSummary();

    return new AiPlaylistSeasonalContext(holidaySummary, weatherSummary);
  }

  private String fetchHolidaySummary(LocalDate date) {
    try {
      LocalDate startDate = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
      LocalDate endDate = startDate.plusDays(6);
      List<String> holidays = new ArrayList<>();

      for (int year = startDate.getYear(); year <= endDate.getYear(); year++) {
        JsonNode response = request(HOLIDAY_API.formatted(year));

        if (!response.isArray()) {
          throw new IllegalStateException("공휴일 API 응답 형식이 올바르지 않습니다.");
        }

        for (JsonNode holiday : response) {
          LocalDate holidayDate = LocalDate.parse(holiday.path("date").asText());

          if (holidayDate.isBefore(startDate) || holidayDate.isAfter(endDate)) {
            continue;
          }

          if (!holiday.path("global").asBoolean(false)) {
            continue;
          }

          String name = holiday.path("localName").asText(
              holiday.path("name").asText()
          );

          holidays.add(holidayDate + " " + name);
        }
      }

      if (holidays.isEmpty()) {
        return "조회 기간에 확인된 대한민국 전국 공휴일 없음";
      }

      return String.join("\n", holidays);
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      log.warn("공휴일 정보 조회 중 인터럽트 발생", exception);
      return "";
    } catch (Exception exception) {
      log.warn("공휴일 정보 조회 실패 - date={}", date, exception);
      return "";
    }
  }

  private String fetchWeatherSummary() {
    try {
      JsonNode daily = request(WEATHER_API).path("daily");

      JsonNode dates = daily.path("time");
      JsonNode codes = daily.path("weather_code");
      JsonNode maxTemperatures = daily.path("temperature_2m_max");
      JsonNode minTemperatures = daily.path("temperature_2m_min");
      JsonNode precipitation = daily.path("precipitation_probability_max");

      if (!dates.isArray() || !codes.isArray()
          || !maxTemperatures.isArray() || !minTemperatures.isArray()
          || !precipitation.isArray()) {
        throw new IllegalStateException("날씨 API 응답 형식이 올바르지 않습니다.");
      }

      List<String> forecasts = new ArrayList<>();

      for (int i = 0; i < dates.size(); i++) {
        forecasts.add("%s: %s, 최저 %.1f도, 최고 %.1f도, 최대 강수확률 %d%%".formatted(
            dates.get(i).asText(),
            describeWeather(codes.get(i).asInt()),
            minTemperatures.get(i).asDouble(),
            maxTemperatures.get(i).asDouble(),
            precipitation.get(i).asInt()
        ));
      }

      return "서울 기준 일별 예보:\n" + forecasts.stream()
          .collect(Collectors.joining("\n"));
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      log.warn("날씨 정보 조회 중 인터럽트 발생", exception);
      return "";
    } catch (Exception exception) {
      log.warn("날씨 정보 조회 실패", exception);
      return "";
    }
  }

  private JsonNode request(String url) throws Exception {
    HttpRequest request = HttpRequest.newBuilder(URI.create(url))
        .timeout(Duration.ofSeconds(10))
        .GET()
        .build();

    HttpResponse<String> response = httpClient.send(
        request,
        HttpResponse.BodyHandlers.ofString()
    );

    if (response.statusCode() != 200) {
      throw new IllegalStateException(
          "외부 API 응답 오류 - status=" + response.statusCode()
      );
    }

    return objectMapper.readTree(response.body());
  }

  private String describeWeather(int code) {
    if (code == 0) {
      return "맑음";
    }
    if (code <= 3) {
      return "구름";
    }
    if (code <= 48) {
      return "안개";
    }
    if (code <= 67) {
      return "비";
    }
    if (code <= 77) {
      return "눈";
    }
    if (code <= 82) {
      return "소나기";
    }
    if (code <= 86) {
      return "눈 소나기";
    }
    if (code <= 99) {
      return "뇌우";
    }
    return "기상 상태 미확인";
  }
}
