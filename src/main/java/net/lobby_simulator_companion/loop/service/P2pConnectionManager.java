package net.lobby_simulator_companion.loop.service;

import net.lobby_simulator_companion.loop.domain.Connection;
import org.pcap4j.core.BpfProgram;
import org.pcap4j.core.NotOpenException;
import org.pcap4j.core.PacketListener;
import org.pcap4j.core.PcapHandle;
import org.pcap4j.core.PcapNativeException;
import org.pcap4j.core.PcapNetworkInterface;
import org.pcap4j.core.Pcaps;
import org.pcap4j.packet.IpV4Packet;
import org.pcap4j.packet.Packet;
import org.pcap4j.packet.UdpPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;


public class P2pConnectionManager implements ConnectionManager {

    private static final int CAPTURED_PACKET_SIZE = 128;
    private static final int CLEANER_POLL_MS = 1000;
    private static final int CONNECTION_TIMEOUT_MS = 6000;

    /**
     * Berkley Packet Filter (BPF):
     * http://biot.com/capstats/bpf.html
     * https://www.tcpdump.org/manpages/pcap-filter.7.html
     */
    private static final String BPF_EXPR__UDP = "udp and (len = 98 or len = 110)";

    private static final Logger logger = LoggerFactory.getLogger(P2pConnectionManager.class);

    private InetAddress localAddr;
    private SnifferListener snifferListener;
    private PcapNetworkInterface networkInterface;
    private Map<InetAddress, Connection> connections = new ConcurrentHashMap<>();
    private PcapHandle pcapHandle;
    private boolean connected;


    public P2pConnectionManager(InetAddress localAddr, SnifferListener snifferListener)
            throws PcapNativeException, NotOpenException, InvalidNetworkInterfaceException {
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

        String filterExpr = String.format(BPF_EXPR__UDP, localAddr.getHostAddress());
        pcapHandle.setFilter(filterExpr, BpfProgram.BpfCompileMode.OPTIMIZE);
    }


    @Override
    public void start() {
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
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
        };

        logger.info("Started sniffing packets.");

        try {
            pcapHandle.loop(-1, listener);
        } catch (InterruptedException e) {
            // can be interrupted on purpose
        }
    }

    private void handlePacket(Packet packet) throws UnknownHostException {
        final IpV4Packet ipPacket = packet.get(IpV4Packet.class);
        final UdpPacket udpPacket = ipPacket.get(UdpPacket.class);

        if (isStunRequestToRemotePeer(ipPacket, udpPacket)) {
            if (!connected) {
                connected = true;
                Connection dummyConnection = new Connection(localAddr, 0, Inet4Address.getByName("0.0.0.0"), 0,
                        pcapHandle.getTimestamp().getTime());
                snifferListener.notifyNewConnection(dummyConnection);
            }

            Inet4Address remoteAddr = ipPacket.getHeader().getDstAddr();
            Connection connection = connections.get(remoteAddr);

            if (connection == null) {
                logger.debug("New peer detected.");
                connection = new Connection(localAddr, udpPacket.getHeader().getSrcPort().valueAsInt(),
                        remoteAddr, udpPacket.getHeader().getDstPort().valueAsInt(),
                        pcapHandle.getTimestamp().getTime());
                connections.put(remoteAddr, connection);
            } else {
                connection.setLastSeen(pcapHandle.getTimestamp().getTime());
            }
        }
    }


    private boolean isStunRequestToRemotePeer(IpV4Packet ipPacket, UdpPacket udpPacket) {
        return ipPacket.getHeader().getSrcAddr().equals(localAddr)
                && udpPacket.getPayload().length() == 56;
    }


    private boolean isStunResponseFromRemotePeer(IpV4Packet ipPacket, UdpPacket udpPacket) {
        return ipPacket.getHeader().getDstAddr().equals(localAddr)
                && udpPacket.getPayload().length() == 68;
    }

    private void startConnectionCleaner() {
        Timer connectionCleanerTimer = new Timer();
        connectionCleanerTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (connections.isEmpty()) {
                    return;
                }

                long currentTime = System.currentTimeMillis();
                List<Connection> connectionsToPrune = connections.values().stream()
                        .filter(c -> currentTime >= c.getLastSeen() + CONNECTION_TIMEOUT_MS)
                        .collect(Collectors.toList());

                for (Connection connection : connectionsToPrune) {
                    connections.remove(connection.getRemoteAddr());
                }

                if (connected && connections.isEmpty()) {
                    connected = false;
                    snifferListener.notifyDisconnect();
                }
            }
        }, 0, CLEANER_POLL_MS);
    }


    @Override
    public void stop() {
        if (pcapHandle != null) {
            logger.info("Cleaning up sniffer...");
            try {
                pcapHandle.breakLoop();
            } catch (NotOpenException e) {
                logger.error("Failed when attempting to stop sniffer.", e);
            }
        }
    }

    public void close() {
        stop();
        pcapHandle.close();
        logger.info("Freed network interface pcapHandle.");
    }


}
