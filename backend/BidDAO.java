import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class BidDAO {
    public static boolean placeBid(int auctionId, String bidder, int amount) throws Exception {
        String checkSql = "SELECT current_bid, status FROM auctions WHERE id = ? FOR UPDATE";
        String updateAuctionSql = "UPDATE auctions SET current_bid = ? WHERE id = ?";
        String insertBidSql = "INSERT INTO bids (auction_id, bidder, amount) VALUES (?, ?, ?)";

        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                int currentBid;
                try (PreparedStatement statement = connection.prepareStatement(checkSql)) {
                    statement.setInt(1, auctionId);
                    try (ResultSet resultSet = statement.executeQuery()) {
                        if (!resultSet.next()) { connection.rollback(); return false; }
                        currentBid = resultSet.getInt("current_bid");
                        if (!"ACTIVE".equals(resultSet.getString("status"))) { connection.rollback(); return false; }
                    }
                }

                if (amount <= currentBid) { connection.rollback(); return false; }

                try (PreparedStatement statement = connection.prepareStatement(updateAuctionSql)) {
                    statement.setInt(1, amount); statement.setInt(2, auctionId);
                    statement.executeUpdate();
                }

                try (PreparedStatement statement = connection.prepareStatement(insertBidSql)) {
                    statement.setInt(1, auctionId); statement.setString(2, bidder); statement.setInt(3, amount);
                    statement.executeUpdate();
                }

                connection.commit();
                return true;
            } catch (Exception e) {
                connection.rollback();
                throw e;
            }
        }
    }
}