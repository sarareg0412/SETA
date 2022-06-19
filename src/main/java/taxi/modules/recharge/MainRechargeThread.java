package taxi.modules.recharge;

import taxi.Taxi;
import taxi.TaxiInfo;
import taxi.TaxiUtils;
import taxi.modules.SendOKThread;
import unimi.dps.taxi.TaxiRPCServiceOuterClass.*;
import utils.Counter;
import utils.Utils;

import java.util.ArrayList;
import java.util.List;

public class MainRechargeThread extends Thread{
    private TaxiUtils taxiUtils;

    public MainRechargeThread(){
        this.taxiUtils = TaxiUtils.getInstance();
    }

    @Override
    public void run() {
        while (!taxiUtils.quit()) {
            // Waits until taxi wants to charge
            while (!taxiUtils.wantsToCharge()) {
                try {
                    synchronized (taxiUtils.getWantsToChargeLock()) {
                        taxiUtils.getWantsToChargeLock().wait();
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            System.out.println("> [RECH] Taxi wants to charge.");
            //Taxi wants to charge

            taxiUtils.setRechargeTimestamp(System.currentTimeMillis());
            // Starts Mutual Exclusion to recharge
            RechargeMsg rechargeMsg = RechargeMsg.newBuilder()
                    .setTimestamp(taxiUtils.getRechargeTimestamp())
                    .setTaxiInfoMsg(TaxiInfoMsg.newBuilder()
                            .setId(taxiUtils.getTaxiInfo().getId())
                            .setPort(taxiUtils.getTaxiInfo().getPort())
                            .setAddress(taxiUtils.getTaxiInfo().getAddress())
                            .build())
                    .build();

            try {
                notifyAllWantsToRecharge(rechargeMsg);
                waitAllOk();
                System.out.println("> [RECH] Taxi can now recharge.");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            System.out.println(taxiUtils);
            System.out.println("> [RECH] Taxi is recharging ...");
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
            //taxiUtils.setAvailable(true);
            taxiUtils.setWantsToCharge(false);
            System.out.println("> [RECH] Taxi fully charged.");
            System.out.println(taxiUtils);
            sendAllPendingOK();
        }
    }

    private void notifyAllWantsToRecharge(RechargeMsg rechargeMsg) throws InterruptedException {
        ArrayList<Taxi> rechargeList = new ArrayList<>(taxiUtils.getTaxisList());
        taxiUtils.setRechargeCounter(new Counter(rechargeList.size()));

        List<AskRechargeThread> threads = new ArrayList<>();

        for (Taxi taxi : rechargeList){
            AskRechargeThread t = new AskRechargeThread(taxi.getTaxiInfo(), rechargeMsg);
            threads.add(t);
            t.start();
        }
    }

    public void waitAllOk() throws InterruptedException {
        synchronized (taxiUtils.getRechargeCounter().getLock()) {
            while (taxiUtils.getRechargeCounter().getResponses() < taxiUtils.getRechargeCounter().getMaxElements()) {
                taxiUtils.getRechargeCounter().getLock().wait();
            }
        }
    }

    public void sendAllPendingOK(){
        List<TaxiInfo> list = new ArrayList<TaxiInfo>(taxiUtils.getRechargeRequests().getAllAndEmptyQueue());

        if (list.size()>0){
                System.out.println("> [RECH] Sending all pending OK.");
            for (TaxiInfo otherTaxiInfo : list){
                SendOKThread t = new SendOKThread(otherTaxiInfo, Utils.RECHARGE);
                t.start();
            }
        System.out.println("> [RECH] Sending completed.");
        //Doesn't wait for the threads to finish for it's unnecessary
        }
    }
}
