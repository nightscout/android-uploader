package com.nightscout.android.upload;

import android.content.Context;

import com.mongodb.MongoClientURI;
import com.nightscout.android.R;
import com.nightscout.android.ToastReceiver;
import com.nightscout.android.events.AndroidEventReporter;
import com.nightscout.core.events.EventReporter;
import com.nightscout.core.events.EventSeverity;
import com.nightscout.core.events.EventType;
import com.nightscout.core.model.v2.Download;
import com.nightscout.core.model.v2.DownloadStatus;
import com.nightscout.core.preferences.NightscoutPreferences;
import com.nightscout.core.upload.BaseUploader;
import com.nightscout.core.upload.MongoUploader;
import com.nightscout.core.upload.RestLegacyUploader;
import com.nightscout.core.upload.RestV1Uploader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import static net.tribe7.common.base.Preconditions.checkNotNull;

public class Uploader {
    protected static final Logger log = LoggerFactory.getLogger(Uploader.class);

    private final List<BaseUploader> uploaders;
    private boolean allUploadersInitalized = true;
    private EventReporter reporter;
    protected NightscoutPreferences preferences;

    public Uploader(Context context, NightscoutPreferences preferences) {
        checkNotNull(context);
        this.preferences = preferences;
        reporter = AndroidEventReporter.getReporter(context);
        uploaders = new ArrayList<>();
        if (preferences.isMongoUploadEnabled()) {
            allUploadersInitalized &= initializeMongoUploader(context, preferences);
        }
        if (preferences.isRestApiEnabled()) {
            allUploadersInitalized &= initializeRestUploaders(context, preferences);
        }
    }

    public boolean areAllUploadersInitalized() {
        return allUploadersInitalized;
    }

    private boolean initializeMongoUploader(Context context, NightscoutPreferences preferences) {
        String dbURI = preferences.getMongoClientUri();
        String collectionName = checkNotNull(preferences.getMongoCollection());
        String dsCollectionName = checkNotNull(preferences.getMongoDeviceStatusCollection());
        MongoClientURI uri;
        try {
            uri = new MongoClientURI(dbURI);
        } catch (IllegalArgumentException e) {
            reporter.report(EventType.UPLOADER, EventSeverity.ERROR,
                            context.getString(R.string.unknown_mongo_host));
            log.error("Error creating mongo client uri for {}.{}", dbURI, e);
            return false;
        } catch (NullPointerException e) {
            reporter.report(EventType.UPLOADER, EventSeverity.ERROR,
                            context.getString(R.string.unknown_mongo_host));
            log.error("Error creating mongo client uri for null value. {}", e);
            return false;
        } catch (StringIndexOutOfBoundsException e) {
            reporter.report(EventType.UPLOADER, EventSeverity.ERROR,
                            context.getString(R.string.unknown_mongo_host));
            log.error("Error creating mongo client uri for null value. {}", e);
            return false;
        }
        uploaders.add(new MongoUploader(preferences, uri, collectionName, dsCollectionName, reporter));
        return true;
    }

    private boolean initializeRestUploaders(Context context, NightscoutPreferences preferences) {
        List<String> baseUrisSetting = preferences.getRestApiBaseUris();
        List<URI> baseUris = new ArrayList<>();
        boolean allInitialized = true;
        for (String baseURLSetting : baseUrisSetting) {
            String baseUriString = baseURLSetting.trim();
            if (baseUriString.isEmpty()) continue;
            try {
                baseUris.add(URI.create(baseUriString));
            } catch (IllegalArgumentException e) {
                reporter.report(EventType.UPLOADER, EventSeverity.ERROR,
                        context.getString(R.string.illegal_rest_url));
                log.error("Error creating rest uri from preferences. {}", e);
            }
        }

        for (URI baseUri : baseUris) {
            if (baseUri.getPath().contains("v1")) {
                try {
                    uploaders.add(new RestV1Uploader(preferences, baseUri));
                } catch (IllegalArgumentException e) {
                    log.error("Error initializing rest v1 uploader. {}", e);
                    allInitialized &= false;
                }
            } else {
                uploaders.add(new RestLegacyUploader(preferences, baseUri));
            }
        }
        return allInitialized;
    }

    public void upload(Download download) {
        if (download == null || download.status != DownloadStatus.SUCCESS
            || download.g4_data == null) {
            return;
        }


    }

    public List<BaseUploader> getUploaders() {
        return uploaders;
    }

    protected boolean areAllUploadersInitialized() {
        return allUploadersInitalized;
    }

}
