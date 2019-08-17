package net.floodlightcontroller.mactracker;

import org.projectfloodlight.openflow.types.MacAddress;
import org.projectfloodlight.openflow.types.OFPort;
import net.floodlightcontroller.core.IOFSwitch;

public class BandwidthInfo {
	private Long bandwidth;
	private MacAddress mac;
	private OFPort ofp;
	private IOFSwitch iof;
	private String sSwitch;
	
	public BandwidthInfo(Long bandwidth, MacAddress mac, OFPort ofp, IOFSwitch iof, String sSwitch) {
		this.bandwidth = bandwidth;
		this.mac = mac;
		this.ofp = ofp;
		this.iof = iof;
		this.sSwitch = sSwitch;
	}
	
	public BandwidthInfo() {
		this.bandwidth = null;
		this.mac = null;
		this.ofp = null;
		this.iof = null;
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
	
	public IOFSwitch getSwitch() {
		return this.iof;
	}
	
	public void setSwitch(IOFSwitch ofSwitch) {
		this.iof = ofSwitch;
	}
	
	public Long getBandwidth() {
		return this.bandwidth;
	}
	
	public void setBandwidth(Long bd) {
		this.bandwidth = bd;
	}
	
	public String getSSwitch() {
		return this.sSwitch;
	}
	
	public void setSSwitch(String bd) {
		this.sSwitch = bd;
	}
}
