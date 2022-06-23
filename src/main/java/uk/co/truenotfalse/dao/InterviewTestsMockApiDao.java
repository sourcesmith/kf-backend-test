package uk.co.truenotfalse.dao;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;
import uk.co.truenotfalse.model.DeviceOutage;

import java.util.List;
import java.util.Map;


/**
 * Interface for a client accessing endpoints of the remote Interview Tests Mock API.
 */
public interface InterviewTestsMockApiDao {
    /**
     * Gets a list of know device outages in the system.
     *
     * @return A future result of the list of know device outages.
     */
    Single<List<DeviceOutage>> getOutages();

    /**
     * Gets the device info for the named site.  This is, currently, simply device names mapped to their IDs.
     *
     * @param siteId The ID of the site e.g. kingfisher
     * @return A map of device IDs to their names.
     */
    Single<Map<Object, String>> getSiteInfo(String siteId);

    /**
     * Updates the outage information with names for the named site.
     *
     * @param siteId        The ID of the site to update.
     * @param outageUpdates The device information to update the site with.
     */
    Completable updateSiteOutages(final String siteId, final List<DeviceOutage> outageUpdates);
}
