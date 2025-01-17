package org.bahmni.module.feedintegration.atomfeed.client;

import org.bahmni.webclients.ClientCookies;
import org.bahmni.webclients.HttpClient;
import org.ict4h.atomfeed.client.repository.AllFeeds;
import org.ict4h.atomfeed.client.repository.jdbc.AllFailedEventsJdbcImpl;
import org.ict4h.atomfeed.client.repository.jdbc.AllMarkersJdbcImpl;
import org.ict4h.atomfeed.client.service.AtomFeedClient;
import org.ict4h.atomfeed.client.service.EventWorker;
import org.ict4h.atomfeed.client.service.FeedClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URISyntaxException;

@Component
@PropertySource("/atomfeed.properties")
public class AtomFeedClientFactory {

    @Autowired
    private AtomFeedHibernateTransactionManager transactionManager;

    @Value("${feed.connectionTimeoutInMilliseconds}")
    private String FEED_CONNECT_TIMEOUT;

    @Value("${feed.replyTimeoutInMilliseconds}")
    private String FEED_REPLY_TIMEOUT;

    @Value("${feed.maxFailedEvents}")
    private String FEED_MAX_FAILED_EVENTS;

    @Value("${feed.failedEventMaxRetry}")
    private String FAILED_EVENT_MAX_RETRY;

    @Value("${openmrs.patient.feed.uri}")
    private String uri;


    public FeedClient get(EventWorker encounterFeedWorker) {
        HttpClient authenticatedWebClient = WebClientFactory.getClient();
        org.bahmni.webclients.ConnectionDetails connectionDetails = ConnectionDetails.get();
        String authUri = connectionDetails.getAuthUrl();
        ClientCookies cookies = getCookies(authenticatedWebClient, authUri);
        return getFeedClient(AtomFeedProperties.getInstance(),
                uri, encounterFeedWorker, cookies);
    }

    private FeedClient getFeedClient(AtomFeedProperties atomFeedProperties, String uri,
                                        EventWorker eventWorker, ClientCookies cookies) {
        try {
            org.ict4h.atomfeed.client.AtomFeedProperties atomFeedClientProperties = createAtomFeedClientProperties(atomFeedProperties);
            
            AllFeeds allFeeds = new AllFeeds(atomFeedClientProperties, cookies);
            AllMarkersJdbcImpl allMarkers = new AllMarkersJdbcImpl(transactionManager);
            AllFailedEventsJdbcImpl allFailedEvents = new AllFailedEventsJdbcImpl(transactionManager);

            return new AtomFeedClient(allFeeds, allMarkers, allFailedEvents,
                    atomFeedClientProperties, transactionManager, new URI(uri), eventWorker);
            
        } catch (URISyntaxException e) {
            throw new RuntimeException(String.format("Is not a valid URI - %s", uri));
        }
    }

    private org.ict4h.atomfeed.client.AtomFeedProperties createAtomFeedClientProperties(AtomFeedProperties atomFeedProperties) {
        org.ict4h.atomfeed.client.AtomFeedProperties feedProperties = new org.ict4h.atomfeed.client.AtomFeedProperties();
        feedProperties.setConnectTimeout(Integer.parseInt(FEED_CONNECT_TIMEOUT));
        feedProperties.setReadTimeout(Integer.parseInt(FEED_REPLY_TIMEOUT));
        feedProperties.setMaxFailedEvents(Integer.parseInt(FEED_MAX_FAILED_EVENTS));
        feedProperties.setFailedEventMaxRetry(Integer.parseInt(FAILED_EVENT_MAX_RETRY));
        feedProperties.setControlsEventProcessing(true);
        return feedProperties;
    }

    private ClientCookies getCookies(HttpClient authenticatedWebClient, String urlString) {
        try {
            return authenticatedWebClient.getCookies(new URI(urlString));
        } catch (URISyntaxException e) {
            throw new RuntimeException("Is not a valid URI - " + urlString);
        }
    }
}
