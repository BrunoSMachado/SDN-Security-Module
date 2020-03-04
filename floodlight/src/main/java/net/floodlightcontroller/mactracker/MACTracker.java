package net.floodlightcontroller.mactracker;

import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFPacketIn;
import org.projectfloodlight.openflow.protocol.OFType;
import org.projectfloodlight.openflow.protocol.OFGroupAdd;
import org.projectfloodlight.openflow.protocol.OFGroupModify;
import org.projectfloodlight.openflow.protocol.OFGroupDelete;
import org.projectfloodlight.openflow.protocol.OFGroupType;
import org.projectfloodlight.openflow.protocol.OFBucket;
import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.OFFlowAdd;
import org.projectfloodlight.openflow.protocol.OFFlowDelete;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.types.EthType;
import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.IpProtocol;
import org.projectfloodlight.openflow.types.MacAddress;
import org.projectfloodlight.openflow.types.OFBufferId;
import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.OFGroup;
import org.projectfloodlight.openflow.types.OFPort;
import org.projectfloodlight.openflow.types.VlanVid;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.protocol.oxm.OFOxms;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IOFMessageListener;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.core.types.MacVlanPair;
import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.packet.Ethernet;
import net.floodlightcontroller.packet.IPv4;
import net.floodlightcontroller.routing.IRoutingService;
import net.floodlightcontroller.topology.ITopologyService;
import net.floodlightcontroller.util.FlowModUtils;
import net.floodlightcontroller.devicemanager.IDeviceService;
import net.floodlightcontroller.learningswitch.ILearningSwitchService;
import net.floodlightcontroller.core.internal.IOFSwitchService;
import net.floodlightcontroller.statistics.IStatisticsService;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import java.security.KeyStore;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import java.lang.Integer;
import java.util.Random;


public class MACTracker implements IOFMessageListener, IFloodlightModule, ILearningSwitchService {
	public static final Integer DEFAULT = 90;
	public static final Integer PRIORITY_1 = 180;
	public static final Integer PRIORITY_2 = 360;
	protected static final Logger log = LoggerFactory.getLogger(MACTracker.class);
	protected IFloodlightProviderService floodlightProvider;
	protected IOFSwitchService switchService;
	protected IRoutingService routingEngineService;
	protected ITopologyService topologyService;
	protected IDeviceService deviceManagerService;
	protected ILearningSwitchService learningSwitch;
	protected IStatisticsService iss;
	protected static Logger logger;
	protected Integer groupNumber = 1;
	protected Integer sent = 0;
	protected Map<DatapathId, SwitchInfo> macs;
	protected Map<OFGroup, Group> groups;
	protected Map<OFGroup, BenignGroup> benignGroups;
	protected Map<DatapathId, Integer> activeFlows;
	protected List<BandwidthInfo> bandwidths;
	protected Map<Integer, Integer> idss;
	protected Integer isDrop = 0;

	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleDependencies() {
		Collection<Class<? extends IFloodlightService>> l =
			new ArrayList<Class<? extends IFloodlightService>>();
		l.add(IFloodlightProviderService.class);
		l.add(IRoutingService.class);
		l.add(IDeviceService.class);
		l.add(ITopologyService.class);
		l.add(ILearningSwitchService.class);
		l.add(IStatisticsService.class);
		l.add(IOFSwitchService.class);
		return l;
	}

	@Override
	public void init(FloodlightModuleContext context) throws FloodlightModuleException {
		floodlightProvider = context.getServiceImpl(IFloodlightProviderService.class);
	    switchService = context.getServiceImpl(IOFSwitchService.class);
	    routingEngineService = context.getServiceImpl(IRoutingService.class);
	    topologyService = context.getServiceImpl(ITopologyService.class);
	    deviceManagerService = context.getServiceImpl(IDeviceService.class);
	    learningSwitch = context.getServiceImpl(ILearningSwitchService.class);
	    iss = context.getServiceImpl(IStatisticsService.class);
	    logger = LoggerFactory.getLogger(MACTracker.class);
	    this.groups = new ConcurrentHashMap<>();
	    this.benignGroups = new ConcurrentHashMap<>();
	    this.activeFlows = new ConcurrentHashMap<>();
	    this.idss = new ConcurrentHashMap<>();
	    this.macs = new HashMap<>();
	    macs.put(DatapathId.of("00:00:00:00:00:00:00:01"), new SwitchInfo("s1", MacAddress.of("00:00:00:00:00:10"), MacAddress.of("00:00:00:00:00:13")));
	    macs.put(DatapathId.of("00:00:00:00:00:00:00:02"), new SwitchInfo("s2", MacAddress.of("00:00:00:00:00:11"), MacAddress.of("00:00:00:00:00:14")));
	    macs.put(DatapathId.of("00:00:00:00:00:00:00:03"), new SwitchInfo("s3", MacAddress.of("00:00:00:00:00:12"),	MacAddress.of("00:00:00:00:00:15")));
	    idss.put(1, 0);
	    idss.put(2, 0);
	    for(DatapathId dp: macs.keySet()) {
	    	activeFlows.put(dp, 0);
	    }
	    SnortSocket ss = new SnortSocket(51234);
	    flowStats fs1 = new flowStats("s1");
	    flowStats fs2 = new flowStats("s2");
	    flowStats fs3 = new flowStats("s3");
	    roundRobin rr = new roundRobin();
	    ss.start();
	    fs1.start();
	    fs2.start();
	    fs3.start();
	    rr.start();
	}

	@Override
	public void startUp(FloodlightModuleContext context) {
		floodlightProvider.addOFMessageListener(OFType.PACKET_IN, this);
	}

	@Override
	public Command receive(IOFSwitch sw, OFMessage msg, FloodlightContext cntx) {
		switch (msg.getType()) {
		   		case PACKET_IN:
		   			return processPacketIn(sw, (OFPacketIn)msg, cntx);

		   		default:
		   			break;
				}
		   return Command.CONTINUE;
	}
	
	@Override
	public String getName() {
		return MACTracker.class.getSimpleName();
	}

	@Override
	public boolean isCallbackOrderingPrereq(OFType type, String name) {
		return false;
	}

	@Override
	public boolean isCallbackOrderingPostreq(OFType type, String name) {
		return false;
	}

	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleServices() {
	        return null;
	}

	@Override
	public Map<Class<? extends IFloodlightService>, IFloodlightService> getServiceImpls() {
        return null;
	}
	
	@Override
	public Map<IOFSwitch, Map<MacVlanPair, OFPort>> getTable() {
		return null;
	}

	@Override
	public OFPort getFromPortMap(IOFSwitch sw, MacAddress mac, VlanVid vlan) {
		return null;
	}



	public Command processPacketIn(IOFSwitch sw, OFPacketIn msg, FloodlightContext cntx) {
	   
	   OFPort op = null;
	   IOFSwitch ms = null;
	   Ethernet eth = IFloodlightProviderService.bcStore.get(cntx, IFloodlightProviderService.CONTEXT_PI_PAYLOAD);
	   OFFactory myFactory = sw.getOFFactory();
	   List<OFAction> actions = new ArrayList<>();
	   List<OFAction> aux1 = new ArrayList<>();
	   IPv4Address srcIp;
	   IPv4Address dstIp;
	   IpProtocol protoType;
	   MacAddress srcMac;
	   MacAddress dstMac;
	   Integer ids;
	   DatapathId bestSwitch;
	   MacAddress mac = null;
	   String sSwitch = null;
	   
	   if(eth.getEtherType() == EthType.IPv4) {
		   IPv4 ipv4 = (IPv4) eth.getPayload();
		   srcIp = ipv4.getSourceAddress();
		   dstIp = ipv4.getDestinationAddress();
		   protoType = ipv4.getProtocol();
		   srcMac = eth.getSourceMACAddress();
		   dstMac = eth.getDestinationMACAddress();
		   

		   for(OFGroup number: groups.keySet()) {
			   if(groups.get(number).equals(srcIp, dstIp, protoType)) {
				   if(groups.get(number).getIsActive() == true) {
					   return Command.CONTINUE;
				   }
				   else {
					   ids = bestIds();
					   bestSwitch = bestSwitch();
					   if(ids == 1) {
						   mac = macs.get(bestSwitch).getMac1();
					   }
					   else if(ids == 2) {
						   mac = macs.get(bestSwitch).getMac2();
					   }
					   
					   ms = switchService.getActiveSwitch(bestSwitch);
					   op = learningSwitch.getFromPortMap(ms, mac, null);
					   if (op == null) {
						   return Command.CONTINUE;
					   }
					   
					   for(DatapathId d: macs.keySet()) {
						   if(ms.getId().equals(d)) {
							   sSwitch = macs.get(d).getSSwitch();
						   }
					   }
					   
					   aux1.add(myFactory.actions().group(number));
			   		   Match match1 = myFactory.buildMatch()
								   .setExact(MatchField.ETH_TYPE, EthType.IPv4)
								   .setExact(MatchField.IP_PROTO, groups.get(number).getProtoType())
								   .setExact(MatchField.IPV4_SRC, groups.get(number).getSrcIp())
								   .setExact(MatchField.IPV4_DST, groups.get(number).getDstIp())
								   .build();
					   
			   		   switch(groups.get(number).getPriority()) {
			   		   		case 0:
			   		   			return Command.CONTINUE;
			   		   		case 4:
					   			if(ms.equals(groups.get(number).getSwitch()) && ids == groups.get(number).getIds()) {
					   				sendFlowMod(groups.get(number).getSwitch(), match1, aux1, msg.getBufferId(), PRIORITY_1);
					   				groups.get(number).setIsActive(true);
					   			}
					   			else {
					   				OFFlowAdd flowAdd = myFactory.buildFlowAdd()
					   						.setBufferId(OFBufferId.NO_BUFFER)
					   						.setPriority(32768)
					   						.setMatch(match1)
					   						.setActions(aux1)
					   						.setHardTimeout(DEFAULT)
					   						.build();
					   				OFFlowDelete flowDelete = FlowModUtils.toFlowDelete(flowAdd);
					   				groups.get(number).getSwitch().write(flowDelete);
					   				sendGroupMod(groups.get(number).getSwitch(), "delete", number, null, null);
					   				sendGroupMod(ms, "add", number, op, mac);
					   				sendFlowMod(ms, match1, aux1, msg.getBufferId(), PRIORITY_1);
					   				groups.get(number).setIsActive(true);
					   				groups.get(number).setSwitch(ms);
									groups.get(number).setMac(mac);
									groups.get(number).setS_Switch(sSwitch);
									idss.put(ids, idss.get(ids) + 1);
									activeFlows.put(bestSwitch, activeFlows.get(bestSwitch) + 1);
									
					   			}
					   			break;
					   		case 3:
					   			if(ms.equals(groups.get(number).getSwitch()) && ids == groups.get(number).getIds()) {
					   				sendFlowMod(groups.get(number).getSwitch(), match1, aux1, msg.getBufferId(), PRIORITY_2);
					   				groups.get(number).setIsActive(true);
					   			}
					   			else {
						   			OFFlowAdd flowAdd1 = myFactory.buildFlowAdd()
					   					.setBufferId(OFBufferId.NO_BUFFER)
					   					.setPriority(32768)
					   					.setMatch(match1)
					   					.setActions(aux1)
					   					.setHardTimeout(DEFAULT)
					   					.build();
						   			OFFlowDelete flowDelete1 = FlowModUtils.toFlowDelete(flowAdd1);
						   			groups.get(number).getSwitch().write(flowDelete1);
						   			sendGroupMod(groups.get(number).getSwitch(), "delete", number, null, null);
					   				sendGroupMod(ms, "add", number, op, mac);
						   			sendFlowMod(ms, match1, aux1, msg.getBufferId(), PRIORITY_2);
						   			groups.get(number).setIsActive(true);
						   			groups.get(number).setSwitch(ms);
									groups.get(number).setMac(mac);
									groups.get(number).setS_Switch(sSwitch);
									idss.put(ids, idss.get(ids) + 1);
									activeFlows.put(bestSwitch, activeFlows.get(bestSwitch) + 1);
					   			}
					   			break;
					   		case 2:
					   			if(ms.equals(groups.get(number).getSwitch()) && ids == groups.get(number).getIds()) {
					   				sendFlowMod(groups.get(number).getSwitch(), match1, aux1, msg.getBufferId(), PRIORITY_2);
					   				groups.get(number).setIsActive(true);
					   			}
					   			else {
						   			OFFlowAdd flowAdd2 = myFactory.buildFlowAdd()
				   						.setBufferId(OFBufferId.NO_BUFFER)
				   						.setPriority(32768)
				   						.setMatch(match1)
				   						.setActions(aux1)
				   						.setHardTimeout(DEFAULT)
				   						.build();
						   			OFFlowDelete flowDelete2 = FlowModUtils.toFlowDelete(flowAdd2);
						   			groups.get(number).getSwitch().write(flowDelete2);
					   				sendGroupMod(groups.get(number).getSwitch(), "delete", number, null, null);
					   				sendGroupMod(ms, "add", number, op, mac);
					   				sendFlowMod(ms, match1, aux1, msg.getBufferId(), PRIORITY_2);
					   				groups.get(number).setSwitch(ms);
									groups.get(number).setMac(mac);
									groups.get(number).setS_Switch(sSwitch);
									idss.put(ids, idss.get(ids) + 1);
									activeFlows.put(bestSwitch, activeFlows.get(bestSwitch) + 1);
					   			}
					   			break;
					   		default:
					   			break;
					   }
					   return Command.CONTINUE;
				   }
			   }
		   }

		   ids = bestIds();
		   bestSwitch = bestSwitch();
		   if(ids == 1) {
			   mac = macs.get(bestSwitch).getMac1();
		   }
		   else if(ids == 2) {
			   mac = macs.get(bestSwitch).getMac2();
		   }
		   
		   ms = switchService.getActiveSwitch(bestSwitch);
		   
		   op = learningSwitch.getFromPortMap(ms, mac, null);
		   if (op == null) {
			   return Command.CONTINUE;
		   }
		   
		   for(DatapathId d: macs.keySet()) {
			   if(ms.getId().equals(d)) {
				   sSwitch = macs.get(d).getSSwitch();
			   }
		   }
		   
		   Group group = new Group(srcIp, dstIp, protoType, macs.get(bestSwitch).getSSwitch(), ms, mac, op, ids, srcMac, dstMac);
		   groups.put(OFGroup.of(groupNumber), group);
		   sendGroupMod(ms, "add", OFGroup.of(groupNumber), op, mac);
		   
		   Match match = myFactory.buildMatch()
				   .setExact(MatchField.ETH_TYPE, EthType.IPv4)
				   .setExact(MatchField.IP_PROTO, protoType)
				   .setExact(MatchField.IPV4_SRC, srcIp)
				   .setExact(MatchField.IPV4_DST, dstIp)
				   .build();
		   
		   actions.add(myFactory.actions().group(OFGroup.of(groupNumber)));
		   groupNumber++;
		   if(msg.getBufferId() != OFBufferId.NO_BUFFER) {
			   sendFlowMod(ms, match, actions, msg.getBufferId(), DEFAULT);
		   }
		   else {
			   sendFlowMod(ms, match, actions, OFBufferId.NO_BUFFER, DEFAULT);
		   }
		   
		   idss.put(ids, idss.get(ids) + 1);
		   activeFlows.put(bestSwitch, activeFlows.get(bestSwitch) + 1);
		   
		   return Command.CONTINUE;
	   }
	   else {
		   return Command.CONTINUE;
	   }
	}

	public void sendGroupMod(IOFSwitch sw, String action, OFGroup of, OFPort op, MacAddress mac) {
		OFFactory myFactory = sw.getOFFactory();
		OFOxms oxms = myFactory.oxms();
		ArrayList<OFBucket> buckets = new ArrayList<OFBucket>(2);
		List<OFAction> list1 = new ArrayList<>();
		List<OFAction> list2 = new ArrayList<>();
		List<OFAction> list3 = new ArrayList<>();
		
		if(op != null) {
			list1.add(myFactory.actions().output(OFPort.NORMAL, Integer.MAX_VALUE));
			list2.add(myFactory.actions().setField(oxms.ethDst(mac)));
			list2.add(myFactory.actions().output(op, Integer.MAX_VALUE));
		}

		switch(action) {
			case "add":
				buckets.add(sw.getOFFactory().buildBucket()
						.setActions(list1)
						.setWatchGroup(OFGroup.ANY)
						.setWatchPort(OFPort.ANY)
						.build());
				
				buckets.add(sw.getOFFactory().buildBucket()
						.setActions(list2)
						.setWatchGroup(OFGroup.ANY)
						.setWatchPort(OFPort.ANY)
						.build());

				OFGroupAdd groupAdd = myFactory.buildGroupAdd()
						.setGroup(of)
						.setGroupType(OFGroupType.ALL)
						.setBuckets(buckets)
						.build();

				sw.write(groupAdd);
				break;

			case "dup-on":
				buckets.add(sw.getOFFactory().buildBucket()
						.setActions(list1)
						.setWatchGroup(OFGroup.ALL)
						.setWatchPort(OFPort.ANY)
						.build());
				buckets.add(sw.getOFFactory().buildBucket()
						.setActions(list2)
						.setWatchGroup(OFGroup.ALL)
						.setWatchPort(OFPort.ANY)
						.build());

				OFGroupModify groupModifyOn = myFactory.buildGroupModify()
						.setGroup(of)
						.setGroupType(OFGroupType.ALL)
						.setBuckets(buckets)
						.build();

				sw.write(groupModifyOn);
				break;

			case "dup-off":
				buckets.add(sw.getOFFactory().buildBucket()
						.setWatchGroup(OFGroup.ALL)
						.setWatchPort(OFPort.ANY)
						.setActions(list1)
						.build());

				OFGroupModify groupModifyOff = myFactory.buildGroupModify()
						.setGroup(of)
						.setGroupType(OFGroupType.ALL)
						.setBuckets(buckets)
						.build();

				sw.write(groupModifyOff);
				break;

			case "delete":
				OFGroupDelete groupDelete = myFactory.buildGroupDelete()
				.setGroup(of)
				.setGroupType(OFGroupType.ALL)
				.build();

				sw.write(groupDelete);
				break;

			case "stop":
				buckets.add(sw.getOFFactory().buildBucket()
						.setWatchGroup(OFGroup.ANY)
						.setWatchPort(OFPort.ANY)
						.setActions(list3)
						.build());

				OFGroupModify groupModifyStop = myFactory.buildGroupModify()
						.setGroup(of)
						.setGroupType(OFGroupType.ALL)
						.setBuckets(buckets)
						.build();

				sw.write(groupModifyStop);
				break;

			case "IDS":
				buckets.add(sw.getOFFactory().buildBucket()
						.setWatchGroup(OFGroup.ANY)
						.setWatchPort(OFPort.ANY)
						.setActions(list2)
						.build());

				OFGroupModify groupModifyIDS = myFactory.buildGroupModify()
						.setGroup(of)
						.setGroupType(OFGroupType.ALL)
						.setBuckets(buckets)
						.build();

				sw.write(groupModifyIDS);
				break;

			case "modify":
				buckets.add(sw.getOFFactory().buildBucket()
						.setActions(list2)
						.setWatchGroup(OFGroup.ANY)
						.setWatchPort(OFPort.ANY)
						.build());

				OFGroupModify groupModify = myFactory.buildGroupModify()
						.setGroup(of)
						.setGroupType(OFGroupType.ALL)
						.setBuckets(buckets)
						.build();

				sw.write(groupModify);
				break;
				
			case "test":
				buckets.add(sw.getOFFactory().buildBucket()
						.setActions(list2)
						.setWatchGroup(OFGroup.ANY)
						.setWatchPort(OFPort.ANY)
						.build());

				OFGroupAdd groupAdd1 = myFactory.buildGroupAdd()
						.setGroup(of)
						.setGroupType(OFGroupType.ALL)
						.setBuckets(buckets)
						.build();

				sw.write(groupAdd1);
				break;

			default:
				break;
		}
	}

	public OFFlowAdd sendFlowMod(IOFSwitch sw, Match match, List<OFAction> actions, OFBufferId buffer_id, Integer time) {
		OFFactory myFactory = sw.getOFFactory();
		OFFlowAdd flowAdd = myFactory.buildFlowAdd()
				.setBufferId(buffer_id)
				.setPriority(32768)
				.setMatch(match)
				.setActions(actions)
				.setHardTimeout(time)
				.build();

		sw.write(flowAdd);
		return flowAdd;
	}

	public void sendFlowModInf(IOFSwitch sw, Match match, List<OFAction> actions, OFBufferId buffer_id) {
		OFFactory myFactory = sw.getOFFactory();
		OFFlowAdd flowAdd = myFactory.buildFlowAdd()
				.setBufferId(buffer_id)
				.setPriority(32768)
				.setMatch(match)
				.setActions(actions)
				.build();
		sw.write(flowAdd);
	}

	public Integer bestIds() {
		if(idss.get(1) <= idss.get(2)) {
			return 1;
		}
		else {
			return 2;
		}
	}

	public DatapathId bestSwitch() {
		Random ran = new Random();
		Integer best = Integer.MAX_VALUE;
		ArrayList<DatapathId> flows = new ArrayList<>();
		for(DatapathId dp: activeFlows.keySet()) {
			if(activeFlows.get(dp) < best) {
				flows = new ArrayList<>();
				best = activeFlows.get(dp);
				flows.add(dp);
			}
			else if(activeFlows.get(dp) == best) {
				flows.add(dp);
			}
		}
		
		return flows.get(ran.nextInt(flows.size()));
	}

	protected class SnortSocket extends Thread {
		private Integer port;
		private final String[] protocols = new String[] {"TLSv1.3"};

		public SnortSocket(Integer port) {
			this.port = port;
		}

		public SSLServerSocket createSocket() throws IOException {
			SSLServerSocket socket = (SSLServerSocket) SSLServerSocketFactory.getDefault()
					.createServerSocket(this.port);
			socket.setEnabledProtocols(protocols);

			return socket;
		}

		@Override
		public void run() {
			List<OFAction> drop = new ArrayList<>();
			Map<DatapathId, IOFSwitch> switches;
			String[] parts;
			char ksPass[] = "floodlight".toCharArray();
			Integer port = 51234;

			try {
				String[] protocols = new String[] {"TLSv1.3"};
				KeyStore ks = KeyStore.getInstance("JKS");
	            ks.load(new FileInputStream("floodlight.jks"), ksPass);
	            KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
	            kmf.init(ks, ksPass);
	            SSLContext sc = SSLContext.getInstance("TLS");
	            sc.init(kmf.getKeyManagers(), null, null);
	            SSLServerSocketFactory ssf = sc.getServerSocketFactory();
	            SSLServerSocket s = (SSLServerSocket) ssf.createServerSocket(port);
	            s.setEnabledProtocols(protocols);
	            SSLSocket c = (SSLSocket) s.accept();
		    	System.out.println("New client connection");
		    	InputStream input = c.getInputStream();
		    	BufferedReader reader = new BufferedReader(new InputStreamReader(input));
		    	switches = switchService.getAllSwitchMap();
		    	while(true) {
			    	String alert = reader.readLine();
			    	System.out.println(alert);
			    	parts = alert.split("[|]");
			    	for(OFGroup number: groups.keySet()) {
						   if(groups.get(number).equals(IPv4Address.of(parts[1]), IPv4Address.of(parts[2]), IpProtocol.of(Short.parseShort(parts[3])))) {
							   if(benignGroups.containsKey(number) == true) {
								   benignGroups.remove(number);
							   }
							   groups.get(number).addRedFlag();
							   groups.get(number).addMsg(parts[0]);
							   groups.get(number).setGen(Integer.parseInt(parts[4]));
							   groups.get(number).setId(Integer.parseInt(parts[5]));
							   groups.get(number).setRev(Integer.parseInt(parts[6]));
							   groups.get(number).addTime(parts[8]);
							   if(Integer.parseInt(parts[7]) > groups.get(number).getPriority()) {
								   groups.get(number).setPriority(Integer.parseInt(parts[7]));
							   }
							   if(groups.get(number).getPriority() == 4) {
								   List<OFAction> actions2 = new ArrayList<>();
								   OFFactory myFactory = groups.get(number).getSwitch().getOFFactory();
								   actions2.add(myFactory.actions().group(number));
								   Match match = myFactory.buildMatch()
										   .setExact(MatchField.ETH_TYPE, EthType.IPv4)
										   .setExact(MatchField.IP_PROTO, groups.get(number).getProtoType())
										   .setExact(MatchField.IPV4_SRC, groups.get(number).getSrcIp())
										   .setExact(MatchField.IPV4_DST, groups.get(number).getDstIp())
										   .build();
								   OFFlowAdd flowAdd = myFactory.buildFlowAdd()
											.setBufferId(OFBufferId.NO_BUFFER)
											.setPriority(32768)
											.setMatch(match)
											.setActions(actions2)
											.setHardTimeout(DEFAULT)
											.build();
								   OFFlowDelete flowDelete = FlowModUtils.toFlowDelete(flowAdd);
								   groups.get(number).getSwitch().write(flowDelete);
								   sendFlowMod(groups.get(number).getSwitch(), match, actions2, OFBufferId.NO_BUFFER, PRIORITY_1);
							   }
							   else if(groups.get(number).getPriority() == 3) {
								   List<OFAction> actions2 = new ArrayList<>();
								   OFFactory myFactory = groups.get(number).getSwitch().getOFFactory();
								   actions2.add(myFactory.actions().group(number));
								   Match match = myFactory.buildMatch()
										   .setExact(MatchField.ETH_TYPE, EthType.IPv4)
										   .setExact(MatchField.IP_PROTO, groups.get(number).getProtoType())
										   .setExact(MatchField.IPV4_SRC, groups.get(number).getSrcIp())
										   .setExact(MatchField.IPV4_DST, groups.get(number).getDstIp())
										   .build();
								   OFFlowAdd flowAdd = myFactory.buildFlowAdd()
											.setBufferId(OFBufferId.NO_BUFFER)
											.setPriority(32768)
											.setMatch(match)
											.setActions(actions2)
											.setHardTimeout(DEFAULT)
											.build();
								   OFFlowDelete flowDelete = FlowModUtils.toFlowDelete(flowAdd);
								   groups.get(number).getSwitch().write(flowDelete);
								   sendFlowMod(groups.get(number).getSwitch(), match, actions2, OFBufferId.NO_BUFFER, PRIORITY_2);
							   }
							   else if(groups.get(number).getPriority() == 2) {
								   List<OFAction> actions2 = new ArrayList<>();
								   OFFactory myFactory = groups.get(number).getSwitch().getOFFactory();
								   actions2.add(myFactory.actions().group(number));
								   Match match = myFactory.buildMatch()
										   .setExact(MatchField.ETH_TYPE, EthType.IPv4)
										   .setExact(MatchField.IP_PROTO, groups.get(number).getProtoType())
										   .setExact(MatchField.IPV4_SRC, groups.get(number).getSrcIp())
										   .setExact(MatchField.IPV4_DST, groups.get(number).getDstIp())
										   .build();
								   OFFlowAdd flowAdd = myFactory.buildFlowAdd()
											.setBufferId(OFBufferId.NO_BUFFER)
											.setPriority(32768)
											.setMatch(match)
											.setActions(actions2)
											.setHardTimeout(DEFAULT)
											.build();
								   OFFlowDelete flowDelete = FlowModUtils.toFlowDelete(flowAdd);
								   groups.get(number).getSwitch().write(flowDelete);
								   sendGroupMod(groups.get(number).getSwitch(), "IDS", number, groups.get(number).getOFPort() , groups.get(number).getMac());
								   sendFlowMod(groups.get(number).getSwitch(), match, actions2, OFBufferId.NO_BUFFER, PRIORITY_2);
							   }
							   else if(groups.get(number).getPriority() == 1) {
								   List<OFAction> actions2 = new ArrayList<>();
								   OFFactory myFactory = groups.get(number).getSwitch().getOFFactory();
								   actions2.add(myFactory.actions().group(number));
								   Match match = myFactory.buildMatch()
										   .setExact(MatchField.ETH_TYPE, EthType.IPv4)
										   .setExact(MatchField.IP_PROTO, groups.get(number).getProtoType())
										   .setExact(MatchField.IPV4_SRC, groups.get(number).getSrcIp())
										   .setExact(MatchField.IPV4_DST, groups.get(number).getDstIp())
										   .build();
								   OFFlowAdd flowAdd = myFactory.buildFlowAdd()
											.setBufferId(OFBufferId.NO_BUFFER)
											.setPriority(32768)
											.setMatch(match)
											.setActions(actions2)
											.setHardTimeout(DEFAULT)
											.build();
								   OFFlowDelete flowDelete = FlowModUtils.toFlowDelete(flowAdd);
								   groups.get(number).getSwitch().write(flowDelete);
								   sendGroupMod(groups.get(number).getSwitch(), "stop", number, null, null);
								   for(IOFSwitch sw: switches.values()) {
									   sendFlowModInf(sw, match, drop, OFBufferId.NO_BUFFER);
								   }
							   }
							   break;
						   }
					   }

			    }
			} catch (Exception ex) {
			    	System.out.println("Server exception: " + ex.getMessage());
			    	ex.printStackTrace();
			    }
		}
	}


	protected class flowStats extends Thread{
		private String n_switch;

		public flowStats(String n_switch) {
			this.n_switch = n_switch;
		}

		public void run() {
			DatapathId dpid = null;
			ProcessBuilder pb = new ProcessBuilder("/bin/bash", "-c", "echo $Passe$ | sudo -S ovs-ofctl dump-flows -O OpenFlow13 " + this.n_switch);
			while(true)	{
				String line;
				String[] parts;
				String[] group;
				Integer n = 0;
				HashMap<OFGroup, String> tabela = new HashMap<>();
				try {
						Process process = pb.start();
			            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			            while ((line = reader.readLine()) != null) {
			            	n++;
			            	parts = line.split(" ");
			            	if(!parts[0].equals("OFPST_FLOW")) {
			            		if(parts.length >= 9) {
			            			group = parts[8].split(":");
			            			if(group[0].equals("actions=group")) {
			            				tabela.put(OFGroup.of(Integer.parseInt(group[1])), n_switch);
			            			}
			            		}
			            	}
			            }
			            n--;
			            for(DatapathId dp: macs.keySet()) {
			            	if(macs.get(dp).getSSwitch() == n_switch) {
			            		dpid = dp;
			            	}
			            }
			            activeFlows.put(dpid, n);

			        } catch (IOException e) {
			            e.printStackTrace();
			        }
				for(OFGroup g: groups.keySet()) {
					if(groups.get(g).getS_Switch().equals(n_switch)) {
						if(!tabela.containsKey(g) && groups.get(g).getIsActive() == true) {
							groups.get(g).setIsActive(false);
							idss.put(groups.get(g).getIds(), idss.get(groups.get(g).getIds()) - 1);
							if(groups.get(g).getPriority() == 0) {
								Group aux = groups.get(g);
								benignGroups.put(g, new BenignGroup(aux.getSrcIp(), aux.getDstIp(), aux.getProtoType(), aux.getSwitch()));
							}
						}
					}
				}
				try {
					Thread.sleep(5000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}
		
	protected class roundRobin extends Thread {
			
		public void run() {
			DatapathId bestSwitch;
			Integer bestIds;
			MacAddress mac = null;
			IOFSwitch ms;
			OFPort op;
			while(true) {
				if(benignGroups.isEmpty() == true) {
					try {
						Thread.sleep(30000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				else {
					while(true) {
						for(OFGroup number: benignGroups.keySet()) {
							List<OFAction> actions = new ArrayList<>();
							OFFactory myFactory = benignGroups.get(number).getSwitch().getOFFactory();
							bestIds = bestIds();
							bestSwitch = bestSwitch();
							if(bestIds == 1) {
								mac = macs.get(bestSwitch).getMac1();
							}
							else if(bestIds == 2) {
								mac = macs.get(bestSwitch).getMac2();
							}
							
							ms = switchService.getActiveSwitch(bestSwitch);
							op = learningSwitch.getFromPortMap(ms, mac, null);
							
							Match match = myFactory.buildMatch()
									.setExact(MatchField.ETH_TYPE, EthType.IPv4)
									.setExact(MatchField.IP_PROTO, groups.get(number).getProtoType())
									.setExact(MatchField.IPV4_SRC, groups.get(number).getSrcIp())
									.setExact(MatchField.IPV4_DST, groups.get(number).getDstIp())
									.build();
							actions.add(myFactory.actions().group(number));
							
							if(ms.equals(groups.get(number).getSwitch()) && bestIds == groups.get(number).getIds()) {
								sendFlowMod(ms, match, actions, OFBufferId.NO_BUFFER, DEFAULT);
								idss.put(bestIds, idss.get(groups.get(number).getIds()) + 1);
								activeFlows.put(bestSwitch, activeFlows.get(bestSwitch) + 1);
							}
							else {
							
								sendGroupMod(groups.get(number).getSwitch(), "delete", number, null, null);
								sendGroupMod(ms, "add", number, op, mac);
								groups.get(number).setSwitch(ms);
								groups.get(number).setMac(mac);
								benignGroups.get(number).setSwitch(ms);
								
								
								for(DatapathId d: macs.keySet()) {
									   if(ms.getId().equals(d)) {
										   groups.get(number).setS_Switch(macs.get(d).getSSwitch());
									   }
								}
								
								sendFlowMod(ms, match, actions, OFBufferId.NO_BUFFER, DEFAULT);
								groups.get(number).setIsActive(true);
								groups.get(number).setIds(bestIds);
								groups.get(number).setMac(mac);
								groups.get(number).setOFPort(op);
								groups.get(number).setSwitch(ms);
								idss.put(bestIds, idss.get(groups.get(number).getIds()) + 1);
								activeFlows.put(bestSwitch, activeFlows.get(bestSwitch) + 1);
							}
							try {
								Thread.sleep(10 * DEFAULT + 10000);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
						}
					}
				}
			}
		}
	}
}












