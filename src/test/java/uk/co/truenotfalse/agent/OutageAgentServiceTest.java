package uk.co.truenotfalse.agent;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.truenotfalse.dao.InterviewTestsMockApiDao;
import uk.co.truenotfalse.model.DeviceOutage;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;


@ExtendWith(MockitoExtension.class)
@DisplayName("OutageAgentService tests")
class OutageAgentServiceTest {

    private final String siteId = "norwich-pear-tree";
    private final Map<Object, String> deviceInfo = Map.of("a79fe094-087b-4b1e-ae20-ac4bf7fa429b", "Device 1",
            "2bf65c61-4452-409d-b416-c58dbd2d7bda", "Device 2",
            "5e44374c-ebf9-45c6-82c4-7bd2ae5c3c2e", "Device 3",
            "b220b24a-0052-4a1d-9f61-a184950ef060", "Device 4");
    private final OffsetDateTime cutoff = OffsetDateTime.parse("2022-01-01T00:00:00.000Z", ISO_OFFSET_DATE_TIME);
    private final OffsetDateTime now = OffsetDateTime.now();
    private final List<DeviceOutage> happyOutages = List.of(
            new DeviceOutage("2bf65c61-4452-409d-b416-c58dbd2d7bda", cutoff, now),
            new DeviceOutage("5e44374c-ebf9-45c6-82c4-7bd2ae5c3c2e", now.minusDays(1L), now),
            new DeviceOutage("b220b24a-0052-4a1d-9f61-a184950ef060", now.minusDays(1L), null)
    );


    @Test
    @DisplayName("The outage, site info and update endpoints are requested when an update is invoked")
    void updateOutages(@Mock final InterviewTestsMockApiDao apiDao) {
        Mockito.when(apiDao.getSiteInfo(eq(siteId))).thenReturn(Single.just(deviceInfo));
        Mockito.when(apiDao.getOutages()).thenReturn(Single.just(happyOutages));
        Mockito.when(apiDao.updateSiteOutages(eq(siteId), anyList())).thenReturn(Completable.complete());

        new OutageAgentService(apiDao).updateOutages(siteId, cutoff).blockingAwait();

        Mockito.verify(apiDao).getOutages();
        Mockito.verify(apiDao).getSiteInfo(eq(siteId));
        Mockito.verify(apiDao).updateSiteOutages(eq(siteId), anyList());
    }


    @Test
    @DisplayName("Outage updates include the expected site name")
    void updateOutagesSiteNameAttached(@Mock final InterviewTestsMockApiDao apiDao) {
        Mockito.when(apiDao.getSiteInfo(eq(siteId))).thenReturn(Single.just(deviceInfo));
        Mockito.when(apiDao.getOutages()).thenReturn(Single.just(happyOutages));
        Mockito.when(apiDao.updateSiteOutages(eq(siteId), anyList())).thenReturn(Completable.complete());

        new OutageAgentService(apiDao).updateOutages(siteId, cutoff).blockingAwait();

        final ArgumentCaptor<List<DeviceOutage>> enrichedOutages = ArgumentCaptor.forClass(List.class);
        Mockito.verify(apiDao).updateSiteOutages(eq(siteId), enrichedOutages.capture());

        for (final DeviceOutage outage : enrichedOutages.getValue()) {
            final String deviceName = deviceInfo.get(outage.getId());
            assertEquals(deviceName, outage.getDeviceName(), "An outage updated contains an unexpected device name.");
        }
    }


    @Test
    @DisplayName("Outages before a cutoff are excluded from updates")
    void updateOutagesCutoffApplied(@Mock final InterviewTestsMockApiDao apiDao) {
        final List<DeviceOutage> outages = new ArrayList<>(happyOutages);
        outages.add(new DeviceOutage("a79fe094-087b-4b1e-ae20-ac4bf7fa429b", cutoff.minusNanos(1000L), now));

        Mockito.when(apiDao.getSiteInfo(eq(siteId))).thenReturn(Single.just(deviceInfo));
        Mockito.when(apiDao.getOutages()).thenReturn(Single.just(happyOutages));
        Mockito.when(apiDao.updateSiteOutages(eq(siteId), anyList())).thenReturn(Completable.complete());

        new OutageAgentService(apiDao).updateOutages(siteId, cutoff).blockingAwait();

        final ArgumentCaptor<List<DeviceOutage>> enrichedOutages = ArgumentCaptor.forClass(List.class);
        Mockito.verify(apiDao).updateSiteOutages(eq(siteId), enrichedOutages.capture());

        for (final DeviceOutage outage : enrichedOutages.getValue()) {
            assertFalse(outage.getBegin().isBefore(cutoff), "An outage before the cutoff was returned.");
        }
    }


    @Test
    @DisplayName("Sites with no info are excluded from outage updates")
    void updateOutagesNoSiteInfoExcluded(@Mock final InterviewTestsMockApiDao apiDao) {
        final List<DeviceOutage> outages = new ArrayList<>(happyOutages);
        outages.add(new DeviceOutage("b2c9c71f-3cc6-478b-a86c-80bab857db08", now.minusDays(1L), now));

        Mockito.when(apiDao.getSiteInfo(eq(siteId))).thenReturn(Single.just(deviceInfo));
        Mockito.when(apiDao.getOutages()).thenReturn(Single.just(happyOutages));
        Mockito.when(apiDao.updateSiteOutages(eq(siteId), anyList())).thenReturn(Completable.complete());

        new OutageAgentService(apiDao).updateOutages(siteId, cutoff).blockingAwait();

        final ArgumentCaptor<List<DeviceOutage>> enrichedOutages = ArgumentCaptor.forClass(List.class);
        Mockito.verify(apiDao).updateSiteOutages(eq(siteId), enrichedOutages.capture());

        for (final DeviceOutage outage : enrichedOutages.getValue()) {
            assertTrue(deviceInfo.containsKey(outage.getId()), "An outage update for a device without site info was included.");
        }
    }
}
