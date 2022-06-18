package statistics.modules;

import taxi.Taxi;
import taxi.TaxiUtils;
import unimi.dps.taxi.TaxiRPCServiceOuterClass.*;
import utils.Counter;

import java.util.ArrayList;
import java.util.List;

public class MainRechargeThread extends Thread{
    private TaxiUtils taxiUtils;
    private Counter counter;                        // Recharge counter

    public MainRechargeThread(){
        this.taxiUtils = TaxiUtils.getInstance();
    }

    @Override
    public void run() {
        // Waits until taxi wants to charge
        while (! taxiUtils.wantsToCharge()){
            try {
                synchronized (taxiUtils.getWantsToChargeLock()){
                    taxiUtils.getWantsToChargeLock().wait();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        // Starts Mutual Exclusion to recharge
        RechargeMsg rechargeMsg = RechargeMsg.newBuilder()
                .setId(taxiUtils.getTaxiInfo().getId())
                .setTimestamp(System.currentTimeMillis())
                .build();

        try {
            notifyAllWantsToRecharge(rechargeMsg);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    private void notifyAllWantsToRecharge(RechargeMsg rechargeMsg) throws InterruptedException {
        ArrayList<Taxi> rechargeList = new ArrayList<>(taxiUtils.getTaxisList());
        counter = new Counter(rechargeList.size());

        List<AskRechargeThread> threads = new ArrayList<>();

        for (Taxi taxi : rechargeList){
            AskRechargeThread t = new AskRechargeThread(taxi.getTaxiInfo(), counter, rechargeMsg);
            threads.add(t);
        }

        for (AskRechargeThread t : threads){
            t.start();
        }

        for (AskRechargeThread t : threads){
            t.join();
        }

    }

}
