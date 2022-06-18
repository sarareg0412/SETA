package taxi.modules;

import taxi.Taxi;
import taxi.TaxiUtils;
import unimi.dps.taxi.TaxiRPCServiceOuterClass.*;
import utils.Counter;
import utils.Utils;

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

        System.out.println("> Taxi wants to charge.");
        //Taxi wants to charge
        Ut
        taxiUtils.setRechargeTimestamp(System.currentTimeMillis());
        // Starts Mutual Exclusion to recharge
        RechargeMsg rechargeMsg = RechargeMsg.newBuilder()
                .setId(taxiUtils.getTaxiInfo().getId())
                .setTimestamp(taxiUtils.getRechargeTimestamp())
                .build();

        try {
            notifyAllWantsToRecharge(rechargeMsg);
            waitAllOk();
            System.out.println("> Taxi can now recharge.");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("> Taxi is recharging ...");
        taxiUtils.setCharging(true);
        taxiUtils.setBatteryLevel(taxiUtils.getBatteryLevel() - (int) Math.floor(Utils.getDistanceFromRechargeStation(taxiUtils.getPosition())));
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        taxiUtils.setPosition(Utils.getNearestStationPosition(taxiUtils.getPosition()));
        taxiUtils.setBatteryLevel(100);
        taxiUtils.setCharging(false);
        taxiUtils.setAvailable(true);
        taxiUtils.setWantsToCharge(false);
        System.out.println("> Taxi fully charged.");

        // Sends to
    }

    private void notifyAllWantsToRecharge(RechargeMsg rechargeMsg) throws InterruptedException {
        ArrayList<Taxi> rechargeList = new ArrayList<>(taxiUtils.getTaxisList());
        taxiUtils.setRechargeCounter(new Counter(rechargeList.size()));

        List<AskRechargeThread> threads = new ArrayList<>();

        for (Taxi taxi : rechargeList){
            AskRechargeThread t = new AskRechargeThread(taxi.getTaxiInfo(), rechargeMsg);
            threads.add(t);
        }

        for (AskRechargeThread t : threads){
            t.start();
        }

        for (AskRechargeThread t : threads){
            t.join();
        }
    }

    public void waitAllOk() throws InterruptedException {
        while (taxiUtils.getRechargeCounter().getMaxElements() < taxiUtils.getRechargeCounter().getResponses()){
            taxiUtils.getRechargeCounter().wait();
        }
    }
}
