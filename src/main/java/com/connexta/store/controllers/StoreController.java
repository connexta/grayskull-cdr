/*
 * Copyright (c) 2019 Connexta, LLC
 *
 * Released under the GNU Lesser General Public License version 3; see
 * https://www.gnu.org/licenses/lgpl-3.0.html
 */
package com.connexta.store.controllers;

import static org.springframework.http.ResponseEntity.ok;

import com.connexta.store.adaptors.FileRetrieveResponse;
import com.connexta.store.rest.models.ErrorMessage;
import com.connexta.store.rest.spring.StoreApi;
import com.connexta.store.service.api.StoreService;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import javax.validation.Valid;
import javax.validation.ValidationException;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@AllArgsConstructor
@RestController
public class StoreController implements StoreApi {

  public static final String ACCEPT_VERSION_HEADER_NAME = "Accept-Version";
  public static final String SUPPORTED_METADATA_TYPE = "irm";
  public static final String CREATE_DATASET_URL_TEMPLATE = "/dataset";
  public static final String ADD_METADATA_URL_TEMPLATE = "/dataset/{datasetId}/{metadataType}";
  public static final String RETRIEVE_FILE_URL_TEMPLATE = "/dataset/{datasetId}";
  public static final String RETRIEVE_IRM_URL_TEMPLATE = "/dataset/{datasetId}/irm";
  public static final MediaType IRM_MEDIA_TYPE = new MediaType("application", "dni-tdf+xml");
  public static final String IRM_MEDIA_TYPE_VALUE = "application/dni-tdf+xml";

  @NotNull private final StoreService storeService;
  @NotBlank private final String storeApiVersion;

  /**
   * TODO Use {@link org.springframework.web.server.ResponseStatusException} instead of catching
   * {@link Exception}s
   */
  @Override
  public ResponseEntity<Void> createDataset(
      final String acceptVersion, @Valid final MultipartFile file) {
    final String expectedAcceptVersion = storeApiVersion;
    if (!StringUtils.equals(acceptVersion, expectedAcceptVersion)) {
      throw new UnsupportedOperationException(
          String.format(
              "%s was \"%s\", but only \"%s\" is currently supported.",
              ACCEPT_VERSION_HEADER_NAME, acceptVersion, expectedAcceptVersion));
    }

    MultipartFileValidator.validate(file);
    final String mediaType = file.getContentType();
    final String fileName = file.getOriginalFilename();

    final URI location;
    try (final InputStream inputStream = file.getInputStream()) {
      location = storeService.createDataset(file.getSize(), mediaType, fileName, inputStream);
    } catch (IOException e) {
      throw new ValidationException(
          String.format(
              "Unable to read file for createDataset request with mediaType=%s and fileName=%s",
              mediaType, fileName),
          e);
    }

    return ResponseEntity.created(location).build();
  }

  @Override
  public ResponseEntity<Void> addMetadata(
      final String acceptVersion,
      @Pattern(regexp = "^[0-9a-zA-Z]+$") @Size(min = 32, max = 32) final String datasetId,
      @Pattern(regexp = "^[0-9a-zA-Z\\-]+$") @Size(min = 1, max = 32) final String metadataType,
      @Valid final MultipartFile file) {
    final String expectedAcceptVersion = storeApiVersion;
    if (!StringUtils.equals(acceptVersion, expectedAcceptVersion)) {
      throw new UnsupportedOperationException(
          String.format(
              "%s was \"%s\", but only \"%s\" is currently supported.",
              ACCEPT_VERSION_HEADER_NAME, acceptVersion, expectedAcceptVersion));
    }

    // TODO Validate other params.

    if (!StringUtils.equals(metadataType, SUPPORTED_METADATA_TYPE)) {
      throw new UnsupportedOperationException(
          String.format("Metadata type %s is not yet supported", metadataType));
    }

    final Long fileSize = file.getSize();
    // TODO validate that fileSize is (0 GB, 10 GB]

    // TODO Verify InputStream is closed in tests
    try (final InputStream inputStream = file.getInputStream()) {
      storeService.addIrm(inputStream, fileSize, datasetId);
    } catch (IOException e) {
      throw new ValidationException(
          String.format(
              String.format(
                  "Unable to read file for addMetadata request for metadataType=%s, id=%s",
                  metadataType, datasetId),
              e));
    }

    return ok().build();
  }

  @ApiOperation(
      value = "Get a file for a dataset.",
      nickname = "retrieveFile",
      response = Resource.class,
      tags = {"store"})
  @ApiResponses(
      value = {
        @ApiResponse(code = 200, message = "Get File", response = Resource.class),
        @ApiResponse(
            code = 401,
            message = "The client could not be authenticated. ",
            response = ErrorMessage.class),
        @ApiResponse(
            code = 400,
            message =
                "The client message could not be understood by the server due to invalid format or syntax. ",
            response = ErrorMessage.class),
        @ApiResponse(
            code = 403,
            message = "The client does not have permission. ",
            response = ErrorMessage.class),
        @ApiResponse(
            code = 501,
            message = "The requested API version is not supported and therefore not implemented. ",
            response = ErrorMessage.class)
      })
  @RequestMapping(
      value = RETRIEVE_FILE_URL_TEMPLATE,
      produces = {MediaType.APPLICATION_OCTET_STREAM_VALUE, MediaType.APPLICATION_JSON_VALUE},
      method = RequestMethod.GET)
  public ResponseEntity<Resource> retrieveFile(
      @Pattern(regexp = "^[0-9a-zA-Z]+$")
          @Size(min = 32, max = 32)
          @ApiParam(value = "The ID of the dataset. ", required = true)
          @PathVariable("datasetId")
          final String datasetId) {
    InputStream inputStream = null;
    try {
      final FileRetrieveResponse fileRetrieveResponse = storeService.retrieveFile(datasetId);
      log.info("Successfully retrieved file for datasetId={}", datasetId);

      final HttpHeaders httpHeaders = new HttpHeaders();
      httpHeaders.setContentDisposition(
          ContentDisposition.builder("attachment")
              .filename(fileRetrieveResponse.getFileName())
              .build());
      inputStream = fileRetrieveResponse.getInputStream();
      return ResponseEntity.ok()
          .contentType(fileRetrieveResponse.getMediaType())
          .headers(httpHeaders)
          .body(new InputStreamResource(inputStream));
    } catch (Throwable t) {
      if (inputStream != null) {
        try {
          inputStream.close();
        } catch (IOException e) {
          log.warn(
              "Unable to close InputStream when retrieving file for datasetId={}", datasetId, e);
        }
      }
      throw t;
    }
  }

  @ApiOperation(
      value = "Get a IRM for a dataset.",
      nickname = "retrieveIrm",
      response = Resource.class,
      tags = {"store"})
  @ApiResponses(
      value = {
        @ApiResponse(code = 200, message = "Get IRM", response = Resource.class),
        @ApiResponse(
            code = 401,
            message = "The client could not be authenticated. ",
            response = ErrorMessage.class),
        @ApiResponse(
            code = 400,
            message =
                "The client message could not be understood by the server due to invalid format or syntax. ",
            response = ErrorMessage.class),
        @ApiResponse(
            code = 403,
            message = "The client does not have permission. ",
            response = ErrorMessage.class),
        @ApiResponse(
            code = 501,
            message = "The requested API version is not supported and therefore not implemented. ",
            response = ErrorMessage.class)
      })
  @RequestMapping(
      value = RETRIEVE_IRM_URL_TEMPLATE,
      produces = {IRM_MEDIA_TYPE_VALUE, MediaType.APPLICATION_JSON_VALUE},
      method = RequestMethod.GET)
  public ResponseEntity<Resource> retrieveIrm(
      @Pattern(regexp = "^[0-9a-zA-Z]+$")
          @Size(min = 32, max = 32)
          @ApiParam(value = "The ID of the dataset. ", required = true)
          @PathVariable("datasetId")
          final String datasetId) {
    InputStream inputStream = null;
    try {
      inputStream = storeService.retrieveIrm(datasetId);
      log.info("Successfully retrieved irm for datasetId={}", datasetId);

      final HttpHeaders httpHeaders = new HttpHeaders();
      httpHeaders.setContentDisposition(
          ContentDisposition.builder("attachment").filename("irm-" + datasetId + ".xml").build());
      return ResponseEntity.ok()
          .contentType(IRM_MEDIA_TYPE)
          .headers(httpHeaders)
          .body(new InputStreamResource(inputStream));
    } catch (Throwable t) {
      if (inputStream != null) {
        try {
          inputStream.close();
        } catch (IOException e) {
          log.warn(
              "Unable to close InputStream when retrieving irm for datasetId={}", datasetId, e);
        }
      }
      throw t;
    }
  }
}
