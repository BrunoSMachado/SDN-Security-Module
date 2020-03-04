package net.floodlightcontroller.mactracker;

import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.IpProtocol;
import org.projectfloodlight.openflow.types.MacAddress;
import org.projectfloodlight.openflow.types.OFPort;

import net.floodlightcontroller.core.IOFSwitch;

import java.util.ArrayList;
public class Group {
	private IPv4Address srcIp;
	private IPv4Address dstIp;
	private IpProtocol protoType;
	private Integer redFlag;
	private ArrayList<String> msgs;
	private boolean isActive;
	private String s_switch;
	private IOFSwitch ofSwitch;
	private Integer sigGenerator;
	private Integer sigId;
	private Integer sigRev;
	private ArrayList<String> times;
	private Integer priority;
	private MacAddress mac;
	private OFPort ofp;
	private Integer ids;
	private MacAddress srcMac;
	private MacAddress dstMac;
	
	public Group(IPv4Address srcIp, IPv4Address dstIp, IpProtocol protoType, String s_switch, IOFSwitch ofSwitch, MacAddress mac,
			OFPort ofp, Integer ids, MacAddress srcMac, MacAddress dstMac) {
		this.srcIp = srcIp;
		this.dstIp = dstIp;
		this.protoType = protoType;
		this.srcMac = srcMac;
		this.dstMac = dstMac;
		this.redFlag = 0;
		this.msgs = new ArrayList<String>();
		this.isActive = true;
		this.s_switch = s_switch; 
		this.ofSwitch = ofSwitch;
		this.sigGenerator = 0;
		this.sigId = 0;
		this.sigRev = 0;
		this.times = new ArrayList<String>();
		this.priority = 0;
		this.mac = mac;
		this.ofp = ofp;
		this.ids = ids;
	}

	public void setDstIp(IPv4Address dstIp) {
		this.dstIp = dstIp;
	}

	public void setSrcIp(IPv4Address srcIp) {
		this.srcIp = srcIp;
	}

	public IPv4Address getSrcIp() {
		return this.srcIp;
	}

	public IPv4Address getDstIp() {
		return this.dstIp;
	}

	public IpProtocol getProtoType() {
		return this.protoType;
	}
	
	public String getS_Switch() {
		return this.s_switch;
	}
	
	public void setS_Switch(String s_switch) {
		this.s_switch = s_switch;
	}

	public void setProtoType(IpProtocol protoType) {
		this.protoType = protoType;
	}
	
	public Integer getPriority() {
		return this.priority;
	}
	
	public void setPriority(Integer priority) {
		this.priority = priority;
	}

	public Integer getRedFlags() {
		return this.redFlag;
	}

	public ArrayList<String> getMsgs(){
		return this.msgs;
	}
	
	public void setIsActive(boolean bool) {
		this.isActive = bool;
	}
	
	public boolean getIsActive() {
		return this.isActive;
	}

	public void addRedFlag() {
		this.redFlag++;
	}

	public IOFSwitch getSwitch() {
		return this.ofSwitch;
	}
	
	public void setSwitch(IOFSwitch ofSwitch) {
		this.ofSwitch = ofSwitch;
	}
	
	public Integer getGen() {
		return this.sigGenerator;
	}
	
	public void setGen(Integer gen) {
		this.sigGenerator = gen;
	}
	
	public Integer getId() {
		return this.sigId;
	}
	
	public void setId(Integer id) {
		this.sigId = id;
	}
	
	public Integer getRev() {
		return this.sigRev;
	}
	
	public void setRev(Integer rev) {
		this.sigRev = rev;
	}
	
	public MacAddress getSrcMac() {
		return this.srcMac;
	}
	
	public void setSrcMac(MacAddress srcMac) {
		this.srcMac = srcMac;
	}
	
	public MacAddress getDstMac() {
		return this.dstMac;
	}
	
	public void setDstMac(MacAddress dstMac) {
		this.srcMac = dstMac;
	}
	
	public ArrayList<String> getTimes(){
		return this.times;
	}
	
	public MacAddress getMac() {
		return this.mac;
	}
	
	public void setMac(MacAddress mac) {
		this.mac = mac;
	}
	
	public OFPort getOFPort() {
		return this.ofp;
	}
	
	public void setOFPort(OFPort ofp) {
		this.ofp = ofp;
	}
	
	public Integer getIds() {
		return this.ids;
	}
	
	public void setIds(Integer ids) {
		this.ids = ids;
	}
	
	public void addMsg(String msg) {
		this.msgs.add(msg);
	}
	
	public void addTime(String time) {
		this.times.add(time);
	}
	
	public Boolean equals(IPv4Address srcIp, IPv4Address dstIp, IpProtocol protoType) {
		if(this.srcIp.equals(srcIp) && this.dstIp.equals(dstIp) && this.protoType.equals(protoType)) {
			return true;
		}
		else {
			return false;
		}
	}

	public String toString() {
		return this.srcIp.toString() + " " + this.dstIp.toString()  + " " + this.protoType.toString() + " "
				+ this.isActive + " " + this.getS_Switch() + " " + this.getIds();
	}
}