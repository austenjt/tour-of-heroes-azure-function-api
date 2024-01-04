package org.example.functions.service;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.BlobItem;
import com.azure.storage.common.StorageSharedKeyCredential;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.example.functions.model.Hero;
import org.example.functions.util.EnvHelper;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

@Slf4j
public class HeroService {
  private static final String HEROES_CONTAINER = "heroes";
  private static final String BLOB_URL = "https://%s.blob.core.windows.net";
  private final ObjectMapper OBJECT_MAPPER;
  private final BlobContainerClient heroesContainerClient;

  public HeroService() {
    String accountName = EnvHelper.getAccountName();
    String accountKey = EnvHelper.getAccountKey();
    OBJECT_MAPPER = JsonMapper.builder()
        .addModule(new JavaTimeModule())
        .build();
    StorageSharedKeyCredential credential = new StorageSharedKeyCredential(accountName, accountKey);

    String endpoint = String.format(Locale.ROOT, BLOB_URL, accountName);

    BlobServiceClient storageClient = new BlobServiceClientBuilder()
        .endpoint(endpoint)
        .credential(credential)
        .buildClient();
    heroesContainerClient = storageClient.getBlobContainerClient(HEROES_CONTAINER);
  }

  public List<Hero> getHeroesList() {
    List<Hero> documentContents = new ArrayList<>();

    for (BlobItem blobItem : heroesContainerClient.listBlobs()) {
      BlobClient blobClient = heroesContainerClient.getBlobClient(blobItem.getName());
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      blobClient.downloadStream(outputStream);

      String content = outputStream.toString(StandardCharsets.UTF_8);
      try {
        Hero heroDetails = OBJECT_MAPPER.readValue(content, Hero.class);
        documentContents.add(heroDetails);
      } catch (JsonProcessingException e) {
        throw new RuntimeException(e);
      }
    }
    return documentContents;
  }
  public boolean updateHero(Hero updatedHero) {
    boolean isUpdated = false;

    for (BlobItem blobItem : heroesContainerClient.listBlobs()) {
      BlobClient blobClient = heroesContainerClient.getBlobClient(blobItem.getName());
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      blobClient.downloadStream(outputStream);

      String content = outputStream.toString(StandardCharsets.UTF_8);
      try {
        Hero oldHero = OBJECT_MAPPER.readValue(content, Hero.class);
        if (oldHero.getId() == updatedHero.getId()) {
          String updatedContent = OBJECT_MAPPER.writeValueAsString(updatedHero);
          ByteArrayInputStream inputStream = new ByteArrayInputStream(updatedContent.getBytes(StandardCharsets.UTF_8));
          blobClient.upload(inputStream, updatedContent.length(), true);
          isUpdated = true;
          break;
        }
      } catch (JsonProcessingException e) {
        throw new RuntimeException("Error processing JSON", e);
      }
    }
    return isUpdated;
  }

  public Hero createHero(Hero newHero) {
      if (newHero.getId() == 0 || newHero.getId() == -1) {
        int newHeroId = findLowestUnusedId();
        log.info("You didn't pass a id param, and so we are using random id {}", newHeroId);
        newHero.setId(newHeroId);
      }
      BlobClient blobClient = heroesContainerClient.getBlobClient(newHero.getId() + ".json");
      try {
          String updatedContent = OBJECT_MAPPER.writeValueAsString(newHero);
          ByteArrayInputStream inputStream = new ByteArrayInputStream(updatedContent.getBytes(StandardCharsets.UTF_8));
          blobClient.upload(inputStream, updatedContent.length(), false);
      } catch (JsonProcessingException e) {
        throw new RuntimeException("Error processing createHero with JSON document.", e);
      }
    return newHero;
  }

  public void deleteHero(int id) {
    log.info("Delete hero by id {}", id);
    BlobClient blobClient = heroesContainerClient.getBlobClient(id + ".json");
    blobClient.delete();
  }

  private int findLowestUnusedId() {
    Random rand = new Random();
    int eightDigitNumber = 10000000 + rand.nextInt(90000000);
    return eightDigitNumber;
  }

}
