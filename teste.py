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
        client = self.addHost('client')
        ids = self.addHost('ids')

        # Adding switches
        s1 = self.addSwitch('s1', protocols='OpenFlow13')


        self.addLink(s1, client)
        self.addLink(s1, ids)

def run():
    c = RemoteController('c', '127.0.0.1', 6653)
    net = Mininet(topo=Topologia(), controller=None, switch=OVSSwitch)
    net.addController(c)
    client = net.get('client')
    ids = net.get('ids')
    client.cmd('ifconfig client-eth0 10.0.0.1 netmask 255.0.0.0 hw ether 00:00:00:00:00:01')
    ids.cmd('ifconfig ids-eth0 10.0.0.2 netmask 255.0.0.0 hw ether 00:00:00:00:00:02')
    ids.cmd('ifconfig ids-eth0 mtu 20000')
    client.cmd('ifconfig client-eth0 mtu 20000')

    net.start()
    CLI(net)
    net.stop()

if __name__ == '__main__':
    setLogLevel('info')
    run()
    os.system("sudo mn -c")
