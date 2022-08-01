package com.alia.nuts.db;

import javax.persistence.*;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "order_tracking")
public class OrderTracking {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id", nullable = false)
    private Integer id;

    @Column(name = "uuid_user", nullable = false)
    private String uuid_user;

    @Column(name = "uuid_session", nullable = false)
    private String uuid_session;

    @Column(name = "shape_layer")
    private String shapeLayer;

    @Column(name = "shape_roi")
    private String shapeRoi;

    @Column(name = "source_mission")
    private String sourceMission;

    @Column(name = "source_data_type")
    private String sourceDataType;

    @Column(name = "start_time")
    private Date startTime;

    @Column(name = "stop_time")
    private Date stopTime;

    @Column(name = "status")
    private String status;

    @Column(name = "ts_estimation_t1")
    private Date tsEstimationT1;

    @Column(name = "ts_estimation_t2")
    private Date tsEstimationT2;

    @Column(name = "ts_estimation_t3")
    private Date tsEstimationT3;

    @Column(name = "ts_elaboration_t1")
    private Date tsElaborationT1;

    @Column(name = "ts_elaboration_t2")
    private Date tsElaborationT2;

    @Column(name = "ts_elaboration_t3")
    private Date tsElaborationT3;

    @Column(name = "ts_download_t1")
    private Date tsDownloadT1;

    @Column(name = "ts_download_t2")
    private Date tsDownloadT2;

    @Column(name = "ts_download_t3")
    private Date tsDownloadT3;

    @Column(name = "weight")
    private Integer weight;

    @Column(name = "download_url")
    private String downloadUrl;

    @Column(name = "invoice_id")
    private String invoiceId;

    @OneToMany(cascade = CascadeType.REMOVE,
            mappedBy = "order",
            orphanRemoval = true)
    private Set<Job> jobs = new HashSet<>();

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getUuid_user() {
        return uuid_user;
    }

    public void setUuid_user(String uuid_user) {
        this.uuid_user = uuid_user;
    }

    public String getUuid_session() {
        return uuid_session;
    }

    public void setUuid_session(String uuid_session) {
        this.uuid_session = uuid_session;
    }

    public String getShapeLayer() {
        return shapeLayer;
    }

    public void setShapeLayer(String shapeLayer) {
        this.shapeLayer = shapeLayer;
    }

    public String getShapeRoi() {
        return shapeRoi;
    }

    public void setShapeRoi(String shapeRoi) {
        this.shapeRoi = shapeRoi;
    }

    public String getSourceMission() {
        return sourceMission;
    }

    public void setSourceMission(String sourceMission) {
        this.sourceMission = sourceMission;
    }

    public String getSourceDataType() {
        return sourceDataType;
    }

    public void setSourceDataType(String sourceDataType) {
        this.sourceDataType = sourceDataType;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Date getStopTime() {
        return stopTime;
    }

    public void setStopTime(Date stopTime) {
        this.stopTime = stopTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Date getTsEstimationT1() {
        return tsEstimationT1;
    }

    public void setTsEstimationT1(Date tsEstimationT1) {
        this.tsEstimationT1 = tsEstimationT1;
    }

    public Date getTsEstimationT2() {
        return tsEstimationT2;
    }

    public void setTsEstimationT2(Date tsEstimationT2) {
        this.tsEstimationT2 = tsEstimationT2;
    }

    public Date getTsEstimationT3() {
        return tsEstimationT3;
    }

    public void setTsEstimationT3(Date tsEstimationT3) {
        this.tsEstimationT3 = tsEstimationT3;
    }

    public Date getTsElaborationT1() {
        return tsElaborationT1;
    }

    public void setTsElaborationT1(Date tsElaborationT1) {
        this.tsElaborationT1 = tsElaborationT1;
    }

    public Date getTsElaborationT2() {
        return tsElaborationT2;
    }

    public void setTsElaborationT2(Date tsElaborationT2) {
        this.tsElaborationT2 = tsElaborationT2;
    }

    public Date getTsElaborationT3() {
        return tsElaborationT3;
    }

    public void setTsElaborationT3(Date tsElaborationT3) {
        this.tsElaborationT3 = tsElaborationT3;
    }

    public Date getTsDownloadT1() {
        return tsDownloadT1;
    }

    public void setTsDownloadT1(Date tsDownloadT1) {
        this.tsDownloadT1 = tsDownloadT1;
    }

    public Date getTsDownloadT2() {
        return tsDownloadT2;
    }

    public void setTsDownloadT2(Date tsDownloadT2) {
        this.tsDownloadT2 = tsDownloadT2;
    }

    public Date getTsDownloadT3() {
        return tsDownloadT3;
    }

    public void setTsDownloadT3(Date tsDownloadT3) {
        this.tsDownloadT3 = tsDownloadT3;
    }

    public Integer getWeight() {
        return weight;
    }

    public void setWeight(Integer weight) {
        this.weight = weight;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    public String getInvoiceId() {
        return invoiceId;
    }

    public void setInvoiceId(String invoiceId) {
        this.invoiceId = invoiceId;
    }

    public Set<Job> getJobs() {
        return jobs;
    }

    public void setJobs(Set<Job> jobs) {
        this.jobs = jobs;
    }

}
