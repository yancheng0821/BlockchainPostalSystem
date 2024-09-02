package com.postal.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.postal.config.FabricConfig;
import com.postal.dao.ParcelDao;
import com.postal.service.FabricClientService;
import com.postal.utils.QRCodeGeneratorUtil;
import com.postal.utils.UUIDUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/parcelEncryption")
public class ParcelEncryptionController {

    private static final Logger logger = LogManager.getLogger(ParcelController.class);

    @Autowired
    private FabricClientService fabricClientService;

    @Autowired
    private FabricConfig fabricConfig;

    private static final ObjectMapper objectMapper = new ObjectMapper();



    @PostMapping("/createWithEncryption")
    public Map<String, String> createParcelWithEncryption(@RequestBody ParcelDao parcel) {
        Map<String, String> response = new HashMap<>();
        try {
            //打印耗时日志
            long startTime = System.currentTimeMillis();
            parcel.setId(UUIDUtils.getUUID());
            parcel.setStatus(ParcelDao.ParcelStatus.PICKUP);
            parcel.setCreatedTime(new Date());
            parcel.setLastModifiedTime(new Date());

            String qrCodeText = "http://localhost:8080/api/parcel/query?id=" + parcel.getId();
            String qrCodeBase64 = QRCodeGeneratorUtil.generateQRCodeImage(qrCodeText);
            parcel.setQrCode(qrCodeBase64);

            // Convert parcel object to JSON string
            ObjectMapper mapper = new ObjectMapper();
            mapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ"));
            String parcelJson = mapper.writeValueAsString(parcel);

            logger.info("Parcel JSON: {}", parcelJson);

            // Encrypt the data
            String encryptedData = fabricClientService.encryptData(fabricConfig.getAlgorithm(), parcelJson);
            // 打印加密完成时间
            long encryptEndTime = System.currentTimeMillis();
            logger.info("Encrypt data cost time: {}ms", (encryptEndTime - startTime));


            System.out.println("Encrypted data: " + encryptedData);
            // Create parcel with encrypted data
            fabricClientService.createParcelWithEncryption(parcel.getId(), encryptedData);

            response.put("message", "Parcel created successfully with encryption");
            response.put("qrCode", qrCodeBase64);
            response.put("id", parcel.getId());
            long endTime = System.currentTimeMillis();
            logger.info("Create parcel with encryption cost time: {}ms", (endTime - encryptEndTime));
            return response;
        } catch (Exception e) {
            logger.error("Error creating parcel with encryption: {}", e.getMessage(), e);
            response.put("message", "Error creating parcel with encryption: " + e.getMessage());
            return response;
        }
    }


    @GetMapping("/queryEncryptedParcel")
    public ResponseEntity<String> queryEncryptedParcel(@RequestParam String id) {
        try {
            // 查询加密的快递信息
            String encryptedParcelInfo = fabricClientService.queryParcel("encrypted_" + id);
            logger.info("Encrypted parcel queried: {}", encryptedParcelInfo);

            // 调用加解密链码进行解密
            String decryptedParcelInfo = fabricClientService.decryptData(fabricConfig.getAlgorithm(), encryptedParcelInfo);
            logger.info("Decrypted parcel info: {}", decryptedParcelInfo);

            // 解析解密后的 JSON 信息
            JsonNode parcelJson = objectMapper.readTree(decryptedParcelInfo);
            logger.info("Decrypted parcel JSON: {}", parcelJson.toPrettyString());

            return ResponseEntity.ok(parcelJson.toPrettyString());
        } catch (Exception e) {
            logger.error("Error querying parcel: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error querying parcel: " + e.getMessage());
        }
    }


}

