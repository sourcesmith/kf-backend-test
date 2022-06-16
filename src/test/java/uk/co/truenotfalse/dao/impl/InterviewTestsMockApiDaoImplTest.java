package uk.co.truenotfalse.dao.impl;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME;

import com.github.tomakehurst.wiremock.extension.responsetemplating.ResponseTemplateTransformer;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.recording.RecordSpec;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.rxjava3.core.Vertx;
import io.vertx.rxjava3.ext.web.client.WebClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import uk.co.truenotfalse.NotFoundException;
import uk.co.truenotfalse.TooManyRequestsException;
import uk.co.truenotfalse.model.DeviceOutage;


@ExtendWith(VertxExtension.class)
public class InterviewTestsMockApiDaoImplTest
{
    @RegisterExtension
    static WireMockExtension wireMock =
      WireMockExtension.newInstance().options(wireMockConfig().dynamicPort().
                                              recordRequestHeadersForMatching(List.of("Accept", "Content-Type", "x-api-key")).
                                              extensions(new ResponseTemplateTransformer(false))).
                                      build();

    @BeforeAll
    public static void setUpClass(final Vertx vertx)
    {
        final WireMockRuntimeInfo runtimeInfo = wireMock.getRuntimeInfo();

        dao = new InterviewTestsMockApiDaoImpl("EltgJ5G8m44IzwE6UN2Y4B4NjPW77Zk6FJK3lL23",
                                               runtimeInfo.getHttpBaseUrl() + basePath,
                                               WebClient.create(vertx));
        unauthedDao = new InterviewTestsMockApiDaoImpl("not_a_valid_key",
                                                       runtimeInfo.getHttpBaseUrl() + basePath,
                                                       WebClient.create(vertx));
    }

    @BeforeEach
    public void setUp()
    {
        // Set to low priority so existing stubs take priority.  This means requests currently without stubs will go
        // to the live server.
        wireMock.stubFor(proxyAllTo("https://api.krakenflex.systems").atPriority(99999));
    }

    @AfterEach
    public void tearDown()
    {
        wireMock.snapshotRecord(recordSpec);
    }


    @Test
    void getOutages(final Vertx vertx, final VertxTestContext testContext)
    {
        dao.getOutages().subscribe(response -> testContext.completeNow(),
                                   error ->
                                   {
                                       if(error.getCause() != null)
                                       {
                                           testContext.failNow(error.getCause());
                                       }
                                       else
                                       {
                                           testContext.failNow(error.getMessage());
                                       }
                                   });
    }


    @Test
    void getOutagesTooManyRequests(final Vertx vertx, final VertxTestContext testContext) throws InterruptedException
    {
        // There is nothing to differentiate this request from the recorded successful stub so set one up for this test.
        final StubMapping tooManyRequestsStub =
          wireMock.stubFor(get(urlEqualTo(basePath + "/outages")).
                             withHeader("Accept", equalTo("application/json")).atPriority(1).
                             willReturn(aResponse().withStatus(429).withHeader("Content-Type", "application/json").
                                                   withBody(
                                                     "{\"message\":\"You have exceeded your limit for your API key\"}")).
                             persistent(false));

        dao.getOutages().subscribe(response -> testContext.failNow("A Too Many Requests response was expected (429)."),
                                   error ->
                                   {
                                       if(error instanceof TooManyRequestsException)
                                       {
                                           testContext.completeNow();
                                       }
                                       else if(error.getCause() != null)
                                       {
                                           testContext.failNow(error.getCause());
                                       }
                                       else
                                       {
                                           testContext.failNow(error.getMessage());
                                       }
                                   });

        try
        {
            testContext.awaitCompletion(10L, TimeUnit.SECONDS);
        }
        finally
        {
            wireMock.removeStub(tooManyRequestsStub);
        }
    }


    @Test
    void getOutagesNotAuthed(final Vertx vertx, final VertxTestContext testContext)
    {
        unauthedDao.getOutages().subscribe(response -> testContext.failNow("A Forbidden response was expected (403)."),
                                           error ->
                                           {
                                               if(error instanceof SecurityException)
                                               {
                                                   testContext.completeNow();
                                               }
                                               else if(error.getCause() != null)
                                               {
                                                   testContext.failNow(error.getCause());
                                               }
                                               else
                                               {
                                                   testContext.failNow(error.getMessage());
                                               }
                                           });
    }


    @Test
    void getSiteInfo(final Vertx vertx, final VertxTestContext testContext)
    {
        dao.getSiteInfo("norwich-pear-tree").
          subscribe(response -> testContext.completeNow(),
                    error ->
                    {
                        if(error.getCause() != null)
                        {
                            testContext.failNow(error.getCause());
                        }
                        else
                        {
                            testContext.failNow(error.getMessage());
                        }
                    });
    }


    @Test
    void getNonExistentSiteInfo(final Vertx vertx, final VertxTestContext testContext)
    {
        dao.getSiteInfo("no-such-site").
          subscribe(response -> testContext.failNow("A Not Found response was expected (404)."),
                    error ->
                    {
                        if(error instanceof NotFoundException)
                        {
                            testContext.completeNow();
                        }
                        else if(error.getCause() != null)
                        {
                            testContext.failNow(error.getCause());
                        }
                        else
                        {
                            testContext.failNow(error.getMessage());
                        }
                    });
    }


    @Test
    void getSiteInfoNotAuthed(final Vertx vertx, final VertxTestContext testContext)
    {
        unauthedDao.getSiteInfo("norwich-pear-tree").
          subscribe(response -> testContext.failNow("A Forbidden response was expected (403)."),
                    error ->
                    {
                        if(error instanceof SecurityException)
                        {
                            testContext.completeNow();
                        }
                        else if(error.getCause() != null)
                        {
                            testContext.failNow(error.getCause());
                        }
                        else
                        {
                            testContext.failNow(error.getMessage());
                        }
                    });
    }


    @Test
    void getSiteInfoTooManyRequests(final Vertx vertx, final VertxTestContext testContext)
    {
        dao.getSiteInfo("too-much-too-soon").
          subscribe(response -> testContext.failNow("A Too Many Requests response was expected (429)."),
                    error ->
                    {
                        if(error instanceof TooManyRequestsException)
                        {
                            testContext.completeNow();
                        }
                        else if(error.getCause() != null)
                        {
                            testContext.failNow(error.getCause());
                        }
                        else
                        {
                            testContext.failNow(error.getMessage());
                        }
                    });
    }


    @Test
    void updateSiteOutages(final Vertx vertx, final VertxTestContext testContext)
    {
        final DeviceOutage outage =
          new DeviceOutage("a79fe094-087b-4b1e-ae20-ac4bf7fa429b",
                           OffsetDateTime.parse("2020-03-03T23:14:30.832Z", ISO_OFFSET_DATE_TIME),
                           OffsetDateTime.parse("2023-12-15T15:12:32.953Z", ISO_OFFSET_DATE_TIME));

        outage.setDeviceName("Battery 5");

        // This is an updated with unexpected content but is does test we reach the endpoint.
        dao.updateSiteOutages("norwich-pear-tree", Collections.singletonList(outage)).
          subscribe(() -> testContext.failNow("Expected a Bad Request response (400)."),
                    error ->
                    {
                        if(error instanceof IllegalArgumentException)
                        {
                            testContext.completeNow();
                        }
                        else if(error.getCause() != null)
                        {
                            testContext.failNow(error.getCause());
                        }
                        else
                        {
                            testContext.failNow(error.getMessage());
                        }
                    });
    }


    @Test
    void updateNonExistentSiteOutages(final Vertx vertx, final VertxTestContext testContext)
    {
        // This is an update with unexpected content but is does test we reach the endpoint.
        dao.updateSiteOutages("no-such-site", Collections.emptyList()).
           subscribe(() -> testContext.failNow("Expected a Not Found response (404)."),
                     error ->
                     {
                         if(error instanceof NotFoundException)
                         {
                             testContext.completeNow();
                         }
                         else if(error.getCause() != null)
                         {
                             testContext.failNow(error.getCause());
                         }
                         else
                         {
                             testContext.failNow(error.getMessage());
                         }
                     });
    }


    @Test
    void updateSiteOutagesNotAuthed(final Vertx vertx, final VertxTestContext testContext)
    {
        unauthedDao.updateSiteOutages("norwich-pear-tree", Collections.emptyList()).
           subscribe(() -> testContext.failNow("Expected a Forbidden response (403)."),
                     error ->
                     {
                         if(error instanceof SecurityException)
                         {
                             testContext.completeNow();
                         }
                         else if(error.getCause() != null)
                         {
                             testContext.failNow(error.getCause());
                         }
                         else
                         {
                             testContext.failNow(error.getMessage());
                         }
                     });
    }


    @Test
    void updateSiteOutagesTooManyRequests(final Vertx vertx, final VertxTestContext testContext)
    {
        dao.updateSiteOutages("too-much-too-soon", Collections.emptyList()).
           subscribe(() -> testContext.failNow("A Too Many Requests response was expected (429)."),
                     error ->
                     {
                         if(error instanceof TooManyRequestsException)
                         {
                             testContext.completeNow();
                         }
                         else if(error.getCause() != null)
                         {
                             testContext.failNow(error.getCause());
                         }
                         else
                         {
                             testContext.failNow(error.getMessage());
                         }
                     });
    }

    private static final String basePath = "/interview-tests-mock-api/v1";

    private final RecordSpec recordSpec = recordSpec().captureHeader("Accept", true).
                                                       captureHeader("Content-Type", true).
                                                       captureHeader("x-api-key").build();

    private static InterviewTestsMockApiDaoImpl dao;
    private static InterviewTestsMockApiDaoImpl unauthedDao;
}
