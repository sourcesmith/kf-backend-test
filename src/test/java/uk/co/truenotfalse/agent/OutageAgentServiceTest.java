package uk.co.truenotfalse.agent;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.truenotfalse.agent.OutageAgentService;
import uk.co.truenotfalse.dao.InterviewTestsMockApiDao;
import uk.co.truenotfalse.model.DeviceOutage;


@ExtendWith(MockitoExtension.class)
class OutageAgentServiceTest
{
    @Test
    void updateOutages(@Mock final InterviewTestsMockApiDao apiDao)
    {
        final OffsetDateTime cutoff = OffsetDateTime.parse("2022-01-01T00:00:00.000Z", ISO_OFFSET_DATE_TIME);
        final OffsetDateTime now = OffsetDateTime.now();
        final String siteId = "norwich-pear-tree";

        final List<DeviceOutage> outages = List.of(
          // Should be excluded based on begin time.
          new DeviceOutage("a79fe094-087b-4b1e-ae20-ac4bf7fa429b", cutoff.minusNanos(1000L), now),
          new DeviceOutage("2bf65c61-4452-409d-b416-c58dbd2d7bda", cutoff, now),
          // Should be excluded based on having in info at the site.
          new DeviceOutage("b2c9c71f-3cc6-478b-a86c-80bab857db08", now.minusDays(1L), now),
          new DeviceOutage("5e44374c-ebf9-45c6-82c4-7bd2ae5c3c2e", now.minusDays(1L), now),
          new DeviceOutage("b220b24a-0052-4a1d-9f61-a184950ef060", now.minusDays(1L), null)
        );

        final Map<Object, String> deviceInfo = Map.of("a79fe094-087b-4b1e-ae20-ac4bf7fa429b", "Device 1",
                                                      "2bf65c61-4452-409d-b416-c58dbd2d7bda", "Device 2",
                                                      "5e44374c-ebf9-45c6-82c4-7bd2ae5c3c2e", "Device 3",
                                                      "b220b24a-0052-4a1d-9f61-a184950ef060", "Device 4");

        Mockito.when(apiDao.getOutages()).thenReturn(Single.just(outages));
        Mockito.when(apiDao.getSiteInfo(eq(siteId))).thenReturn(Single.just(deviceInfo));
        Mockito.when(apiDao.updateSiteOutages(eq(siteId), anyList())).thenReturn(Completable.complete());

        final OutageAgentService outageAgent = new OutageAgentService(apiDao);

        outageAgent.updateOutages(siteId, cutoff).blockingAwait();

        final ArgumentCaptor<List<DeviceOutage>> enrichedOutages = ArgumentCaptor.forClass(List.class);
        Mockito.verify(apiDao, Mockito.times(1)).
          updateSiteOutages(eq(siteId), enrichedOutages.capture());

        for(final DeviceOutage outage: enrichedOutages.getValue())
        {
            assertFalse(outage.getBegin().isBefore(cutoff), "An outage before the cutoff was returned.");

            final String deviceName = deviceInfo.get(outage.getId());
            assertNotNull(deviceName, "An outage update for a device without site info was included.");
            assertEquals(deviceName, outage.getDeviceName(), "An outage updated contains an unexpected device name.");
        }
    }
}
