package statistics.modules;

import taxi.Taxi;
import taxi.TaxiInfo;
import unimi.dps.taxi.TaxiRPCServiceOuterClass.*;
import utils.Counter;

import java.util.List;

public class AskRechargeThread extends Thread{
    Counter counter;
    TaxiInfo otherTaxiInfo;
    RechargeMsg rechargeMsg;

    public AskRechargeThread(TaxiInfo otherTaxiInfo, Counter counter, RechargeMsg rechargeMsg) {
        this.otherTaxiInfo = otherTaxiInfo;
        this.counter = counter;
        this.rechargeMsg = rechargeMsg;
    }

    @Override
    public void run() {

    }

}
