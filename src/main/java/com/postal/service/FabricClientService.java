package com.postal.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.postal.dao.ParcelDao;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hyperledger.fabric.gateway.Contract;
import org.hyperledger.fabric.gateway.ContractException;
import org.hyperledger.fabric.gateway.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeoutException;

@Service
public class FabricClientService {

    private static final Logger logger = LogManager.getLogger(FabricClientService.class);

    @Autowired
    @Qualifier("org1MyccContract")
    private Contract org1MyccContract;

    @Autowired
    @Qualifier("org1Mycc2Contract")
    private Contract org1Mycc2Contract;

    @Autowired
    @Qualifier("org1Mycc3Contract")
    private Contract org1Mycc3Contract;

    @Autowired
    @Qualifier("org2MyccContract")
    private Contract org2MyccContract;

    @Autowired
    @Qualifier("org2Mycc2Contract")
    private Contract org2Mycc2Contract;

    @Autowired
    @Qualifier("org2Mycc3Contract")
    private Contract org2Mycc3Contract;

    private Boolean org1ContractFlag = true;

    public void createParcel(ParcelDao parcel) throws ContractException, TimeoutException, InterruptedException {
        try {
            logger.info("Creating parcel: {}", parcel);

            Transaction transaction = null;
            //Implement polling to call contracts between two organizations
            if(org1ContractFlag){
                transaction = org1MyccContract.createTransaction("createParcel");
                org1ContractFlag = false;
            } else {
                transaction = org2MyccContract.createTransaction("createParcel");
                org1ContractFlag = true;
            }

            Date completed = parcel.getCompletedTime();
            String completedTimeString = completed != null ? completed.toString() : " ";
            byte[] result = transaction.submit(parcel.getId(), parcel.getSender(), parcel.getReceiver(), parcel.getStatus().name(), parcel.getStatus().getLocation(), parcel.getCreatedTime().toString(), completedTimeString, parcel.getLastModifiedTime().toString(), parcel.getQrCode());
            logger.info("Parcel created: {}", new String(result, StandardCharsets.UTF_8));

            // 调用审计链码记录日志
            auditLog(parcel.getId(),"CREATE_PARCEL", parcel.getSender(), parcel.getReceiver(), parcel.getStatus().name());

        } catch (Exception e) {
            logger.error("Error creating parcel: {}", e.getMessage(), e);
            throw e;
        }
    }

    public void createParcels(List<ParcelDao> parcels) throws ContractException, TimeoutException, InterruptedException, JsonProcessingException {
        List<String> parcelJsons = new ArrayList<>();
        for (ParcelDao parcel : parcels) {
            Map<String, String> parcelMap = new HashMap<>();
            parcelMap.put("id", parcel.getId());
            parcelMap.put("sender", parcel.getSender());
            parcelMap.put("receiver", parcel.getReceiver());
            parcelMap.put("status", parcel.getStatus().name());
            parcelMap.put("createdTime", parcel.getCreatedTime().toString());
            parcelMap.put("completedTime", parcel.getCompletedTime() != null ? parcel.getCompletedTime().toString() : " ");
            parcelMap.put("lastModifiedTime", parcel.getLastModifiedTime().toString());
            String parcelJson = new ObjectMapper().writeValueAsString(parcelMap);
            parcelJsons.add(parcelJson);
        }

        //实现轮询调用两个组织的合约
        if(org1ContractFlag){
            Transaction transaction = org1MyccContract.createTransaction("createParcelsBatch");
            transaction.submit(parcelJsons.toArray(new String[0]));
            org1ContractFlag = false;
        } else {
            Transaction transaction = org2MyccContract.createTransaction("createParcelsBatch");
            transaction.submit(parcelJsons.toArray(new String[0]));
            org1ContractFlag = true;
        }

    }



    public String updateStatus(String id, String status, String location, Date lastModifyTime, Date completeTime) throws ContractException, TimeoutException, InterruptedException {
        try {
            Transaction transaction = org2MyccContract.createTransaction("updateStatus");
            byte[] result = transaction.submit(id, status, location, lastModifyTime.toString(), completeTime != null ? completeTime.toString() : " ");
            String response = new String(result, StandardCharsets.UTF_8);

            // 调用审计链码记录日志
            auditLog(id,"UPDATE_STATUS", id, status, location);

            return response;
        } catch (Exception e) {
            logger.error("Error updating status: {}", e.getMessage(), e);
            throw e;
        }
    }

    public ParcelDao.ParcelStatus getCurrentStatus(String id) throws ContractException, IOException {
        try {
            byte[] result = org1MyccContract.evaluateTransaction("getCurrentStatus", id);
            String status = new String(result, StandardCharsets.UTF_8);
            return ParcelDao.ParcelStatus.valueOf(status);
        } catch (Exception e) {
            logger.error("Error getting current status for parcel ID: {}", id, e);
            throw e;
        }
    }

    public String queryParcel(String id) throws ContractException, IOException {
        try {
            byte[] result = org2MyccContract.evaluateTransaction("queryParcel", id);
            return new String(result, StandardCharsets.UTF_8);
        } catch (Exception e) {
            logger.error("Error querying parcel: {}", e.getMessage(), e);
            throw e;
        }
    }

    public String queryAllParcels() throws ContractException, IOException {
        try {
            byte[] result = org1MyccContract.evaluateTransaction("queryAllParcels");
            return new String(result, StandardCharsets.UTF_8);
        } catch (Exception e) {
            logger.error("Error querying all parcels: {}", e.getMessage(), e);
            throw e;
        }
    }

    public String queryEncryptedParcels() throws ContractException, IOException {
        try {
            byte[] result = org1MyccContract.evaluateTransaction("queryEncryptedParcels");
            return new String(result, StandardCharsets.UTF_8);
        } catch (Exception e) {
            logger.error("Error querying all encrypted parcels: {}", e.getMessage(), e);
            throw e;
        }
    }


    public String getHistoryForParcel(String id) throws ContractException, IOException {
        try {
            byte[] result = org1MyccContract.evaluateTransaction("getHistoryForParcel", id);
            return new String(result, StandardCharsets.UTF_8);
        } catch (Exception e) {
            logger.error("Error querying parcel history: {}", e.getMessage(), e);
            throw e;
        }
    }

    public void createParcelWithEncryption(String uid, String encryptedData) throws ContractException, TimeoutException, InterruptedException {
        try {
            logger.info("Creating parcel with encryption, uid: {}", uid);
            Transaction transaction = null;
            //实现轮询调用两个组织的合约
            if(org1ContractFlag){
                transaction = org1MyccContract.createTransaction("createParcelWithEncryption");
                org1ContractFlag = false;
            } else {
                transaction = org1MyccContract.createTransaction("createParcelWithEncryption");
                org1ContractFlag = true;
            }
            byte[] result = transaction.submit(uid, encryptedData);
            logger.info("Parcel created with encryption: {}", new String(result, StandardCharsets.UTF_8));

        } catch (Exception e) {
            logger.error("Error creating parcel with encryption: {}", e.getMessage(), e);
            throw e;
        }
    }

    public String encryptData(String algorithm, String data) throws ContractException, TimeoutException, InterruptedException {
        try {
            logger.info("Algorithm: {} Encrypting data: {} ", algorithm, data);
            Transaction transaction = null;
            //实现轮询调用两个组织的合约
            if(org1ContractFlag){
                transaction = org1Mycc2Contract.createTransaction("encrypt");
                org1ContractFlag = false;
            } else {
                transaction = org2Mycc2Contract.createTransaction("encrypt");
                org1ContractFlag = true;
            }
            byte[] result = transaction.submit(algorithm, data);
            logger.info("result: {}", new String(result, StandardCharsets.UTF_8));
            return new String(result, StandardCharsets.UTF_8);
        } catch (Exception e) {
            logger.error("Error encrypting data: {}", e.getMessage(), e);
            throw e;
        }
    }

    public String decryptData(String algorithm, String data) throws ContractException, TimeoutException, InterruptedException {
        try {
            Transaction transaction = org2Mycc2Contract.createTransaction("decrypt");
            byte[] result = transaction.submit(algorithm, data);
            return new String(result, StandardCharsets.UTF_8);
        } catch (Exception e) {
            logger.error("Error decrypting data: {}", e.getMessage(), e);
            throw e;
        }
    }

    private void auditLog(String parcelId, String operationType, String... params) throws ContractException, TimeoutException, InterruptedException {
        try {
            Transaction transaction = org1Mycc3Contract.createTransaction("auditLog");
            String[] args = new String[params.length + 2];
            args[0] = parcelId;
            args[1] = operationType;
            System.arraycopy(params, 0, args, 2, params.length);
            byte[] result = transaction.submit(args);
            logger.info("Audit log for {} with params {}: {}", operationType, params, new String(result, StandardCharsets.UTF_8));
        } catch (Exception e) {
            logger.error("Error creating audit log for {} with params {}: {}", operationType, params, e.getMessage());
            throw e;
        }
    }


    public String queryAllAuditLogs() throws ContractException, TimeoutException, InterruptedException {
        try {
            Transaction transaction = org1Mycc3Contract.createTransaction("queryAllLogs");
            byte[] result = transaction.evaluate();
            return new String(result, StandardCharsets.UTF_8);
        } catch (Exception e) {
            logger.error("Error querying audit logs: {}", e.getMessage(), e);
            throw e;
        }
    }

    public String queryAuditLogHistory(String parcelId) throws ContractException, IOException {
        try {
            byte[] result = org1Mycc3Contract.evaluateTransaction("getHistory", parcelId);
            return new String(result, StandardCharsets.UTF_8);
        } catch (Exception e) {
            logger.error("Error querying audit log history for parcelId: {}", parcelId, e);
            throw e;
        }
    }



}
