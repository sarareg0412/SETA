package taxi.modules.recharge;

import taxi.TaxiUtils;
import utils.Utils;

public class CheckBatteryThread extends Thread{

    @Override
    public void run() {
        while (!TaxiUtils.getInstance().quit()){
            // Taxi asked to recharge or its battery is low, but the recharge process hasn't started yet
            if ((TaxiUtils.getInstance().askedToRecharge() || TaxiUtils.getInstance().getBatteryLevel() < Utils.MIN_BATTERY_LEVEL)
                && !TaxiUtils.getInstance().wantsToCharge() ){
                System.out.println("> [RECH] Taxi needs to charge! ");
                // Waits until taxi is available
                while (!TaxiUtils.getInstance().isAvailable()) {
                    try {
                        synchronized (TaxiUtils.getInstance().getAvailableLock()) {
                            TaxiUtils.getInstance().getAvailableLock().wait();
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                System.out.println("> [RECH] Taxi is now free to recharge ");
                TaxiUtils.getInstance().setAskRecharge(false);
                TaxiUtils.getInstance().setAvailable(false);
                TaxiUtils.getInstance().setWantsToCharge(true);
            }
        }
    }
}
