package com.postal.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.postal.config.FabricConfig;
import com.postal.dao.ParcelDao;
import com.postal.dao.ParcelStatusUpdateDao;
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
import java.util.*;

@RestController
@RequestMapping("/api/parcel")
public class ParcelController {

    private static final Logger logger = LogManager.getLogger(ParcelController.class);

    @Autowired
    private FabricClientService fabricClientService;

    @Autowired
    private FabricConfig fabricConfig;

    private static final ObjectMapper objectMapper = new ObjectMapper();


    @PostMapping("/create")
    public Map<String, String> createParcel(@RequestBody ParcelDao parcel) {
        Map<String, String> response = new HashMap<>();
        try {
            // 打印耗时日志
            long startTime = System.currentTimeMillis();
            parcel.setId(UUIDUtils.getUUID());
            parcel.setStatus(ParcelDao.ParcelStatus.PICKUP);
            parcel.setCreatedTime(new Date());
            parcel.setLastModifiedTime(new Date());

            String qrCodeText = "http://localhost:8080/api/parcel/query?id=" + parcel.getId();
            String qrCodeBase64 = QRCodeGeneratorUtil.generateQRCodeImage(qrCodeText);
            parcel.setQrCode(qrCodeBase64);

            fabricClientService.createParcel(parcel);

            response.put("message", "Parcel created successfully");
            response.put("qrCode", qrCodeBase64);
            response.put("id", parcel.getId());
            long endTime = System.currentTimeMillis();
            logger.info("Create parcel cost time: {}ms", (endTime - startTime));
            return response;
        } catch (Exception e) {
            logger.error("Error creating parcel: {}", e.getMessage(), e);
            response.put("message", "Error creating parcel: " + e.getMessage());
            return response;
        }
    }

    @PostMapping("/createBatch")
    public Map<String, Object> createParcels(@RequestBody List<ParcelDao> parcels) {
        Map<String, Object> response = new HashMap<>();
        List<Map<String, String>> parcelResponses = new ArrayList<>();

        try {
            long startTime = System.currentTimeMillis();

            for (ParcelDao parcel : parcels) {
                parcel.setId(UUIDUtils.getUUID());
                parcel.setStatus(ParcelDao.ParcelStatus.PICKUP);
                parcel.setCreatedTime(new Date());
                parcel.setLastModifiedTime(new Date());

                String qrCodeText = "http://localhost:8080/api/parcel/query?id=" + parcel.getId();
                String qrCodeBase64 = QRCodeGeneratorUtil.generateQRCodeImage(qrCodeText);
                parcel.setQrCode(qrCodeBase64);
            }

            fabricClientService.createParcels(parcels);

            for (ParcelDao parcel : parcels) {
                Map<String, String> parcelResponse = new HashMap<>();
                parcelResponse.put("message", "Parcel created successfully");
                parcelResponse.put("qrCode", parcel.getQrCode());
                parcelResponse.put("id", parcel.getId());
                parcelResponses.add(parcelResponse);
            }

            long endTime = System.currentTimeMillis();
            logger.info("Create batch parcels cost time: {}ms", (endTime - startTime));

            response.put("results", parcelResponses);
            return response;
        } catch (Exception e) {
            logger.error("Error creating batch parcels: {}", e.getMessage(), e);
            response.put("message", "Error creating batch parcels: " + e.getMessage());
            return response;
        }
    }




    @PostMapping("/updateStatus")
    public String updateStatus(@RequestBody ParcelStatusUpdateDao parcelStatusUpdate) {
        try {
            // 获取传入的目标状态
            ParcelDao.ParcelStatus newStatus;
            try {
                newStatus = ParcelDao.ParcelStatus.valueOf(parcelStatusUpdate.getStatus().toUpperCase());
            } catch (IllegalArgumentException e) {
                logger.error("Invalid status value: {}", parcelStatusUpdate.getStatus(), e);
                return "Invalid status value: " + parcelStatusUpdate.getStatus();
            }

            // 获取当前包裹状态（假设方法fabricClientService.getCurrentStatus(id)可以获取当前状态）
            ParcelDao.ParcelStatus currentStatus = fabricClientService.getCurrentStatus(parcelStatusUpdate.getId());

            // 检查状态顺序是否合法
            if (currentStatus != null && currentStatus.getNextStatus() != newStatus) {
                logger.error("Invalid status transition from {} to {}", currentStatus, newStatus);
                return "Invalid status transition from " + currentStatus + " to " + newStatus;
            }

            // 如果新状态是DELIVERED，设置完成时间
            if (newStatus == ParcelDao.ParcelStatus.DELIVERED) {
                parcelStatusUpdate.setCompletedTime(new Date());
            }

            // 更新状态
            return fabricClientService.updateStatus(
                    parcelStatusUpdate.getId(),
                    newStatus.name(),
                    newStatus.getLocation(),
                    new Date(),
                    parcelStatusUpdate.getCompletedTime()
            );
        } catch (Exception e) {
            logger.error("Error updating status: {}", e.getMessage(), e);
            return "Error updating status: " + e.getMessage();
        }
    }


    @GetMapping("/queryAll")
    public String queryAllParcels() {
        try {
            String queryAllParcels = fabricClientService.queryAllParcels();

            ArrayNode parcelsArray = objectMapper.createArrayNode();
            if (!queryAllParcels.isEmpty() && !queryAllParcels.equals("null")) {
                //去掉结果中sender和receiver为空的数据
                parcelsArray = (ArrayNode) objectMapper.readTree(queryAllParcels);
                for (Iterator<JsonNode> it = parcelsArray.elements(); it.hasNext(); ) {
                    JsonNode parcel = it.next();
                    if (parcel.get("sender").asText().isEmpty() && parcel.get("receiver").asText().isEmpty()) {
                        it.remove();
                    }
                }
            }

//            //单独查询加密的包裹数据
//            String encryptedParcels = fabricClientService.queryEncryptedParcels();
//
//            //如果不为空
//            if (!encryptedParcels.isEmpty() && !encryptedParcels.equals("null")) {
//                //解密加密的包裹数据
//                ArrayNode encryptedParcelsArray = (ArrayNode) objectMapper.readTree(encryptedParcels);
//                for (JsonNode encryptedParcel : encryptedParcelsArray) {
//                    String encryptedData = encryptedParcel.get("Data").asText();
//                    String decryptedData = fabricClientService.decryptData(fabricConfig.getAlgorithm(), encryptedData);
//                    JsonNode decryptedParcel = objectMapper.readTree(decryptedData);
//                    parcelsArray.add(decryptedParcel);
//                }
//            }
            return objectMapper.writeValueAsString(parcelsArray);

        } catch (Exception e) {
            logger.error("Error querying all parcels: {}", e.getMessage(), e);
            return "Error querying all parcels: " + e.getMessage();
        }
    }

    @GetMapping("/history")
    public String getHistoryForParcel(@RequestParam String id) {
        try {
            return fabricClientService.getHistoryForParcel(id);
        } catch (Exception e) {
            logger.error("Error querying parcel history: {}", e.getMessage(), e);
            return "Error querying parcel history: " + e.getMessage();
        }
    }


    @GetMapping("/query")
    public String queryParcel(@RequestParam String id) {
        try {
            String parcelInfo = fabricClientService.queryParcel(id);
            logger.info("Parcel queried: {}", parcelInfo);

            // 解析 JSON 并去除 qrCode 字段
            JsonNode parcelJson = objectMapper.readTree(parcelInfo);
            if (parcelJson.isObject()) {
                ((ObjectNode) parcelJson).remove("qrCode");
            }

            // 获取历史记录并添加到 JSON
            String historyInfo = fabricClientService.getHistoryForParcel(id);
            ArrayNode historyArray = (ArrayNode) objectMapper.readTree(historyInfo);

            // 去除历史记录中的 qrCode 字段
            for (JsonNode historyItem : historyArray) {
                JsonNode value = historyItem.get("Value");
                if (value != null && value.isObject()) {
                    ((ObjectNode) value).remove("qrCode");
                }
            }

            // 构建 HTML 响应
            StringBuilder htmlResponse = new StringBuilder();
            htmlResponse.append("<html><body>");
            htmlResponse.append("<style>");
            htmlResponse.append("body { font-family: Arial, sans-serif; }");
            htmlResponse.append("h1, h2 { color: #333; }");
            htmlResponse.append("p { line-height: 1.6; }");
            htmlResponse.append(".label { font-weight: bold; }");
            htmlResponse.append("</style>");
            htmlResponse.append("<h1>Parcel Details</h1>");
            htmlResponse.append("<p><span class='label'>Sender:</span> ").append(parcelJson.get("sender").asText()).append("</p>");
            htmlResponse.append("<p><span class='label'>Receiver:</span> ").append(parcelJson.get("receiver").asText()).append("</p>");
            htmlResponse.append("<p><span class='label'>Status:</span> ").append(parcelJson.get("status").asText()).append("</p>");
            htmlResponse.append("<p><span class='label'>Location:</span> ").append(parcelJson.get("location").asText()).append("</p>");
            htmlResponse.append("<p><span class='label'>Created Time:</span> ").append(formatDate(parcelJson.get("createdTime").asText())).append("</p>");
            htmlResponse.append("<p><span class='label'>Last Modified Time:</span> ").append(formatDate(parcelJson.get("lastModifiedTime").asText())).append("</p>");
            htmlResponse.append("<p><span class='label'>Completed Time:</span> ").append(formatDate(parcelJson.get("completedTime").asText())).append("</p>");

            htmlResponse.append("<h2>History</h2>");
            for (JsonNode historyItem : historyArray) {
                htmlResponse.append("<div>");
                htmlResponse.append("<p><span class='label'>Transaction ID:</span> ").append(historyItem.get("TxId").asText()).append("</p>");
                htmlResponse.append("<p><span class='label'>Timestamp:</span> ").append(formatTimestamp(historyItem.get("Timestamp").get("seconds").asLong())).append("</p>");
                JsonNode historyValue = historyItem.get("Value");
                if (historyValue != null && historyValue.isObject()) {
                    htmlResponse.append("<p><span class='label'>Status:</span> ").append(historyValue.get("status").asText()).append("</p>");
                    htmlResponse.append("<p><span class='label'>Location:</span> ").append(historyValue.get("location").asText()).append("</p>");
                }
                htmlResponse.append("</div><hr>");
            }

            htmlResponse.append("</body></html>");

            return htmlResponse.toString();
        } catch (Exception e) {
            logger.error("Error querying parcel: {}", e.getMessage(), e);
            return "<html><body><h1>Error querying parcel: " + e.getMessage() + "</h1></body></html>";
        }
    }


    private String formatDate(String dateStr) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy");
            SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date date = inputFormat.parse(dateStr);
            return outputFormat.format(date);
        } catch (Exception e) {
            return dateStr;
        }
    }

    private String formatTimestamp(long timestamp) {
        Date date = new Date(timestamp * 1000);
        SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return outputFormat.format(date);
    }
}
