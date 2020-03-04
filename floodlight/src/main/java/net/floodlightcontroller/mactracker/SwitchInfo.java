package net.floodlightcontroller.mactracker;
import org.projectfloodlight.openflow.types.MacAddress;

public class SwitchInfo {
	private String sSwitch;
	private MacAddress mac1;
	private MacAddress mac2;
	
	
	
	public SwitchInfo(String sSwitch, MacAddress mac1, MacAddress mac2) {
		this.sSwitch = sSwitch;
		this.mac1 = mac1;
		this.mac2 = mac2;
	}
	
	public SwitchInfo() {
		this.sSwitch = "";
		this.mac1 = null;
		this.mac2 = null;
	}
	
	public MacAddress getMac1() {
		return this.mac1;
	}
	
	public void setMac1(MacAddress mac) {
		this.mac1 = mac;
	}
	
	public String getSSwitch() {
		return this.sSwitch;
	}
	
	public void setSSwitch(String sSwitch) {
		this.sSwitch = sSwitch;
	}
	
	public MacAddress getMac2() {
		return this.mac2;
	}
	
	public void setMac2(MacAddress mac) {
		this.mac2 = mac;
	}
}
