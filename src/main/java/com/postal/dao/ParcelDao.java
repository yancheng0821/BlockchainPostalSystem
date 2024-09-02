package com.postal.dao;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;
import java.util.Date;

@Setter
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class ParcelDao {

    @Getter
    public enum ParcelStatus {
        PICKUP("From Address"),
        SORTING_LOCAL("Local Sorting Center"),
        IN_TRANSIT("Transit Hub"),
        SORTING_DESTINATION("Destination Sorting Center"),
        OUT_FOR_DELIVERY("Last Mile Delivery Center"),
        DELIVERED("Delivery Address");

        private final String location;

        ParcelStatus(String location) {
            this.location = location;
        }

        public ParcelStatus getNextStatus() {
            switch (this) {
                case PICKUP:
                    return SORTING_LOCAL;
                case SORTING_LOCAL:
                    return IN_TRANSIT;
                case IN_TRANSIT:
                    return SORTING_DESTINATION;
                case SORTING_DESTINATION:
                    return OUT_FOR_DELIVERY;
                case OUT_FOR_DELIVERY:
                    return DELIVERED;
                default:
                    return null;
            }
        }
    }


    private String id;
    private String sender;
    private String receiver;
    private ParcelStatus status;

    private Date createdTime;
    private Date completedTime;
    private Date lastModifiedTime;
    private String qrCode;


    public ParcelDao() {
    }

    public ParcelDao(String id, String sender, String receiver, ParcelStatus status, Date createdTime, Date completedTime, Date lastModifiedTime, String qrCode) {
        this.id = id;
        this.sender = sender;
        this.receiver = receiver;
        this.status = status;
        this.createdTime = createdTime;
        this.completedTime = completedTime;
        this.lastModifiedTime = lastModifiedTime;
        this.qrCode = qrCode;
    }

    @Override
    public String toString() {
        return "ParcelDao{" +
                "id='" + id + '\'' +
                ", sender='" + sender + '\'' +
                ", receiver='" + receiver + '\'' +
                ", status=" + status +
                ", createdTime=" + createdTime +
                ", completedTime=" + completedTime +
                ", lastModifiedTime=" + lastModifiedTime +
                ", qrCode='" + qrCode + '\'' +
                ", location='" + status.getLocation() + '\'' +
                '}';
    }
}
