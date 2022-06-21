package taxi.modules.recharge;

import taxi.TaxiUtils;
import utils.Utils;

public class CheckBatteryThread extends Thread{

    @Override
    public void run() {
        while (!TaxiUtils.getInstance().quit()){
            if (!TaxiUtils.getInstance().wantsToCharge() &&
                    TaxiUtils.getInstance().getBatteryLevel() < Utils.MIN_BATTERY_LEVEL){
                System.out.println("> [RECH] Taxi needs to charge! ");
                TaxiUtils.getInstance().setWantsToCharge(true);
            }
        }
    }
}
