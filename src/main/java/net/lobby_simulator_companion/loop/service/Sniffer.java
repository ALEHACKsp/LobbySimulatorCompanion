package net.lobby_simulator_companion.loop.service;

import org.pcap4j.core.*;
import org.pcap4j.packet.IpV4Packet;
import org.pcap4j.packet.Packet;
import org.pcap4j.packet.UdpPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.stream.Collectors;

public class Sniffer implements Runnable {

    private static final int CAPTURED_PACKET_SIZE = 1024;
    private static final int CLEANER_POLL_MS = 60000;
    private static final int PEER_TIMEOUT_MS = 5000;

    private static final Logger logger = LoggerFactory.getLogger(Sniffer.class);

    private InetAddress localAddr;
    private SnifferListener snifferListener;
    private PcapNetworkInterface networkInterface;
    private HashMap<Inet4Address, Long> active = new HashMap<>();
    private PcapHandle handle = null;


    public Sniffer(InetAddress localAddr, SnifferListener snifferListener) throws PcapNativeException, NotOpenException, InvalidNetworkInterfaceException {
        this.localAddr = localAddr;
        this.snifferListener = snifferListener;
        initNetworkInterface();
        startConnectionCleaner();
    }


    private void initNetworkInterface() throws PcapNativeException, InvalidNetworkInterfaceException, NotOpenException {
        networkInterface = Pcaps.getDevByAddress(localAddr);
        if (networkInterface == null) {
            throw new InvalidNetworkInterfaceException();
        }

        final PcapNetworkInterface.PromiscuousMode mode = PcapNetworkInterface.PromiscuousMode.NONPROMISCUOUS;
        handle = networkInterface.openLive(CAPTURED_PACKET_SIZE, mode, 0);

        // Berkley Packet Filter (BPF): http://biot.com/capstats/bpf.html
        handle.setFilter("udp && less 150", BpfProgram.BpfCompileMode.OPTIMIZE);
    }


    private void startConnectionCleaner() {
        Timer connectionCleaner = new Timer();
        connectionCleaner.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                cleanConnections();
            }
        }, 0, CLEANER_POLL_MS);
    }


    private void cleanConnections() {
        long currentTime = System.currentTimeMillis();
        Set<Inet4Address> connectionsToRemove = active.entrySet().stream().
                filter(e -> currentTime - e.getValue() >= PEER_TIMEOUT_MS)
                .map(e -> e.getKey()).collect(Collectors.toSet());

        for (Inet4Address connectionToRemove : connectionsToRemove) {
            active.remove(connectionToRemove);
        }
    }


    @Override
    public void run() {
        try {
            sniffPackets();
        } catch (Exception e) {
            snifferListener.handleException(e);
        }
    }

    private void sniffPackets() throws PcapNativeException, NotOpenException {
        // Create a listener that defines what to do with the received packets
        PacketListener listener = packet -> handlePacket(packet);

        logger.info("Started sniffing packets...");

        try {
            handle.loop(-1, listener);
        } catch (InterruptedException e) {
            // can be interrupted on purpose
        }
    }

    private void handlePacket(Packet packet) {
        final IpV4Packet ippacket = packet.get(IpV4Packet.class);

        if (ippacket != null) {
            final UdpPacket udppack = ippacket.get(UdpPacket.class);

            if (udppack != null && udppack.getPayload() != null) {
                final Inet4Address srcAddr = ippacket.getHeader().getSrcAddr();
                final Inet4Address dstAddr = ippacket.getHeader().getDstAddr();
                final int payloadLen = udppack.getPayload().getRawData().length;

                //Packets are STUN related: 56 is request, 68 is response
                if (active.containsKey(srcAddr) && !srcAddr.equals(localAddr)) {
                    // it's a response from a peer to the local address
                    if (active.get(srcAddr) != null && payloadLen == 68 && dstAddr.equals(localAddr)) {
                        int ping = (int) (handle.getTimestamp().getTime() - active.get(srcAddr));
                        snifferListener.updatePing(ippacket.getHeader().getSrcAddr(), ping);
                        active.put(srcAddr, null); // no longer expect ping
                    }
                } else {
                    if (payloadLen == 56 && srcAddr.equals(localAddr)) {
                        // it's a request from the local address to a peer
                        // we will store the peer address
                        Inet4Address peerAddresss = ippacket.getHeader().getDstAddr();
                        active.put(peerAddresss, handle.getTimestamp().getTime());
                    }
                }
            }
        }
    }


    public void close() {
        // TODO: Fix this. There seems to be an issue with Pcap where it hangs when trying to close.
        // https://github.com/kaitoy/pcap4j/issues/199

//        byte[] mac = new byte[6];
//
//        int i = 0;
//        if (networkInterface.getLinkLayerAddresses().get(0) != null) {
//            for (byte b : networkInterface.getLinkLayerAddresses().get(0).getAddress()) {
//                mac[i] = b;
//                i++;
//            }
//        }
//        EthernetPacket.Builder eb = new EthernetPacket.Builder();
//        eb.type(EtherType.ARP)
//                .srcAddr(MacAddress.getByAddress(mac))
//                .dstAddr(MacAddress.ETHER_BROADCAST_ADDRESS)
//                .paddingAtBuild(true)
//                .payloadBuilder(new UnknownPacket.Builder().rawData(new byte[5]));
//
//        try {
//            logger.info("Try to send a bogus packet to let the captor break.");
//            handle.sendPacket(eb.build());
//        } catch (PcapNativeException e) {
//            logger.error("Failed to send bogus packet", e);
//        } catch (NotOpenException e) {
//            throw new AssertionError("Never get here.");
//        }
//
//        if (handle != null) {
//            logger.info("Cleaning up system resources...");
//            try {
//                handle.breakLoop();
//            } catch (NotOpenException e) {
//                e.printStackTrace();
//            }
//            handle.close();
//            logger.info("Freed network interface handle.");
//        }
    }

}