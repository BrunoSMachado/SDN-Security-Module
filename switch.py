from mininet.topo import Topo
from mininet.net import Mininet
from mininet.log import setLogLevel, info, lg
from mininet.node import RemoteController, OVSSwitch, Node
from mininet.cli import CLI
from mininet.link import Link, TCLink, Intf
from mininet.topolib import TreeNet
from mininet.nodelib import NAT
import os

class Topologia(Topo):

    def __init__(self, **opts):
        super(Topologia, self).__init__(**opts)

        # Add hosts
        cliente1 = self.addHost('cliente1')
        cliente2 = self.addHost('cliente2')
        cliente3 = self.addHost('cliente3')
        servidor1 = self.addHost('servidor1')
        servidor2 = self.addHost('servidor2')
        servidor3 = self.addHost('servidor3')
        ids1 = self.addHost('ids1')
        ids2 = self.addHost('ids2')

        # Adding switches
        s1 = self.addSwitch('s1', protocols='OpenFlow13')
        s2 = self.addSwitch('s2', protocols='OpenFlow13')
        s3 = self.addSwitch('s3', protocols='OpenFlow13')

        self.addLink(s1, s2)
        self.addLink(s2, s3)
        # triangulo
        #self.addLink(s3, s1)
        # links hosts
        self.addLink(s1, cliente1)
        self.addLink(s1, cliente2)
        self.addLink(s1, cliente3)
        self.addLink(s3, servidor1)
        self.addLink(s3, servidor2)
        self.addLink(s3, servidor3)
        self.addLink(s1, ids1)
        self.addLink(s2, ids1)
        self.addLink(s3, ids1)
        self.addLink(s1, ids2)
        self.addLink(s2, ids2)
        self.addLink(s3, ids2)

def run():
    c = RemoteController('c', '127.0.0.1', 6653)
    net = Mininet(topo=Topologia(), controller=None, switch=OVSSwitch)
    net.addController(c)

    cliente1 = net.get('cliente1')
    cliente2 = net.get('cliente2')
    cliente3 = net.get('cliente3')
    servidor1 = net.get('servidor1')
    servidor2 = net.get('servidor2')
    servidor3 = net.get('servidor3')
    ids1 = net.get('ids1')
    ids2 = net.get('ids2')
    ids1.cmd('ifconfig ids1-eth0 10.0.0.7 netmask 255.0.0.0 hw ether 00:00:00:00:00:10')
    ids1.cmd('ifconfig ids1-eth1 10.0.0.8 netmask 255.0.0.0 hw ether 00:00:00:00:00:11')
    ids1.cmd('ifconfig ids1-eth2 10.0.0.9 netmask 255.0.0.0 hw ether 00:00:00:00:00:12')
    ids2.cmd('ifconfig ids2-eth0 10.0.0.10 netmask 255.0.0.0 hw ether 00:00:00:00:00:13')
    ids2.cmd('ifconfig ids2-eth1 10.0.0.11 netmask 255.0.0.0 hw ether 00:00:00:00:00:14')
    ids2.cmd('ifconfig ids2-eth2 10.0.0.12 netmask 255.0.0.0 hw ether 00:00:00:00:00:15')
    #Intf( 'wlp4s0' , node=s1) deita a net a baixo e nao funciona
    cliente1.cmd('ifconfig cliente1-eth0 10.0.0.1 netmask 255.0.0.0 hw ether 00:00:00:00:00:01')
    cliente2.cmd('ifconfig cliente2-eth0 10.0.0.2 netmask 255.0.0.0 hw ether 00:00:00:00:00:02')
    cliente3.cmd('ifconfig cliente3-eth0 10.0.0.3 netmask 255.0.0.0 hw ether 00:00:00:00:00:03')
    servidor1.cmd('ifconfig servidor1-eth0 10.0.0.4 netmask 255.0.0.0 hw ether 00:00:00:00:00:04')
    servidor2.cmd('ifconfig servidor2-eth0 10.0.0.5 netmask 255.0.0.0 hw ether 00:00:00:00:00:05')
    servidor3.cmd('ifconfig servidor3-eth0 10.0.0.6 netmask 255.0.0.0 hw ether 00:00:00:00:00:06')

    """
    ids1.cmd('snort -D -i ids1-eth0 -c /etc/snort/snort.conf -A unsock -l /tmp')
    ids1.cmd('snort -D -i ids1-eth1 -c /etc/snort/snort.conf -A unsock -l /tmp')
    ids1.cmd('snort -D -i ids1-eth2 -c /etc/snort/snort.conf -A unsock -l /tmp')
    ids2.cmd('snort -D -i ids2-eth0 -c /etc/snort/snort.conf -A unsock -l /tmp')
    ids2.cmd('snort -D -i ids2-eth1 -c /etc/snort/snort.conf -A unsock -l /tmp')
    ids2.cmd('snort -D -i ids2-eth2 -c /etc/snort/snort.conf -A unsock -l /tmp')
    """

    net.start()
    CLI(net)
    net.stop()

if __name__ == '__main__':
    setLogLevel('info')
    run()
    os.system("sudo mn -c")
