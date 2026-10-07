import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AuctionManager {
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    public static void scheduleAuctionClose(int auctionId, int durationHours) {
        scheduler.schedule(() -> {
            try { AuctionDAO.closeAuction(auctionId); } catch (Exception e) { e.printStackTrace(); }
        }, durationHours, TimeUnit.HOURS);
    }
}