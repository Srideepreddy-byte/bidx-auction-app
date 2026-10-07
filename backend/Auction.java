public class Auction {
    private int id, startingBid, currentBid, durationHours;
    private String itemName, category, itemCondition, description, pickupLocation, sellerUpi, imagePath, status;

    public Auction(int id, String itemName, String category, String itemCondition, int startingBid, int currentBid, int durationHours, String description, String pickupLocation, String sellerUpi, String imagePath, String status) {
        this.id = id; this.itemName = itemName; this.category = category; this.itemCondition = itemCondition;
        this.startingBid = startingBid; this.currentBid = currentBid; this.durationHours = durationHours;
        this.description = description; this.pickupLocation = pickupLocation; this.sellerUpi = sellerUpi;
        this.imagePath = imagePath; this.status = status;
    }

    public int getId() { return id; }
    public String getItemName() { return itemName; }
    public String getCategory() { return category; }
    public String getItemCondition() { return itemCondition; }
    public int getStartingBid() { return startingBid; }
    public int getCurrentBid() { return currentBid; }
    public int getDurationHours() { return durationHours; }
    public String getDescription() { return description; }
    public String getPickupLocation() { return pickupLocation; }
    public String getSellerUpi() { return sellerUpi; }
    public String getImagePath() { return imagePath; }
    public String getStatus() { return status; }
}