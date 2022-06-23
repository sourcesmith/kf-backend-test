package uk.co.truenotfalse.agent;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.truenotfalse.dao.InterviewTestsMockApiDao;
import uk.co.truenotfalse.model.DeviceOutage;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;


/**
 * Agent service class to consume and update the interview mock test API.
 */
public class OutageAgentService {
    /**
     * Creates an instance of this class.
     *
     * @param apiDao The DAO instance to use for communicating with the interview tests mock API.
     */
    public OutageAgentService(final InterviewTestsMockApiDao apiDao) {
        Objects.requireNonNull(apiDao, "An instance of API DAO is required.");

        this.apiDao = apiDao;
    }


    public Completable updateOutages(final String siteId, final OffsetDateTime cutoff) {
        LOG.debug("updateOutages('{}', {}) called.", siteId, cutoff);

        Objects.requireNonNull(siteId, "A site ID is required.");
        Objects.requireNonNull(cutoff, "A cutoff date-time is required.");

        if (siteId.isBlank()) {
            throw new IllegalArgumentException("A meaningful site ID is required.");
        }

        // Filter out outages before the required cut-off.
        final Single<List<DeviceOutage>> deviceOutages =
                apiDao.getOutages().map(outages -> outages.stream().filter(outage -> !outage.getBegin().isBefore(cutoff)).toList());
        final Single<Map<Object, String>> sitesInfo = apiDao.getSiteInfo(siteId);

        return
                deviceOutages.zipWith(sitesInfo, (outages, info) ->
                                outages.stream().filter(outage ->
                                {
                                    // Attach the names using the site info -
                                    // a little naughty to have a side effect but
                                    // feeds straight in to the filter logic and
                                    // saves looping twice.
                                    outage.setDeviceName(info.get(outage.getId()));

                                    // Any device with no info in the site should
                                    // be removed which means any device without a
                                    // name at this point.
                                    return outage.getDeviceName() != null;
                                }).toList()).
                        flatMapCompletable(outages -> apiDao.updateSiteOutages(siteId, outages));
    }


    private final Logger LOG = LoggerFactory.getLogger(OutageAgentService.class);

    private final InterviewTestsMockApiDao apiDao;
}
