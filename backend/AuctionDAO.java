import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class AuctionDAO {

    public static int createAuction(
            String itemName, String category, String itemCondition,
            int startingBid, int durationHours, String description,
            String pickupLocation, String sellerUpi, String imagePath) throws Exception {

        String sql = """
            INSERT INTO auctions
            (item_name, category, item_condition, starting_bid,
             current_bid, duration_hours, description,
             pickup_location, seller_upi, image_path)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS)) {

            statement.setString(1, itemName);
            statement.setString(2, category);
            statement.setString(3, itemCondition);
            statement.setInt(4, startingBid);
            statement.setInt(5, startingBid);
            statement.setInt(6, durationHours);
            statement.setString(7, description);
            statement.setString(8, pickupLocation);
            statement.setString(9, sellerUpi);
            statement.setString(10, imagePath);

            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        }
        return -1;
    }

    public static List<Auction> getAuctions() throws Exception {
        String sql = "SELECT id, item_name, category, item_condition, starting_bid, current_bid, duration_hours, description, pickup_location, seller_upi, image_path, status FROM auctions ORDER BY id DESC";
        List<Auction> auctions = new ArrayList<>();

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                auctions.add(new Auction(
                        resultSet.getInt("id"),
                        resultSet.getString("item_name") != null ? resultSet.getString("item_name") : "",
                        resultSet.getString("category") != null ? resultSet.getString("category") : "",
                        resultSet.getString("item_condition") != null ? resultSet.getString("item_condition") : "",
                        resultSet.getInt("starting_bid"),
                        resultSet.getInt("current_bid"),
                        resultSet.getInt("duration_hours"),
                        resultSet.getString("description") != null ? resultSet.getString("description") : "",
                        resultSet.getString("pickup_location") != null ? resultSet.getString("pickup_location") : "",
                        resultSet.getString("seller_upi") != null ? resultSet.getString("seller_upi") : "",
                        resultSet.getString("image_path") != null ? resultSet.getString("image_path") : "",
                        resultSet.getString("status") != null ? resultSet.getString("status") : "ACTIVE"
                ));
            }
        }
        return auctions;
    }

    public static Auction getAuctionById(int auctionId) throws Exception {
        String sql = "SELECT id, item_name, category, item_condition, starting_bid, current_bid, duration_hours, description, pickup_location, seller_upi, image_path, status FROM auctions WHERE id = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, auctionId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return new Auction(
                            resultSet.getInt("id"),
                            resultSet.getString("item_name") != null ? resultSet.getString("item_name") : "",
                            resultSet.getString("category") != null ? resultSet.getString("category") : "",
                            resultSet.getString("item_condition") != null ? resultSet.getString("item_condition") : "",
                            resultSet.getInt("starting_bid"),
                            resultSet.getInt("current_bid"),
                            resultSet.getInt("duration_hours"),
                            resultSet.getString("description") != null ? resultSet.getString("description") : "",
                            resultSet.getString("pickup_location") != null ? resultSet.getString("pickup_location") : "",
                            resultSet.getString("seller_upi") != null ? resultSet.getString("seller_upi") : "",
                            resultSet.getString("image_path") != null ? resultSet.getString("image_path") : "",
                            resultSet.getString("status") != null ? resultSet.getString("status") : "ACTIVE"
                    );
                }
            }
        }
        return null;
    }

    public static void closeAuction(int auctionId) throws Exception {
        String sql = "UPDATE auctions SET status = 'CLOSED' WHERE id = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, auctionId);
            statement.executeUpdate();
        }
    }
}