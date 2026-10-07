import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BidXServer {

    public static void main(String[] args) throws IOException {
        WebSocketServer ws = new WebSocketServer(8081);
        ws.start();

        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

        server.createContext("/auction", exchange -> {
            addCorsHeaders(exchange);
            if (exchange.getRequestMethod().equals("OPTIONS")) { sendResponse(exchange, 204, ""); return; }
            if (!exchange.getRequestMethod().equals("POST")) { sendResponse(exchange, 405, "Method not allowed"); return; }

            try {
                String body = readRequestBody(exchange);
                Map<String, String> data = parseFormData(body);

                String itemName = data.get("item-name");
                String category = data.get("category");
                String itemCondition = data.get("condition");
                String startingPrice = data.get("starting-price");
                String duration = data.get("duration");
                String description = data.get("description");
                String pickupLocation = data.get("pickup-location");
                String sellerUpi = data.get("upi");
                String imagePath = data.get("image-path");

                if (isEmpty(itemName) || isEmpty(category) || isEmpty(startingPrice) || isEmpty(duration)) {
                    sendResponse(exchange, 400, "Required fields are missing");
                    return;
                }

                int startingBid = Integer.parseInt(startingPrice);
                int durationHours = Integer.parseInt(duration);

                int auctionId = AuctionDAO.createAuction(
                        itemName, category, itemCondition, startingBid,
                        durationHours, description, pickupLocation, sellerUpi, imagePath
                );

                if (auctionId == -1) {
                    sendResponse(exchange, 500, "Failed to create auction");
                    return;
                }

                AuctionManager.scheduleAuctionClose(auctionId, durationHours);
                sendResponse(exchange, 200, "Auction created successfully. ID: " + auctionId);

            } catch (Exception e) {
                e.printStackTrace();
                sendResponse(exchange, 500, "ERROR: " + e.getClass().getName() + " - " + e.getMessage());
            }
        });

        server.createContext("/auctions", exchange -> {
            addCorsHeaders(exchange);
            if (exchange.getRequestMethod().equals("OPTIONS")) { sendResponse(exchange, 204, ""); return; }
            try {
                List<Auction> auctions = AuctionDAO.getAuctions();
                String json = auctionsToJson(auctions);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                sendResponse(exchange, 200, json);
            } catch (Exception e) {
                e.printStackTrace();
                sendResponse(exchange, 500, "Failed to load auctions: " + e.getMessage());
            }
        });

        server.createContext("/bid", exchange -> {
            addCorsHeaders(exchange);
            if (exchange.getRequestMethod().equals("OPTIONS")) { sendResponse(exchange, 204, ""); return; }
            try {
                String body = readRequestBody(exchange);
                Map<String, String> data = parseFormData(body);
                int auctionId = Integer.parseInt(data.get("auction-id"));
                String bidder = data.get("bidder");
                int amount = Integer.parseInt(data.get("amount"));

                boolean success = BidDAO.placeBid(auctionId, bidder, amount);
                if (success) {
                    sendResponse(exchange, 200, "Bid accepted");
                } else {
                    sendResponse(exchange, 400, "Bid rejected. Too low or closed.");
                }
            } catch (Exception e) {
                e.printStackTrace();
                sendResponse(exchange, 500, "Failed to place bid");
            }
        });

        server.start();
        System.out.println("BidX server running on port 8080");
    }

    private static String readRequestBody(HttpExchange exchange) throws IOException {
        try (InputStream inputStream = exchange.getRequestBody()) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static Map<String, String> parseFormData(String body) {
        Map<String, String> data = new HashMap<>();
        if (body == null || body.isEmpty()) return data;
        for (String pair : body.split("&")) {
            String[] parts = pair.split("=", 2);
            if (parts.length != 2) continue;
            data.put(URLDecoder.decode(parts[0], StandardCharsets.UTF_8),
                     URLDecoder.decode(parts[1], StandardCharsets.UTF_8));
        }
        return data;
    }

    private static boolean isEmpty(String value) { return value == null || value.isBlank(); }

    private static void addCorsHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
    }

    private static void sendResponse(HttpExchange exchange, int status, String response) throws IOException {
        byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
        exchange.sendResponseHeaders(status, responseBytes.length);
        try { 
            exchange.getResponseBody().write(responseBytes); 
        } finally { 
            exchange.getResponseBody().close(); 
        }
    }

    private static String auctionsToJson(List<Auction> auctions) {
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < auctions.size(); i++) {
            Auction a = auctions.get(i);
            json.append("{")
                .append("\"id\":").append(a.getId()).append(",")
                .append("\"itemName\":\"").append(escapeJson(a.getItemName())).append("\",")
                .append("\"category\":\"").append(escapeJson(a.getCategory())).append("\",")
                .append("\"condition\":\"").append(escapeJson(a.getItemCondition())).append("\",")
                .append("\"startingBid\":").append(a.getStartingBid()).append(",")
                .append("\"currentBid\":").append(a.getCurrentBid()).append(",")
                .append("\"durationHours\":").append(a.getDurationHours()).append(",")
                .append("\"description\":\"").append(escapeJson(a.getDescription())).append("\",")
                .append("\"pickupLocation\":\"").append(escapeJson(a.getPickupLocation())).append("\",")
                .append("\"sellerUpi\":\"").append(escapeJson(a.getSellerUpi())).append("\",")
                .append("\"imagePath\":\"").append(escapeJson(a.getImagePath())).append("\",")
                .append("\"status\":\"").append(escapeJson(a.getStatus())).append("\"")
                .append("}");
            if (i < auctions.size() - 1) json.append(",");
        }
        json.append("]");
        return json.toString();
    }

    private static String escapeJson(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }
}