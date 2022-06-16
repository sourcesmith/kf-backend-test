package uk.co.truenotfalse.dao.impl;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava3.core.buffer.Buffer;
import io.vertx.rxjava3.ext.web.client.HttpRequest;
import io.vertx.rxjava3.ext.web.client.HttpResponse;
import io.vertx.rxjava3.ext.web.client.WebClient;
import io.vertx.rxjava3.ext.web.client.predicate.ErrorConverter;
import io.vertx.rxjava3.ext.web.client.predicate.ResponsePredicate;
import io.vertx.rxjava3.ext.web.client.predicate.ResponsePredicateResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.co.truenotfalse.NotFoundException;
import uk.co.truenotfalse.TooManyRequestsException;
import uk.co.truenotfalse.dao.InterviewTestsMockApiDao;
import uk.co.truenotfalse.model.DeviceOutage;


/**
 *  Implementation of the {@link InterviewTestsMockApiDao} interface using Unirest.
 */
public class InterviewTestsMockApiDaoImpl implements InterviewTestsMockApiDao
{
    /**
     *  Creates an instance of this class.
     *
     *  @param apiKey  The API key to authorize requests with.
     *  @param baseUri  The base URI to use for request endpoints.
     *  @param webClient  The web client instance to use for making requests.
     */
    public InterviewTestsMockApiDaoImpl(final String apiKey, final String baseUri, final WebClient webClient)
    {
        LOG.trace("Creating instance with base URI of '{}'.", baseUri);

        Objects.requireNonNull(apiKey, "An API key is required.");
        Objects.requireNonNull(baseUri, "A base URI is required.");
        Objects.requireNonNull(webClient, "A web client instance is required.");

        if(apiKey.isBlank())
        {
            throw new IllegalArgumentException("A non-blank API key is required.");
        }
        if(apiKey.isBlank())
        {
            throw new IllegalArgumentException("A non-blank base URI is required.");
        }

        this.baseUri = baseUri;
        this.apiKey = apiKey;
        this.webClient = webClient;
    }


    /**
     *  {@inheritDoc}
     */
    @Override
    public Single<List<DeviceOutage>> getOutages()
    {
        LOG.trace("getOutages() called.");

        return authorize(webClient.getAbs(baseUri + OUTAGES_PATH)).
          putHeader(ACCEPT_HEADER_KEY, JSON_MEDIA_TYPE).expect(errorPredicate).timeout(10000L).rxSend().
            map(response -> mapOutagesResponse(response.bodyAsJsonArray()));
    }


    /**
     *  {@inheritDoc}
     */
    @Override
    public Single<Map<Object, String>> getSiteInfo(final String siteId)
    {
        LOG.trace("getSiteInfo('{}') called.", siteId);

        return authorize(webClient.getAbs(baseUri + SITE_INFO_PATH + siteId)).
          putHeader(ACCEPT_HEADER_KEY, JSON_MEDIA_TYPE).expect(errorPredicate).timeout(10000L).rxSend().
            map(response -> mapSiteInfoResponse(response.bodyAsJsonObject()));
    }


    /**
     *  {@inheritDoc}
     */
    @Override
    public Single<Completable> updateSiteOutages(final String siteId, final List<DeviceOutage> outageUpdates)
    {
        LOG.trace("updateSiteOutages('{}', ...) called.", siteId);

        final JsonArray body =
          new JsonArray(outageUpdates.stream().
                                      map(outage -> new JsonObject().put("id", outage.getId().toString()).
                                                                     put("begin", ISO_OFFSET_DATE_TIME.format(outage.getBegin())).
                                                                     put("end", outage.getEnd() != null ? ISO_OFFSET_DATE_TIME.format(outage.getEnd()) : null).
                                                                     put("name", outage.getDeviceName())).toList());

        return authorize(webClient.postAbs(baseUri + SITE_OUTAGES_PATH + siteId)).
          putHeader(CONTENT_TYPE_KEY, JSON_MEDIA_TYPE).putHeader(ACCEPT_HEADER_KEY, JSON_MEDIA_TYPE).
            expect(errorPredicate).timeout(10000L).rxSendJson(body).map(bufferHttpResponse -> Completable.complete());
    }


    private <T> HttpRequest<T> authorize(final HttpRequest<T> request)
    {
        return request.putHeader(API_HEADER_KEY, apiKey);
    }


    private List<DeviceOutage> mapOutagesResponse(final JsonArray response)
    {
        return response.stream().
                        map(outage ->
                            {
                                final JsonObject outageJson = (JsonObject)outage;

                                return new DeviceOutage(outageJson.getValue("id"),
                                                        OffsetDateTime.parse(outageJson.getString("begin"), ISO_OFFSET_DATE_TIME),
                                                        OffsetDateTime.parse(outageJson.getString("end"), ISO_OFFSET_DATE_TIME));
                            }).toList();
    }


    private Map<Object, String> mapSiteInfoResponse(final JsonObject response)
    {
        return response.getJsonArray("devices").stream().map(JsonObject.class::cast).
                        collect(Collectors.toUnmodifiableMap(device -> device.getValue("id"),
                                                             device -> device.getString("name")));
    }


    private static final String API_HEADER_KEY = "x-api-key";
    private static final String OUTAGES_PATH = "/outages";
    private static final String SITE_INFO_PATH = "/site-info/";
    private static final String SITE_OUTAGES_PATH = "/site-outages/";
    private static final String ACCEPT_HEADER_KEY = "Accept";
    private static final String CONTENT_TYPE_KEY = "Content-Type";
    private static final String JSON_MEDIA_TYPE = "application/json";

    private final Logger LOG = LoggerFactory.getLogger(InterviewTestsMockApiDaoImpl.class);

    private final ResponsePredicate errorPredicate =
      ResponsePredicate.create(response -> response.statusCode() >= 400 ? ResponsePredicateResult.failure(response.statusMessage()) : ResponsePredicateResult.success(),
                               ErrorConverter.createFullBody(result ->
                                                             {
                                                                 final HttpResponse<Buffer> response = result.response();

                                                                 if(response.getHeader("content-type").equals("application/json"))
                                                                 {
                                                                     final String message =
                                                                       response.bodyAsJsonObject().getString("message");

                                                                     return
                                                                       switch(result.response().statusCode())
                                                                       {
                                                                           case 400 -> new IllegalArgumentException(message);
                                                                           // Key not valid should be a 401, doing something not permitted to a valid key should be a 403.
                                                                           case 403 -> new SecurityException(message);
                                                                           case 404 -> new NotFoundException(message);
                                                                           case 429 -> new TooManyRequestsException(message);
                                                                           default -> new RuntimeException(message);
                                                                       };
                                                                 }

                                                                 return new RuntimeException(response.statusMessage());
                                                             }));

    private final String baseUri;
    private final String apiKey;
    private final WebClient webClient;
}
