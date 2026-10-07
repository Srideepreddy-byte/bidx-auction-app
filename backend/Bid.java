public class Bid {
    private int id, auctionId, amount;
    private String bidder;

    public Bid(int id, int auctionId, String bidder, int amount) {
        this.id = id; this.auctionId = auctionId; this.bidder = bidder; this.amount = amount;
    }

    public int getId() { return id; }
    public int getAuctionId() { return auctionId; }
    public String getBidder() { return bidder; }
    public int getAmount() { return amount; }
}