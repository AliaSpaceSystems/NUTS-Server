package com.alia.nuts.db;

import java.util.Date;

public interface TrackingData {
    String getStatus();
    Date getTsEstimationT1();
    Date getTsEstimationT2();
    Date getTsEstimationT3();
    Date getTsElaborationT1();
    Date getTsElaborationT2();
    Date getTsElaborationT3();
    Date getTsDownloadT1();
    Date getTsDownloadT2();
    Date getTsDownloadT3();
    Integer getWeight();
    String getDownloadUrl();
    String getInvoiceId();
}
