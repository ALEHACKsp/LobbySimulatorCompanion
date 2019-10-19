package net.lobby_simulator_companion.loop.service;

import net.lobby_simulator_companion.loop.domain.Connection;
import org.pcap4j.core.BpfProgram;
import org.pcap4j.core.NotOpenException;
import org.pcap4j.core.PacketListener;
import org.pcap4j.core.PcapHandle;
import org.pcap4j.core.PcapNativeException;
import org.pcap4j.core.PcapNetworkInterface;
import org.pcap4j.core.Pcaps;
import org.pcap4j.packet.IpPacket;
import org.pcap4j.packet.IpV4Packet;
import org.pcap4j.packet.Packet;
import org.pcap4j.packet.TcpPacket;
import org.pcap4j.packet.UdpPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.util.Timer;
import java.util.TimerTask;

/**
 * The initial handshake with the dedicated server hosting the match (including lobby) is through WireGuard protocol:
 * https://www.wireguard.com/protocol/
 *
 * @author NickyRamone
 */
public class DedicatedServerConnectionManager implements ConnectionManager {

    private static final int CAPTURED_PACKET_SIZE = 17000;
    private static final int CLEANER_POLL_MS = 1000;
    private static final int CONNECTION_TIMEOUT_MS = 6000;

    private static final class PacketInfo {
        private enum Protocol {TCP, UDP}

        Protocol protocol;
        int packetLen;
        InetAddress srcAddress;
        InetAddress dstAddress;
        int srcPort;
        int dstPort;
        int payloadLen;
        Packet payload;

        @Override
        public String toString() {
            return "PacketInfo{" +
                    "protocol=" + protocol +
                    ", packetLen=" + packetLen +
                    ", srcAddress=" + srcAddress +
                    ", dstAddress=" + dstAddress +
                    ", srcPort=" + srcPort +
                    ", dstPort=" + dstPort +
                    ", payloadLen=" + payloadLen +
                    '}';
        }
    }

    private enum State {Idle, Connected}

    /**
     * Berkley Packet Filter (BPF):
     * http://biot.com/capstats/bpf.html
     * https://www.tcpdump.org/manpages/pcap-filter.7.html
     */
    private static final String BPF = "tcp or udp and len <= 17000";

    private static final Logger logger = LoggerFactory.getLogger(DedicatedServerConnectionManager.class);

    private InetAddress localAddr;
    private SnifferListener snifferListener;
    private PcapNetworkInterface networkInterface;
    private PcapHandle pcapHandle;
    private Connection matchConn;
    private State state = State.Idle;


    public DedicatedServerConnectionManager(InetAddress localAddr, SnifferListener snifferListener) throws PcapNativeException, NotOpenException, InvalidNetworkInterfaceException {
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

        String filterExpr = String.format(BPF, localAddr.getHostAddress(), localAddr.getHostAddress());
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
        PacketListener listener = packet -> handlePacket(packet);

        logger.info("Started sniffing packets.");

        try {
            pcapHandle.loop(-1, listener);
        } catch (InterruptedException e) {
            // can be interrupted on purpose
        }
    }

    private void handlePacket(Packet packet) {
        PacketInfo packetInfo = getPacketInfo(packet);
        if (packetInfo == null) {
            return;
        }

        if (packetInfo.packetLen >= 10000) {
            logger.debug("big packet: {}", packetInfo);
        }

        if (isMatchConnect(packetInfo)) {
            logger.debug("Connected to match.");
            state = State.Connected;
            matchConn = new Connection(localAddr, packetInfo.srcPort, packetInfo.dstAddress, packetInfo.dstPort);
            snifferListener.notifyMatchConnect(matchConn);

        } else if (isExchangeWithMatchServer(packetInfo)) {
            matchConn.setLastSeen(System.currentTimeMillis());
        }
    }


    private PacketInfo getPacketInfo(Packet packet) {
        IpPacket ipPacket = packet.get(IpV4Packet.class);

        if (ipPacket == null) {
            return null;
        }

        PacketInfo info = new PacketInfo();
        info.packetLen = packet.length();
        TcpPacket tcpPacket = ipPacket.get(TcpPacket.class);
        UdpPacket udpPacket = ipPacket.get(UdpPacket.class);
        info.srcAddress = ipPacket.getHeader().getSrcAddr();
        info.dstAddress = ipPacket.getHeader().getDstAddr();

        if (tcpPacket != null) {
            info.protocol = PacketInfo.Protocol.TCP;
            info.srcPort = tcpPacket.getHeader().getSrcPort().valueAsInt();
            info.dstPort = tcpPacket.getHeader().getDstPort().valueAsInt();
            info.payloadLen = tcpPacket.getPayload() != null ? tcpPacket.getPayload().length() : 0;
            info.payload = tcpPacket.getPayload();
        } else if (udpPacket != null) {
            info.protocol = PacketInfo.Protocol.UDP;
            info.srcPort = udpPacket.getHeader().getSrcPort().valueAsInt();
            info.dstPort = udpPacket.getHeader().getDstPort().valueAsInt();
            info.payloadLen = udpPacket.getPayload() != null ? udpPacket.getPayload().length() : 0;
            info.payload = udpPacket.getPayload();
        }

        return info;
    }

    private boolean isMatchConnect(PacketInfo packetInfo) {
        return state == State.Idle && isWireGuardHandshakeInit(packetInfo);
    }


    private boolean isWireGuardHandshakeInit(PacketInfo packetInfo) {
        if (packetInfo == null) {
            return false;
        }
        byte[] rawData = packetInfo.payload != null ? packetInfo.payload.getRawData() : new byte[0];

        return packetInfo.srcAddress.equals(localAddr)
                && rawData.length >= 4
                && rawData[0] == 0x01 && rawData[1] == 0x00 && rawData[2] == 0x00 && rawData[3] == 0x00;
    }

    private boolean isExchangeWithMatchServer(PacketInfo packetInfo) {
        return (state == State.Connected)
                && matchConn != null
                && packetInfo.protocol == PacketInfo.Protocol.UDP
                && packetInfo.srcAddress.equals(matchConn.getRemoteAddr()) && packetInfo.srcPort == matchConn.getRemotePort();
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


    private void startConnectionCleaner() {
        Timer connectionCleanerTimer = new Timer();
        connectionCleanerTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                long currentTime = System.currentTimeMillis();

                if ((state == State.Connected)
                        && currentTime > matchConn.getLastSeen() + CONNECTION_TIMEOUT_MS) {
                    matchConn = null;
                    state = State.Idle;
                    snifferListener.notifyMatchDisconnect();
                }
            }
        }, 0, CLEANER_POLL_MS);
    }


}