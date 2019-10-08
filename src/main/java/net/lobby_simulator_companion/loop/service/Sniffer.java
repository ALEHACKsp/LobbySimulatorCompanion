package net.lobby_simulator_companion.loop.service;

import org.pcap4j.core.*;
import org.pcap4j.packet.IpPacket;
import org.pcap4j.packet.IpV4Packet;
import org.pcap4j.packet.Packet;
import org.pcap4j.packet.UdpPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.*;

public class Sniffer implements Runnable {

    private static final int CAPTURED_PACKET_SIZE = 128;
    private static final int CLEANER_POLL_MS = 5000;
    private static final int CONNECTION_TIMEOUT_MS = 6000;

    /**
     * Berkley Packet Filter (BPF):
     * http://biot.com/capstats/bpf.html
     * https://www.tcpdump.org/manpages/pcap-filter.7.html
     *
     * The initial handshake seems through WireGuard protocol:
     * https://www.wireguard.com/protocol/
    */
    private static final String BPF_EXPR__NEW_CONNECTION = "udp[8:4] = 0x01000000 and (src host %s)";
    private static final String BPF_EXPR__KEEP_ALIVE = "udp and ((udp[8:4] = 0x01000000) and (src host %s)) or ((host %s and %s) and (port %s and %s))";

    private static final Logger logger = LoggerFactory.getLogger(Sniffer.class);

    private InetAddress localAddr;
    private SnifferListener snifferListener;
    private PcapNetworkInterface networkInterface;
    private Connection connection;
    private PcapHandle pcapHandle;


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
        pcapHandle = networkInterface.openLive(CAPTURED_PACKET_SIZE, mode, 1000);

        String filterExpr = String.format(BPF_EXPR__NEW_CONNECTION, localAddr.getHostAddress());
        pcapHandle.setFilter(filterExpr, BpfProgram.BpfCompileMode.OPTIMIZE);
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
        PacketListener listener = packet -> {
            try {
                handlePacket(packet);
            } catch (PcapNativeException e) {
                logger.error("Encountered a problem while handling a packet.", e);
            } catch (NotOpenException e) {
                logger.error("Encountered a problem while handling a packet.", e);
            }
        };

        logger.info("Started sniffing packets.");

        try {
            pcapHandle.loop(-1, listener);
        } catch (InterruptedException e) {
            // can be interrupted on purpose
        }
    }

    private void handlePacket(Packet packet) throws PcapNativeException, NotOpenException {
        final IpV4Packet ipPacket = packet.get(IpV4Packet.class);
        final UdpPacket udpPacket = ipPacket.get(UdpPacket.class);

        if (isWireGuardHandshakeInit(ipPacket, udpPacket)) {
            createConnection(ipPacket, udpPacket);
        } else {
            final Inet4Address srcAddr = ipPacket.getHeader().getSrcAddr();

            if (connection != null && srcAddr.equals(connection.getRemoteAddr())) {
                // keep connection alive
                connection.setLastSeen(pcapHandle.getTimestamp().getTime());
            }
        }
    }

    private boolean isWireGuardHandshakeInit(IpPacket ipPacket, UdpPacket udpPacket) {
        byte[] rawData = udpPacket.getPayload().getRawData();

        return ipPacket.getHeader().getSrcAddr().equals(localAddr)
                && rawData.length >= 4
                && rawData[0] == 0x01 && rawData[1] == 0x00 && rawData[2] == 0x00 && rawData[3] == 0x00;
    }


    private void createConnection(IpPacket ipPacket, UdpPacket udpPacket) throws PcapNativeException, NotOpenException {
        connection = new Connection(localAddr, udpPacket.getHeader().getSrcPort().valueAsInt(),
                ipPacket.getHeader().getDstAddr(), udpPacket.getHeader().getDstPort().valueAsInt(),
                pcapHandle.getTimestamp().getTime());
        snifferListener.notifyNewConnection(connection);

        String filterExpr = String.format(
                BPF_EXPR__KEEP_ALIVE,
                connection.getLocalAddr().getHostAddress(),
                connection.getLocalAddr().getHostAddress(), connection.getRemoteAddr().getHostAddress(),
                connection.getLocalPort(), connection.getRemotePort());
        pcapHandle.setFilter(filterExpr, BpfProgram.BpfCompileMode.OPTIMIZE);
    }


    public void close() {
        if (pcapHandle != null) {
            logger.info("Cleaning up sniffer...");
            try {
                pcapHandle.breakLoop();
            } catch (NotOpenException e) {
                logger.error("Failed when attempting to stop sniffer.", e);
            }
            pcapHandle.close();
            logger.info("Freed network interface pcapHandle.");
        }
    }


    private void startConnectionCleaner() {
        Timer connectionCleanerTimer = new Timer();
        connectionCleanerTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (connection == null) {
                    return;
                }

                long currentTime = System.currentTimeMillis();

                if (currentTime > connection.getLastSeen() + CONNECTION_TIMEOUT_MS) {
                    connection = null;
                    snifferListener.notifyDisconnect();
                }
            }
        }, 0, CLEANER_POLL_MS);
    }


}