package org.example.functions;

import java.util.*;

import com.azure.storage.blob.models.BlobStorageException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.microsoft.azure.functions.annotation.*;
import com.microsoft.azure.functions.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.example.functions.model.Hero;
import org.example.functions.service.HeroService;

@Slf4j
public class HeroTriggers {
  private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder()
      .addModule(new JavaTimeModule())
      .build();

  @FunctionName("getAllHeroes")
  public HttpResponseMessage getAllHeroes(
      @HttpTrigger(
          name = "getAllHeroes",
          route = "heroes",
          methods = {HttpMethod.GET},
          authLevel = AuthorizationLevel.ANONYMOUS)
      HttpRequestMessage<Optional<String>> request) {
    log.info("Processing getAllHeroes function.");

    HeroService heroService = new HeroService();

    List<Hero> comicData = heroService.getHeroesList();
    try {
      return request.createResponseBuilder(HttpStatus.OK)
          .header("Access-Control-Allow-Origin", "*")
          .header("Access-Control-Allow-Methods", "*")
          .header("Content-Type", "application/json")
          .body(OBJECT_MAPPER.writerWithDefaultPrettyPrinter()
              .writeValueAsString(comicData))
          .build();
    } catch (JsonProcessingException e) {
      log.error("Severe error processing listAllComics.", e);
      return request.createResponseBuilder(HttpStatus.I_AM_A_TEAPOT)
          .header("Access-Control-Allow-Origin", "*")
          .header("Access-Control-Allow-Methods", "*")
          .header("Content-Type", "text/plain")
          .body(ExceptionUtils.getMessage(e))
          .build();
    }
  }

  @FunctionName("getHeroById")
  public HttpResponseMessage getHeroById(
      @HttpTrigger(
          name = "getHeroById",
          route = "heroes/{id}",
          methods = {HttpMethod.GET},
          authLevel = AuthorizationLevel.ANONYMOUS)
      HttpRequestMessage<Optional<String>> request,
      @BindingName("id") String id) {
    log.info("Processing getHeroById {} function.", id);

    HeroService heroService = new HeroService();

    List<Hero> heroData = heroService.getHeroesList();
    Optional<Hero> matchingHero = heroData.stream()
        .filter(hero -> hero.getId() == Integer.parseInt(id))
        .findFirst();

    try {
      return request.createResponseBuilder(HttpStatus.OK)
          .header("Access-Control-Allow-Origin", "*")
          .header("Access-Control-Allow-Methods", "*")
          .header("Content-Type", "application/json")
          .body(OBJECT_MAPPER.writerWithDefaultPrettyPrinter()
              .writeValueAsString(matchingHero.orElseThrow()))
          .build();
    } catch (JsonProcessingException e) {
      log.error("Severe error processing getHeroById.", e);
      return request.createResponseBuilder(HttpStatus.I_AM_A_TEAPOT)
          .header("Access-Control-Allow-Origin", "*")
          .header("Access-Control-Allow-Methods", "*")
          .header("Content-Type", "text/plain")
          .body(ExceptionUtils.getMessage(e))
          .build();
    }
  }

  @FunctionName("updateHero")
  public HttpResponseMessage updateHero(
      @HttpTrigger(
          name = "updateHero",
          route = "heroes",
          methods = {HttpMethod.PUT},
          authLevel = AuthorizationLevel.ANONYMOUS)
      HttpRequestMessage<Optional<String>> request) {
    log.info("Processing updateHero function.");

    HeroService heroService = new HeroService();

    String requestBody = request.getBody().orElse(null);
    Hero updatedHero = null;
    try {
      updatedHero = OBJECT_MAPPER.readValue(requestBody, Hero.class);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }

    boolean successfulUpdate = heroService.updateHero(updatedHero);

    return request.createResponseBuilder(HttpStatus.OK)
        .header("Access-Control-Allow-Origin", "*")
        .header("Access-Control-Allow-Methods", "*")
        .header("Content-Type", "application/json")
        .body(String.format("{ \"updated\": \"%s\", \"id\": %d }", successfulUpdate, updatedHero.getId()))
        .build();
  }

  @FunctionName("createHero")
  public HttpResponseMessage createHero(
      @HttpTrigger(
          name = "createHero",
          route = "heroes",
          methods = {HttpMethod.POST},
          authLevel = AuthorizationLevel.ANONYMOUS)
      HttpRequestMessage<Optional<String>> request) {
    log.info("Processing createHero function.");

    try {
      HeroService heroService = new HeroService();
      String requestBody = request.getBody().orElse(null);
      Hero newHero = OBJECT_MAPPER.readValue(requestBody, Hero.class);
      Optional<String> optionalId = Optional.ofNullable(request.getQueryParameters().get("id"));
      optionalId.ifPresent(s -> newHero.setId(Integer.parseInt(s)));
      Optional<Hero> existingHero = heroService.getHeroesList().stream()
          .filter(hero -> Objects.equals(hero.getName(),newHero.getName()))
          .findAny();
      Hero addedHero;
      if (existingHero.isPresent()) {
        throw new RuntimeException("Hero was already in the database.");
      } else {
        addedHero = heroService.createHero(newHero);
      }
      return request.createResponseBuilder(HttpStatus.OK)
          .header("Access-Control-Allow-Origin", "*")
          .header("Access-Control-Allow-Methods", "*")
          .header("Content-Type", "application/json")
          .body(OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(addedHero))
          .build();
    } catch (BlobStorageException bse) {
      log.error("Blob storage error.", bse);
      return request.createResponseBuilder(HttpStatus.OK)
          .header("Access-Control-Allow-Origin", "*")
          .header("Access-Control-Allow-Methods", "*")
          .header("Content-Type", "text/plain")
          .body(ExceptionUtils.getMessage(bse))
          .build();
    } catch (Exception e) {
      log.error("There was a create hero error.", e);
      return request.createResponseBuilder(HttpStatus.OK)
          .header("Access-Control-Allow-Origin", "*")
          .header("Access-Control-Allow-Methods", "*")
          .header("Content-Type", "text/plain")
          .body(ExceptionUtils.getMessage(e))
          .build();
    }
  }

  @FunctionName("deleteHero")
  public HttpResponseMessage deleteHero(
      @HttpTrigger(
          name = "deleteHero",
          route = "heroes/{id}",
          methods = {HttpMethod.DELETE},
          authLevel = AuthorizationLevel.ANONYMOUS)
      HttpRequestMessage<Optional<String>> request,
      @BindingName("id") String id) {
    log.info("Processing deleteHero function.");

    final int targetId = Integer.parseInt(id);
    HeroService heroService = new HeroService();

    List<Hero> heroesList = heroService.getHeroesList();

    Optional<Hero> existingHero = heroesList.stream()
        .filter(hero -> hero.getId() == targetId)
        .findFirst();
    try {
      if (existingHero.isPresent()) {
        heroService.deleteHero(existingHero.get().getId());
      } else {
        return request.createResponseBuilder(HttpStatus.NOT_FOUND)
            .header("Access-Control-Allow-Origin", "*")
            .header("Access-Control-Allow-Methods", "*")
            .header("Access-Control-Allow-Headers", "Content-Type")
            .header("Content-Type", "application/json")
            .body(String.format("{ \"deleted\": \"%s\", \"id\": %s }", false, targetId))
            .build();
      }
    } catch (Exception e) {
      return request.createResponseBuilder(HttpStatus.I_AM_A_TEAPOT)
          .header("Access-Control-Allow-Origin", "*")
          .header("Access-Control-Allow-Methods", "*")
          .header("Access-Control-Allow-Headers", "Content-Type")
          .header("Content-Type", "text/plain")
          .body(ExceptionUtils.getMessage(e))
          .build();
    }
    return request.createResponseBuilder(HttpStatus.OK)
        .header("Access-Control-Allow-Origin", "*")
        .header("Access-Control-Allow-Methods", "*")
        .header("Access-Control-Allow-Headers", "Content-Type")
        .header("Content-Type", "application/json")
        .body(String.format("{ \"deleted\": \"%s\", \"id\": %d }", true, targetId))
        .build();
  }

  /* CORS */

  @FunctionName("preflightDefault")
  public HttpResponseMessage preflightDefault(
      @HttpTrigger(
          name = "preflightDefault",
          route = "heroes",
          methods = {HttpMethod.OPTIONS},
          authLevel = AuthorizationLevel.ANONYMOUS)
      HttpRequestMessage<Optional<String>> request) {
    log.info("Processing preflightDefault function.");
      return request.createResponseBuilder(HttpStatus.OK)
          .header("Access-Control-Allow-Origin", "*")
          .header("Access-Control-Allow-Methods", "*")
          .header("Access-Control-Allow-Headers", "Content-Type")
          .build();
  }

  @FunctionName("preflightWithId")
  public HttpResponseMessage preflightWithId(
      @HttpTrigger(
          name = "preflightWithId",
          route = "heroes/{id}",
          methods = {HttpMethod.OPTIONS},
          authLevel = AuthorizationLevel.ANONYMOUS)
      HttpRequestMessage<Optional<String>> request) {
    log.info("Processing preflightWithId function.");
    return request.createResponseBuilder(HttpStatus.OK)
        .header("Access-Control-Allow-Origin", "*")
        .header("Access-Control-Allow-Methods", "*")
        .header("Access-Control-Allow-Headers", "Content-Type")
        .build();
  }

  @FunctionName("loadData")
  public HttpResponseMessage loadData(
      @HttpTrigger(
          name = "loadData",
          route = "heroes/data",
          methods = {HttpMethod.POST},
          authLevel = AuthorizationLevel.ANONYMOUS)
      HttpRequestMessage<Optional<String>> request) {
    log.info("Processing loadData function.");
    return null;
  }

}
