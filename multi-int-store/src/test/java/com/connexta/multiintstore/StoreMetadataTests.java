/*
 * Copyright (c) 2019 Connexta, LLC
 *
 * Released under the GNU Lesser General Public License version 3; see
 * https://www.gnu.org/licenses/lgpl-3.0.html
 */
package com.connexta.multiintstore;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.ignoreStubs;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.amazonaws.services.s3.AmazonS3;
import java.nio.charset.StandardCharsets;
import javax.inject.Inject;
import org.apache.commons.io.IOUtils;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.SolrRequest.METHOD;
import org.apache.solr.common.params.SolrParams;
import org.junit.After;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

/**
 * This class contains tests for the store metadata endpoint that use a mocked {@link AmazonS3} and
 * {@link SolrClient}.
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
@MockBean(AmazonS3.class)
public class StoreMetadataTests {

  @MockBean private SolrClient mockSolrClient;

  @Inject private MockMvc mockMvc;

  @After
  public void after() {
    verifyNoMoreInteractions(ignoreStubs(mockSolrClient));
  }

  @Test
  public void testContextLoads() {}

  @Test
  public void testBadRequest() throws Exception {
    mockMvc
        .perform(
            multipart("/mis/product/1234/cst")
                .param("mimeType", "plain/text")
                .param("fileName", "test_file_name.txt")
                .header("Accept-Version", "1.2.1")
                .with(
                    request -> {
                      request.setMethod(HttpMethod.PUT.toString());
                      return request;
                    })
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.MULTIPART_FORM_DATA))
        .andExpect(status().isBadRequest());
  }

  @Test
  @Ignore("TODO")
  public void testCantReadAttachment() {
    // TODO verify 400
  }

  @Test
  public void testNonCST() throws Exception {
    mockMvc
        .perform(
            multipart("/mis/product/1234/anotherMetadataType")
                .file(
                    new MockMultipartFile(
                        "file",
                        "test_file_name.txt",
                        "text/plain",
                        IOUtils.toInputStream(
                            "All the color had been leached from Winterfell until only grey and white remained",
                            StandardCharsets.UTF_8)))
                .header("Accept-Version", "1.2.1")
                .with(
                    request -> {
                      request.setMethod(HttpMethod.PUT.toString());
                      return request;
                    })
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.MULTIPART_FORM_DATA))
        .andExpect(status().isNotImplemented());
  }

  /** @see SolrClient#query(String, SolrParams, METHOD) */
  @Test
  public void testSolrCommunicationError() throws Exception {
    when(mockSolrClient.query(eq("searchTerms"), any(SolrQuery.class), eq(SolrRequest.METHOD.GET)))
        .thenThrow(RuntimeException.class);

    mockMvc
        .perform(
            multipart("/mis/product/1234/cst")
                .file(
                    new MockMultipartFile(
                        "file",
                        "test_file_name.txt",
                        "text/plain",
                        IOUtils.toInputStream(
                            "All the color had been leached from Winterfell until only grey and white remained",
                            StandardCharsets.UTF_8)))
                .header("Accept-Version", "1.2.1")
                .with(
                    request -> {
                      request.setMethod(HttpMethod.PUT.toString());
                      return request;
                    })
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.MULTIPART_FORM_DATA))
        .andExpect(status().isInternalServerError());
  }

  /** @see SolrClient#query(String, SolrParams, METHOD) */
  @Test
  public void testSolrServerError() throws Exception {
    when(mockSolrClient.query(eq("searchTerms"), any(SolrQuery.class), eq(SolrRequest.METHOD.GET)))
        .thenThrow(RuntimeException.class);

    mockMvc
        .perform(
            multipart("/mis/product/1234/cst")
                .file(
                    new MockMultipartFile(
                        "file",
                        "test_file_name.txt",
                        "text/plain",
                        IOUtils.toInputStream(
                            "All the color had been leached from Winterfell until only grey and white remained",
                            StandardCharsets.UTF_8)))
                .header("Accept-Version", "1.2.1")
                .with(
                    request -> {
                      request.setMethod(HttpMethod.PUT.toString());
                      return request;
                    })
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.MULTIPART_FORM_DATA))
        .andExpect(status().isInternalServerError());
  }

  @Test
  public void testSolrThrowsRuntimeException() throws Exception {
    when(mockSolrClient.query(eq("searchTerms"), any(SolrQuery.class), eq(SolrRequest.METHOD.GET)))
        .thenThrow(RuntimeException.class);

    mockMvc
        .perform(
            multipart("/mis/product/1234/cst")
                .file(
                    new MockMultipartFile(
                        "file",
                        "test_file_name.txt",
                        "text/plain",
                        IOUtils.toInputStream(
                            "All the color had been leached from Winterfell until only grey and white remained",
                            StandardCharsets.UTF_8)))
                .header("Accept-Version", "1.2.1")
                .with(
                    request -> {
                      request.setMethod(HttpMethod.PUT.toString());
                      return request;
                    })
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.MULTIPART_FORM_DATA))
        .andExpect(status().isInternalServerError());
  }
}