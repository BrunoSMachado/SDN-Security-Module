package net.floodlightcontroller.mactracker;

import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.IpProtocol;
import net.floodlightcontroller.core.IOFSwitch;

public class BenignGroup {
	private IPv4Address srcIp;
	private IPv4Address dstIp;
	private IpProtocol protoType;
	private IOFSwitch ofSwitch;
	
	public BenignGroup(IPv4Address srcIp, IPv4Address dstIp, IpProtocol protoType, IOFSwitch ofSwitch) {
		this.srcIp = srcIp;
		this.dstIp = dstIp;
		this.protoType = protoType;
		this.ofSwitch = ofSwitch;
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
	
	public void setProtoType(IpProtocol ipProto) {
		this.protoType = ipProto;
	}
	
	public IOFSwitch getSwitch() {
		return this.ofSwitch;
	}
	
	public void setSwitch(IOFSwitch ofSwitch) {
		this.ofSwitch = ofSwitch;
	}
}