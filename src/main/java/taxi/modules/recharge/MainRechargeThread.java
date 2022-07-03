package taxi.modules.recharge;

import taxi.Taxi;
import taxi.TaxiInfo;
import taxi.TaxiUtils;
import unimi.dps.ride.Ride;
import unimi.dps.taxi.TaxiRPCServiceOuterClass.*;
import utils.Counter;
import utils.Utils;

import java.util.ArrayList;
import java.util.List;

/* Main Thread holding the recharge process */
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
            //Taxi wants to charge
            TaxiUtils.getInstance().setAvailable(false);
            taxiUtils.setRechargeTimestamp(System.currentTimeMillis());
            // Starts Mutual Exclusion to recharge
            RechargeMsg rechargeMsg = RechargeMsg.newBuilder()
                    .setTimestamp(taxiUtils.getRechargeTimestamp())
                    .setTaxiInfoMsg(TaxiInfoMsg.newBuilder()
                            .setId(taxiUtils.getTaxiInfo().getId())
                            .setPort(taxiUtils.getTaxiInfo().getPort())
                            .setAddress(taxiUtils.getTaxiInfo().getAddress())
                            .setPosition(Ride.RideMsg.PositionMsg.newBuilder()
                                    .setX(taxiUtils.getPosition().getY())
                                    .setY(taxiUtils.getPosition().getY())
                                    .build())
                            .build())
                    .build();

            try {
                notifyAllWantsToRecharge(rechargeMsg);
                waitAllOKs();
                System.out.println("> [RECH] Taxi can now recharge.");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            System.out.println(taxiUtils);
            taxiUtils.setCharging(true);
            taxiUtils.setWantsToCharge(false);

            taxiUtils.setBatteryLevel(taxiUtils.getBatteryLevel() - (int) Math.floor(Utils.getDistanceFromRechargeStation(taxiUtils.getPosition())));
            taxiUtils.setPosition(Utils.getNearestStationPosition(taxiUtils.getPosition()));

            System.out.println("> [RECH] Taxi is recharging ...");
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            taxiUtils.setBatteryLevel(100);
            System.out.println("> [RECH] Taxi fully charged.");
            taxiUtils.setCharging(false);
            taxiUtils.setAvailable(true);
            System.out.println(taxiUtils);
            sendAllPendingOK();
        }
    }

    /* A Thread is created to ask each taxi if the current one can charge */
    private void notifyAllWantsToRecharge(RechargeMsg rechargeMsg) throws InterruptedException {
        ArrayList<Taxi> rechargeList = new ArrayList<>(taxiUtils.getTaxisList());
        taxiUtils.setRechargeCounter(new Counter(rechargeList.size()));

        for (Taxi taxi : rechargeList){
            AskRechargeThread t = new AskRechargeThread(taxi.getTaxiInfo(), rechargeMsg);
            t.start();
        }
    }

    /* Main Thread stops until it receives all OKs needed to recharge */
    public void waitAllOKs() throws InterruptedException {
        synchronized (taxiUtils.getRechargeCounter().getLock()) {
            while (taxiUtils.getRechargeCounter().getResponses() < taxiUtils.getRechargeCounter().getMaxResponses()) {
                taxiUtils.getRechargeCounter().getLock().wait();
            }
        }
    }

    /* If taxi queued some requests while it was recharging, it sends back OK as soon as it finishes to recharge. */
    public void sendAllPendingOK(){
        List<TaxiInfo> list = new ArrayList<>(taxiUtils.getRechargeRequests().getAllAndEmptyQueue());
        OKMsg msg = OKMsg.newBuilder().setOk("OK").setId(TaxiUtils.getInstance().getTaxiInfo().getId()).build();
        if (list.size()>0){
                System.out.println("> [RECH] Sending all pending OK.");
            for (TaxiInfo otherTaxiInfo : list){
                SendOKThread t = new SendOKThread(otherTaxiInfo, msg);
                t.start();
            }
        System.out.println("> [RECH] Sending completed.");
        //Doesn't wait for the threads to finish for it's unnecessary
        }
    }
}
