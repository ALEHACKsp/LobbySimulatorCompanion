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
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.stream.Collectors;

/**
 * The initial handshake with the dedicated server hosting the match (including lobby) is through WireGuard protocol:
 * https://www.wireguard.com/protocol/
 *
 * @author NickyRamone
 */
public class DedicatedServerConnectionManager implements ConnectionManager {

    private static final int MAX_CAPTURED_PACKET_SIZE = 1500;
    private static final long MIN_MS_FROM_CONNECT_TILL_MATCH_START = 40000;
    private static final int CLEANER_POLL_MS = 1000;
    private static final int CONNECTION_TIMEOUT_MS = 6000;
    private static final String BHVR_BACKEND_HOSTNAME = "latest.live.dbd.bhvronline.com";

    private static final byte TLS_CONTENT_TYPE__APPLICATION_DATA = 0x17;
    private static final byte[] TLS_VERSION__1_2 = {0x03, 0x03};
    private static final int HTTPS_PORT = 443;
    private static final Logger logger = LoggerFactory.getLogger(DedicatedServerConnectionManager.class);

    /**
     * Berkley Packet Filter (BPF):
     * http://biot.com/capstats/bpf.html
     * https://www.tcpdump.org/manpages/pcap-filter.7.html
     */
    private static final String BPF = "tcp or udp and len <= " + MAX_CAPTURED_PACKET_SIZE;
    private static final int ENCRYPTED_DATA_HEADER_LEN = 6;


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

    private enum State {Idle, Connected, InMatch}

    private InetAddress localAddr;
    private SnifferListener snifferListener;
    private PcapNetworkInterface networkInterface;
    private PcapHandle pcapHandle;
    private Set<Byte> bhvrBackendPartialAddresses;
    private Set<InetAddress> bhvrBackendAddresses = new HashSet<>();
//    private Connection bhvrBackendConn;
    private Connection matchConn;
    private State state = State.Idle;
    private byte[] encryptedDataHeader = new byte[ENCRYPTED_DATA_HEADER_LEN];
    private Integer matchSearchPacketLen;
    private Integer matchStartPacketLen;
    private long matchStartTime;


    public DedicatedServerConnectionManager(InetAddress localAddr, SnifferListener snifferListener) throws PcapNativeException, NotOpenException, InvalidNetworkInterfaceException {
        this.localAddr = localAddr;
        this.snifferListener = snifferListener;
        initNetworkInterface();
        startConnectionCleaner();

        try {
//            bhvrBackendAddresses = Arrays.stream(InetAddress.getAllByName(BHVR_BACKEND_HOSTNAME))
//                    .collect(Collectors.toSet());
//            System.out.println("getallbyname: " + bhvrBackendAddresses);
//
//            bhvrBackendAddresses = Arrays.stream(
//                    new DNSNameService().lookupAllHostAddr(BHVR_BACKEND_HOSTNAME)).collect(Collectors.toSet());
//            System.out.println("by dns: " + bhvrBackendAddresses);
            bhvrBackendPartialAddresses = Arrays.stream(InetAddress.getAllByName(BHVR_BACKEND_HOSTNAME))
                    .map(a -> a.getAddress()[0])
                    .collect(Collectors.toSet());

        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void initNetworkInterface() throws PcapNativeException, InvalidNetworkInterfaceException, NotOpenException {
        networkInterface = Pcaps.getDevByAddress(localAddr);
        if (networkInterface == null) {
            throw new InvalidNetworkInterfaceException();
        }

        final PcapNetworkInterface.PromiscuousMode mode = PcapNetworkInterface.PromiscuousMode.NONPROMISCUOUS;
        pcapHandle = networkInterface.openLive(MAX_CAPTURED_PACKET_SIZE, mode, 1000);

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

//        if (bhvrBackendConn == null && isBackendRequest(packetInfo)) {
//            bhvrBackendConn = new Connection(localAddr, packetInfo.srcPort, packetInfo.dstAddress, packetInfo.dstPort);
//            logger.debug("Detected backend server connection: {}", bhvrBackendConn);
//        }
//        else if (isMatchSearch(packetInfo)) {
//            logger.debug("Search match.");
////            bhvrBackendConn = new Connection(localAddr, packetInfo.srcPort, packetInfo.dstAddress, packetInfo.dstPort);
////            logger.debug("backend connection: {}", bhvrBackendConn);
////            bhvrBackendAddresses.add(packetInfo.dstAddress);
////            matchSearchPacketLen = packetInfo.packetLen;
//            matchSearchPacketLen = packetInfo.packetLen;
//            snifferListener.notifyMatchSearch();
//
//        }
//        else if (isMatchSearchCancel(packetInfo)) {
//            logger.debug("Cancel match search.");
//            snifferListener.notifyMatchDisconnect();
//        }
        if (isBackendRequest(packetInfo)) {
//            Connection bhvrBackendConn = new Connection(localAddr, packetInfo.srcPort, packetInfo.dstAddress, packetInfo.dstPort);
            logger.debug("Detected backend server connection: {}", packetInfo.dstAddress);
            bhvrBackendAddresses.add(packetInfo.dstAddress);
        }
        else if (isMatchConnect(packetInfo)) {
            logger.debug("Connected to match.");
            logger.debug("Backend connections: {}", bhvrBackendAddresses);
            state = State.Connected;
            matchConn = new Connection(localAddr, packetInfo.srcPort, packetInfo.dstAddress, packetInfo.dstPort);
            snifferListener.notifyMatchConnect(matchConn);

        } else if (isExchangeWithMatchServer(packetInfo)) {
            matchConn.setLastSeen(System.currentTimeMillis());

        } else if (isMatchStart(packetInfo)) {
            logger.debug("Start match.");
            byte[] tcpPayload = packetInfo.payload.getRawData();
            System.arraycopy(tcpPayload, 11, encryptedDataHeader, 0, ENCRYPTED_DATA_HEADER_LEN);
            matchStartPacketLen = packetInfo.packetLen;
            state = State.InMatch;
            matchStartTime = System.currentTimeMillis();
            snifferListener.notifyMatchStart();

        } else if (isMatchEnd(packetInfo)) {
            logger.debug("Match ended for current player.");
            snifferListener.notifyMatchEnd();
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


    private boolean isBackendRequest(PacketInfo packetInfo){
        return state == State.Idle
                && isHttpOverTlsV12Request(packetInfo)
                && packetInfo.packetLen == 856
                && bhvrBackendPartialAddresses.contains(packetInfo.dstAddress.getAddress()[0]);
    }

//    private boolean isMatchSearch(PacketInfo packetInfo) {
//        return state == State.Idle
//                && bhvrBackendConn != null
//                && isHttpOverTlsV12Request(packetInfo)
////                && packetInfo.packetLen >= 470 && packetInfo.packetLen <= 490
//                && packetInfo.packetLen >= 400 && packetInfo.packetLen <= 700
//                && packetInfo.dstAddress.equals(bhvrBackendConn.getRemoteAddr());
//    }
//
//    private boolean isMatchSearchCancel(PacketInfo packetInfo) {
//        return state == State.Idle
//                && bhvrBackendConn != null
//                && matchSearchPacketLen != null
//                && isHttpOverTlsV12Request(packetInfo)
//                && packetInfo.packetLen == matchSearchPacketLen + 1;
//    }


    private boolean isMatchConnect(PacketInfo packetInfo) {
//        return state == State.Idle && bhvrBackendConn != null && isWireGuardHandshakeInit(packetInfo);
        return state == State.Idle && !bhvrBackendAddresses.isEmpty() && isWireGuardHandshakeInit(packetInfo);
    }

    private boolean isMatchStart(PacketInfo packetInfo) {
        if (state == State.Connected
                && isHttpOverTlsV12Response(packetInfo)
                && packetInfo.packetLen >= 1000 && packetInfo.packetLen <= 1020
                && System.currentTimeMillis() - matchConn.getCreated() >= MIN_MS_FROM_CONNECT_TILL_MATCH_START) {

            logger.debug("Possible match start packet: {}", packetInfo);
            logger.debug("Backend addresses: {}", bhvrBackendAddresses);
        }


        return state == State.Connected
                && isHttpOverTlsV12Response(packetInfo)
                && bhvrBackendAddresses.contains(packetInfo.srcAddress)
//                && bhvrBackendConn.getRemoteAddr().equals(packetInfo.srcAddress)
                && packetInfo.packetLen >= 1000 && packetInfo.packetLen <= 1020
                && System.currentTimeMillis() - matchConn.getCreated() >= MIN_MS_FROM_CONNECT_TILL_MATCH_START;
    }

    private boolean isMatchEnd(PacketInfo packetInfo) {
        return state == State.InMatch
                && isHttpOverTlsV12Response(packetInfo)
                && bhvrBackendAddresses.contains(packetInfo.srcAddress)
//                && bhvrBackendConn.getRemoteAddr().equals(packetInfo.srcAddress)
                && packetInfo.packetLen == matchStartPacketLen
                && System.currentTimeMillis() - matchStartTime >= 1000;
    }


    private boolean isHttpOverTlsV12Request(PacketInfo packetInfo) {
        if (packetInfo.payload == null) {
            return false;
        }
        byte[] payload = packetInfo.payload.getRawData();

        return packetInfo.srcAddress.equals(localAddr)
                && packetInfo.dstPort == HTTPS_PORT
                && packetInfo.protocol == PacketInfo.Protocol.TCP
                && payload[0] == TLS_CONTENT_TYPE__APPLICATION_DATA
                && payload[1] == TLS_VERSION__1_2[0] && payload[2] == TLS_VERSION__1_2[1];
    }


    private boolean isHttpOverTlsV12Response(PacketInfo packetInfo) {
        if (packetInfo.payload == null) {
            return false;
        }
        byte[] payload = packetInfo.payload.getRawData();

        return packetInfo.dstAddress.equals(localAddr)
                && packetInfo.srcPort == HTTPS_PORT
                && packetInfo.protocol == PacketInfo.Protocol.TCP
                && payload[0] == TLS_CONTENT_TYPE__APPLICATION_DATA
                && payload[1] == TLS_VERSION__1_2[0] && payload[2] == TLS_VERSION__1_2[1];
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
        return (state == State.Connected || state == State.InMatch)
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
        logger.info("Freed network interface handle.");
    }


    private void startConnectionCleaner() {
        Timer connectionCleanerTimer = new Timer();
        connectionCleanerTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                long currentTime = System.currentTimeMillis();

                if ((state == State.Connected || state == State.InMatch)
                        && currentTime > matchConn.getLastSeen() + CONNECTION_TIMEOUT_MS) {
                    logger.debug("Detected match disconnection.");
                    matchConn = null;
//                    bhvrBackendConn = null;
//                    bhvrBackendAddresses.clear();
                    matchStartPacketLen = null;
                    state = State.Idle;
                    snifferListener.notifyMatchDisconnect();
                }
            }
        }, 0, CLEANER_POLL_MS);
    }


}