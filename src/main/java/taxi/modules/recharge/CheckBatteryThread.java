package taxi.modules.recharge;

import taxi.TaxiUtils;
import utils.Utils;

public class CheckBatteryThread extends Thread{

    @Override
    public void run() {
        while (!TaxiUtils.getInstance().quit()){
            if (TaxiUtils.getInstance().getBatteryLevel() < Utils.MIN_BATTERY_LEVEL){
                System.out.println(TaxiUtils.getInstance());
                TaxiUtils.getInstance().setWantsToCharge(true);
            }
        }
    }
}
