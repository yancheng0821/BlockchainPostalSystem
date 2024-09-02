package com.postal.service;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.postal.utils.ParcelStatusDeserializer;
import com.postal.utils.QRCodeGeneratorUtil;
import com.postal.utils.UUIDUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.postal.dao.ParcelDao;
import com.postal.dao.ParcelDao.ParcelStatus;
import com.postal.service.FabricClientService;

@Component
public class ParcelStatusScheduler {

    private static final int MAX_THREADS = 5;
    private static final int INITIAL_DELAY = 0;
    private static final int PERIOD = 10; // 每10秒检查一次

    @Autowired
    private FabricClientService fabricClientService;

    private final ScheduledExecutorService scheduler;
    private final ObjectMapper objectMapper;

    public ParcelStatusScheduler() {
        this.scheduler = Executors.newScheduledThreadPool(MAX_THREADS);
        this.objectMapper = new ObjectMapper();
        configureObjectMapper();
    }

    private void configureObjectMapper() {
        SimpleModule module = new SimpleModule();
        module.addDeserializer(ParcelStatus.class, new ParcelStatusDeserializer());
        objectMapper.registerModule(module);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.setDateFormat(new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy"));
    }

    public void start() {
        Runnable task = () -> {
            try {
                // 创建新的快递
                createNewParcel();

                // 获取所有快递并处理状态
                String parcelsJson = fabricClientService.queryAllParcels();
                List<ParcelDao> parcels = parseParcels(parcelsJson);
                parcels.stream()
                        .filter(parcel -> !ParcelStatus.DELIVERED.name().equals(parcel.getStatus().name()))
                        .forEach(this::processParcel);
            } catch (Exception e) {
                e.printStackTrace();
            }
        };

        scheduler.scheduleAtFixedRate(task, INITIAL_DELAY, PERIOD, TimeUnit.SECONDS);
    }

    private void createNewParcel() {
        Runnable task = () -> {
            try {
                ParcelDao parcel = new ParcelDao();

                parcel.setId(UUIDUtils.getUUID());
                parcel.setSender(generateRandomString(3, 5));
                parcel.setReceiver(generateRandomString(3, 5));
                parcel.setCreatedTime(new Date());
                parcel.setLastModifiedTime(new Date());
                parcel.setStatus(ParcelStatus.PICKUP);

                String qrCodeText = "http://localhost:8080/api/parcel/query?id=" + parcel.getId();
                String qrCodeBase64 = QRCodeGeneratorUtil.generateQRCodeImage(qrCodeText);
                parcel.setQrCode(qrCodeBase64);

                // 调用服务创建快递
                fabricClientService.createParcel(parcel);
            } catch (Exception e) {
                e.printStackTrace();
            }
        };

        scheduler.submit(task);
    }

    private void processParcel(ParcelDao parcel) {
        Runnable task = () -> {
            try {
                ParcelStatus currentStatus = ParcelStatus.valueOf(parcel.getStatus().name());
                ParcelStatus nextStatus = currentStatus.getNextStatus();
                if (nextStatus != null) {
                    Date now = new Date();
                    Date completedTime = nextStatus == ParcelStatus.DELIVERED ? now : null;
                    fabricClientService.updateStatus(parcel.getId(), nextStatus.name(), nextStatus.getLocation(), now, completedTime);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        };

        scheduler.submit(task);
    }

    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(60, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
        }
    }

    private List<ParcelDao> parseParcels(String parcelsJson) {
        try {
            return objectMapper.readValue(parcelsJson, new TypeReference<List<ParcelDao>>() {});
        } catch (IOException e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    private String generateRandomString(int minLength, int maxLength) {
        String characters = "abcdefghijklmnopqrstuvwxyz";
        Random random = new Random();
        int length = random.nextInt(maxLength - minLength + 1) + minLength;
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(characters.charAt(random.nextInt(characters.length())));
        }
        return sb.toString();
    }
}
