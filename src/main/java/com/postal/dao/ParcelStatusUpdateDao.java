package com.postal.dao;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Setter
@Getter
public class ParcelStatusUpdateDao {

    private String id;
    private String status;
    private Date lastModifyTime;
    private Date completedTime;

    public ParcelStatusUpdateDao() {
    }

    public ParcelStatusUpdateDao(String id, String status, Date lastModifyTime, Date completedTime) {
        this.id = id;
        this.status = status;
        this.lastModifyTime = lastModifyTime;
        this.completedTime = completedTime;
    }

    @Override
    public String toString() {
        return "ParcelStatusUpdateDao{" +
                "id='" + id + '\'' +
                ", status='" + status + '\'' +
                ", timestamp='" + lastModifyTime + '\'' +
                ", completedTime='" + completedTime + '\'' +
                '}';
    }
}
