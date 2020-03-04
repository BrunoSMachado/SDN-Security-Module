# Security SDN Module

## Installation

### Snort

> The file Snort/spo_alert_unixsock.c should replace the one with the same at the Snort installation folder in the following path:
> Snort_Instalation_Folder/src/output-plugins/

### Compile:
```bash
make

sudo make install
```


### UnixSocket

> The UnixSocket.py requires a certificate that can be generated with the following command:
```bash
openssl req -newkey rsa:2048 -new -nodes -x509 -days 3650 -keyout key.pem -out cert.pem

cat key.pem >> cert.pem
```
### Floodlight

> On line 764 of floodlight/src/main/java/net/floodlightcontroller/mactracker/MACTracker.java substitute \$Password\$ with the user sudo password
> Note: This process can be optimized in security and efficiency. Floodlight has already a flow stats service that can be used wiht some work of implementation.

## Run

### Floodlight

> Run the floodlight project from the Eclipse IDE

### UnixSocket
```bash
python UnixSocket.py
```
### Mininet Scripts

```bash
sudo python2 topology1.py
```
### Snort

> Run a snort instance on a mininet host

```bash
sudo snort -i {network_interface} -A unsock -c /etc/snort/snort.conf
```

### Tcpreplay

```bash
tcpreplay --intf1={network_interface} {pcap file}
```
